package za.co.infernos.cfm_integrated.cc;

import dan200.computercraft.api.lua.IComputerSystem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaFunction;
import za.co.infernos.cfm_integrated.arcade.ArcadeCatalog;

import java.util.List;
import java.util.Map;

/**
 * Global {@code furniture} table on computers — catalog + version, no wrap required.
 */
public final class FurnitureApi implements ILuaAPI {
    public FurnitureApi(IComputerSystem computer) {
        // catalog is static; computer unused
    }
    @Override
    public String[] getNames() {
        return new String[]{"furniture"};
    }

    @LuaFunction
    public final String version() {
        return "1.0.0";
    }

    @LuaFunction
    public final List<Map<String, String>> arcade() {
        return ArcadeCatalog.luaRows();
    }
}
