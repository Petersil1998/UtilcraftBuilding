package net.petersil98.utilcraft_building;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.petersil98.utilcraft_building.blocks.ArchitectTable;
import net.petersil98.utilcraft_building.blocks.BlueprintBlock;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.BlueprintBlockContainer;
import net.petersil98.utilcraft_building.container.architect_table.ArchitectTableContainer;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.renderer.BlueprintBlockTileEntityRenderer;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;
import net.petersil98.utilcraft_building.screen.BlueprintBlockScreen;
import net.petersil98.utilcraft_building.block_entities.BlueprintBlockEntity;
import net.petersil98.utilcraft_building.block_entities.UtilcraftBuildingBlockEntities;

import javax.annotation.Nonnull;

@Mod(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuilding
{
    public static final String MOD_ID = "utilcraft_building";

    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab(MOD_ID) {

        @Nonnull
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(UtilcraftBuildingBlocks.ARCHITECT_TABLE);
        }
    };

    public UtilcraftBuilding() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerExtraModels);
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        CapabilityBlueprint.register();
        PacketHandler.registerMessages();
    }

    private void clientSetup(@Nonnull final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(UtilcraftBuildingBlocks.ARCHITECT_TABLE, RenderType.translucent());
        MenuScreens.register(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER, ArchitectTableScreen::new);
        MenuScreens.register(UtilcraftBuildingContainer.BLUEPRINT_BLOCK_CONTAINER, BlueprintBlockScreen::new);
        BlockEntityRenderers.register(UtilcraftBuildingBlockEntities.BLUEPRINT_BLOCK, BlueprintBlockTileEntityRenderer::new);
    }

    private void registerExtraModels(final ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(BlueprintBlockTileEntityRenderer.CUBE_MODEL);
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerBlocks(@Nonnull final RegistryEvent.Register<Block> blockRegistryEvent) {
            blockRegistryEvent.getRegistry().register(new ArchitectTable().setRegistryName("architect_table"));
            blockRegistryEvent.getRegistry().register(new BlueprintBlock().setRegistryName("blueprint_block"));
        }

        @SubscribeEvent
        public static void registerItems(@Nonnull final RegistryEvent.Register<Item> itemRegistryEvent) {
            itemRegistryEvent.getRegistry().register(new BlockItem(UtilcraftBuildingBlocks.ARCHITECT_TABLE, new Item.Properties().tab(ITEM_GROUP)).setRegistryName("architect_table"));

            itemRegistryEvent.getRegistry().register(new Blueprint().setRegistryName("blueprint"));
        }

        @SubscribeEvent
        public static void registerEntities(@Nonnull final RegistryEvent.Register<BlockEntityType<?>> tileEntityRegister) {
            tileEntityRegister.getRegistry().register(BlockEntityType.Builder.of(BlueprintBlockEntity::new, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK).build(null).setRegistryName("blueprint_block"));
        }

        @SubscribeEvent
        public static void registerContainer(@Nonnull final RegistryEvent.Register<MenuType<?>> containerRegister) {
            containerRegister.getRegistry().register(IForgeContainerType.create((windowId, inv, data) -> new ArchitectTableContainer(windowId, inv)).setRegistryName("architect_table"));
            containerRegister.getRegistry().register(IForgeContainerType.create(BlueprintBlockContainer::new).setRegistryName("blueprint_block"));
        }
    }
}
