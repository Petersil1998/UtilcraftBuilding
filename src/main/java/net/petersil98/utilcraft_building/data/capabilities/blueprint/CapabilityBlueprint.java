package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CapabilityBlueprint {
    @CapabilityInject(IBluePrint.class)
    public static Capability<IBluePrint> BLUEPRINT_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IBluePrint.class, new Storage(), DefaultBlueprint::new);
    }

    public static class Storage implements Capability.IStorage<IBluePrint> {

        @Nullable
        @Override
        public INBT writeNBT(Capability<IBluePrint> capability, @Nonnull IBluePrint instance, Direction side) {
            CompoundNBT tag = new CompoundNBT();
            List<List<List<BlockState>>> pattern = instance.getPattern();
            ListNBT list = new ListNBT();
            if(pattern != null && pattern.size() > 0) {
                tag.putInt("layers", pattern.size());
                for (int i = 0; i < pattern.size(); i++) {
                    for (int j = 0; j < ArchitectTableContainer.SIZE; j++) {
                        for (int k = 0; k < ArchitectTableContainer.SIZE; k++) {
                            list.add(i * ArchitectTableContainer.SIZE * ArchitectTableContainer.SIZE + j * ArchitectTableContainer.SIZE + k, NBTUtil.writeBlockState(pattern.get(i).get(j).get(k)));
                        }
                    }
                }
                tag.put("blockstates", list);
            }
            return tag;
        }

        @Override
        public void readNBT(Capability<IBluePrint> capability, @Nonnull IBluePrint instance, Direction side, INBT nbt) {
            CompoundNBT tag = ((CompoundNBT)nbt);
            int layers = tag.getInt("layers");
            List<List<List<BlockState>>> pattern = new ArrayList<>(layers);
            ListNBT list = (ListNBT) tag.get("blockstates");
            for(int i = 0; i < layers; i++) {
                pattern.add(new ArrayList<>());
                for (int j = 0; j < ArchitectTableContainer.SIZE; j++) {
                    pattern.get(i).add(new ArrayList<>());
                    for (int k = 0; k < ArchitectTableContainer.SIZE; k++) {
                        pattern.get(i).get(j).add(NBTUtil.readBlockState(list.getCompound(i * ArchitectTableContainer.SIZE * ArchitectTableContainer.SIZE + j * ArchitectTableContainer.SIZE + k)));
                    }
                }
            }
            instance.setPattern(pattern);
        }
    }
}
