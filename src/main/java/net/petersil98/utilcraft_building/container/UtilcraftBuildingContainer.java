package net.petersil98.utilcraft_building.container;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.ObjectHolder;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;

@ObjectHolder(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuildingContainer {

    @ObjectHolder("architect_table")
    public static MenuType<ArchitectTableContainer> ARCHITECT_TABLE_CONTAINER;

    @ObjectHolder("blueprint_block")
    public static MenuType<BlueprintBlockContainer> BLUEPRINT_BLOCK_CONTAINER;
}
