package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(@Nonnull GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        generator.addProvider(new Recipes(generator));
        generator.addProvider(new LootTables(generator));
        BlockTags blockTags = new BlockTags(generator, existingFileHelper);
        generator.addProvider(blockTags);
        generator.addProvider(new ItemTags(generator, blockTags, existingFileHelper));
        generator.addProvider(new GlobalLootModifiers(generator));
        generator.addProvider(new Advancements(generator));
        generator.addProvider(Languages.getEnglish(generator));
        generator.addProvider(Languages.getGerman(generator));
        generator.addProvider(new BlockStates(generator, existingFileHelper));
        generator.addProvider(new ItemModels(generator, existingFileHelper));
    }
}