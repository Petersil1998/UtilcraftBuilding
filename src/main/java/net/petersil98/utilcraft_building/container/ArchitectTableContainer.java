package net.petersil98.utilcraft_building.container;

import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
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
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
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
                        return stack.getItem() instanceof BlockItem;
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
                if(player instanceof ServerPlayerEntity) {
                    ItemStack result = updateBlueprintFromInventory(stack);
                    currentInventory.clear();
                    updateBlueprintLayer();
                    return result;
                }
                return super.onTake(player, stack);
            }

            @Override
            public void onSlotChanged() {
                if(!playerEntity.world.isRemote) {
                    if (this.getStack().getItem() instanceof Blueprint && currentInventory.isEmpty()) {
                        currentLayer = 0;
                        updateInventoryFromBlueprint(this.getStack());
                        updateBlueprintLayer();
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

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return isWithinUsableDistance(this.worldPosCallable, player, UtilcraftBuildingBlocks.ARCHITECT_TABLE);
    }

    public void nextLayer() {
        if(this.hasNextLayer()) {
            ItemStack stack = blueprintInventory.getStackInSlot(0);
            stack = this.updateBlueprintFromInventory(stack);
            blueprintInventory.setInventorySlotContents(0, stack);
            this.currentLayer++;
            this.updateBlueprintLayer();
        }
    }

    public void previousLayer() {
        if(this.hasPreviousLayer()) {
            ItemStack stack = blueprintInventory.getStackInSlot(0);
            stack = this.updateBlueprintFromInventory(stack);
            blueprintInventory.setInventorySlotContents(0, stack);
            this.currentLayer--;
            this.updateBlueprintLayer();
        }
    }

    public boolean hasNextLayer() {
        return this.currentLayer < this.maxLayer;
    }

    public boolean hasPreviousLayer() {
        return this.currentLayer > 0;
    }

    public int getLayer() {
        return this.currentLayer;
    }

    public int getMaxLayers() {
        return this.maxLayer+1;
    }

    private void updateBlueprintLayer() {
        for(int i = 0; i < SIZE; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                int slot = j + i * SIZE;
                this.getSlot(slot).putStack(currentInventory.getStackInSlot(slot));
            }
        }
        this.detectAndSendChanges();
    }

    private void updateInventoryFromBlueprint(ItemStack stack) {
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

    private ItemStack updateBlueprintFromInventory(ItemStack stack) {
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
    /*@Nonnull
    public ItemStack transferStackInSlot(@Nonnull PlayerEntity player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < this.numRows * 9) {
                if (!this.mergeItemStack(itemstack1, this.numRows * 9, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, this.numRows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }*/

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(@Nonnull PlayerEntity player) {
        super.onContainerClosed(player);
        this.worldPosCallable.consume((p_217068_2_, p_217068_3_) -> this.clearContainer(player, p_217068_2_, this.blueprintInventory));
    }
}
