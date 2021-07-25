package net.petersil98.utilcraft_building.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;

public class LootTables extends BaseLootTableProvider {

    public LootTables(DataGenerator dataGenerator) {
        super(dataGenerator);
    }

    @Override
    protected void addTables() {

    }

    private void addSimpleBlock(Block block) {
        lootTables.put(block, createSimpleTable(block));
    }

    private void addSimpleBlockWithName(Block block) {
        lootTables.put(block, createSimpleTableWithName(block));
    }

    private void addSlab(SlabBlock block) {
        lootTables.put(block, createSlabTable(block));
    }

    private void addOreBlock(Block block, Item drop) {
        lootTables.put(block, createOreTable(block, drop));
    }

    private void addDoor(DoorBlock block) {
        lootTables.put(block, createDoorTable(block));
    }

    private void addLeave(LeavesBlock block, SaplingBlock sapling, boolean dropSticks, boolean dropApples) {
        lootTables.put(block, createLeaveTable(block, sapling, dropSticks, dropApples));
    }
}
