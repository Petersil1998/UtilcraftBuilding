package net.petersil98.utilcraft_building.block_entities;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.ObjectHolder;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

@ObjectHolder(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuildingBlockEntities {

    @ObjectHolder("blueprint_block")
    public static BlockEntityType<BlueprintBlockEntity> BLUEPRINT_BLOCK;
}
