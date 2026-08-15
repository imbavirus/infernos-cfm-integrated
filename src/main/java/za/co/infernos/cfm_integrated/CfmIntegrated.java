package za.co.infernos.cfm_integrated;

import com.mojang.logging.LogUtils;
import com.mrcrayfish.furniture.refurbished.computer.Computer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import za.co.infernos.cfm_integrated.arcade.InfernosArcadeProgram;
import za.co.infernos.cfm_integrated.bridge.FurnitureBridge;
import za.co.infernos.cfm_integrated.ha.HomeAssistantBridge;
import za.co.infernos.cfm_integrated.link.InfernosLinkProgram;
import za.co.infernos.cfm_integrated.mek.FurnitureItemIo;
import za.co.infernos.cfm_integrated.registry.ModBlockEntities;
import za.co.infernos.cfm_integrated.registry.ModBlocks;
import za.co.infernos.cfm_integrated.registry.ModCreativeTabs;
import za.co.infernos.cfm_integrated.registry.ModItems;

@Mod(CfmIntegrated.MOD_ID)
public final class CfmIntegrated {
    public static final String MOD_ID = "cfm_integrated";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CfmIntegrated(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);

        NeoForge.EVENT_BUS.register(HomeAssistantBridge.class);
        NeoForge.EVENT_BUS.register(FurnitureBridge.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Computer.get().installProgram(id("infernos_arcade"), InfernosArcadeProgram::new);
            Computer.get().installProgram(id("infernos_link"), InfernosLinkProgram::new);
            LOGGER.info("Arcade + Link programs installed on furniture computers");
        });
        if (ModList.get().isLoaded("computercraft")) {
            event.enqueueWork(() -> {
                try {
                    Class.forName("za.co.infernos.cfm_integrated.cc.CCCompat")
                            .getMethod("registerApis")
                            .invoke(null);
                } catch (ReflectiveOperationException e) {
                    LOGGER.error("Failed to register CC APIs", e);
                }
            });
        }
        LOGGER.info("MrCrayfish Furniture Integrated loaded");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        FurnitureItemIo.register(event);
        za.co.infernos.cfm_integrated.registry.ModCapabilities.register(event);
        if (ModList.get().isLoaded("computercraft")) {
            try {
                Class.forName("za.co.infernos.cfm_integrated.cc.CCCompat")
                        .getMethod("registerCapabilities", RegisterCapabilitiesEvent.class)
                        .invoke(null, event);
            } catch (ReflectiveOperationException e) {
                LOGGER.error("Failed to register CC peripherals", e);
            }
        }
    }
}
