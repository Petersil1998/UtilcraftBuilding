package net.petersil98.utilcraft_building.datagen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.block.*;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.HashCache;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.petersil98.utilcraft_building.utils.BlockItemUtils;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public abstract class BaseLootTableProvider extends LootTableProvider {

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
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(ApplyExplosionDecay.explosionDecay())
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(StatePropertiesPredicate.Builder
                                                .properties()
                                                .hasProperty(SlabBlock.TYPE, SlabType.DOUBLE))))
                );
        return LootTable.lootTable().withPool(builder);
    }

    protected LootTable.Builder createDoorTable(Block block) {
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block).
                                setProperties(StatePropertiesPredicate.Builder
                                        .properties()
                                        .hasProperty(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                                )
                        )
                ).when(ExplosionCondition.survivesExplosion());
        return LootTable.lootTable().withPool(builder);
    }

    protected LootTable.Builder createOreTable(Block block, Item drop) {
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .when(MatchTool.toolMatches(ItemPredicate.Builder.item().hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1)))))
                        .otherwise(LootItem.lootTableItem(drop)
                                .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))
                                .apply(ApplyExplosionDecay.explosionDecay())
                        )
                );
        return LootTable.lootTable().withPool(builder);
    }

    protected LootTable.Builder createLeaveTable(Block block, Block sapling, boolean dropSticks, boolean dropApples) {
        LootTable.Builder table = LootTable.lootTable();
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(AlternativesEntry.alternatives()
                        .otherwise(LootItem.lootTableItem(block)
                                .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
                                        .or(MatchTool.toolMatches(ItemPredicate.Builder.item()
                                                .hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1)))))))
                        .otherwise(LootItem.lootTableItem(sapling)
                                .when(ExplosionCondition.survivesExplosion())
                                .when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE, 0.05f, 0.0625f, 0.083333336f, 0.1f))));
        table.withPool(builder);

        if(dropSticks) {
            LootPool.Builder builder2 = LootPool.lootPool()
                    .name(BlockItemUtils.name(block))
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.STICK)
                            .when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE, 0.02f, 0.022222223f, 0.025f, 0.033333335f, 0.1f))
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))
                            .apply(ApplyExplosionDecay.explosionDecay()))
                    .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
                            .or(MatchTool.toolMatches(ItemPredicate.Builder.item()
                                    .hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1))))).invert());
            table.withPool(builder2);
        }

        if(dropApples) {
            LootPool.Builder builder3 = LootPool.lootPool()
                    .name(BlockItemUtils.name(block))
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.APPLE)
                            .when(BonusLevelTableCondition.bonusLevelFlatChance(Enchantments.BLOCK_FORTUNE, 0.005f, 0.0055555557f, 0.00625f, 0.008333334f, 0.025f))
                            .when(ExplosionCondition.survivesExplosion()))
                    .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))
                            .or(MatchTool.toolMatches(ItemPredicate.Builder.item()
                                    .hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1))))).invert());
            table.withPool(builder3);
        }

        return table;
    }

    protected LootTable.Builder createSimpleTable(Block block) {
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(ExplosionCondition.survivesExplosion());
        return LootTable.lootTable().withPool(builder);
    }

    protected LootTable.Builder createSimpleTableWithName(Block block) {
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block)
                        .apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY)))
                .when(ExplosionCondition.survivesExplosion());
        return LootTable.lootTable().withPool(builder);
    }

    protected LootTable.Builder createPottedFlower(FlowerPotBlock potBlock, Block flowerBlock) {
        LootPool.Builder pot = LootPool.lootPool()
                .name(BlockItemUtils.name(potBlock))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(potBlock))
                .when(ExplosionCondition.survivesExplosion());
        LootPool.Builder flower = LootPool.lootPool()
                .name(BlockItemUtils.name(potBlock))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(flowerBlock))
                .when(ExplosionCondition.survivesExplosion());
        return LootTable.lootTable().withPool(pot).withPool(flower);
    }

    protected LootTable.Builder createSilkTouchBlock(Block block) {
        LootPool.Builder builder = LootPool.lootPool()
                .name(BlockItemUtils.name(block))
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block))
                .when(MatchTool.toolMatches(ItemPredicate.Builder.item()
                        .hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1))))
                );
        return LootTable.lootTable().withPool(builder);
    }

    @Override
    public void run(@Nonnull HashCache cache) {
        addTables();

        Map<ResourceLocation, LootTable> tables = new HashMap<>();
        for (Map.Entry<Block, LootTable.Builder> entry : lootTables.entrySet()) {
            tables.put(entry.getKey().getLootTable(), entry.getValue().setParamSet(LootContextParamSets.BLOCK).build());
        }
        writeTables(cache, tables);
    }

    private void writeTables(HashCache cache, @Nonnull Map<ResourceLocation, LootTable> tables) {
        Path outputFolder = this.generator.getOutputFolder();
        tables.forEach((key, lootTable) -> {
            Path path = outputFolder.resolve("data/" + key.getNamespace() + "/loot_tables/" + key.getPath() + ".json");
            try {
                DataProvider.save(GSON, cache, LootTables.serialize(lootTable), path);
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