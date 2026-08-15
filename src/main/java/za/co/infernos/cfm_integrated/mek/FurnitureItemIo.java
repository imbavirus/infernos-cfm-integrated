package za.co.infernos.cfm_integrated.mek;

import com.mrcrayfish.furniture.refurbished.core.ModBlockEntities;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import za.co.infernos.cfm_integrated.CfmIntegrated;

/**
 * Furniture storage is a Container but only freezer/stove/recycle bin expose ItemHandler.
 * This fills that gap so Mek logistical transporters (and any FE-pipe cousin) can insert/extract.
 */
public final class FurnitureItemIo {
    private FurnitureItemIo() {}

    public static void register(RegisterCapabilitiesEvent event) {
        registerContainer(event, ModBlockEntities.FRIDGE.get());
        registerContainer(event, ModBlockEntities.DRAWER.get());
        registerContainer(event, ModBlockEntities.CRATE.get());
        registerContainer(event, ModBlockEntities.KITCHEN_DRAWER.get());
        registerContainer(event, ModBlockEntities.STORAGE_CABINET.get());
        registerContainer(event, ModBlockEntities.COOLER.get());
        registerContainer(event, ModBlockEntities.MICROWAVE.get());
        registerContainer(event, ModBlockEntities.TOASTER.get());
        registerContainer(event, ModBlockEntities.MAIL_BOX.get());
        registerContainer(event, ModBlockEntities.STORAGE_JAR.get());
        CfmIntegrated.LOGGER.info("Registered ItemHandler on furniture storage BEs");
    }

    private static <T extends BlockEntity> void registerContainer(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> {
            if (be instanceof WorldlyContainer worldly && side != null) {
                return new SidedInvWrapper(worldly, side);
            }
            if (be instanceof Container container) {
                return new InvWrapper(container);
            }
            return null;
        });
    }
}
