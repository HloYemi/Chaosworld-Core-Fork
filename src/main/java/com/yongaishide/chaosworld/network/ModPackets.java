package com.yongaishide.chaosworld.network;

import com.yongaishide.chaosworld.network.packet.VeinUpgradePacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPackets {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("chaosworld_core").versioned("1.0");

        registrar.playToServer(
                VeinUpgradePacket.TYPE,
                VeinUpgradePacket.STREAM_CODEC,
                VeinUpgradePacket::handle
        );
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
}
