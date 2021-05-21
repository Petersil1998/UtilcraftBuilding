package net.petersil98.utilcraft_building.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.petersil98.utilcraft_building.container.ArchitectTableContainer;
import net.petersil98.utilcraft_building.screen.ArchitectTableScreen;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SyncButtonPressed {

    private final int buttonId;

    public SyncButtonPressed(int buttonId) {
        this.buttonId = buttonId;
    }

    public SyncButtonPressed(@Nonnull PacketBuffer packetBuffer) {
        buttonId = packetBuffer.readInt();
    }

    public void encode(@Nonnull PacketBuffer buf) {
        buf.writeInt(buttonId);
    }

    public boolean handle(@Nonnull Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if(player != null && player.openContainer instanceof ArchitectTableContainer) {
                switch (buttonId) {
                    case ArchitectTableScreen.PREVIOUS_BUTTON_ID: {
                        ((ArchitectTableContainer) player.openContainer).previousLayer();
                        break;
                    }
                    case ArchitectTableScreen.NEXT_BUTTON_ID: {
                        ((ArchitectTableContainer) player.openContainer).nextLayer();
                        break;
                    }
                    case ArchitectTableScreen.ADD_LAYER_BUTTON_ID: {
                        ((ArchitectTableContainer) player.openContainer).addLayer();
                        break;
                    }
                    case ArchitectTableScreen.REMOVE_LAYER_BUTTON_ID: {
                        ((ArchitectTableContainer) player.openContainer).removeCurrentLayer();
                        break;
                    }
                }
            }
        });
        return true;
    }
}
