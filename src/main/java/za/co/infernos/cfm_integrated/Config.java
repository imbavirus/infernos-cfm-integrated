package za.co.infernos.cfm_integrated;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue ADAPTOR_CAPACITY = BUILDER
            .comment("FE buffer on the Mek power adaptor")
            .defineInRange("adaptorCapacity", 200_000, 1000, 100_000_000);

    public static final ModConfigSpec.IntValue ADAPTOR_FE_PER_TICK = BUILDER
            .comment("FE consumed per tick while the adaptor is powering a furniture network")
            .defineInRange("adaptorFePerTick", 40, 1, 100_000);

    public static final ModConfigSpec.IntValue ADAPTOR_PULL_PER_TICK = BUILDER
            .comment("Max FE pulled from adjacent cables/machines per tick")
            .defineInRange("adaptorPullPerTick", 2000, 1, 1_000_000);

    public static final ModConfigSpec.IntValue HOTPLATE_CAPACITY = BUILDER
            .comment("FE buffer on the Mek hotplate")
            .defineInRange("hotplateCapacity", 20_000, 100, 10_000_000);

    public static final ModConfigSpec.IntValue HOTPLATE_FE_PER_TICK = BUILDER
            .comment("FE consumed per tick while the hotplate is heating")
            .defineInRange("hotplateFePerTick", 20, 1, 10_000);

    public static final ModConfigSpec.BooleanValue ITEM_IO = BUILDER
            .comment("Expose ItemHandler on furniture storage (fridge, drawers, crates, cabinets, cooler, microwave, toaster, mailbox, jar)")
            .define("itemIo", true);

    public static final ModConfigSpec.BooleanValue HA_ENABLED = BUILDER
            .comment("Publish furniture Home Control devices to Home Assistant via MQTT discovery")
            .define("haEnabled", false);

    public static final ModConfigSpec.ConfigValue<String> HA_HOST = BUILDER
            .comment("MQTT broker host")
            .define("haHost", "127.0.0.1");

    public static final ModConfigSpec.IntValue HA_PORT = BUILDER
            .comment("MQTT broker port")
            .defineInRange("haPort", 1883, 1, 65535);

    public static final ModConfigSpec.ConfigValue<String> HA_USERNAME = BUILDER
            .comment("MQTT username (empty = anonymous)")
            .define("haUsername", "");

    public static final ModConfigSpec.ConfigValue<String> HA_PASSWORD = BUILDER
            .comment("MQTT password")
            .define("haPassword", "");

    public static final ModConfigSpec.ConfigValue<String> HA_DISCOVERY_PREFIX = BUILDER
            .comment("Home Assistant discovery prefix")
            .define("haDiscoveryPrefix", "homeassistant");

    public static final ModConfigSpec.ConfigValue<String> HA_TOPIC_PREFIX = BUILDER
            .comment("State/command topic prefix")
            .define("haTopicPrefix", "cfm_integrated");

    public static final ModConfigSpec.IntValue HA_PUBLISH_INTERVAL = BUILDER
            .comment("Ticks between MQTT state publishes")
            .defineInRange("haPublishInterval", 40, 10, 1200);

    public static final ModConfigSpec.ConfigValue<String> ARCADE_API_BASE = BUILDER
            .comment("Infernos Arcade API base (no trailing slash)")
            .define("arcadeApiBase", "https://infernos.co.za");

    public static final ModConfigSpec.BooleanValue BRIDGE_ENABLED = BUILDER
            .comment("Opt-in Infernos website bridge. Furniture computers attach to the existing MekAcc server (same URL + token).")
            .define("bridgeEnabled", false);

    public static final ModConfigSpec.ConfigValue<String> BRIDGE_URL = BUILDER
            .comment("Infernos site origin, same as infernos_mekacc bridgeUrl (e.g. https://infernos.co.za)")
            .define("bridgeUrl", "");

    public static final ModConfigSpec.ConfigValue<String> BRIDGE_TOKEN = BUILDER
            .comment("Must match MEKACC_BRIDGE_TOKEN / the MekAcc server token. Furniture computers land on that server.")
            .define("bridgeToken", "");

    public static final ModConfigSpec.IntValue BRIDGE_HEARTBEAT_SECONDS = BUILDER
            .comment("Seconds between furniture-computer heartbeats")
            .defineInRange("bridgeHeartbeatSeconds", 30, 15, 3600);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
