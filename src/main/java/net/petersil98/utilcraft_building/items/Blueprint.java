package net.petersil98.utilcraft_building.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.BlueprintProvider;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class Blueprint extends Item {

    public Blueprint() {
        super(new Properties()
                .group(UtilcraftBuilding.ITEM_GROUP)
                .maxStackSize(1)
        );
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new BlueprintProvider();
    }

    @Override
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        Map<BlockState, Integer> blocks = BlueprintUtils.listBlockStatesFromCapability(stack);
        blocks.forEach((blockState, count) -> tooltip.add(new TranslationTextComponent("blueprint.utilcraft_building.tooltip", count, new TranslationTextComponent(blockState.getBlock().getTranslationKey())).setStyle(Style.EMPTY.setColor(Color.fromInt(TextFormatting.BLUE.getColor())))));
    }

    @Nonnull
    @Override
    public ActionResultType onItemUse(@Nonnull ItemUseContext context) {
        return this.tryPlace(new BlockItemUseContext(context));
    }

    private ActionResultType tryPlace(@Nonnull BlockItemUseContext context) {
        if (!context.canPlace()) {
            return ActionResultType.FAIL;
        } else {
            BlockState blockstate = UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.getStateForPlacement(context);
            if (blockstate == null) {
                return ActionResultType.FAIL;
            } else if (!context.getWorld().setBlockState(context.getPos(), blockstate, 11)) {
                return ActionResultType.FAIL;
            } else {
                BlockPos blockpos = context.getPos();
                World world = context.getWorld();
                PlayerEntity playerentity = context.getPlayer();
                ItemStack itemstack = context.getItem();
                BlockState otherState = world.getBlockState(blockpos);
                Block other = otherState.getBlock();
                if (other == blockstate.getBlock()) {
                    otherState = this.getRealBlockState(blockpos, world, itemstack, otherState);
                    BlockItem.setTileEntityNBT(world, playerentity, blockpos, itemstack);
                    other.onBlockPlacedBy(world, blockpos, otherState, playerentity, itemstack);
                    this.setCapability(world, blockpos, context.getItem());
                    if (playerentity instanceof ServerPlayerEntity) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity)playerentity, blockpos, itemstack);
                    }
                }

                SoundType soundtype = otherState.getSoundType(world, blockpos, context.getPlayer());
                world.playSound(playerentity, blockpos, otherState.getSoundType(world, blockpos, context.getPlayer()).getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                return ActionResultType.func_233537_a_(world.isRemote);
            }
        }
    }

    private BlockState getRealBlockState(BlockPos blockPos, World world, @Nonnull ItemStack itemStack, BlockState blockState) {
        BlockState blockstate = blockState;
        CompoundNBT compoundnbt = itemStack.getTag();
        if (compoundnbt != null) {
            CompoundNBT blockStateTag = compoundnbt.getCompound("BlockStateTag");
            StateContainer<Block, BlockState> stateContainer = blockState.getBlock().getStateContainer();

            for(String s : blockStateTag.keySet()) {
                Property<?> property = stateContainer.getProperty(s);
                if (property != null) {
                    String s1 = blockStateTag.get(s).getString();
                    blockstate = applyProperty(blockstate, property, s1);
                }
            }
        }

        if (blockstate != blockState) {
            world.setBlockState(blockPos, blockstate, 2);
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState blockState, @Nonnull Property<T> property, String state) {
        return property.parseValue(state).map((value) -> blockState.with(property, value)).orElse(blockState);
    }

    private void setCapability(@Nonnull World world, BlockPos pos, ItemStack itemStack) {
        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof BlueprintBlockTileEntity) {
            itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                    .ifPresent(iBluePrint -> tileentity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                            .ifPresent(iBluePrint2 -> iBluePrint2.setPattern(iBluePrint.getPattern()))
                    );
        }
    }
}
