package eu.pb4.graves.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GravesNetworking {
    public static final Identifier GRAVE_CREATED_PACKET = new Identifier("universal_graves", "grave_created");

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(GRAVE_CREATED_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                GraveClientState.graveCreated = true;
            });
        });
    }

    public static void sendGraveCreated(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, GRAVE_CREATED_PACKET, PacketByteBufs.create());
    }
}
