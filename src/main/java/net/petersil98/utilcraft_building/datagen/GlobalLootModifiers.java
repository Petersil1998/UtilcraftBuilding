package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

public class GlobalLootModifiers extends GlobalLootModifierProvider {

    public GlobalLootModifiers(DataGenerator generator) {
        super(generator, UtilcraftBuilding.MOD_ID);
    }

    @Override
    protected void start() {

    }
}
