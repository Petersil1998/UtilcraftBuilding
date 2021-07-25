package net.petersil98.utilcraft_building.data.capabilities.blueprint;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class CapabilityBlueprint {
    @CapabilityInject(IBluePrint.class)
    public static Capability<IBluePrint> BLUEPRINT_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IBluePrint.class);
    }
}
