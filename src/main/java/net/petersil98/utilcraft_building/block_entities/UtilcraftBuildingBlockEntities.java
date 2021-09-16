package net.petersil98.utilcraft_building.block_entities;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;

public class UtilcraftBuildingBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, UtilcraftBuilding.MOD_ID);

    public static final RegistryObject<BlockEntityType<BlueprintBlockEntity>> BLUEPRINT_BLOCK = BLOCK_ENTITIES.register("blueprint_block", () -> BlockEntityType.Builder.of(BlueprintBlockEntity::new, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK.get()).build(null));
}
