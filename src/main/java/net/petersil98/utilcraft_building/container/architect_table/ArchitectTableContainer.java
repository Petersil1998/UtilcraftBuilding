package net.petersil98.utilcraft_building.container.architect_table;

import com.google.common.collect.Sets;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Registry;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
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

public class ArchitectTableContainer extends AbstractContainerMenu {

    private final ContainerLevelAccess worldPosCallable;
    private final BlueprintInventory currentInventory;
    private final SimpleContainer blueprintInventory = new SimpleContainer(1);
    private final Player playerEntity;
    private int currentLayer;
    private int maxLayer;
    public static final int SIZE = 10;
    public static final int MAX_LAYERS = 50;

    private int dragEvent;
    private int dragMode = -1;
    private final Set<Slot> dragSlots = Sets.newHashSet();

    private final Method onSwapCraft = ObfuscationReflectionHelper.findMethod(Slot.class, "m_6405_", int.class);

    public ArchitectTableContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, ContainerLevelAccess.NULL);
    }

    public ArchitectTableContainer(int id, @Nonnull Inventory playerInventory, ContainerLevelAccess worldPosCallable) {
        super(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER, id);
        this.worldPosCallable = worldPosCallable;
        this.currentLayer = 0;
        this.maxLayer = 0;
        this.currentInventory = new BlueprintInventory(SIZE * SIZE);
        this.playerEntity = playerInventory.player;
        this.addSlots(playerInventory);
    }

    protected void addSlots(Inventory playerInventory) {
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

            @Override
            public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {
                if(!player.level.isClientSide) {
                    updateBlueprintFromInventory(stack);
                    currentLayer = 0;
                    maxLayer = 0;
                    currentInventory.clearContent();
                    broadcastChanges();
                    syncData();
                }
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

    @Override
    public void clicked(int slotId, int dragType, @Nonnull ClickType clickType, @Nonnull Player player) {
        try {
            this.handleClick(slotId, dragType, clickType, player);
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

    private void handleClick(int slotId, int dragType, ClickType clickType, @Nonnull Player player) {
        //Drag Event: 0 : start drag, 1 : add slot, 2 : end drag
        //Drag Mode: 0 : evenly split, 1 : one item by slot, 2 : not used
        //Drag Type: Button ID
        Inventory playerinventory = player.getInventory();
        if (clickType == ClickType.QUICK_CRAFT) {
            int dragEvent = this.dragEvent;
            this.dragEvent = getQuickcraftHeader(dragType);
            if ((dragEvent != 1 || this.dragEvent != 2) && dragEvent != this.dragEvent) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
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
                ItemStack clickedStack = this.getCarried();
                if (canItemQuickReplace(slot, clickedStack, true) && slot.mayPlace(clickedStack) && (dragMode == 2 || clickedStack.getCount() > this.dragSlots.size()) && this.canDragTo(slot)) {
                    this.dragSlots.add(slot);
                }
            } else if (this.dragEvent == 2) {
                //End Drag
                if (!this.dragSlots.isEmpty()) {
                    ItemStack stack = this.getCarried().copy();
                    int count = this.getCarried().getCount();
                    for(Slot dragSlot : this.dragSlots) {
                        ItemStack clickedStack = this.getCarried();
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
                    this.setCarried(stack);
                }
                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.dragEvent != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (dragType == 0) {
                        player.drop(this.getCarried(), true);
                        this.setCarried(ItemStack.EMPTY);
                    }

                    if (dragType == 1) {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return;
                }

                Slot slot = this.slots.get(slotId);
                if (!slot.mayPickup(player)) {
                    return;
                }

                for(ItemStack transferredStack = this.quickMoveStack(player, slotId); !transferredStack.isEmpty() && ItemStack.isSame(slot.getItem(), transferredStack); transferredStack = this.quickMoveStack(player, slotId)) {
                }
            } else {
                if (slotId < 0) {
                    return;
                }

                Slot slot = this.slots.get(slotId);
                ItemStack stackInSlot = slot.getItem();
                ItemStack clickedStack = this.getCarried();

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
                            this.setCarried(ItemStack.EMPTY);
                        } else {
                            int amountToReduce = dragType == 0 ? stackInSlot.getCount() : (stackInSlot.getCount() + 1) / 2;
                            ItemStack stack = slot.remove(amountToReduce);
                            if(!(slot.container instanceof BlueprintInventory)) {
                                this.setCarried(stack);
                            }
                            if (stackInSlot.isEmpty()) {
                                slot.set(ItemStack.EMPTY);
                            }

                            slot.onTake(player, this.getCarried());
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
                            this.setCarried(stackInSlot);
                        }
                    } else if (clickedStack.getMaxStackSize() > 1 && consideredTheSameItem(stackInSlot, clickedStack) && !stackInSlot.isEmpty()) {
                        int i3 = stackInSlot.getCount();
                        if (i3 + clickedStack.getCount() <= clickedStack.getMaxStackSize()) {
                            clickedStack.grow(i3);
                            stackInSlot = slot.remove(i3);
                            if (stackInSlot.isEmpty()) {
                                slot.set(ItemStack.EMPTY);
                            }

                            slot.onTake(player, this.getCarried());
                        }
                    }
                }

                slot.setChanged();
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
                            e.printStackTrace();
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
        } else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            if (inventorySlot.hasItem()) {
                ItemStack slotStackCopy = inventorySlot.getItem().copy();
                slotStackCopy.setCount(slotStackCopy.getMaxStackSize());
                this.setCarried(slotStackCopy);
            }
        } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            if (inventorySlot.hasItem() && inventorySlot.mayPickup(player)) {
                ItemStack decreasedStack = inventorySlot.remove(dragType == 0 ? 1 : inventorySlot.getItem().getCount());
                inventorySlot.onTake(player, decreasedStack);
                player.drop(decreasedStack, true);
            }
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot inventorySlot = this.slots.get(slotId);
            ItemStack clickedStack = this.getCarried();
            if (!clickedStack.isEmpty() && (!inventorySlot.hasItem() || !inventorySlot.mayPickup(player))) {
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
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean stillValid(@Nonnull Player player) {
        return stillValid(this.worldPosCallable, player, UtilcraftBuildingBlocks.ARCHITECT_TABLE);
    }

    public void nextLayer() {
        if (this.hasNextLayer() && !playerEntity.level.isClientSide) {
            ItemStack stack = this.getBlueprint();
            stack = this.updateBlueprintFromInventory(stack);
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
            stack = this.updateBlueprintFromInventory(stack);
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
                        ItemStack itemStack = state.getPickBlock(new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, false),
                                worldPosCallable.evaluate((world, blockPos) -> world).orElse(null),
                                worldPosCallable.evaluate((world, blockPos) -> blockPos).orElse(null),
                                playerEntity);
                        currentInventory.setItem(j * SIZE + k, itemStack);
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
        return stack;
    }

    /**
     * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
     * inventory and the other inventory(s).
     */
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }

    /**
     * Called when the container is closed.
     */
    public void removed(@Nonnull Player player) {
        super.removed(player);
        this.worldPosCallable.execute((p_217068_2_, p_217068_3_) -> this.clearContainer(player, this.blueprintInventory));
    }

    public void setCurrentLayer(int currentLayer) {
        this.currentLayer = currentLayer;
    }

    public void setMaxLayer(int maxLayer) {
        this.maxLayer = maxLayer;
    }

    private void syncData() {
        PacketHandler.sendToClient(new SyncArchitectTableDataPoint(this.currentLayer, this.maxLayer), (ServerPlayer) this.playerEntity);
    }

    protected void resetQuickCraft() {
        this.dragEvent = 0;
        this.dragSlots.clear();
    }

    public static boolean consideredTheSameItem(ItemStack itemStack, ItemStack other) {
        return itemStack.getItem() == other.getItem() && ItemStack.tagMatches(itemStack, other);
    }
}
