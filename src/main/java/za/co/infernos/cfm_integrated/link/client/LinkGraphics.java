package za.co.infernos.cfm_integrated.link.client;

import com.mrcrayfish.furniture.refurbished.computer.client.DisplayableProgram;
import com.mrcrayfish.furniture.refurbished.computer.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import za.co.infernos.cfm_integrated.link.InfernosLinkProgram;

public class LinkGraphics extends DisplayableProgram<InfernosLinkProgram> {
    public LinkGraphics(InfernosLinkProgram program) {
        super(program, 200, 100);
        this.setWindowTitleBarColour(0xFFE85C1C);
        this.setWindowTitleLabelColour(0xFF1A120E);
        this.setWindowBackgroundColour(0xFF161418);
        this.setScene(new Panel(this));
    }

    public static final class Panel extends Scene {
        private final LinkGraphics graphics;

        public Panel(LinkGraphics graphics) {
            this.graphics = graphics;
        }

        @Override
        public void updateWidgets(int contentStart, int contentTop) {}

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            BlockPos pos = this.graphics.getProgram().getComputer().getComputerPos();
            String code = ClientLinkState.pairingCode(pos);
            int devices = ClientLinkState.deviceCount(pos);
            var font = Minecraft.getInstance().font;
            g.fill(0, 0, this.graphics.getWidth(), 16, 0xFFE85C1C);
            g.drawString(font, "INFERNOS LINK", 8, 4, 0xFF1A120E, false);
            g.drawString(font, "Claim on infernos.co.za", 8, 24, 0xFFE8D8C8, false);
            g.drawString(font, "Dashboard → Furniture", 8, 36, 0xFF8A7060, false);
            g.drawString(font, code.isBlank() ? "Code: …" : "Code: " + code, 8, 56, 0xFFFFC040, false);
            g.drawString(font, devices + " home devices on this PC", 8, 72, 0xFF8A7060, false);
            g.drawString(font, "Owner can share by MC name", 8, 86, 0xFF6A5848, false);
        }
    }
}
