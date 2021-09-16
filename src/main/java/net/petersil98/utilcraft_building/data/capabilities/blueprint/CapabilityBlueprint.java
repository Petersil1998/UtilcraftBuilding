package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class CapabilityBlueprint {
    @CapabilityInject(IBluePrint.class)
    public static Capability<IBluePrint> BLUEPRINT_CAPABILITY = null;

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IBluePrint.class);
    }
}
