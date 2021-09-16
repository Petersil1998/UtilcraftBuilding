package net.petersil98.utilcraft_building;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.UtilcraftBuildingItems;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.renderer.BlueprintBlockTileEntityRenderer;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;
import net.petersil98.utilcraft_building.screen.BlueprintBlockScreen;
import net.petersil98.utilcraft_building.tile_entities.UtilcraftBuildingTileEntities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Mod(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuilding
{
    public static final String MOD_ID = "utilcraft_building";

    public static final ItemGroup ITEM_GROUP = new ItemGroup(MOD_ID) {

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

        UtilcraftBuildingBlocks.BLOCKS.register(eventBus);
        UtilcraftBuildingItems.ITEMS.register(eventBus);
        UtilcraftBuildingTileEntities.TILE_ENTITIES.register(eventBus);
        UtilcraftBuildingContainer.CONTAINERS.register(eventBus);
    }

    private void setup(final FMLCommonSetupEvent event) {
        CapabilityBlueprint.register();
        PacketHandler.registerMessages();
    }

    private void clientSetup(@Nonnull final FMLClientSetupEvent event) {
        RenderTypeLookup.setRenderLayer(UtilcraftBuildingBlocks.ARCHITECT_TABLE.get(), RenderType.translucent());
        ClientRegistry.bindTileEntityRenderer(UtilcraftBuildingTileEntities.BLUEPRINT_BLOCK.get(), BlueprintBlockTileEntityRenderer::new);
        event.enqueueWork(() -> {
            ScreenManager.register(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER.get(), ArchitectTableScreen::new);
            ScreenManager.register(UtilcraftBuildingContainer.BLUEPRINT_BLOCK_CONTAINER.get(), BlueprintBlockScreen::new);
        });
    }

    private void registerExtraModels(final ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(BlueprintBlockTileEntityRenderer.CUBE_MODEL);
    }
}
