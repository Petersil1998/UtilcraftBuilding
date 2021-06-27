package net.petersil98.utilcraft_building.container.architect_table;

import com.google.common.collect.Sets;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.AirItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.network.SyncArchitectTableDataPoint;
import net.petersil98.utilcraft_building.network.SyncBlueprintItemCapability;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

//TODO: Fix changing blueprints in table
public class ArchitectTableContainer extends Container {

    private final IWorldPosCallable worldPosCallable;
    private final BlueprintInventory currentInventory;
    private final Inventory blueprintInventory = new Inventory(1);
    private final PlayerEntity playerEntity;
    private int currentLayer;
    private int maxLayer;
    public static final int SIZE = 10;
    public static final int MAX_LAYERS = 50;

    private int dragEvent;
    private int dragMode = -1;
    private final Set<Slot> dragSlots = Sets.newHashSet();

    private final Method onSwapCraft = ObfuscationReflectionHelper.findMethod(Slot.class, "func_190900_b", int.class);

    public ArchitectTableContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, IWorldPosCallable.DUMMY);
    }

    public ArchitectTableContainer(int id, @Nonnull PlayerInventory playerInventory, IWorldPosCallable worldPosCallable) {
        super(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER, id);
        this.worldPosCallable = worldPosCallable;
        this.currentLayer = 0;
        this.maxLayer = 0;
        this.currentInventory = new BlueprintInventory(SIZE * SIZE);
        this.playerEntity = playerInventory.player;
        this.addSlots(playerInventory);
    }

    protected void addSlots(PlayerInventory playerInventory) {
        for(int i = 0; i < SIZE; ++i) {
            for(int j = 0; j < SIZE; ++j) {
                this.addSlot(new Slot(this.currentInventory, j + i * SIZE, 26 + j * 18, i * 18 - 48) {
                    @Override
                    public boolean isItemValid(@Nonnull ItemStack stack) {
                        return stack.getItem() instanceof BlockItem && !getBlueprint().equals(ItemStack.EMPTY);
                    }
                });
            }
        }
        this.addSlot(new Slot(this.blueprintInventory, 0, 188, 207) {
            @Override
            public boolean isItemValid(@Nonnull ItemStack stack) {
                return stack.getItem() instanceof Blueprint;
            }

            @Nonnull
            @Override
            public ItemStack onTake(@Nonnull PlayerEntity player, @Nonnull ItemStack stack) {
                if(!player.world.isRemote) {
                    ItemStack result = updateBlueprintFromInventory(stack);
                    currentLayer = 0;
                    maxLayer = 0;
                    currentInventory.clear();
                    detectAndSendChanges();
                    syncData();
                    syncCapabilities();
                    return result;
                }
                return super.onTake(player, stack);
            }

            @Override
            public void onSlotChanged() {
                if(!playerEntity.world.isRemote) {
                    if (this.getStack().getItem() instanceof Blueprint) {
                        currentLayer = 0;
                        maxLayer = 0;
                        currentInventory.clear();
                        updateInventoryFromBlueprint(this.getStack());
                        detectAndSendChanges();
                        syncData();
                    }
                }
                super.onSlotChanged();
            }
        });
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 26 + j * 18, 149 + i * 18));
            }
        }

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 26 + i * 18, 207));
        }
    }

    @Nonnull
    @Override
    public ItemStack slotClick(int slotId, int dragType, @Nonnull ClickType clickType, @Nonnull PlayerEntity player) {
        try {
            return this.handleClick(slotId, dragType, clickType, player);
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.makeCrashReport(exception, "Container click");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Click info");
            crashreportcategory.addDetail("Menu Type", () -> Registry.MENU.getKey(this.getType()).toString());
            crashreportcategory.addDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashreportcategory.addDetail("Slot Count", this.inventorySlots.size());
            crashreportcategory.addDetail("Slot", slotId);
            crashreportcategory.addDetail("Button", dragType);
            crashreportcategory.addDetail("Type", clickType);
            throw new ReportedException(crashreport);
        }
    }

    private ItemStack handleClick(int slotId, int dragType, ClickType clickType, @Nonnull PlayerEntity player) {
        //Drag Event: 0 : start drag, 1 : add slot, 2 : end drag
        //Drag Mode: 0 : evenly split, 1 : one item by slot, 2 : not used
        //Drag Type: Button ID
        ItemStack returnStack = ItemStack.EMPTY;
        PlayerInventory playerinventory = player.inventory;
        if (clickType == ClickType.QUICK_CRAFT) {
            int dragEvent = this.dragEvent;
            this.dragEvent = getDragEvent(dragType);
            if ((dragEvent != 1 || this.dragEvent != 2) && dragEvent != this.dragEvent) {
                this.resetDrag();
            } else if (playerinventory.getItemStack().isEmpty()) {
                this.resetDrag();
            } else if (this.dragEvent == 0) {
                // Start Drag
                this.dragMode = extractDragMode(dragType);
                if (isValidDragMode(this.dragMode, player)) {
                    this.dragEvent = 1;
                    this.dragSlots.clear();
                } else {
                    this.resetDrag();
                }
            } else if (this.dragEvent == 1) {
                // Add Slot
                Slot slot = this.inventorySlots.get(slotId);
                ItemStack clickedStack = playerinventory.getItemStack();
                if (slot != null && canAddItemToSlot(slot, clickedStack, true) && slot.isItemValid(clickedStack) && (dragMode == 2 || clickedStack.getCount() > this.dragSlots.size()) && this.canDragIntoSlot(slot)) {
                    this.dragSlots.add(slot);
                }
            } else if (this.dragEvent == 2) {
                //End Drag
                if (!this.dragSlots.isEmpty()) {
                    ItemStack stack = playerinventory.getItemStack().copy();
                    int count = playerinventory.getItemStack().getCount();
                    for(Slot dragSlot : this.dragSlots) {
                        ItemStack clickedStack = playerinventory.getItemStack();
                        if (dragSlot != null && canAddItemToSlot(dragSlot, clickedStack, true) && dragSlot.isItemValid(clickedStack) && (this.dragMode == 2 || clickedStack.getCount() >= this.dragSlots.size()) && this.canDragIntoSlot(dragSlot)) {
                            ItemStack clickedStackCopyCopy = stack.copy();
                            if(!(dragSlot.inventory instanceof BlueprintInventory)) {
                                int toTransfer = dragSlot.getHasStack() ? dragSlot.getStack().getCount() : 0;
                                computeStackSize(this.dragSlots, this.dragMode, clickedStackCopyCopy, toTransfer);
                                int k3 = Math.min(clickedStackCopyCopy.getMaxStackSize(), dragSlot.getItemStackLimit(clickedStackCopyCopy));
                                if (clickedStackCopyCopy.getCount() > k3) {
                                    clickedStackCopyCopy.setCount(k3);
                                }
                                count -= clickedStackCopyCopy.getCount() - toTransfer;
                            } else {
                                clickedStackCopyCopy.setCount(1);
                            }
                            dragSlot.putStack(clickedStackCopyCopy);
                        }
                    }
                    stack.setCount(count);
                    playerinventory.setItemStack(stack);
                }
                this.resetDrag();
            } else {
                this.resetDrag();
            }
        } else if (this.dragEvent != 0) {
            this.resetDrag();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!playerinventory.getItemStack().isEmpty()) {
                    if (dragType == 0) {
                        player.dropItem(playerinventory.getItemStack(), true);
                        playerinventory.setItemStack(ItemStack.EMPTY);
                    }

                    if (dragType == 1) {
                        player.dropItem(playerinventory.getItemStack().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot = this.inventorySlots.get(slotId);
                if (slot == null || !slot.canTakeStack(player)) {
                    return ItemStack.EMPTY;
                }

                for(ItemStack transferredStack = this.transferStackInSlot(player, slotId); !transferredStack.isEmpty() && ItemStack.areItemsEqual(slot.getStack(), transferredStack); transferredStack = this.transferStackInSlot(player, slotId)) {
                    returnStack = transferredStack.copy();
                }
            } else {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot = this.inventorySlots.get(slotId);
                if (slot != null) {
                    ItemStack stackInSlot = slot.getStack();
                    ItemStack clickedStack = playerinventory.getItemStack();
                    if (!stackInSlot.isEmpty()) {
                        returnStack = stackInSlot.copy();
                    }

                    if (stackInSlot.isEmpty()) {
                        if (!clickedStack.isEmpty() && slot.isItemValid(clickedStack)) {
                            if(!(slot.inventory instanceof BlueprintInventory)) {
                                int amountToTransfer = dragType == 0 ? clickedStack.getCount() : 1;
                                if (amountToTransfer > slot.getItemStackLimit(clickedStack)) {
                                    amountToTransfer = slot.getItemStackLimit(clickedStack);
                                }
                                slot.putStack(clickedStack.split(amountToTransfer));
                            } else {
                                ItemStack newStack = clickedStack.copy();
                                newStack.setCount(1);
                                slot.putStack(newStack);
                            }
                        }
                    } else if (slot.canTakeStack(player)) {
                        if (clickedStack.isEmpty()) {
                            if (stackInSlot.isEmpty()) {
                                slot.putStack(ItemStack.EMPTY);
                                playerinventory.setItemStack(ItemStack.EMPTY);
                            } else {
                                int amountToReduce = dragType == 0 ? stackInSlot.getCount() : (stackInSlot.getCount() + 1) / 2;
                                ItemStack stack = slot.decrStackSize(amountToReduce);
                                if(!(slot.inventory instanceof BlueprintInventory)) {
                                    playerinventory.setItemStack(stack);
                                } else {
                                    returnStack = ItemStack.EMPTY;
                                }
                                if (stackInSlot.isEmpty()) {
                                    slot.putStack(ItemStack.EMPTY);
                                }

                                slot.onTake(player, playerinventory.getItemStack());
                            }
                        } else if (slot.isItemValid(clickedStack)) {
                            if (areItemsAndTagsEqual(stackInSlot, clickedStack)) {
                                int amountThatCanFit = dragType == 0 ? clickedStack.getCount() : 1;
                                if (amountThatCanFit > slot.getItemStackLimit(clickedStack) - stackInSlot.getCount()) {
                                    amountThatCanFit = slot.getItemStackLimit(clickedStack) - stackInSlot.getCount();
                                }

                                if (amountThatCanFit > clickedStack.getMaxStackSize() - stackInSlot.getCount()) {
                                    amountThatCanFit = clickedStack.getMaxStackSize() - stackInSlot.getCount();
                                }

                                clickedStack.shrink(amountThatCanFit);
                                stackInSlot.grow(amountThatCanFit);
                            } else if (clickedStack.getCount() <= slot.getItemStackLimit(clickedStack)) {
                                slot.putStack(clickedStack);
                                playerinventory.setItemStack(stackInSlot);
                            }
                        } else if (clickedStack.getMaxStackSize() > 1 && areItemsAndTagsEqual(stackInSlot, clickedStack) && !stackInSlot.isEmpty()) {
                            int i3 = stackInSlot.getCount();
                            if (i3 + clickedStack.getCount() <= clickedStack.getMaxStackSize()) {
                                clickedStack.grow(i3);
                                stackInSlot = slot.decrStackSize(i3);
                                if (stackInSlot.isEmpty()) {
                                    slot.putStack(ItemStack.EMPTY);
                                }

                                slot.onTake(player, playerinventory.getItemStack());
                            }
                        }
                    }

                    slot.onSlotChanged();
                }
            }
        } else if (clickType == ClickType.SWAP) {
            Slot slot = this.inventorySlots.get(slotId);
            ItemStack selectedItem = playerinventory.getStackInSlot(dragType);
            ItemStack stackInSlot = slot.getStack();
            if (!selectedItem.isEmpty() || !stackInSlot.isEmpty()) {
                if (selectedItem.isEmpty()) {
                    if (slot.canTakeStack(player)) {
                        playerinventory.setInventorySlotContents(dragType, stackInSlot);
                        try {
                            onSwapCraft.invoke(slot, stackInSlot.getCount());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        slot.putStack(ItemStack.EMPTY);
                        slot.onTake(player, stackInSlot);
                    }
                } else if (stackInSlot.isEmpty()) {
                    if (slot.isItemValid(selectedItem)) {
                        int limit = slot.getItemStackLimit(selectedItem);
                        if (selectedItem.getCount() > limit) {
                            slot.putStack(selectedItem.split(limit));
                        } else {
                            slot.putStack(selectedItem);
                            playerinventory.setInventorySlotContents(dragType, ItemStack.EMPTY);
                        }
                    }
                } else if (slot.canTakeStack(player) && slot.isItemValid(selectedItem)) {
                    int limit = slot.getItemStackLimit(selectedItem);
                    if (selectedItem.getCount() > limit) {
                        slot.putStack(selectedItem.split(limit));
                        slot.onTake(player, stackInSlot);
                        if (!playerinventory.addItemStackToInventory(stackInSlot)) {
                            player.dropItem(stackInSlot, true);
                        }
                    } else {
                        slot.putStack(selectedItem);
                        playerinventory.setInventorySlotContents(dragType, stackInSlot);
                        slot.onTake(player, stackInSlot);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.abilities.isCreativeMode && playerinventory.getItemStack().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.inventorySlots.get(slotId);
            if (inventorySlot != null && inventorySlot.getHasStack()) {
                ItemStack slotStackCopy = inventorySlot.getStack().copy();
                slotStackCopy.setCount(slotStackCopy.getMaxStackSize());
                playerinventory.setItemStack(slotStackCopy);
            }
        } else if (clickType == ClickType.THROW && playerinventory.getItemStack().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.inventorySlots.get(slotId);
            if (inventorySlot != null && inventorySlot.getHasStack() && inventorySlot.canTakeStack(player)) {
                ItemStack decreasedStack = inventorySlot.decrStackSize(dragType == 0 ? 1 : inventorySlot.getStack().getCount());
                inventorySlot.onTake(player, decreasedStack);
                player.dropItem(decreasedStack, true);
            }
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot inventorySlot = this.inventorySlots.get(slotId);
            ItemStack clickedStack = playerinventory.getItemStack();
            if (!clickedStack.isEmpty() && (inventorySlot == null || !inventorySlot.getHasStack() || !inventorySlot.canTakeStack(player))) {
                int j1 = dragType == 0 ? 0 : this.inventorySlots.size() - 1;
                int sign = dragType == 0 ? 1 : -1;

                for(int j = 0; j < 2; ++j) {
                    for(int k = j1; k >= 0 && k < this.inventorySlots.size() && clickedStack.getCount() < clickedStack.getMaxStackSize(); k += sign) {
                        Slot currentSlot = this.inventorySlots.get(k);
                        if (currentSlot.getHasStack() && canAddItemToSlot(currentSlot, clickedStack, true) && currentSlot.canTakeStack(player) && this.canMergeSlot(clickedStack, currentSlot)) {
                            ItemStack currentStack = currentSlot.getStack();
                            if (j != 0 || currentStack.getCount() != currentStack.getMaxStackSize()) {
                                int min = Math.min(clickedStack.getMaxStackSize() - clickedStack.getCount(), currentStack.getCount());
                                ItemStack decreasedStack = currentSlot.decrStackSize(min);
                                clickedStack.grow(min);
                                if (decreasedStack.isEmpty()) {
                                    currentSlot.putStack(ItemStack.EMPTY);
                                }

                                currentSlot.onTake(player, decreasedStack);
                            }
                        }
                    }
                }
            }
            this.detectAndSendChanges();
        }
        return returnStack;
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return isWithinUsableDistance(this.worldPosCallable, player, UtilcraftBuildingBlocks.ARCHITECT_TABLE);
    }

    public void nextLayer() {
        if (this.hasNextLayer() && !playerEntity.world.isRemote) {
            ItemStack stack = this.getBlueprint();
            stack = this.updateBlueprintFromInventory(stack);
            this.currentLayer++;
            this.updateInventoryFromBlueprint(stack);
            this.blueprintInventory.setInventorySlotContents(0, stack);
            this.detectAndSendChanges();
            this.syncData();
        }
    }

    public void previousLayer() {
        if(this.hasPreviousLayer() && !playerEntity.world.isRemote) {
            ItemStack stack = this.getBlueprint();
            stack = this.updateBlueprintFromInventory(stack);
            if(hasPreviousLayer()) {
                this.currentLayer--;
            }
            this.updateInventoryFromBlueprint(stack);
            this.blueprintInventory.setInventorySlotContents(0, stack);
            this.detectAndSendChanges();
            this.syncData();
        }
    }

    public boolean hasNextLayer() {
        return this.currentLayer < this.maxLayer;
    }

    public boolean hasPreviousLayer() {
        return this.currentLayer > 0;
    }

    public int getCurrentLayer() {
        return this.currentLayer;
    }

    public int getCurrentMaxLayers() {
        return this.maxLayer+1;
    }

    public void addLayer() {
        if(!playerEntity.world.isRemote) {
            this.maxLayer++;
            this.getBlueprint().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
                List<List<BlockState>> emptyList = new ArrayList<>();
                for (int i = 0; i < SIZE; i++) {
                    List<BlockState> blockStates = new ArrayList<>();
                    for (int j = 0; j < SIZE; j++) {
                        blockStates.add(Blocks.AIR.getDefaultState());
                    }
                    emptyList.add(blockStates);
                }
                iBluePrint.getPattern().add(emptyList);
            });
            this.detectAndSendChanges();
            this.syncData();
        }
    }

    public void removeCurrentLayer() {
        if(!playerEntity.world.isRemote) {
            AtomicReference<List<List<List<BlockState>>>> patternReference = new AtomicReference<>();
            this.getBlueprint().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> patternReference.set(iBluePrint.getPattern()));
            List<List<List<BlockState>>> pattern = patternReference.get();
            pattern.remove(this.currentLayer);
            this.maxLayer--;
            this.currentLayer--;
            this.updateInventoryFromBlueprint(this.getBlueprint());
            this.detectAndSendChanges();
            this.syncData();
        }
    }

    @Nonnull
    private ItemStack getBlueprint() {
        return this.blueprintInventory.getStackInSlot(0);
    }

    public boolean containsBlueprint() {
        return !this.getBlueprint().equals(ItemStack.EMPTY);
    }

    private void updateInventoryFromBlueprint(@Nonnull ItemStack stack) {
        AtomicReference<List<List<List<BlockState>>>> patternReference = new AtomicReference<>();
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> patternReference.set(iBluePrint.getPattern()));
        List<List<List<BlockState>>> pattern = patternReference.get();
        if(pattern != null && pattern.size() > 0) {
            this.maxLayer = pattern.size() - 1;
            List<List<BlockState>> lists = pattern.get(currentLayer);
            for (int j = 0; j < lists.size(); j++) {
                for (int k = 0; k < lists.get(j).size(); k++) {
                    BlockState state = lists.get(j).get(k);
                    if (state.getBlock() instanceof AirBlock) {
                        currentInventory.setInventorySlotContents(j * SIZE + k, ItemStack.EMPTY);
                    } else {
                        ItemStack itemStack = state.getPickBlock(new BlockRayTraceResult(Vector3d.ZERO, Direction.DOWN, BlockPos.ZERO, false),
                                worldPosCallable.apply((world, blockPos) -> world).orElse(null),
                                worldPosCallable.apply((world, blockPos) -> blockPos).orElse(null),
                                playerEntity);
                        currentInventory.setInventorySlotContents(j * SIZE + k, itemStack);
                    }
                }
            }
        }
    }

    @Nonnull
    private ItemStack updateBlueprintFromInventory(@Nonnull ItemStack stack) {
        AtomicReference<List<List<List<BlockState>>>> patternReference = new AtomicReference<>();
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> patternReference.set(iBluePrint.getPattern()));
        List<List<List<BlockState>>> pattern = patternReference.get();
        List<List<BlockState>> list;
        if(pattern.size() > currentLayer) {
            list = pattern.get(currentLayer);
            list.clear();
        } else {
            list = new ArrayList<>();
            pattern.add(list);
        }
        for (int j = 0; j < SIZE; j++) {
            List<BlockState> blockStates = new ArrayList<>();
            for (int k = 0; k < SIZE; k++) {
                Item item = currentInventory.getStackInSlot(j * SIZE + k).getItem();
                if(item instanceof AirItem) {
                    blockStates.add(Blocks.AIR.getDefaultState());
                } else if(item instanceof BlockItem) {
                    blockStates.add(((BlockItem)item).getBlock().getDefaultState());
                }
            }
            list.add(blockStates);
        }
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> iBluePrint.setPattern(pattern));
        return stack;
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    @Nonnull
    public ItemStack transferStackInSlot(@Nonnull PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(@Nonnull PlayerEntity player) {
        super.onContainerClosed(player);
        this.worldPosCallable.consume((p_217068_2_, p_217068_3_) -> this.clearContainer(player, p_217068_2_, this.blueprintInventory));
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
    }

    public void setMaxLayer(int maxLayer) {
        this.maxLayer = maxLayer;
    }

    private void syncData() {
        PacketHandler.sendToClient(new SyncArchitectTableDataPoint(this.currentLayer, this.maxLayer), (ServerPlayerEntity) this.playerEntity);
    }

    private void syncCapabilities() {
        PacketHandler.sendToClient(new SyncBlueprintItemCapability(this.getBlueprint()), (ServerPlayerEntity) this.playerEntity);
    }

    protected void resetDrag() {
        this.dragEvent = 0;
        this.dragSlots.clear();
    }
}
