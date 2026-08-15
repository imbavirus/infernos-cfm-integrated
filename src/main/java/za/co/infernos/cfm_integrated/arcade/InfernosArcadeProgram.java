package za.co.infernos.cfm_integrated.arcade;

import com.mrcrayfish.furniture.refurbished.blockentity.IComputer;
import com.mrcrayfish.furniture.refurbished.computer.Program;
import net.minecraft.resources.ResourceLocation;

public class InfernosArcadeProgram extends Program {
    private int score;
    private boolean playing;

    public InfernosArcadeProgram(ResourceLocation id, IComputer computer) {
        super(id, computer);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int amount) {
        this.score += amount;
    }

    public void resetScore() {
        this.score = 0;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }
}
