package net.petersil98.utilcraft_building.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.*;
import net.minecraft.loot.conditions.BlockStateProperty;
import net.minecraft.loot.conditions.MatchTool;
import net.minecraft.loot.conditions.SurvivesExplosion;
import net.minecraft.loot.conditions.TableBonus;
import net.minecraft.loot.functions.ApplyBonus;
import net.minecraft.loot.functions.CopyName;
import net.minecraft.loot.functions.ExplosionDecay;
import net.minecraft.loot.functions.SetCount;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.data.ForgeLootTableProvider;
import net.petersil98.utilcraft_building.utils.BlockItemUtils;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseLootTableProvider extends ForgeLootTableProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    protected final Map<Block, LootTable.Builder> lootTables = new HashMap<>();
    private final DataGenerator generator;

    public BaseLootTableProvider(DataGenerator dataGenerator) {
        super(dataGenerator);
        this.generator = dataGenerator;
    }

    protected abstract void addTables();

    protected LootTable.Builder createSlabTable(Block block) {
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(ItemLootEntry.builder(block)
                        .acceptFunction(ExplosionDecay.builder())
                        .acceptFunction(SetCount.builder(new ConstantRange(2))
                                .acceptCondition(BlockStateProperty.builder(block)
                                        .fromProperties(StatePropertiesPredicate.Builder
                                            .newBuilder()
                                            .withProp(SlabBlock.TYPE, SlabType.DOUBLE))))
                );
        return LootTable.builder().addLootPool(builder);
    }

    protected LootTable.Builder createDoorTable(Block block) {
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(ItemLootEntry.builder(block)
                        .acceptCondition(BlockStateProperty.builder(block).
                                fromProperties(StatePropertiesPredicate.Builder
                                        .newBuilder()
                                        .withProp(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                                )
                        )
                ).acceptCondition(SurvivesExplosion.builder());
        return LootTable.builder().addLootPool(builder);
    }

    protected LootTable.Builder createOreTable(Block block, Item drop) {
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(ItemLootEntry.builder(block)
                        .acceptCondition(MatchTool.builder(ItemPredicate.Builder.create().enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1)))))
                        .alternatively(ItemLootEntry.builder(drop)
                                .acceptFunction(ApplyBonus.oreDrops(Enchantments.FORTUNE))
                                .acceptFunction(ExplosionDecay.builder())
                        )
                );
        return LootTable.builder().addLootPool(builder);
    }

    protected LootTable.Builder createLeaveTable(Block block, Block sapling, boolean dropSticks, boolean dropApples) {
        LootTable.Builder table = LootTable.builder();
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(AlternativesLootEntry.builder()
                        .alternatively(ItemLootEntry.builder(block)
                                    .acceptCondition(MatchTool.builder(ItemPredicate.Builder.create().item(Items.SHEARS))
                                            .alternative(MatchTool.builder(ItemPredicate.Builder.create()
                                                    .enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1)))))))
                        .alternatively(ItemLootEntry.builder(sapling)
                                .acceptCondition(SurvivesExplosion.builder())
                                .acceptCondition(TableBonus.builder(Enchantments.FORTUNE, 0.05f, 0.0625f, 0.083333336f, 0.1f))));
        table.addLootPool(builder);

        if(dropSticks) {
            LootPool.Builder builder2 = LootPool.builder()
                    .name(BlockItemUtils.name(block))
                    .rolls(ConstantRange.of(1))
                    .addEntry(ItemLootEntry.builder(Items.STICK)
                            .acceptCondition(TableBonus.builder(Enchantments.FORTUNE, 0.02f, 0.022222223f, 0.025f, 0.033333335f, 0.1f))
                            .acceptFunction(SetCount.builder(RandomValueRange.of(1.0f, 2.0f)))
                            .acceptFunction(ExplosionDecay.builder()))
                    .acceptCondition(MatchTool.builder(ItemPredicate.Builder.create().item(Items.SHEARS))
                            .alternative(MatchTool.builder(ItemPredicate.Builder.create()
                                    .enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1))))).inverted());
            table.addLootPool(builder2);
        }

        if(dropApples) {
            LootPool.Builder builder3 = LootPool.builder()
                    .name(BlockItemUtils.name(block))
                    .rolls(ConstantRange.of(1))
                    .addEntry(ItemLootEntry.builder(Items.APPLE)
                            .acceptCondition(TableBonus.builder(Enchantments.FORTUNE, 0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f))
                            .acceptCondition(SurvivesExplosion.builder()))
                    .acceptCondition(MatchTool.builder(ItemPredicate.Builder.create().item(Items.SHEARS))
                            .alternative(MatchTool.builder(ItemPredicate.Builder.create()
                                    .enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.IntBound.atLeast(1))))).inverted());
            table.addLootPool(builder3);
        }

        return table;
    }

    protected LootTable.Builder createSimpleTable(Block block) {
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(ItemLootEntry.builder(block))
                .acceptCondition(SurvivesExplosion.builder());
        return LootTable.builder().addLootPool(builder);
    }

    protected LootTable.Builder createSimpleTableWithName(Block block) {
        LootPool.Builder builder = LootPool.builder()
                .name(BlockItemUtils.name(block))
                .rolls(ConstantRange.of(1))
                .addEntry(ItemLootEntry.builder(block)
                        .acceptFunction(CopyName.builder(CopyName.Source.BLOCK_ENTITY)))
                .acceptCondition(SurvivesExplosion.builder());
        return LootTable.builder().addLootPool(builder);
    }

    @Override
    public void act(@Nonnull DirectoryCache cache) {
        addTables();

        Map<ResourceLocation, LootTable> tables = new HashMap<>();
        for (Map.Entry<Block, LootTable.Builder> entry : lootTables.entrySet()) {
            tables.put(entry.getKey().getLootTable(), entry.getValue().setParameterSet(LootParameterSets.BLOCK).build());
        }
        writeTables(cache, tables);
    }

    private void writeTables(DirectoryCache cache, @Nonnull Map<ResourceLocation, LootTable> tables) {
        Path outputFolder = this.generator.getOutputFolder();
        tables.forEach((key, lootTable) -> {
            Path path = outputFolder.resolve("data/" + key.getNamespace() + "/loot_tables/" + key.getPath() + ".json");
            try {
                IDataProvider.save(GSON, cache, LootTableManager.toJson(lootTable), path);
            } catch (IOException e) {
                LOGGER.error("Couldn't write loot table {}", path, e);
            }
        });
    }

    @Nonnull
    @Override
    public String getName() {
        return String.format("%s LootTables", UtilcraftBuilding.MOD_ID);
    }
}