package za.co.infernos.cfm_integrated.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.network.payload.LinkStateS2C;

@EventBusSubscriber(modid = CfmIntegrated.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar(CfmIntegrated.MOD_ID).versioned("1");
        reg.playToClient(LinkStateS2C.TYPE, LinkStateS2C.STREAM_CODEC, LinkStateS2C::handle);
    }
}
