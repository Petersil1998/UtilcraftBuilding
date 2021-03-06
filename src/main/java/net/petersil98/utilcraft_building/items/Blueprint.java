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
import net.minecraft.nbt.INBT;
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
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.network.SyncBlueprintTECapability;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Blueprint extends Item {

    public Blueprint(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new BlueprintProvider();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        Map<BlockState, Integer> blocks = BlueprintUtils.listBlockStatesFromCapability(stack);
        blocks.forEach((blockState, count) -> tooltip.add(new TranslationTextComponent("blueprint.utilcraft_building.tooltip", count, new TranslationTextComponent(blockState.getBlock().getDescriptionId())).setStyle(Style.EMPTY.withColor(Color.fromRgb(TextFormatting.BLUE.getColor())))));
    }

    @Nonnull
    @Override
    public ActionResultType useOn(@Nonnull ItemUseContext context) {
        return this.tryPlace(new BlockItemUseContext(context));
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundNBT nbt) {
        super.readShareTag(stack, nbt);
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint ->
                CapabilityBlueprint.BLUEPRINT_CAPABILITY.readNBT(iBluePrint, null, nbt.get(UtilcraftBuilding.MOD_ID+"_capData")));
    }

    @Nullable
    @Override
    public CompoundNBT getShareTag(ItemStack stack) {
        CompoundNBT tag = super.getShareTag(stack);
        if(tag == null) {
            tag = new CompoundNBT();
        }
        AtomicReference<INBT> capTag = new AtomicReference<>();
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            capTag.set(CapabilityBlueprint.BLUEPRINT_CAPABILITY.writeNBT(iBluePrint, null));
        });
        tag.put(UtilcraftBuilding.MOD_ID+"_capData", capTag.get());
        return tag;
    }

    private ActionResultType tryPlace(@Nonnull BlockItemUseContext context) {
        if (!context.canPlace()) {
            return ActionResultType.FAIL;
        } else {
            BlockState blockstate = UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.get().getStateForPlacement(context);
            if (blockstate == null) {
                return ActionResultType.FAIL;
            } else if (!context.getLevel().setBlock(context.getClickedPos(), blockstate, 11)) {
                return ActionResultType.FAIL;
            } else {
                BlockPos blockpos = context.getClickedPos();
                World world = context.getLevel();
                PlayerEntity player = context.getPlayer();
                ItemStack itemstack = context.getItemInHand();
                BlockState otherState = world.getBlockState(blockpos);
                Block other = otherState.getBlock();
                if (other == blockstate.getBlock()) {
                    otherState = this.getRealBlockState(blockpos, world, itemstack, otherState);
                    BlockItem.updateCustomBlockEntityTag(world, player, blockpos, itemstack);
                    other.setPlacedBy(world, blockpos, otherState, player, itemstack);
                    this.setCapability(world, blockpos, context.getItemInHand());
                    if (player instanceof ServerPlayerEntity) {
                        PacketHandler.sendToClient(new SyncBlueprintTECapability(blockpos, world), (ServerPlayerEntity) player);
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity)player, blockpos, itemstack);
                    }
                }

                SoundType soundtype = otherState.getSoundType(world, blockpos, context.getPlayer());
                world.playSound(player, blockpos, otherState.getSoundType(world, blockpos, context.getPlayer()).getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                return ActionResultType.sidedSuccess(world.isClientSide);
            }
        }
    }

    private BlockState getRealBlockState(BlockPos blockPos, World world, @Nonnull ItemStack itemStack, BlockState blockState) {
        BlockState blockstate = blockState;
        CompoundNBT compoundnbt = itemStack.getTag();
        if (compoundnbt != null) {
            CompoundNBT blockStateTag = compoundnbt.getCompound("BlockStateTag");
            StateContainer<Block, BlockState> stateContainer = blockState.getBlock().getStateDefinition();

            for(String s : blockStateTag.getAllKeys()) {
                Property<?> property = stateContainer.getProperty(s);
                if (property != null) {
                    String s1 = blockStateTag.get(s).getAsString();
                    blockstate = applyProperty(blockstate, property, s1);
                }
            }
        }

        if (blockstate != blockState) {
            world.setBlock(blockPos, blockstate, 2);
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState blockState, @Nonnull Property<T> property, String state) {
        return property.getValue(state).map((value) -> blockState.setValue(property, value)).orElse(blockState);
    }

    private void setCapability(@Nonnull World world, BlockPos pos, ItemStack itemStack) {
        TileEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof BlueprintBlockTileEntity) {
            itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                    .ifPresent(iBluePrint -> tileentity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                            .ifPresent(iBluePrint2 -> iBluePrint2.setPattern(iBluePrint.getPattern()))
                    );
        }
    }
}
