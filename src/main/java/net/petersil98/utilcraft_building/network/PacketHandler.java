package net.petersil98.utilcraft_building.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import net.petersil98.utilcraft_building.UtilcraftBuilding;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1.0";
    private static int id = 0;
    private static SimpleChannel INSTANCE;

    public static void registerMessages() {
        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(UtilcraftBuilding.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        INSTANCE.messageBuilder(SyncArchitectTableDataPoint.class, id++)
                .encoder(SyncArchitectTableDataPoint::encode)
                .decoder(SyncArchitectTableDataPoint::new)
                .consumer(SyncArchitectTableDataPoint::handle)
                .add();

        INSTANCE.messageBuilder(SyncButtonPressed.class, id++)
                .encoder(SyncButtonPressed::encode)
                .decoder(SyncButtonPressed::new)
                .consumer(SyncButtonPressed::handle)
                .add();

        INSTANCE.messageBuilder(SyncBlueprintTECapability.class, id++)
                .encoder(SyncBlueprintTECapability::encode)
                .decoder(SyncBlueprintTECapability::new)
                .consumer(SyncBlueprintTECapability::handle)
                .add();

        INSTANCE.messageBuilder(CreateStructurePacket.class, id++)
                .encoder(CreateStructurePacket::encode)
                .decoder(CreateStructurePacket::new)
                .consumer(CreateStructurePacket::handle)
                .add();
    }

    public static <PACKET> void sendToClient(PACKET packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static <PACKET> void sendToServer(PACKET packet){
        INSTANCE.sendToServer(packet);
    }
}
