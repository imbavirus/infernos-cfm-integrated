package za.co.infernos.cfm_integrated.blockentity;

import com.mrcrayfish.furniture.refurbished.blockentity.IHeatingSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import za.co.infernos.cfm_integrated.Config;
import za.co.infernos.cfm_integrated.registry.ModBlockEntities;

public class MekHotplateBlockEntity extends BlockEntity implements IHeatingSource {
    private boolean heating;
    private final EnergyStorage energy = new EnergyStorage(20_000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int got = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && got > 0) {
                setChanged();
            }
            return got;
        }
    };

    public MekHotplateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEK_HOTPLATE.get(), pos, state);
    }

    public IEnergyStorage energyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MekHotplateBlockEntity be) {
        be.pullAdjacent(level, pos);
        boolean on = be.energy.getEnergyStored() >= Config.HOTPLATE_FE_PER_TICK.get();
        if (on) {
            be.energy.extractEnergy(Config.HOTPLATE_FE_PER_TICK.get(), false);
        }
        if (be.heating != on) {
            be.heating = on;
            be.setChanged();
        }
    }

    private void pullAdjacent(Level level, BlockPos pos) {
        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (room <= 0) {
            return;
        }
        int budget = Math.min(room, 500);
        for (Direction dir : Direction.values()) {
            if (budget <= 0) {
                break;
            }
            IEnergyStorage other = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (other == null || !other.canExtract()) {
                continue;
            }
            int extracted = other.extractEnergy(budget, false);
            if (extracted > 0) {
                energy.receiveEnergy(extracted, false);
                budget -= extracted;
            }
        }
    }

    @Override
    public boolean isProcessing() {
        return this.heating;
    }

    @Override
    public boolean isHeating() {
        return this.heating;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.heating = tag.getBoolean("Heating");
        if (tag.contains("Energy")) {
            energy.receiveEnergy(tag.getInt("Energy"), false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Heating", this.heating);
        tag.putInt("Energy", energy.getEnergyStored());
    }
}
