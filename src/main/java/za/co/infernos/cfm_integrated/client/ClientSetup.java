package za.co.infernos.cfm_integrated.client;

import com.mrcrayfish.furniture.refurbished.computer.Display;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.arcade.InfernosArcadeProgram;
import za.co.infernos.cfm_integrated.arcade.client.ArcadeGraphics;
import za.co.infernos.cfm_integrated.link.InfernosLinkProgram;
import za.co.infernos.cfm_integrated.link.client.LinkGraphics;

@EventBusSubscriber(modid = CfmIntegrated.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            Display.get().bind(InfernosArcadeProgram.class, ArcadeGraphics::new);
            Display.get().bind(InfernosLinkProgram.class, LinkGraphics::new);
        });
    }
}
