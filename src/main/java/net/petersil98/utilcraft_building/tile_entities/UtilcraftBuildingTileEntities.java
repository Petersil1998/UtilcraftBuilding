package net.petersil98.utilcraft_building.tile_entities;

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

@ObjectHolder(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuildingTileEntities {

    @ObjectHolder("blueprint_block")
    public static TileEntityType<BlueprintBlockTileEntity> BLUEPRINT_BLOCK;
}
