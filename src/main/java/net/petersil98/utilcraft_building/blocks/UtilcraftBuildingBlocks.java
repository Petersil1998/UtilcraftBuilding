package net.petersil98.utilcraft_building.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

public class UtilcraftBuildingBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, UtilcraftBuilding.MOD_ID);

    public static final RegistryObject<ArchitectTable> ARCHITECT_TABLE = BLOCKS.register("architect_table", () -> new ArchitectTable(Block.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(3.5F).noOcclusion()));
    public static final RegistryObject<BlueprintBlock> BLUEPRINT_BLOCK = BLOCKS.register("blueprint_block", () -> new BlueprintBlock(Block.Properties.of(Material.STONE).strength(3.5F).noDrops().noCollission()));

}
