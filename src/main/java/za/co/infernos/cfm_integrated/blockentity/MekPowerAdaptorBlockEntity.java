package za.co.infernos.cfm_integrated.blockentity;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricitySourceBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.IPowerSwitch;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import za.co.infernos.cfm_integrated.Config;
import za.co.infernos.cfm_integrated.block.MekPowerAdaptorBlock;
import za.co.infernos.cfm_integrated.registry.ModBlockEntities;

public class MekPowerAdaptorBlockEntity extends ElectricitySourceBlockEntity implements IPowerSwitch, IHomeControlDevice {
    private boolean enabled = true;
    private final EnergyStorage energy = new EnergyStorage(200_000) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int got = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && got > 0) {
                setChanged();
            }
            return got;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int got = super.extractEnergy(maxExtract, simulate);
            if (!simulate && got > 0) {
                setChanged();
            }
            return got;
        }
    };

    public MekPowerAdaptorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEK_POWER_ADAPTOR.get(), pos, state);
    }

    public IEnergyStorage energyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MekPowerAdaptorBlockEntity be) {
        be.pullAdjacent(level, pos);
        boolean shouldPower = be.enabled && be.energy.getEnergyStored() >= Config.ADAPTOR_FE_PER_TICK.get();
        if (shouldPower) {
            be.energy.extractEnergy(Config.ADAPTOR_FE_PER_TICK.get(), false);
        }
        if (be.isNodePowered() != shouldPower) {
            be.setNodePowered(shouldPower);
        }
    }

    private void pullAdjacent(Level level, BlockPos pos) {
        int room = energy.getMaxEnergyStored() - energy.getEnergyStored();
        if (room <= 0) {
            return;
        }
        int budget = Math.min(room, Config.ADAPTOR_PULL_PER_TICK.get());
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
    public boolean isNodePowered() {
        BlockState state = this.getBlockState();
        return state.hasProperty(MekPowerAdaptorBlock.POWERED) && state.getValue(MekPowerAdaptorBlock.POWERED);
    }

    @Override
    public void setNodePowered(boolean powered) {
        BlockState state = this.getBlockState();
        if (level != null && state.hasProperty(MekPowerAdaptorBlock.POWERED) && state.getValue(MekPowerAdaptorBlock.POWERED) != powered) {
            level.setBlock(this.worldPosition, state.setValue(MekPowerAdaptorBlock.POWERED, powered), Block.UPDATE_ALL);
        }
    }

    @Override
    public void togglePower() {
        this.enabled = !this.enabled;
        this.setChanged();
    }

    @Override
    public BlockPos getDevicePos() {
        return this.worldPosition;
    }

    @Override
    public boolean isDeviceEnabled() {
        return this.enabled;
    }

    @Override
    public void toggleDeviceState() {
        togglePower();
    }

    @Override
    public void setDeviceState(boolean enabled) {
        this.enabled = enabled;
        this.setChanged();
    }

    @Override
    public Component getDeviceName() {
        return Component.translatable("block.cfm_integrated.mek_power_adaptor");
    }

    public int getStoredEnergy() {
        return energy.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energy.getMaxEnergyStored();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        if (tag.contains("Energy")) {
            energy.receiveEnergy(tag.getInt("Energy"), false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("Enabled", this.enabled);
        tag.putInt("Energy", energy.getEnergyStored());
    }
}
