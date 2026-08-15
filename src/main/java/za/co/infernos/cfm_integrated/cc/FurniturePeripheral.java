package za.co.infernos.cfm_integrated.cc;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.IPowerSwitch;
import com.mrcrayfish.furniture.refurbished.blockentity.IProcessingBlock;
import com.mrcrayfish.furniture.refurbished.blockentity.MailboxBlockEntity;
import com.mrcrayfish.furniture.refurbished.electricity.IElectricityNode;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import za.co.infernos.cfm_integrated.arcade.ArcadeCatalog;
import za.co.infernos.cfm_integrated.blockentity.MekPowerAdaptorBlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FurniturePeripheral implements IPeripheral {
    private final BlockEntity be;

    public FurniturePeripheral(BlockEntity be) {
        this.be = be;
    }

    @NotNull
    @Override
    public String getType() {
        if (be instanceof ElectricityGeneratorBlockEntity || be instanceof MekPowerAdaptorBlockEntity) {
            return "furniture_generator";
        }
        if (be instanceof MailboxBlockEntity) {
            return "furniture_mail";
        }
        if (be instanceof IProcessingBlock) {
            return "furniture_appliance";
        }
        return "furniture_switch";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof FurniturePeripheral o && o.be == this.be;
    }

    @LuaFunction
    public final String getBlockId() {
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
    }

    @LuaFunction
    public final Map<String, Integer> getPos() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("x", be.getBlockPos().getX());
        m.put("y", be.getBlockPos().getY());
        m.put("z", be.getBlockPos().getZ());
        return m;
    }

    @LuaFunction
    public final boolean isPowered() {
        return be instanceof IElectricityNode node && node.isNodePowered();
    }

    @LuaFunction
    public final boolean isEnabled() {
        return be instanceof IHomeControlDevice device && device.isDeviceEnabled();
    }

    @LuaFunction
    public final void setEnabled(boolean enabled) {
        if (be instanceof IHomeControlDevice device) {
            device.setDeviceState(enabled);
        }
    }

    @LuaFunction
    public final void toggle() {
        if (be instanceof IHomeControlDevice device) {
            device.toggleDeviceState();
        } else if (be instanceof IPowerSwitch sw) {
            sw.togglePower();
        }
    }

    @LuaFunction
    public final String getName() {
        if (be instanceof IHomeControlDevice device) {
            return device.getDeviceName().getString();
        }
        return be.getBlockState().getBlock().getName().getString();
    }

    @LuaFunction
    public final int getEnergy() {
        if (be instanceof MekPowerAdaptorBlockEntity adaptor) {
            return adaptor.getStoredEnergy();
        }
        if (be instanceof IProcessingBlock processing) {
            return processing.getEnergy();
        }
        return 0;
    }

    @LuaFunction
    public final int getProgress() {
        if (be instanceof IProcessingBlock processing) {
            int total = processing.getTotalProcessingTime();
            if (total <= 0) {
                return 0;
            }
            return processing.getProcessingTime();
        }
        return 0;
    }

    @LuaFunction
    public final boolean canProcess() {
        return be instanceof IProcessingBlock processing && processing.canProcess();
    }

    @LuaFunction
    public final List<Map<String, Object>> listItems() {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!(be instanceof Container container)) {
            return out;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("slot", i + 1);
            row.put("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            row.put("count", stack.getCount());
            out.add(row);
        }
        return out;
    }

    @LuaFunction
    public final List<Map<String, String>> arcadeCatalog() {
        return ArcadeCatalog.luaRows();
    }
}
