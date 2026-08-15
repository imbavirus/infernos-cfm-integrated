package za.co.infernos.cfm_integrated.arcade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import za.co.infernos.cfm_integrated.CfmIntegrated;

/**
 * Opens Infernos Arcade in CinemaMod MCEF — the same Chromium embed BlueMap Viewer uses.
 * MCEF classes are loaded only after {@code mcef} is confirmed present.
 */
public final class ArcadeBrowser {
    private ArcadeBrowser() {}

    public static boolean available() {
        return ModList.get().isLoaded("mcef");
    }

    public static void open(String title, String url) {
        Minecraft mc = Minecraft.getInstance();
        if (!available()) {
            SystemToast.addOrUpdate(
                    mc.getToasts(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("Infernos Arcade"),
                    Component.literal("Install MCEF (Chrome) to load games")
            );
            return;
        }
        try {
            Class<?> cls = Class.forName("za.co.infernos.cfm_integrated.arcade.client.McefBrowserScreen");
            Screen screen = (Screen) cls.getConstructor(Component.class, String.class)
                    .newInstance(Component.literal(title), url);
            mc.setScreen(screen);
        } catch (ReflectiveOperationException e) {
            CfmIntegrated.LOGGER.error("Failed to open MCEF arcade browser for {}", url, e);
            SystemToast.addOrUpdate(
                    mc.getToasts(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("Infernos Arcade"),
                    Component.literal("Chrome failed to open")
            );
        }
    }
}
