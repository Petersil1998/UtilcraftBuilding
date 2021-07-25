package net.petersil98.utilcraft_building.container;

import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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

public class BlueprintBlockContainer extends AbstractContainerMenu {

    private final ContainerLevelAccess worldPosCallable;
    private final SimpleContainer inventory;
    private final Player player;
    private final int rows;

    public BlueprintBlockContainer(int id, Inventory playerInventory, @Nonnull FriendlyByteBuf data) {
        this(id, playerInventory, ContainerLevelAccess.create(playerInventory.player.level, data.readBlockPos()));
    }

    public BlueprintBlockContainer(int id, @Nonnull Inventory playerInventory, ContainerLevelAccess worldPosCallable) {
        super(UtilcraftBuildingContainer.BLUEPRINT_BLOCK_CONTAINER, id);
        this.worldPosCallable = worldPosCallable;
        this.player = playerInventory.player;
        this.rows = 3;
        this.inventory = new SimpleContainer(this.rows * 9);
        this.addSlots(playerInventory);
        if(!player.level.isClientSide) {
            syncCapabilitiesToClient();
        }
    }

    protected void addSlots(Inventory playerInventory) {
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(this.inventory, j + i * 9, 8 + j * 18, i * 18 + 18) {
                    @Override
                    public boolean mayPlace(@Nonnull ItemStack stack) {
                        return true;
                    }

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if(!player.level.isClientSide) {
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
    public boolean stillValid(@Nonnull Player player) {
        return stillValid(this.worldPosCallable, player, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK);
    }

    /**
     * Called when the container is closed.
     */
    public void removed(@Nonnull Player player) {
        super.removed(player);
        this.worldPosCallable.execute((world, blockPos) -> this.clearContainer(player, this.inventory));
    }

    public BlockEntity getTileEntity() {
        return worldPosCallable.evaluate(Level::getBlockEntity).orElse(null);
    }

    private void syncCapabilitiesToClient() {
        PacketHandler.sendToClient(new SyncBlueprintTECapability(worldPosCallable.evaluate((world, blockPos) -> blockPos).orElse(BlockPos.ZERO), player.level), (ServerPlayer) player);
    }

    public SimpleContainer getContainerInventory() {
        return this.inventory;
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();
            if (index < this.rows * 9) {
                if (!this.moveItemStackTo(stackInSlot, this.rows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, this.rows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    public void createStructure() {
        BlockState blockState = this.worldPosCallable.evaluate(Level::getBlockState).orElse(null);
        if(blockState != null) {
            AtomicReference<List<List<List<BlockState>>>> pattern = new AtomicReference<>();
            this.getTileEntity().getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> pattern.set(iBluePrint.getPattern()));
            Direction direction = blockState.getValue(BlueprintBlock.FACING);
            Map<Block, Integer> required = BlueprintUtils.fromBlockStateToBlock(BlueprintUtils.listBlockStatesFromCapability(this.getTileEntity()));
            Map<Item, Integer> supplied = BlueprintUtils.listBlockItemsFromInventory(this.getContainerInventory());
            Map<Block, Integer> blocksSupplied = supplied.entrySet().stream().filter(e -> e.getKey() instanceof BlockItem).collect(Collectors.toMap((e -> ((BlockItem) e.getKey()).getBlock()), Map.Entry::getValue));
            for(Map.Entry<Block, Integer> entry: blocksSupplied.entrySet()) {
                if(required.containsKey(entry.getKey())) {
                    this.removeItemFromInventory(entry.getKey().asItem(), required.get(entry.getKey()));
                }
            }
            this.removed(this.player);
            this.getTileEntity().setRemoved();
            BlockPos pos = this.worldPosCallable.evaluate((world, blockPos) -> blockPos).orElse(BlockPos.ZERO);
            blockState.removedByPlayer(this.player.level, pos, this.player, true, this.player.level.getFluidState(pos));

            this.placeStructure(direction, pattern.get(), pos);
        }
    }

    private void removeItemFromInventory(Item item, int amountToRemove) {
        for(int i = 0; i < this.inventory.getContainerSize() && amountToRemove > 0; i++) {
            ItemStack currentStack = this.inventory.getItem(i);
            if(currentStack.getItem().equals(item)) {
                if(amountToRemove >= currentStack.getCount()) {
                    amountToRemove -= currentStack.getCount();
                    this.inventory.removeItemNoUpdate(i);
                } else {
                    currentStack.setCount(currentStack.getCount() - amountToRemove);
                    amountToRemove = 0;
                }
            }
        }
    }

    private void placeStructure(@Nonnull Direction direction, List<List<List<BlockState>>> pattern, BlockPos pos) {
        switch (direction) {
            case NORTH -> placeStructureNorthSouth(true, pattern, pos);
            case SOUTH -> placeStructureNorthSouth(false, pattern, pos);
            case WEST -> placeStructureWestEast(true, pattern, pos);
            case EAST -> placeStructureWestEast(false, pattern, pos);
        }
    }

    private void placeStructureNorthSouth(boolean isNorth, @Nonnull List<List<List<BlockState>>> pattern, BlockPos pos) {
        int sign = isNorth ? 1 : -1;
        for(int i = 0; i < pattern.size(); i++) {
            BlockPos currentY = pos.above(i);
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
            BlockPos currentY = pos.above(i);
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
        this.player.level.getBlockState(pos).removedByPlayer(this.player.level, pos, this.player, true, this.player.level.getFluidState(pos));
        this.player.level.setBlock(pos, state, 11);
        state.getBlock().setPlacedBy(this.player.level, pos, state, this.player, ItemStack.EMPTY);
        SoundType soundtype = state.getSoundType(this.player.level, pos, this.player);
        this.player.level.playSound(this.player, pos, state.getSoundType(this.player.level, pos, this.player).getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
    }
}
