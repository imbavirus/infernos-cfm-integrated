package za.co.infernos.cfm_integrated.network.payload;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.link.client.ClientLinkState;

public record LinkStateS2C(BlockPos pos, String pairingCode, int deviceCount) implements CustomPacketPayload {
    public static final Type<LinkStateS2C> TYPE = new Type<>(CfmIntegrated.id("link_state"));

    public static final StreamCodec<ByteBuf, LinkStateS2C> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LinkStateS2C::pos,
            ByteBufCodecs.STRING_UTF8,
            LinkStateS2C::pairingCode,
            ByteBufCodecs.VAR_INT,
            LinkStateS2C::deviceCount,
            LinkStateS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LinkStateS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLinkState.apply(msg.pos, msg.pairingCode, msg.deviceCount));
    }
}
