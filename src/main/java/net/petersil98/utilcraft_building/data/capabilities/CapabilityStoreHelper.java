package net.petersil98.utilcraft_building.data.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.IBluePrint;

import java.util.ArrayList;
import java.util.List;

public class CapabilityStoreHelper {

    public static CompoundTag storeBlueprintCapability(IBluePrint instance) {
        CompoundTag tag = new CompoundTag();
        List<List<List<BlockState>>> pattern = instance.getPattern();
        ListTag list = new ListTag();
        if(pattern != null && pattern.size() > 0) {
            tag.putInt("layers", pattern.size());
            for (int i = 0; i < pattern.size(); i++) {
                for (int j = 0; j < ArchitectTableContainer.SIZE; j++) {
                    for (int k = 0; k < ArchitectTableContainer.SIZE; k++) {
                        list.add(i * ArchitectTableContainer.SIZE * ArchitectTableContainer.SIZE + j * ArchitectTableContainer.SIZE + k, NbtUtils.writeBlockState(pattern.get(i).get(j).get(k)));
                    }
                }
            }
            tag.put("blockstates", list);
        }
        return tag;
    }

    public static void readBlueprintCapability(CompoundTag tag, IBluePrint instance) {
        int layers = tag.getInt("layers");
        List<List<List<BlockState>>> pattern = new ArrayList<>(layers);
        ListTag list = (ListTag) tag.get("blockstates");
        for(int i = 0; i < layers; i++) {
            pattern.add(new ArrayList<>());
            for (int j = 0; j < ArchitectTableContainer.SIZE; j++) {
                pattern.get(i).add(new ArrayList<>());
                for (int k = 0; k < ArchitectTableContainer.SIZE; k++) {
                    pattern.get(i).get(j).add(NbtUtils.readBlockState(list.getCompound(i * ArchitectTableContainer.SIZE * ArchitectTableContainer.SIZE + j * ArchitectTableContainer.SIZE + k)));
                }
            }
        }
        instance.setPattern(pattern);
    }
}
