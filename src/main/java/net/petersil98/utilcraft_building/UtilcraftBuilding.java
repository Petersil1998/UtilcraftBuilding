package net.petersil98.utilcraft_building;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleType;
import net.minecraft.potion.Effect;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.petersil98.utilcraft_building.blocks.ArchitectTable;
import net.petersil98.utilcraft_building.blocks.UtilcraftBuildingBlocks;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;
import net.petersil98.utilcraft_building.container.UtilcraftBuildingContainer;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.CapabilityBlueprint;
import net.petersil98.utilcraft_building.items.Blueprint;
import net.petersil98.utilcraft_building.network.PacketHandler;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;

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
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

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
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerBlocks(final RegistryEvent.Register<Block> blockRegistryEvent) {
            blockRegistryEvent.getRegistry().register(new ArchitectTable().setRegistryName("architect_table"));
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
        public static void registerEntities(@Nonnull final RegistryEvent.Register<EntityType<?>> entityRegister) {
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