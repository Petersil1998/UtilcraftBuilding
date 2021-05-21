package net.petersil98.utilcraft_building.event;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.petersil98.utilcraft_building.UtilcraftBuilding;
import net.petersil98.utilcraft_building.data.capabilities.blueprint.BlueprintProvider;
import net.petersil98.utilcraft_building.tile_entities.BlueprintBlockTileEntity;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = UtilcraftBuilding.MOD_ID)
public class AttachCapabilities {

    @SubscribeEvent
    public static void attachVeinMinerCapability(@Nonnull AttachCapabilitiesEvent<TileEntity> event) {
        if (event.getObject() instanceof BlueprintBlockTileEntity) {
            BlueprintProvider provider = new BlueprintProvider();
            event.addCapability(new ResourceLocation(UtilcraftBuilding.MOD_ID, "blueprint"), provider);
            event.addListener(provider::invalidate);
        }
    }
}
