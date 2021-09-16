package net.petersil98.utilcraft_building.items;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;

public class UtilcraftBuildingItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, UtilcraftBuilding.MOD_ID);

    public static final RegistryObject<BlockItem> ARCHITECT_TABLE = ITEMS.register("architect_table", () -> new BlockItem(UtilcraftBuildingBlocks.ARCHITECT_TABLE.get(), new Item.Properties().tab(UtilcraftBuilding.ITEM_GROUP)));

    public static final RegistryObject<Blueprint> BLUEPRINT = ITEMS.register("blueprint", () -> new Blueprint(new Item.Properties().tab(UtilcraftBuilding.ITEM_GROUP).stacksTo(1)));
}
