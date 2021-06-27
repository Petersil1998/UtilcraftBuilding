package net.petersil98.utilcraft_building.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SyncArchitectTableDataPoint {

    private final int currentLayer;
    private final int maxLayer;

    public SyncArchitectTableDataPoint(int currentLayer, int maxLayer) {
        this.currentLayer = currentLayer;
        this.maxLayer = maxLayer;
    }

    public SyncArchitectTableDataPoint(@Nonnull PacketBuffer packetBuffer) {
        this.currentLayer = packetBuffer.readInt();
        this.maxLayer = packetBuffer.readInt();
    }

    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeInt(this.currentLayer);
        buf.writeInt(this.maxLayer);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(Minecraft.getInstance().currentScreen instanceof ArchitectTableScreen) {
                ArchitectTableScreen screen = (ArchitectTableScreen) Minecraft.getInstance().currentScreen;
                screen.getContainer().setMaxLayer(this.maxLayer);
                screen.getContainer().setCurrentLayer(this.currentLayer);
            }
        });
        return true;
    }
}
