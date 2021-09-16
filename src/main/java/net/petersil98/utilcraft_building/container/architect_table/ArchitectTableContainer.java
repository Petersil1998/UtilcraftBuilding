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
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.network.SyncArchitectTableDataPoint;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
        this(id, playerInventory, IWorldPosCallable.NULL);
    }

    public ArchitectTableContainer(int id, @Nonnull PlayerInventory playerInventory, IWorldPosCallable worldPosCallable) {
        super(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER.get(), id);
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
                    public boolean mayPlace(@Nonnull ItemStack stack) {
                        return stack.getItem() instanceof BlockItem && !getBlueprint().equals(ItemStack.EMPTY);
                    }
                });
            }
        }
        this.addSlot(new Slot(this.blueprintInventory, 0, 188, 207) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack stack) {
                return stack.getItem() instanceof Blueprint;
            }

            @Nonnull
            @Override
            public ItemStack onTake(@Nonnull PlayerEntity player, @Nonnull ItemStack stack) {
                if(!player.level.isClientSide) {
                    updateBlueprintFromInventory(stack);
                    currentLayer = 0;
                    maxLayer = 0;
                    currentInventory.clearContent();
                    broadcastChanges();
                    syncData();
                }
                return super.onTake(player, stack);
            }

            @Override
            public void setChanged() {
                if(!playerEntity.level.isClientSide) {
                    if (this.getItem().getItem() instanceof Blueprint) {
                        currentLayer = 0;
                        maxLayer = 0;
                        currentInventory.clearContent();
                        updateInventoryFromBlueprint(this.getItem());
                        broadcastChanges();
                        syncData();
                    }
                }
                super.setChanged();
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
    public ItemStack clicked(int slotId, int dragType, @Nonnull ClickType clickType, @Nonnull PlayerEntity player) {
        try {
            return this.handleClick(slotId, dragType, clickType, player);
        } catch (Exception exception) {
            CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
            crashreportcategory.setDetail("Menu Type", () -> Registry.MENU.getKey(this.getType()).toString());
            crashreportcategory.setDetail("Menu Class", () -> this.getClass().getCanonicalName());
            crashreportcategory.setDetail("Slot Count", this.slots.size());
            crashreportcategory.setDetail("Slot", slotId);
            crashreportcategory.setDetail("Button", dragType);
            crashreportcategory.setDetail("Type", clickType);
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
            this.dragEvent = getQuickcraftHeader(dragType);
            if ((dragEvent != 1 || this.dragEvent != 2) && dragEvent != this.dragEvent) {
                this.resetQuickCraft();
            } else if (playerinventory.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.dragEvent == 0) {
                // Start Drag
                this.dragMode = getQuickcraftType(dragType);
                if (isValidQuickcraftType(this.dragMode, player)) {
                    this.dragEvent = 1;
                    this.dragSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.dragEvent == 1) {
                // Add Slot
                Slot slot = this.slots.get(slotId);
                ItemStack clickedStack = playerinventory.getCarried();
                if (slot != null && canItemQuickReplace(slot, clickedStack, true) && slot.mayPlace(clickedStack) && (dragMode == 2 || clickedStack.getCount() > this.dragSlots.size()) && this.canDragTo(slot)) {
                    this.dragSlots.add(slot);
                }
            } else if (this.dragEvent == 2) {
                //End Drag
                if (!this.dragSlots.isEmpty()) {
                    ItemStack stack = playerinventory.getCarried().copy();
                    int count = playerinventory.getCarried().getCount();
                    for(Slot dragSlot : this.dragSlots) {
                        ItemStack clickedStack = playerinventory.getCarried();
                        if (dragSlot != null && canItemQuickReplace(dragSlot, clickedStack, true) && dragSlot.mayPlace(clickedStack) && (this.dragMode == 2 || clickedStack.getCount() >= this.dragSlots.size()) && this.canDragTo(dragSlot)) {
                            ItemStack clickedStackCopyCopy = stack.copy();
                            if(!(dragSlot.container instanceof BlueprintInventory)) {
                                int toTransfer = dragSlot.hasItem() ? dragSlot.getItem().getCount() : 0;
                                getQuickCraftSlotCount(this.dragSlots, this.dragMode, clickedStackCopyCopy, toTransfer);
                                int k3 = Math.min(clickedStackCopyCopy.getMaxStackSize(), dragSlot.getMaxStackSize(clickedStackCopyCopy));
                                if (clickedStackCopyCopy.getCount() > k3) {
                                    clickedStackCopyCopy.setCount(k3);
                                }
                                count -= clickedStackCopyCopy.getCount() - toTransfer;
                            } else {
                                clickedStackCopyCopy.setCount(1);
                            }
                            dragSlot.set(clickedStackCopyCopy);
                        }
                    }
                    stack.setCount(count);
                    playerinventory.setCarried(stack);
                }
                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.dragEvent != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!playerinventory.getCarried().isEmpty()) {
                    if (dragType == 0) {
                        player.drop(playerinventory.getCarried(), true);
                        playerinventory.setCarried(ItemStack.EMPTY);
                    }

                    if (dragType == 1) {
                        player.drop(playerinventory.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot = this.slots.get(slotId);
                if (slot == null || !slot.mayPickup(player)) {
                    return ItemStack.EMPTY;
                }

                for(ItemStack transferredStack = this.quickMoveStack(player, slotId); !transferredStack.isEmpty() && ItemStack.isSame(slot.getItem(), transferredStack); transferredStack = this.quickMoveStack(player, slotId)) {
                    returnStack = transferredStack.copy();
                }
            } else {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot = this.slots.get(slotId);
                if (slot != null) {
                    ItemStack stackInSlot = slot.getItem();
                    ItemStack clickedStack = playerinventory.getCarried();
                    if (!stackInSlot.isEmpty()) {
                        returnStack = stackInSlot.copy();
                    }

                    if (stackInSlot.isEmpty()) {
                        if (!clickedStack.isEmpty() && slot.mayPlace(clickedStack)) {
                            if(!(slot.container instanceof BlueprintInventory)) {
                                int amountToTransfer = dragType == 0 ? clickedStack.getCount() : 1;
                                if (amountToTransfer > slot.getMaxStackSize(clickedStack)) {
                                    amountToTransfer = slot.getMaxStackSize(clickedStack);
                                }
                                slot.set(clickedStack.split(amountToTransfer));
                            } else {
                                ItemStack newStack = clickedStack.copy();
                                newStack.setCount(1);
                                slot.set(newStack);
                            }
                        }
                    } else if (slot.mayPickup(player)) {
                        if (clickedStack.isEmpty()) {
                            if (stackInSlot.isEmpty()) {
                                slot.set(ItemStack.EMPTY);
                                playerinventory.setCarried(ItemStack.EMPTY);
                            } else {
                                int amountToReduce = dragType == 0 ? stackInSlot.getCount() : (stackInSlot.getCount() + 1) / 2;
                                ItemStack stack = slot.remove(amountToReduce);
                                if(!(slot.container instanceof BlueprintInventory)) {
                                    playerinventory.setCarried(stack);
                                } else {
                                    returnStack = ItemStack.EMPTY;
                                }
                                if (stackInSlot.isEmpty()) {
                                    slot.set(ItemStack.EMPTY);
                                }

                                slot.onTake(player, playerinventory.getCarried());
                            }
                        } else if (slot.mayPlace(clickedStack)) {
                            if (consideredTheSameItem(stackInSlot, clickedStack)) {
                                int amountThatCanFit = dragType == 0 ? clickedStack.getCount() : 1;
                                if (amountThatCanFit > slot.getMaxStackSize(clickedStack) - stackInSlot.getCount()) {
                                    amountThatCanFit = slot.getMaxStackSize(clickedStack) - stackInSlot.getCount();
                                }

                                if (amountThatCanFit > clickedStack.getMaxStackSize() - stackInSlot.getCount()) {
                                    amountThatCanFit = clickedStack.getMaxStackSize() - stackInSlot.getCount();
                                }

                                clickedStack.shrink(amountThatCanFit);
                                stackInSlot.grow(amountThatCanFit);
                            } else if (clickedStack.getCount() <= slot.getMaxStackSize(clickedStack)) {
                                slot.set(clickedStack);
                                playerinventory.setCarried(stackInSlot);
                            }
                        } else if (clickedStack.getMaxStackSize() > 1 && consideredTheSameItem(stackInSlot, clickedStack) && !stackInSlot.isEmpty()) {
                            int i3 = stackInSlot.getCount();
                            if (i3 + clickedStack.getCount() <= clickedStack.getMaxStackSize()) {
                                clickedStack.grow(i3);
                                stackInSlot = slot.remove(i3);
                                if (stackInSlot.isEmpty()) {
                                    slot.set(ItemStack.EMPTY);
                                }

                                slot.onTake(player, playerinventory.getCarried());
                            }
                        }
                    }

                    slot.setChanged();
                }
            }
        } else if (clickType == ClickType.SWAP) {
            Slot slot = this.slots.get(slotId);
            ItemStack selectedItem = playerinventory.getItem(dragType);
            ItemStack stackInSlot = slot.getItem();
            if (!selectedItem.isEmpty() || !stackInSlot.isEmpty()) {
                if (selectedItem.isEmpty()) {
                    if (slot.mayPickup(player)) {
                        playerinventory.setItem(dragType, stackInSlot);
                        try {
                            onSwapCraft.invoke(slot, stackInSlot.getCount());
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            UtilcraftBuilding.LOGGER.error("Failed to invoke Method onSwapCraft", e);
                        }
                        slot.set(ItemStack.EMPTY);
                        slot.onTake(player, stackInSlot);
                    }
                } else if (stackInSlot.isEmpty()) {
                    if (slot.mayPlace(selectedItem)) {
                        int limit = slot.getMaxStackSize(selectedItem);
                        if (selectedItem.getCount() > limit) {
                            slot.set(selectedItem.split(limit));
                        } else {
                            slot.set(selectedItem);
                            playerinventory.setItem(dragType, ItemStack.EMPTY);
                        }
                    }
                } else if (slot.mayPickup(player) && slot.mayPlace(selectedItem)) {
                    int limit = slot.getMaxStackSize(selectedItem);
                    if (selectedItem.getCount() > limit) {
                        slot.set(selectedItem.split(limit));
                        slot.onTake(player, stackInSlot);
                        if (!playerinventory.add(stackInSlot)) {
                            player.drop(stackInSlot, true);
                        }
                    } else {
                        slot.set(selectedItem);
                        playerinventory.setItem(dragType, stackInSlot);
                        slot.onTake(player, stackInSlot);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.abilities.instabuild && playerinventory.getCarried().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            if (inventorySlot != null && inventorySlot.hasItem()) {
                ItemStack slotStackCopy = inventorySlot.getItem().copy();
                slotStackCopy.setCount(slotStackCopy.getMaxStackSize());
                playerinventory.setCarried(slotStackCopy);
            }
        } else if (clickType == ClickType.THROW && playerinventory.getCarried().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            if (inventorySlot != null && inventorySlot.hasItem() && inventorySlot.mayPickup(player)) {
                ItemStack decreasedStack = inventorySlot.remove(dragType == 0 ? 1 : inventorySlot.getItem().getCount());
                inventorySlot.onTake(player, decreasedStack);
                player.drop(decreasedStack, true);
            }
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            ItemStack clickedStack = playerinventory.getCarried();
            if (!clickedStack.isEmpty() && (inventorySlot == null || !inventorySlot.hasItem() || !inventorySlot.mayPickup(player))) {
                int j1 = dragType == 0 ? 0 : this.slots.size() - 1;
                int sign = dragType == 0 ? 1 : -1;

                for(int j = 0; j < 2; ++j) {
                    for(int k = j1; k >= 0 && k < this.slots.size() && clickedStack.getCount() < clickedStack.getMaxStackSize(); k += sign) {
                        Slot currentSlot = this.slots.get(k);
                        if (currentSlot.hasItem() && canItemQuickReplace(currentSlot, clickedStack, true) && currentSlot.mayPickup(player) && this.canTakeItemForPickAll(clickedStack, currentSlot)) {
                            ItemStack currentStack = currentSlot.getItem();
                            if (j != 0 || currentStack.getCount() != currentStack.getMaxStackSize()) {
                                int min = Math.min(clickedStack.getMaxStackSize() - clickedStack.getCount(), currentStack.getCount());
                                ItemStack decreasedStack = currentSlot.remove(min);
                                clickedStack.grow(min);
                                if (decreasedStack.isEmpty()) {
                                    currentSlot.set(ItemStack.EMPTY);
                                }

                                currentSlot.onTake(player, decreasedStack);
                            }
                        }
                    }
                }
            }
            this.broadcastChanges();
        }
        return returnStack;
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return stillValid(this.worldPosCallable, player, UtilcraftBuildingBlocks.ARCHITECT_TABLE.get());
    }

    public void nextLayer() {
        if (this.hasNextLayer() && !playerEntity.level.isClientSide) {
            ItemStack stack = this.getBlueprint();
            this.updateBlueprintFromInventory(stack);
            this.currentLayer++;
            this.updateInventoryFromBlueprint(stack);
            this.blueprintInventory.setItem(0, stack);
            this.broadcastChanges();
            this.syncData();
        }
    }

    public void previousLayer() {
        if(this.hasPreviousLayer() && !playerEntity.level.isClientSide) {
            ItemStack stack = this.getBlueprint();
            this.updateBlueprintFromInventory(stack);
            if(hasPreviousLayer()) {
                this.currentLayer--;
            }
            this.updateInventoryFromBlueprint(stack);
            this.blueprintInventory.setItem(0, stack);
            this.broadcastChanges();
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
        if(!playerEntity.level.isClientSide) {
            this.maxLayer++;
            this.getBlueprint().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
                List<List<BlockState>> emptyList = new ArrayList<>();
                for (int i = 0; i < SIZE; i++) {
                    List<BlockState> blockStates = new ArrayList<>();
                    for (int j = 0; j < SIZE; j++) {
                        blockStates.add(Blocks.AIR.defaultBlockState());
                    }
                    emptyList.add(blockStates);
                }
                iBluePrint.getPattern().add(emptyList);
            });
            this.broadcastChanges();
            this.syncData();
        }
    }

    public void removeCurrentLayer() {
        if(!playerEntity.level.isClientSide) {
            AtomicReference<List<List<List<BlockState>>>> patternReference = new AtomicReference<>();
            this.getBlueprint().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> patternReference.set(iBluePrint.getPattern()));
            List<List<List<BlockState>>> pattern = patternReference.get();
            pattern.remove(this.currentLayer);
            this.maxLayer--;
            this.currentLayer--;
            this.updateInventoryFromBlueprint(this.getBlueprint());
            this.broadcastChanges();
            this.syncData();
        }
    }

    @Nonnull
    private ItemStack getBlueprint() {
        return this.blueprintInventory.getItem(0);
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
                        currentInventory.setItem(j * SIZE + k, ItemStack.EMPTY);
                    } else {
                        ItemStack itemStack = state.getPickBlock(new BlockRayTraceResult(Vector3d.ZERO, Direction.DOWN, BlockPos.ZERO, false),
                                worldPosCallable.evaluate((world, blockPos) -> world).orElse(null),
                                worldPosCallable.evaluate((world, blockPos) -> blockPos).orElse(null),
                                playerEntity);
                        currentInventory.setItem(j * SIZE + k, itemStack);
                    }
                }
            }
        }
    }

    private void updateBlueprintFromInventory(@Nonnull ItemStack stack) {
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
                Item item = currentInventory.getItem(j * SIZE + k).getItem();
                if(item instanceof AirItem) {
                    blockStates.add(Blocks.AIR.defaultBlockState());
                } else if(item instanceof BlockItem) {
                    blockStates.add(((BlockItem)item).getBlock().defaultBlockState());
                }
            }
            list.add(blockStates);
        }
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> iBluePrint.setPattern(pattern));
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Called when the container is closed.
     */
    public void removed(@Nonnull PlayerEntity player) {
        super.removed(player);
        this.worldPosCallable.execute((p_217068_2_, p_217068_3_) -> this.clearContainer(player, p_217068_2_, this.blueprintInventory));
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

    protected void resetQuickCraft() {
        this.dragEvent = 0;
        this.dragSlots.clear();
    }
}
