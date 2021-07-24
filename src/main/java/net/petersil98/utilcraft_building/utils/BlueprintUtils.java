package net.petersil98.utilcraft_building.utils;

import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlueprintUtils {

    @Nonnull
    public static Map<BlockState, Integer> listBlockStatesFromCapability(@Nonnull ItemStack stack) {
        Map<BlockState, Integer> blocks = new HashMap<>();
        stack.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            if(iBluePrint.getPattern() != null) {
                iBluePrint.getPattern().forEach(
                    lists -> lists.forEach(
                        blockStates -> blockStates.forEach(blockState -> {
                            if(!(blockState.getBlock() instanceof AirBlock)) {
                                if (blocks.containsKey(blockState)) {
                                    blocks.put(blockState, blocks.get(blockState) + 1);
                                } else {
                                    blocks.put(blockState, 1);
                                }
                            }
                        })
                    )
                );
            }
        });
        return blocks;
    }

    @Nonnull
    public static Map<BlockState, Integer> listBlockStatesFromCapability(@Nonnull TileEntity tileEntity) {
        Map<BlockState, Integer> blocks = new HashMap<>();
        tileEntity.getCapability(CapabilityBlueprint.BLUEPRINT_CAPABILITY).ifPresent(iBluePrint -> {
            if(iBluePrint.getPattern() != null) {
                iBluePrint.getPattern().forEach(
                    lists -> lists.forEach(
                        blockStates -> blockStates.forEach(blockState -> {
                            if(!(blockState.getBlock() instanceof AirBlock)) {
                                if (blocks.containsKey(blockState)) {
                                    blocks.put(blockState, blocks.get(blockState) + 1);
                                } else {
                                    blocks.put(blockState, 1);
                                }
                            }
                        })
                    )
                );
            }
        });
        return blocks;
    }

    @Nonnull
    public static Map<Item, Integer> listBlockItemsFromSlots(@Nonnull List<Slot> slots) {
        Map<Item, Integer> blocks = new HashMap<>();
        for (Slot slot: slots) {
            ItemStack current = slot.getItem();
            if (!current.equals(ItemStack.EMPTY)) {
                if(blocks.containsKey(current.getItem())) {
                    blocks.put(current.getItem(), blocks.get(current.getItem()) + current.getCount());
                } else {
                    blocks.put(current.getItem(), current.getCount());
                }
            }
        }
        return blocks;
    }

    @Nonnull
    public static Map<Item, Integer> listBlockItemsFromInventory(@Nonnull Inventory inventory) {
        Map<Item, Integer> blocks = new HashMap<>();
        for(int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (!current.equals(ItemStack.EMPTY)) {
                if(blocks.containsKey(current.getItem())) {
                    blocks.put(current.getItem(), blocks.get(current.getItem()) + current.getCount());
                } else {
                    blocks.put(current.getItem(), current.getCount());
                }
            }
        }
        return blocks;
    }

    public static <T> Map<Block, T> fromBlockStateToBlock(@Nonnull Map<BlockState, T> blockStates) {
        return blockStates.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getBlock(), Map.Entry::getValue));
    }
}
