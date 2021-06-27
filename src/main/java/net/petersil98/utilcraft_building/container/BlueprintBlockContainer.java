package net.petersil98.utilcraft_building.container;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.petersil98.utilcraft_building.blocks.BlueprintBlock;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.network.SyncBlueprintTECapability;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BlueprintBlockContainer extends Container {

    private final IWorldPosCallable worldPosCallable;
    private final Inventory inventory;
    private final PlayerEntity player;
    private final int rows;

    public BlueprintBlockContainer(int id, PlayerInventory playerInventory, @Nonnull PacketBuffer data) {
        this(id, playerInventory, IWorldPosCallable.of(playerInventory.player.world, data.readBlockPos()));
    }

    public BlueprintBlockContainer(int id, @Nonnull PlayerInventory playerInventory, IWorldPosCallable worldPosCallable) {
        super(UtilcraftBuildingContainer.BLUEPRINT_BLOCK_CONTAINER, id);
        this.worldPosCallable = worldPosCallable;
        this.player = playerInventory.player;
        this.rows = 3;
        this.inventory = new Inventory(this.rows * 9);
        this.addSlots(playerInventory);
        if(!player.world.isRemote) {
            syncCapabilitiesToClient();
        }
    }

    protected void addSlots(PlayerInventory playerInventory) {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(this.inventory, j + i * 9, 8 + j * 18, i * 18 + 18) {
                    @Override
                    public boolean isItemValid(@Nonnull ItemStack stack) {
                        return true;
                    }

                    @Override
                    public void onSlotChanged() {
                        super.onSlotChanged();
                        if(!player.world.isRemote) {
                            syncCapabilitiesToClient();
                        }
                    }
                });
            }
        }

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 86 + i * 18));
            }
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 144));
        }
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return isWithinUsableDistance(this.worldPosCallable, player, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK);
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(@Nonnull PlayerEntity player) {
        super.onContainerClosed(player);
        this.worldPosCallable.consume((world, blockPos) -> this.clearContainer(player, world, this.inventory));
    }

    public TileEntity getTileEntity() {
        return worldPosCallable.apply(World::getTileEntity).orElse(null);
    }

    private void syncCapabilitiesToClient() {
        PacketHandler.sendToClient(new SyncBlueprintTECapability(worldPosCallable.apply((world, blockPos) -> blockPos).orElse(BlockPos.ZERO), player.world), (ServerPlayerEntity) player);
    }

    public Inventory getContainerInventory() {
        return this.inventory;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull PlayerEntity player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemstack = stackInSlot.copy();
            if (index < this.rows * 9) {
                if (!this.mergeItemStack(stackInSlot, this.rows * 9, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(stackInSlot, 0, this.rows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    public void createStructure() {
        BlockState blockState = this.worldPosCallable.apply(World::getBlockState).orElse(null);
        if(blockState != null) {
            AtomicReference<List<List<List<BlockState>>>> pattern = new AtomicReference<>();
            this.getTileEntity().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> pattern.set(iBluePrint.getPattern()));
            Direction direction = blockState.get(BlueprintBlock.FACING);
            Map<Block, Integer> required = BlueprintUtils.fromBlockStateToBlock(BlueprintUtils.listBlockStatesFromCapability(this.getTileEntity()));
            Map<Item, Integer> supplied = BlueprintUtils.listBlockItemsFromInventory(this.getContainerInventory());
            Map<Block, Integer> blocksSupplied = supplied.entrySet().stream().filter(e -> e.getKey() instanceof BlockItem).collect(Collectors.toMap((e -> ((BlockItem) e.getKey()).getBlock()), Map.Entry::getValue));
            for(Map.Entry<Block, Integer> entry: blocksSupplied.entrySet()) {
                if(required.containsKey(entry.getKey())) {
                    this.removeItemFromInventory(entry.getKey().asItem(), required.get(entry.getKey()));
                }
            }
            this.onContainerClosed(this.player);
            this.getTileEntity().remove();
            BlockPos pos = this.worldPosCallable.apply((world, blockPos) -> blockPos).orElse(BlockPos.ZERO);
            blockState.removedByPlayer(this.player.world, pos, this.player, true, this.player.world.getFluidState(pos));

            this.placeStructure(direction, pattern.get(), pos);
        }
    }

    private void removeItemFromInventory(Item item, int amountToRemove) {
        for(int i = 0; i < this.inventory.getSizeInventory() && amountToRemove > 0; i++) {
            ItemStack currentStack = this.inventory.getStackInSlot(i);
            if(currentStack.getItem().equals(item)) {
                if(amountToRemove >= currentStack.getCount()) {
                    amountToRemove -= currentStack.getCount();
                    this.inventory.removeStackFromSlot(i);
                } else {
                    currentStack.setCount(currentStack.getCount() - amountToRemove);
                    amountToRemove = 0;
                }
            }
        }
    }

    private void placeStructure(@Nonnull Direction direction, List<List<List<BlockState>>> pattern, BlockPos pos) {
        switch (direction) {
            case NORTH: {
                placeStructureNorthSouth(true, pattern, pos);
                break;
            }
            case SOUTH: {
                placeStructureNorthSouth(false, pattern, pos);
                break;
            }
            case WEST: {
                placeStructureWestEast(true, pattern, pos);
                break;
            }
            case EAST: {
                placeStructureWestEast(false, pattern, pos);
                break;
            }
        }
    }

    private void placeStructureNorthSouth(boolean isNorth, @Nonnull List<List<List<BlockState>>> pattern, BlockPos pos) {
        int sign = isNorth ? 1 : -1;
        for(int i = 0; i < pattern.size(); i++) {
            BlockPos currentY = pos.up(i);
            for(int j = 0; j < pattern.get(i).size(); j++) {
                int posZ = sign*(pattern.get(i).size()/2-j);
                BlockPos currentZY = currentY.north(posZ);
                for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                    int posX = sign*(pattern.get(i).get(j).size()/2-k);
                    BlockState state = pattern.get(i).get(j).get(k);
                    if(!(state.getBlock() instanceof AirBlock)) {
                        placeBlock(state, currentZY.west(posX));
                    }
                }
            }
        }
    }

    private void placeStructureWestEast(boolean isWest, @Nonnull List<List<List<BlockState>>> pattern, BlockPos pos) {
        int sign = isWest ? 1 : -1;
        for(int i = 0; i < pattern.size(); i++) {
            BlockPos currentY = pos.up(i);
            for(int j = 0; j < pattern.get(i).size(); j++) {
                int posX = sign*(pattern.get(i).size()/2-j);
                BlockPos currentXY = currentY.west(posX);
                for(int k = 0; k < pattern.get(i).get(j).size(); k++) {
                    int posZ = sign*(pattern.get(i).get(j).size()/2-k);
                    BlockState state = pattern.get(i).get(j).get(k);
                    if(!(state.getBlock() instanceof AirBlock)) {
                        placeBlock(state, currentXY.south(posZ));
                    }
                }
            }
        }
    }

    private void placeBlock(BlockState state, BlockPos pos) {
        this.player.world.getBlockState(pos).removedByPlayer(this.player.world, pos, this.player, true, this.player.world.getFluidState(pos));
        this.player.world.setBlockState(pos, state, 11);
        state.getBlock().onBlockPlacedBy(this.player.world, pos, state, this.player, ItemStack.EMPTY);
        SoundType soundtype = state.getSoundType(this.player.world, pos, this.player);
        this.player.world.playSound(this.player, pos, state.getSoundType(this.player.world, pos, this.player).getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
    }
}
