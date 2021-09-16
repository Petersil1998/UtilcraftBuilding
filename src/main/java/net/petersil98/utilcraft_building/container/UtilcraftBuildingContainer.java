package net.petersil98.utilcraft_building.container;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;

public class UtilcraftBuildingContainer {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, UtilcraftBuilding.MOD_ID);

    public static final RegistryObject<ContainerType<ArchitectTableContainer>> ARCHITECT_TABLE_CONTAINER = CONTAINERS.register("architect_table", () -> IForgeContainerType.create((windowId, inv, data) -> new ArchitectTableContainer(windowId, inv)));
    public static final RegistryObject<ContainerType<BlueprintBlockContainer>> BLUEPRINT_BLOCK_CONTAINER = CONTAINERS.register("blueprint_block", () -> IForgeContainerType.create(BlueprintBlockContainer::new));
}
