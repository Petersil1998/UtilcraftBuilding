package net.petersil98.utilcraft_building;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.petersil98.utilcraft_building.blocks.ArchitectTable;
import net.petersil98.utilcraft_building.blocks.BlueprintBlock;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.renderer.BlueprintBlockTileEntityRenderer;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;
import net.petersil98.utilcraft_building.tile_entities.UtilcraftBuildingTileEntities;

import javax.annotation.Nonnull;

@Mod(UtilcraftBuilding.MOD_ID)
public class UtilcraftBuilding
{
    public static final String MOD_ID = "utilcraft_building";

    public static final ItemGroup ITEM_GROUP = new ItemGroup(MOD_ID) {

        @Nonnull
        @Override
        public ItemStack createIcon() {
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
        RenderTypeLookup.setRenderLayer(UtilcraftBuildingBlocks.ARCHITECT_TABLE, RenderType.getTranslucent());
        ScreenManager.registerFactory(UtilcraftBuildingContainer.ARCHITECT_TABLE_CONTAINER, ArchitectTableScreen::new);
        ClientRegistry.bindTileEntityRenderer(UtilcraftBuildingTileEntities.BLUEPRINT_BLOCK, BlueprintBlockTileEntityRenderer::new);
    }

    private void registerExtraModels(final ModelRegistryEvent event) {
        ModelLoader.addSpecialModel(BlueprintBlockTileEntityRenderer.CUBE_MODEL);
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerBlocks(final RegistryEvent.Register<Block> blockRegistryEvent) {
            blockRegistryEvent.getRegistry().register(new ArchitectTable().setRegistryName("architect_table"));
            blockRegistryEvent.getRegistry().register(new BlueprintBlock().setRegistryName("blueprint_block"));
        }

        @SubscribeEvent
        public static void registerItems(@Nonnull final RegistryEvent.Register<Item> itemRegistryEvent) {
            itemRegistryEvent.getRegistry().register(new BlockItem(UtilcraftBuildingBlocks.ARCHITECT_TABLE, new Item.Properties().group(ITEM_GROUP)).setRegistryName("architect_table"));

            itemRegistryEvent.getRegistry().register(new Blueprint().setRegistryName("blueprint"));
        }

        @SubscribeEvent
        public static void registerEffects(@Nonnull final RegistryEvent.Register<Effect> effectRegistryEvent) {
        }

        @SubscribeEvent
        public static void registerEntities(@Nonnull final RegistryEvent.Register<TileEntityType<?>> tileEntityRegister) {
            tileEntityRegister.getRegistry().register(TileEntityType.Builder.create(BlueprintBlockTileEntity::new, UtilcraftBuildingBlocks.BLUEPRINT_BLOCK).build(null).setRegistryName("blueprint_block"));
        }

        @SubscribeEvent
        public static void registerParticleTypes(@Nonnull final RegistryEvent.Register<ParticleType<?>> particleTypeRegister) {
        }

        @SubscribeEvent
        public static void registerContainer(@Nonnull final RegistryEvent.Register<ContainerType<?>> containerRegister) {
            containerRegister.getRegistry().register(IForgeContainerType.create((windowId, inv, data) -> new ArchitectTableContainer(windowId, inv)).setRegistryName("architect_table"));
        }
    }
}
