package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

public class BlockTags extends BlockTagsProvider {

    public BlockTags(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, UtilcraftBuilding.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags() {
    }
}
