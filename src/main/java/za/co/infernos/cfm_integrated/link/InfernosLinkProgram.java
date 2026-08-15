package za.co.infernos.cfm_integrated.link;

import com.mrcrayfish.furniture.refurbished.blockentity.ComputerBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IComputer;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.computer.Program;
import com.mrcrayfish.furniture.refurbished.electricity.IElectricityNode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import za.co.infernos.cfm_integrated.bridge.ComputerDirectory;
import za.co.infernos.cfm_integrated.bridge.FurnitureDevices;
import za.co.infernos.cfm_integrated.network.payload.LinkStateS2C;

public class InfernosLinkProgram extends Program {
    private int tick;

    public InfernosLinkProgram(ResourceLocation id, IComputer computer) {
        super(id, computer);
    }

    @Override
    public void tick() {
        this.tick++;
        if (this.tick % 20 != 0) {
            return;
        }
        Player user = this.computer.getUser();
        if (!(user instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = this.computer.getComputerPos();
        if (!(level.getBlockEntity(pos) instanceof ComputerBlockEntity computerBe)) {
            return;
        }
        ComputerDirectory.Entry entry = ComputerDirectory.get().discover(level, pos);
        int devices = FurnitureDevices.list(computerBe).size();
        PacketDistributor.sendToPlayer(
                player,
                new LinkStateS2C(pos, ComputerDirectory.formatDisplay(entry.pairingCode()), devices)
        );
    }
}
