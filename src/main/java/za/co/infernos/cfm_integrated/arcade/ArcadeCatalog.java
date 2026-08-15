package za.co.infernos.cfm_integrated.arcade;

import za.co.infernos.cfm_integrated.Config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArcadeCatalog {
    public record Game(String slug, String name, String blurb) {}

    public static final List<Game> GAMES = List.of(
            new Game("brickfall", "Brickfall", "Clear the wall. Don't drop the brick."),
            new Game("cubewrought", "Cubewrought", "Voxel sandbox. Punch wood."),
            new Game("snake", "Snake", "Eat. Grow. Don't bite yourself."),
            new Game("2048", "2048", "Slide tiles. Hit 2048."),
            new Game("minesweeper", "Minesweeper", "Flag the bombs."),
            new Game("solitaire", "Solitaire", "Klondike. Clear the tableau."),
            new Game("freecell", "Freecell", "Four free cells. No luck required."),
            new Game("chess", "Chess", "Over the board."),
            new Game("sudoku", "Sudoku", "Nine by nine."),
            new Game("paddle-duel", "Paddle Duel", "Two paddles. One ball."),
            new Game("pellet-chase", "Pellet Chase", "Eat dots. Avoid ghosts."),
            new Game("space-shooter", "Space Shooter", "Waves from above."),
            new Game("flap-pipe", "Flap Pipe", "Tap to clear the pipes."),
            new Game("lights-out", "Lights Out", "Toggle until dark."),
            new Game("whack-a-mole", "Whack-a-Mole", "Hit the heads."),
            new Game("threes", "Threes", "Slide 1s and 2s into 3s."),
            new Game("color-switch", "Color Switch", "Match the gate.")
    );

    private ArcadeCatalog() {}

    public static String baseUrl() {
        String base = Config.ARCADE_API_BASE.get();
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    public static String hubUrl() {
        return baseUrl() + "/arcade";
    }

    public static String gameUrl(String slug) {
        return baseUrl() + "/games/" + slug;
    }

    public static List<Map<String, String>> luaRows() {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Game game : GAMES) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("slug", game.slug());
            row.put("name", game.name());
            row.put("blurb", game.blurb());
            row.put("url", gameUrl(game.slug()));
            rows.add(row);
        }
        return rows;
    }

    public static String leaderboardUrl(String slug) {
        return baseUrl() + "/api/games/v1/games/" + slug + "/leaderboard?playerCount=1&limit=5";
    }
}
