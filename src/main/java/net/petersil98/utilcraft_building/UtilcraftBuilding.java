package net.petersil98.utilcraft_building;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.petersil98.utilcraft_building.block_entities.UtilcraftBuildingBlockEntities;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.UtilcraftBuildingItems;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.renderer.BlueprintBlockTileEntityRenderer;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;
import net.petersil98.utilcraft_building.screen.BlueprintBlockScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuilding
{
    public static final String MOD_ID = "utilcraft_building";

    public static final CreativeModeTab ITEM_GROUP = new CreativeModeTab(MOD_ID) {

        @Nonnull
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(UtilcraftBuildingBlocks.ARCHITECT_TABLE.get());
        }
    };

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public UtilcraftBuilding() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::setup);
        eventBus.addListener(this::clientSetup);
        eventBus.addListener(this::registerExtraModels);
        eventBus.addListener(this::registerCapabilities);

        UtilcraftBuildingBlocks.BLOCKS.register(eventBus);
        UtilcraftBuildingItems.ITEMS.register(eventBus);
        UtilcraftBuildingBlockEntities.BLOCK_ENTITIES.register(eventBus);
        UtilcraftBuildingContainer.CONTAINERS.register(eventBus);
    }

    private void setup(final FMLCommonSetupEvent event) {
        PacketHandler.registerMessages();
    }

    private void clientSetup(@Nonnull final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(UtilcraftBuildingBlocks.ARCHITECT_TABLE.get(), RenderType.translucent());
        BlockEntityRenderers.register(UtilcraftBuildingBlockEntities.BLUEPRINT_BLOCK.get(), BlueprintBlockTileEntityRenderer::new);
        event.enqueueWork(() -> {
            MenuScreens.register(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER.get(), ArchitectTableScreen::new);
            MenuScreens.register(UtilcraftBuildingContainer.BLUEPRINT_BLOCK_CONTAINER.get(), BlueprintBlockScreen::new);
        });
    }

    private void registerExtraModels(final ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(BlueprintBlockTileEntityRenderer.CUBE_MODEL);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        CapabilityBlueprint.register(event);
    }
}
