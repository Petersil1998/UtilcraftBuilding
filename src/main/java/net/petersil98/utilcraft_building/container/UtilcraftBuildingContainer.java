package net.petersil98.utilcraft_building.container;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.registries.ObjectHolder;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

@ObjectHolder(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuildingContainer {

    @ObjectHolder("architect_table")
    public static ContainerType<ArchitectTableContainer> ARCHITECT_TABLE_CONTAINER;
}
