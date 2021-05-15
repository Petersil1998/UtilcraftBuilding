package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

public class ItemTags extends ItemTagsProvider {

    public ItemTags(DataGenerator generator, BlockTags blockTags, ExistingFileHelper existingFileHelper) {
        super(generator, blockTags, UtilcraftBuilding.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerTags() {
    }
}
