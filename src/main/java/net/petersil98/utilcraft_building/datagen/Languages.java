package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.items.UtilcraftBuildingItems;

public class Languages {

    public static English getEnglish(DataGenerator generator){
        return new English(generator);
    }

    public static German getGerman(DataGenerator generator){
        return new German(generator);
    }

    private static class English extends LanguageProvider {

        public English(DataGenerator generator) {
            super(generator, UtilcraftBuilding.MOD_ID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(UtilcraftBuildingBlocks.ARCHITECT_TABLE, "Architect's Table");
            add(UtilcraftBuildingItems.BLUEPRINT, "Blueprint");

            add(String.format("itemGroup.%s", UtilcraftBuilding.MOD_ID), "Utilcraft Building");
            add(String.format("architect_table.%s.layer", UtilcraftBuilding.MOD_ID), "Layer %d/%d");
        }
    }

    private static class German extends LanguageProvider {

        public German(DataGenerator generator) {
            super(generator, UtilcraftBuilding.MOD_ID, "de_de");
        }

        @Override
        protected void addTranslations() {
        }
    }
}
