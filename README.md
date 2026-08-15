# MrCrayfish Furniture Integrated

First-party Infernos bridge for **MrCrayfish's Furniture Mod: Refurbished** on NeoForge 1.21.1.

| Surface | What it does |
|---------|----------------|
| **Mekanism / pipes** | ItemHandler on fridge, drawers, crates, cabinets, cooler, microwave, toaster, mailbox, jar. `mek_power_adaptor` is an `ISourceNode` that burns FE (Mek cables included). `mek_hotplate` is an `IHeatingSource` for the frying pan. |
| **CC:Tweaked / MekaCC** | Peripherals on switches, appliances, generator, mailbox, adaptor. Global Lua `furniture.arcade()`. |
| **Home Assistant** | MQTT discovery + state + command for `IHomeControlDevice`. Off by default (`haEnabled`). |
| **Infernos Arcade** | Furniture computer program: catalog + local Brickfall. Hub / game rows open infernos.co.za/arcade in **MCEF** (same Chromium as BlueMap Viewer). |
| **Infernos Link** | Pairing code on the furniture computer. Claim/share on the website. Computers attach to the existing **MekAcc server** (same bridge token). |

## IDs

- Mod id: `cfm_integrated`
- Display: MrCrayfish Furniture Integrated
- Required: `refurbished_furniture` 1.0.22+, `framework` 0.13.10+
- Optional: Mekanism, CC:Tweaked, **MCEF** (`mcef`) for Arcade Chrome

## Config (`cfm_integrated-common.toml`)

- `itemIo` — furniture storage pipes
- `adaptorFePerTick` / `hotplateFePerTick`
- `haEnabled`, `haHost`, `haPort`, `haUsername`, `haPassword`
- `arcadeApiBase`

## License

Code: All Rights Reserved (Infernos). Do not copy Refurbished assets (ARR). Compile against the official jars.
