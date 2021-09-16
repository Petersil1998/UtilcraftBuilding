package net.petersil98.utilcraft_building.tile_entities;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;

public class UtilcraftBuildingTileEntities {

    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, UtilcraftBuilding.MOD_ID);

    public static final RegistryObject<TileEntityType<BlueprintBlockTileEntity>> BLUEPRINT_BLOCK = TILE_ENTITIES.register("blueprint_block", () -> TileEntityType.Builder.of(BlueprintBlockTileEntity::new, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.get()).build(null));
}
