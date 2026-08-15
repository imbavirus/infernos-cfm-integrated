package za.co.infernos.cfm_integrated.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import za.co.infernos.cfm_integrated.CfmIntegrated;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CfmIntegrated.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cfm_integrated"))
                    .icon(() -> new ItemStack(ModItems.MEK_POWER_ADAPTOR.get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.MEK_POWER_ADAPTOR.get());
                        out.accept(ModItems.MEK_HOTPLATE.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
