package za.co.infernos.cfm_integrated.link.client;

import net.minecraft.core.BlockPos;

public final class ClientLinkState {
    private static BlockPos pos;
    private static String pairingCode = "";
    private static int deviceCount;

    private ClientLinkState() {}

    public static void apply(BlockPos blockPos, String code, int devices) {
        pos = blockPos.immutable();
        pairingCode = code == null ? "" : code;
        deviceCount = devices;
    }

    public static String pairingCode(BlockPos at) {
        if (pos != null && pos.equals(at)) {
            return pairingCode;
        }
        return "";
    }

    public static int deviceCount(BlockPos at) {
        if (pos != null && pos.equals(at)) {
            return deviceCount;
        }
        return 0;
    }
}
