package za.co.infernos.cfm_integrated.bridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mrcrayfish.furniture.refurbished.Config;
import com.mrcrayfish.furniture.refurbished.blockentity.ComputerBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.electricity.IElectricityNode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FurnitureDevices {
    private FurnitureDevices() {}

    public static List<IHomeControlDevice> list(ComputerBlockEntity computer) {
        int max = Config.SERVER.electricity.maximumNodesInNetwork.get();
        List<IHomeControlDevice> out = new ArrayList<>();
        for (IElectricityNode node : IElectricityNode.searchNodes(
                computer, max, true, n -> true, n -> n instanceof IHomeControlDevice)) {
            if (node instanceof IHomeControlDevice device) {
                out.add(device);
            }
        }
        out.sort(Comparator.comparing(d -> d.getDeviceName().getString()));
        return out;
    }

    public static JsonArray snapshot(ComputerBlockEntity computer) {
        JsonArray arr = new JsonArray();
        for (IHomeControlDevice device : list(computer)) {
            JsonObject row = new JsonObject();
            BlockPos pos = device.getDevicePos();
            row.addProperty("pos", pos.getX() + " " + pos.getY() + " " + pos.getZ());
            row.addProperty("name", device.getDeviceName().getString());
            row.addProperty("enabled", device.isDeviceEnabled());
            arr.add(row);
        }
        return arr;
    }

    public static boolean set(ComputerBlockEntity computer, BlockPos devicePos, boolean enabled) {
        for (IHomeControlDevice device : list(computer)) {
            if (device.getDevicePos().equals(devicePos)) {
                device.setDeviceState(enabled);
                return true;
            }
        }
        return false;
    }
}
