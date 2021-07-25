package net.petersil98.utilcraft_building.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
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

    public SyncArchitectTableDataPoint(@Nonnull FriendlyByteBuf packetBuffer) {
        this.currentLayer = packetBuffer.readInt();
        this.maxLayer = packetBuffer.readInt();
    }

    public void encode(@Nonnull FriendlyByteBuf buf) {
        buf.writeInt(this.currentLayer);
        buf.writeInt(this.maxLayer);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(Minecraft.getInstance().screen instanceof ArchitectTableScreen) {
                ArchitectTableScreen screen = (ArchitectTableScreen) Minecraft.getInstance().screen;
                screen.getMenu().setMaxLayer(this.maxLayer);
                screen.getMenu().setCurrentLayer(this.currentLayer);
            }
        });
        return true;
    }
}
