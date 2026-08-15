package za.co.infernos.cfm_integrated.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.block.MekHotplateBlock;
import za.co.infernos.cfm_integrated.block.MekPowerAdaptorBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CfmIntegrated.MOD_ID);

    public static final DeferredBlock<MekPowerAdaptorBlock> MEK_POWER_ADAPTOR = BLOCKS.register(
            "mek_power_adaptor",
            () -> new MekPowerAdaptorBlock(machine("steel"))
    );

    public static final DeferredBlock<MekHotplateBlock> MEK_HOTPLATE = BLOCKS.register(
            "mek_hotplate",
            () -> new MekHotplateBlock(machine("heat"))
    );

    private ModBlocks() {}

    private static BlockBehaviour.Properties machine(String kind) {
        MapColor color = "heat".equals(kind) ? MapColor.COLOR_ORANGE : MapColor.METAL;
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
