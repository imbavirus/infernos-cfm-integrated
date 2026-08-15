package za.co.infernos.cfm_integrated.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import za.co.infernos.cfm_integrated.CfmIntegrated;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CfmIntegrated.MOD_ID);

    public static final DeferredItem<BlockItem> MEK_POWER_ADAPTOR = ITEMS.register(
            "mek_power_adaptor",
            () -> new BlockItem(ModBlocks.MEK_POWER_ADAPTOR.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> MEK_HOTPLATE = ITEMS.register(
            "mek_hotplate",
            () -> new BlockItem(ModBlocks.MEK_HOTPLATE.get(), new Item.Properties())
    );

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
