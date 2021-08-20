package net.petersil98.utilcraft_building.items;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.data.capabilities.CapabilityStoreHelper;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.BlueprintProvider;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.block_entities.BlueprintBlockEntity;
import net.petersil98.utilcraft_building.utils.BlueprintUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

public class Blueprint extends Item {

    public Blueprint() {
        super(new Properties()
                .tab(UtilcraftBuilding.ITEM_GROUP)
                .stacksTo(1)
        );
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new BlueprintProvider();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level world, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        Map<BlockState, Integer> blocks = BlueprintUtils.listBlockStatesFromCapability(stack);
        blocks.forEach((blockState, count) -> tooltip.add(new TranslatableComponent("blueprint.utilcraft_building.tooltip", count, new TranslatableComponent(blockState.getBlock().getDescriptionId())).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(ChatFormatting.BLUE.getColor())))));
    }

    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        return this.tryPlace(new BlockPlaceContext(context));
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        super.readShareTag(stack, nbt);
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            CapabilityStoreHelper.readBlueprintCapability((CompoundTag) nbt.get(UtilcraftBuilding.MOD_ID+"_capData"), iBluePrint);
        });
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = super.getShareTag(stack);
        if(tag == null) {
            tag = new CompoundTag();
        }
        AtomicReference<Tag> capTag = new AtomicReference<>();
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            capTag.set(CapabilityStoreHelper.storeBlueprintCapability(iBluePrint));
        });
        tag.put(UtilcraftBuilding.MOD_ID+"_capData", capTag.get());
        return tag;
    }

    private InteractionResult tryPlace(@Nonnull BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        } else {
            BlockState blockstate = UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.getStateForPlacement(context);
            if (blockstate == null) {
                return InteractionResult.FAIL;
            } else if (!context.getLevel().setBlock(context.getClickedPos(), blockstate, 11)) {
                return InteractionResult.FAIL;
            } else {
                BlockPos blockpos = context.getClickedPos();
                Level world = context.getLevel();
                Player playerentity = context.getPlayer();
                ItemStack itemstack = context.getItemInHand();
                BlockState otherState = world.getBlockState(blockpos);
                Block other = otherState.getBlock();
                if (other == blockstate.getBlock()) {
                    otherState = this.getRealBlockState(blockpos, world, itemstack, otherState);
                    BlockItem.updateCustomBlockEntityTag(world, playerentity, blockpos, itemstack);
                    other.setPlacedBy(world, blockpos, otherState, playerentity, itemstack);
                    this.setCapability(world, blockpos, context.getItemInHand());
                    if (playerentity instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)playerentity, blockpos, itemstack);
                    }
                }

                SoundType soundtype = otherState.getSoundType(world, blockpos, context.getPlayer());
                world.playSound(playerentity, blockpos, otherState.getSoundType(world, blockpos, context.getPlayer()).getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);

                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }
    }

    private BlockState getRealBlockState(BlockPos blockPos, Level world, @Nonnull ItemStack itemStack, BlockState blockState) {
        BlockState blockstate = blockState;
        CompoundTag compoundnbt = itemStack.getTag();
        if (compoundnbt != null) {
            CompoundTag blockStateTag = compoundnbt.getCompound("BlockStateTag");
            StateDefinition<Block, BlockState> stateContainer = blockState.getBlock().getStateDefinition();

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

    private void setCapability(@Nonnull Level world, BlockPos pos, ItemStack itemStack) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof BlueprintBlockEntity) {
            itemStack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                    .ifPresent(iBluePrint -> tileentity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY)
                            .ifPresent(iBluePrint2 -> iBluePrint2.setPattern(iBluePrint.getPattern()))
                    );
        }
    }
}
