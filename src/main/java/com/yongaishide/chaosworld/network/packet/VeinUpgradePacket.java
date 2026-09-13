package com.yongaishide.chaosworld.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 虚脉钻探机专属升级指令:点击升级钻头按钮 -> 钻头等级+1。
 */
public record VeinUpgradePacket(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VeinUpgradePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("chaosworld_core", "vein_upgrade"));

    public static final StreamCodec<ByteBuf, VeinUpgradePacket> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(VeinUpgradePacket::new, VeinUpgradePacket::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VeinUpgradePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockEntity be = player.level().getBlockEntity(payload.pos());
                if (be instanceof com.yongaishide.chaosworld.mekanism.vein.tile.TileEntityVeinDrill drill) {
                    drill.upgradeDrill();
                }
            }
        });
    }
}
