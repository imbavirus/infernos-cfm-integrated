package za.co.infernos.cfm_integrated.cc;

import com.mrcrayfish.furniture.refurbished.blockentity.ElectricityGeneratorBlockEntity;
import com.mrcrayfish.furniture.refurbished.blockentity.IHomeControlDevice;
import com.mrcrayfish.furniture.refurbished.blockentity.MailboxBlockEntity;
import com.mrcrayfish.furniture.refurbished.core.ModBlockEntities;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import za.co.infernos.cfm_integrated.CfmIntegrated;
import za.co.infernos.cfm_integrated.blockentity.MekPowerAdaptorBlockEntity;

public final class CCCompat {
    private CCCompat() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        register(event, ModBlockEntities.LIGHTSWITCH.get());
        register(event, ModBlockEntities.MICROWAVE.get());
        register(event, ModBlockEntities.FREEZER.get());
        register(event, ModBlockEntities.STOVE.get());
        register(event, ModBlockEntities.RECYCLE_BIN.get());
        register(event, ModBlockEntities.ELECTRICITY_GENERATOR.get());
        register(event, ModBlockEntities.MAIL_BOX.get());
        register(event, ModBlockEntities.TOASTER.get());
        register(event, za.co.infernos.cfm_integrated.registry.ModBlockEntities.MEK_POWER_ADAPTOR.get());
        CfmIntegrated.LOGGER.info("Registered furniture CC peripherals");
    }

    private static <T extends BlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(PeripheralCapability.get(), type, (be, side) -> peripheralFor(be));
    }

    private static IPeripheral peripheralFor(BlockEntity be) {
        if (be instanceof ElectricityGeneratorBlockEntity
                || be instanceof MekPowerAdaptorBlockEntity
                || be instanceof IHomeControlDevice
                || be instanceof MailboxBlockEntity) {
            return new FurniturePeripheral(be);
        }
        return null;
    }

    public static void registerApis() {
        ComputerCraftAPI.registerAPIFactory(FurnitureApi::new);
        CfmIntegrated.LOGGER.info("Registered global furniture Lua API");
    }
}
