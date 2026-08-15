package za.co.infernos.cfm_integrated.arcade.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import za.co.infernos.cfm_integrated.CfmIntegrated;

/**
 * Fullscreen Chromium (MCEF) — same path as {@code bluemap_viewer.BasicBrowser}.
 */
public class McefBrowserScreen extends Screen {
    private static final int DRAW_OFFSET = 0;

    private final String url;
    private final Screen parent;
    private MCEFBrowser browser;

    public McefBrowserScreen(Component title, String url) {
        super(title);
        this.url = url;
        this.parent = Minecraft.getInstance().screen;
    }

    @Override
    protected void init() {
        super.init();
        if (this.browser == null) {
            if (!MCEF.isInitialized()) {
                CfmIntegrated.LOGGER.warn("MCEF is not initialized; cannot open {}", this.url);
                this.onClose();
                return;
            }
            this.browser = MCEF.createBrowser(this.url, false);
            this.resizeBrowser();
        }
    }

    private int scaleX(double value) {
        return (int) (value * this.minecraft.getWindow().getGuiScale());
    }

    private int scaleY(double value) {
        return (int) (value * this.minecraft.getWindow().getGuiScale());
    }

    private void resizeBrowser() {
        if (this.browser != null) {
            this.browser.resize(this.scaleX(this.width), this.scaleY(this.height));
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        this.resizeBrowser();
    }

    @Override
    public void onClose() {
        if (this.browser != null) {
            this.browser.close();
            this.browser = null;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.browser == null) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, this.browser.getRenderer().getTextureID());
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(DRAW_OFFSET, this.height - DRAW_OFFSET, 0).setUv(0, 1).setColor(255, 255, 255, 255);
        buffer.addVertex(this.width - DRAW_OFFSET, this.height - DRAW_OFFSET, 0).setUv(1, 1).setColor(255, 255, 255, 255);
        buffer.addVertex(this.width - DRAW_OFFSET, DRAW_OFFSET, 0).setUv(1, 0).setColor(255, 255, 255, 255);
        buffer.addVertex(DRAW_OFFSET, DRAW_OFFSET, 0).setUv(0, 0).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
    }

    private int mouseX(double x) {
        return this.scaleX(x);
    }

    private int mouseY(double y) {
        return this.scaleY(y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.browser != null) {
            this.browser.sendMousePress(this.mouseX(mouseX), this.mouseY(mouseY), button);
            this.browser.setFocus(true);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.browser != null) {
            this.browser.sendMouseRelease(this.mouseX(mouseX), this.mouseY(mouseY), button);
            this.browser.setFocus(true);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.browser != null) {
            this.browser.sendMouseMove(this.mouseX(mouseX), this.mouseY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.browser != null) {
            this.browser.sendMouseMove(this.mouseX(mouseX), this.mouseY(mouseY));
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.browser != null) {
            this.browser.sendMouseWheel(this.mouseX(mouseX), this.mouseY(mouseY), scrollY, 0);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.browser != null) {
            this.browser.sendKeyPress(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (this.browser != null) {
            this.browser.sendKeyRelease(keyCode, scanCode, modifiers);
            this.browser.setFocus(true);
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.browser != null) {
            this.browser.sendKeyTyped(codePoint, modifiers);
            this.browser.setFocus(true);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
