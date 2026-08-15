package za.co.infernos.cfm_integrated.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.blockentity.MekHotplateBlockEntity;
import za.co.infernos.cfm_integrated.blockentity.MekPowerAdaptorBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CfmIntegrated.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MekPowerAdaptorBlockEntity>> MEK_POWER_ADAPTOR =
            BLOCK_ENTITIES.register("mek_power_adaptor", () ->
                    BlockEntityType.Builder.of(MekPowerAdaptorBlockEntity::new, ModBlocks.MEK_POWER_ADAPTOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MekHotplateBlockEntity>> MEK_HOTPLATE =
            BLOCK_ENTITIES.register("mek_hotplate", () ->
                    BlockEntityType.Builder.of(MekHotplateBlockEntity::new, ModBlocks.MEK_HOTPLATE.get()).build(null));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
