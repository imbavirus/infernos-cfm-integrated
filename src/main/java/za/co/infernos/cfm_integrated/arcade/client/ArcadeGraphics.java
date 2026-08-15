package za.co.infernos.cfm_integrated.arcade.client;

import com.mrcrayfish.furniture.refurbished.computer.client.DisplayableProgram;
import com.mrcrayfish.furniture.refurbished.computer.client.Scene;
import com.mrcrayfish.furniture.refurbished.computer.client.widget.ComputerButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import za.co.infernos.cfm_integrated.arcade.ArcadeCatalog;
import za.co.infernos.cfm_integrated.arcade.InfernosArcadeProgram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ArcadeGraphics extends DisplayableProgram<InfernosArcadeProgram> {
    public ArcadeGraphics(InfernosArcadeProgram program) {
        super(program, 200, 100);
        this.setWindowTitleBarColour(0xFFE85C1C);
        this.setWindowTitleLabelColour(0xFF1A120E);
        this.setWindowBackgroundColour(0xFF161418);
        this.setScene(new Catalog(this));
    }

    public static final class Catalog extends Scene {
        private static final int PAGE = 3;

        private final ArcadeGraphics graphics;
        private final ComputerButton prev;
        private final ComputerButton next;
        private final ComputerButton hub;
        private final ComputerButton brickfall;
        private final List<ComputerButton> rows = new ArrayList<>();
        private int offset;

        public Catalog(ArcadeGraphics graphics) {
            this.graphics = graphics;
            this.prev = this.addWidget(new ComputerButton(18, 12, Component.literal("<"), btn -> {
                this.offset = Math.max(0, this.offset - PAGE);
                this.refreshLabels();
            }));
            this.next = this.addWidget(new ComputerButton(18, 12, Component.literal(">"), btn -> {
                this.offset = Math.min(maxOffset(), this.offset + PAGE);
                this.refreshLabels();
            }));
            this.hub = this.addWidget(new ComputerButton(52, 12, Component.literal("Hub"), btn ->
                    ArcadeBrowser.open("Infernos Arcade", ArcadeCatalog.hubUrl())));
            this.brickfall = this.addWidget(new ComputerButton(70, 12, Component.literal("Play Brickfall"), btn -> {
                graphics.getProgram().resetScore();
                graphics.getProgram().setPlaying(true);
                graphics.setScene(new Brickfall(graphics));
            }));
            for (int i = 0; i < PAGE; i++) {
                final int slot = i;
                ComputerButton row = this.addWidget(new ComputerButton(192, 14, Component.empty(), btn -> {
                    int index = this.offset + slot;
                    if (index < ArcadeCatalog.GAMES.size()) {
                        ArcadeCatalog.Game game = ArcadeCatalog.GAMES.get(index);
                        ArcadeBrowser.open(game.name(), ArcadeCatalog.gameUrl(game.slug()));
                    }
                }));
                this.rows.add(row);
            }
            this.refreshLabels();
        }

        private static int maxOffset() {
            return Math.max(0, ArcadeCatalog.GAMES.size() - PAGE);
        }

        private void refreshLabels() {
            for (int i = 0; i < PAGE; i++) {
                int index = this.offset + i;
                ComputerButton row = this.rows.get(i);
                if (index < ArcadeCatalog.GAMES.size()) {
                    row.setMessage(Component.literal(ArcadeCatalog.GAMES.get(index).name()));
                    row.active = true;
                    row.visible = true;
                } else {
                    row.setMessage(Component.empty());
                    row.active = false;
                    row.visible = false;
                }
            }
        }

        @Override
        public void updateWidgets(int contentStart, int contentTop) {
            this.prev.setPosition(contentStart + 4, contentTop + 2);
            this.next.setPosition(contentStart + 24, contentTop + 2);
            this.hub.setPosition(contentStart + 46, contentTop + 2);
            this.brickfall.setPosition(contentStart + 100, contentTop + 2);
            for (int i = 0; i < this.rows.size(); i++) {
                this.rows.get(i).setPosition(contentStart + 4, contentTop + 18 + i * 16);
            }
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, this.graphics.getWidth(), 16, 0xFFE85C1C);
            g.drawString(Minecraft.getInstance().font, "INFERNOS ARCADE", 120, 4, 0xFF1A120E, false);
            String footer = ArcadeBrowser.available() ? "MCEF Chrome  infernos.co.za/arcade" : "Need MCEF (Chrome)  infernos.co.za/arcade";
            g.drawString(Minecraft.getInstance().font, footer, 6, 90, 0xFF8A7060, false);
        }
    }

    public static final class Brickfall extends Scene {
        private static final int COLS = 8;
        private static final int ROWS = 10;
        private static final int[][] SHAPES = {
                {0, 1, 2, 3},
                {0, 1, 4, 5},
                {1, 4, 5, 8},
                {0, 4, 5, 9},
                {1, 4, 5, 6}
        };

        private final ArcadeGraphics graphics;
        private final int[] grid = new int[COLS * ROWS];
        private int px = 3;
        private int py = 0;
        private int shape = 1;
        private int tick;
        private boolean over;
        private final Random rng = new Random();

        public Brickfall(ArcadeGraphics graphics) {
            this.graphics = graphics;
            this.spawn();
            this.addWidget(new ComputerButton(22, 12, Component.literal("<"), btn -> this.move(-1)));
            this.addWidget(new ComputerButton(22, 12, Component.literal(">"), btn -> this.move(1)));
            this.addWidget(new ComputerButton(28, 12, Component.literal("ROT"), btn -> this.rotate()));
            this.addWidget(new ComputerButton(28, 12, Component.literal("DROP"), btn -> this.drop()));
            this.addWidget(new ComputerButton(28, 12, Component.literal("BACK"), btn -> {
                graphics.getProgram().setPlaying(false);
                graphics.setScene(new Catalog(graphics));
            }));
        }

        @Override
        public void updateWidgets(int contentStart, int contentTop) {
            List<? extends net.minecraft.client.gui.components.events.GuiEventListener> widgets = this.getWidgets();
            int x = contentStart + 118;
            int y = contentTop + 20;
            for (int i = 0; i < widgets.size(); i++) {
                if (widgets.get(i) instanceof ComputerButton btn) {
                    btn.setPosition(x, y + i * 14);
                }
            }
        }

        @Override
        public void tick() {
            if (this.over) {
                return;
            }
            this.tick++;
            if (this.tick % 12 == 0) {
                this.step();
            }
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, this.graphics.getWidth(), 14, 0xFFE85C1C);
            String title = this.over ? "GAME OVER  " + this.graphics.getProgram().getScore() : "BRICKFALL  " + this.graphics.getProgram().getScore();
            g.drawString(Minecraft.getInstance().font, title, 6, 3, 0xFF1A120E, false);
            int cell = 8;
            for (int y = 0; y < ROWS; y++) {
                for (int x = 0; x < COLS; x++) {
                    int v = this.grid[y * COLS + x];
                    int color = v == 0 ? 0xFF1E1A18 : 0xFFE85C1C;
                    g.fill(6 + x * cell, 18 + y * cell, 6 + x * cell + cell - 1, 18 + y * cell + cell - 1, color);
                }
            }
            if (!this.over) {
                for (int idx : SHAPES[this.shape]) {
                    int sx = this.px + (idx % 4);
                    int sy = this.py + (idx / 4);
                    if (sy >= 0 && sy < ROWS && sx >= 0 && sx < COLS) {
                        g.fill(6 + sx * cell, 18 + sy * cell, 6 + sx * cell + cell - 1, 18 + sy * cell + cell - 1, 0xFFFFC040);
                    }
                }
            }
        }

        private void spawn() {
            this.px = 2;
            this.py = 0;
            this.shape = this.rng.nextInt(SHAPES.length);
            if (this.collides(this.px, this.py, this.shape)) {
                this.over = true;
            }
        }

        private boolean collides(int x, int y, int sh) {
            for (int idx : SHAPES[sh]) {
                int sx = x + (idx % 4);
                int sy = y + (idx / 4);
                if (sx < 0 || sx >= COLS || sy >= ROWS) {
                    return true;
                }
                if (sy >= 0 && this.grid[sy * COLS + sx] != 0) {
                    return true;
                }
            }
            return false;
        }

        private void move(int dx) {
            if (!this.over && !this.collides(this.px + dx, this.py, this.shape)) {
                this.px += dx;
            }
        }

        private void rotate() {
            if (this.over) {
                return;
            }
            int next = (this.shape + 1) % SHAPES.length;
            if (!this.collides(this.px, this.py, next)) {
                this.shape = next;
            }
        }

        private void drop() {
            if (this.over) {
                return;
            }
            while (!this.collides(this.px, this.py + 1, this.shape)) {
                this.py++;
            }
            this.lock();
        }

        private void step() {
            if (!this.collides(this.px, this.py + 1, this.shape)) {
                this.py++;
            } else {
                this.lock();
            }
        }

        private void lock() {
            for (int idx : SHAPES[this.shape]) {
                int sx = this.px + (idx % 4);
                int sy = this.py + (idx / 4);
                if (sy >= 0 && sy < ROWS && sx >= 0 && sx < COLS) {
                    this.grid[sy * COLS + sx] = 1;
                }
            }
            this.clearLines();
            this.spawn();
        }

        private void clearLines() {
            for (int y = ROWS - 1; y >= 0; y--) {
                boolean full = true;
                for (int x = 0; x < COLS; x++) {
                    if (this.grid[y * COLS + x] == 0) {
                        full = false;
                        break;
                    }
                }
                if (full) {
                    System.arraycopy(this.grid, 0, this.grid, COLS, y * COLS);
                    Arrays.fill(this.grid, 0, COLS, 0);
                    this.graphics.getProgram().addScore(100);
                    y++;
                }
            }
        }
    }
}
