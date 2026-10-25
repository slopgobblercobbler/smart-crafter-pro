# SmartCrafter Pro

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20%2B%20%7C%201.21%2B%20%7C%2026.x-blue.svg)](https://fabricmc.net/)
[![Fabric](https://img.shields.io/badge/Platform-Fabric-lightgrey.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21%20%2F%2025-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/JUnit%205-14%20Passed-brightgreen.svg)]()

**SmartCrafter Pro** is an advanced, high-performance **client-side Fabric mod** for Minecraft that introduces recursive auto-crafting, dependency graph resolution, anti-desync transaction sequencing, and one-click bulk batch compression.

Designed for vanilla servers, SMPs, technical Minecraft redstone builders, and storage room managers, SmartCrafter Pro requires **zero server-side mods or plugins** and operates safely within vanilla container protocol boundaries without triggering anti-cheat or rate-limit kicks.

---

## 🌟 Core Features

### 1. 🌲 Recursive Recipe Tree Graph Resolution
- **Deep Dependency Traversal**: Request any target item (e.g. **64 Pistons**, **128 Observers**, or **64 Firework Rockets**), and SmartCrafter Pro automatically resolves the entire multi-tier recipe tree down to raw materials:
  - `Oak Log` $\rightarrow$ `Oak Planks` $\rightarrow$ `Sticks`
  - `Planks` + `Cobblestone` + `Iron Ingot` + `Redstone` $\rightarrow$ `Pistons`
  - `Sugar Cane` $\rightarrow$ `Paper` + `Gunpowder` $\rightarrow$ `Firework Rockets`
  - `Cobblestone` + `Redstone` + `Nether Quartz` $\rightarrow$ `Observers`
- **Smart Inventory Stock Reuse**: Intelligently inspects the player's current inventory. If you already have 20 planks or 12 sticks, it uses them first instead of over-crafting from raw logs.
- **Deficit & Shortfall Calculation**: Immediately calculates and reports exact missing raw materials (e.g., *"Missing: 32 Oak Logs, 64 Cobblestone"*) before executing any clicks.
- **Topological Execution Order**: Formulates a bottom-up post-order execution plan where lower-tier ingredients are crafted first and transferred to subsequent assembly tiers.

### 2. ⚡ Client-Side Safe Transaction Sequencing (Anti-Desync)
- **Rate-Limit & Anti-Cheat Safe**: Paces atomic container slot interactions with a configurable tick delay (`actionDelayTicks`, default 2 ticks) to avoid triggering server kicks for *Invalid Packet*, *Too Many Clicks*, or inventory desync on paper/spigot/fabric servers (GrimAC, NoCheatPlus, Vulcan compatible).
- **Accurate Container Slot Mapping**: Translates abstract recipe grids into native Minecraft container slot IDs:
  - **3x3 Crafting Table**: Result slot 0, Grid slots 1–9, Main inventory slots 10–36, Hotbar slots 37–45.
  - **2x2 Player Inventory**: Result slot 0, Grid slots 1–4, Main inventory slots 9–35, Hotbar slots 36–44.
- **Cursor Sync & Rollback Protection**: Verifies cursor and slot state before every action. If a crafting screen is closed unexpectedly or interrupted, held items are returned to safe inventory slots with zero item drops.

### 3. 📦 One-Click Bulk Batch Compressor
- **Recursive Compression**: Instantly condenses loose items across the player's inventory into dense blocks:
  - **3-Tier Recursive Compression**: `81 Iron Nuggets` $\rightarrow$ `9 Iron Ingots` $\rightarrow$ `1 Block of Iron` in a single automated pass.
  - **Metals**: Iron, Gold, Copper, Netherite, Raw Iron, Raw Gold, Raw Copper.
  - **Gems & Minerals**: Diamond, Emerald, Redstone, Lapis Lazuli, Coal, Quartz, Amethyst.
  - **Crops & Farmables**: Wheat $\rightarrow$ Hay Bales, Bone Meal $\rightarrow$ Bone Blocks, Bones $\rightarrow$ Bone Meal.
  - **Mob Drops & Misc**: Slimeballs $\rightarrow$ Slime Blocks, Glowstone Dust $\rightarrow$ Glowstone, Clay $\rightarrow$ Clay Blocks.
- **Inventory Space Optimization**: Frees up dozens of inventory slots in seconds.

### 4. 🖥️ Interactive GUI & Actionbar Notifications
- **In-Game Search & Picker**: Fast autocomplete search for recipes.
- **Quantity Selector**: `-1`, `+1`, `+16`, `+64`, and `Max` (calculates maximum craftable amount based on current inventory).
- **Recipe Tree Breakdown**: Displays required base raw materials, intermediate surplus, and feasibility status.
- **Non-Intrusive Feedback**: Real-time progress updates sent to the actionbar overlay to avoid HUD clutter during gameplay.

---

## 🎮 Controls & Keybindings

| Keybinding | Default Key | Description |
|---|---|---|
| **Open SmartCrafter GUI** | `K` | Opens the interactive recipe search and crafting screen |
| **Instant Batch Compress** | `B` | Instantly compresses loose items in player inventory |
| **Emergency Abort** | `X` | Immediately halts active crafting transactions and restores cursor |

*All keybindings can be customized in the vanilla Minecraft Controls menu.*

---

## ⚙️ Configuration (`config/smartcrafter.json`)

```json
{
  "actionDelayTicks": 2,
  "safeMode": true,
  "soundFeedback": true,
  "autoCloseScreenWhenDone": false,
  "defaultCategory": "ALL"
}
```

- `actionDelayTicks`: Delay between container slot clicks in game ticks (default: `2`). Set higher (e.g. `3`-`4`) for high-latency servers.
- `safeMode`: Enforces strict container open state and inventory sync verification before dispatching clicks.
- `soundFeedback`: Plays subtle audio confirmation cues upon crafting completion.
- `defaultCategory`: Filter for 1-click compression (`ALL`, `METALS`, `GEMS`, `MINERALS`, `CROPS`, `MOB_DROPS`).

---

## 🏗️ Architecture & Codebase Structure

```
smart-crafter-pro/
├── .github/workflows/
│   └── build.yml               # GitHub Actions CI workflow (Java 21, test & build)
├── src/main/java/com/smartcrafter/
│   ├── SmartCrafterMod.java    # Fabric client mod entrypoint & orchestrator
│   ├── compat/
│   │   └── ModMenuIntegration.java   # ModMenu screen provider
│   ├── config/
│   │   └── SmartCrafterConfig.java   # JSON configuration persistence
│   ├── engine/
│   │   ├── model/
│   │   │   ├── ItemIdentifier.java   # Immutable item abstraction & category
│   │   │   ├── ItemStackSim.java     # Lightweight simulation item stack
│   │   │   ├── Recipe.java           # Shaped & shapeless recipe definitions
│   │   │   ├── CraftingStep.java     # Scaled execution batch step
│   │   │   └── CraftingPlan.java     # Complete resolved DAG execution plan
│   │   ├── recipe/
│   │   │   ├── RecipeRegistry.java   # Vanilla recipe tree registry & compression matrix
│   │   │   └── RecipeGraphSolver.java# Recursive DAG dependency resolution engine
│   │   ├── inventory/
│   │   │   ├── InventorySnapshot.java# Virtual slot snapshot (36 storage slots + offhand)
│   │   │   └── InventoryDeductionSimulator.java # Pre-execution overflow & deficit validator
│   │   ├── compressor/
│   │   │   ├── CompressionCategory.java # Filter categories (Metals, Gems, Crops...)
│   │   │   └── BatchCompressor.java  # Multi-tier recursive compression planner
│   │   └── transaction/
│   │       ├── SlotActionType.java   # Atomic slot interactions (PICKUP, PLACE_ONE, QUICK_MOVE...)
│   │       ├── TransactionStep.java  # Container slot click packet definition
│   │       ├── TransactionPlanner.java # Translates steps into Minecraft container clicks
│   │       ├── TransactionExecutor.java # Tick-driven asynchronous scheduler
│   │       └── ClientPacketDispatcher.java # MultiPlayerGameMode interaction bridge
│   ├── gui/
│   │   ├── SmartCrafterScreen.java   # Interactive recipe browser & quantity picker
│   │   └── CraftingHudOverlay.java   # Live progress bar overlay
│   └── event/
│       ├── KeybindHandler.java       # Client keybinding registrations & polling
│       └── ClientTickHandler.java    # 20 Hz tick driver for transaction scheduler
├── src/test/java/com/smartcrafter/
│   ├── engine/recipe/
│   │   └── RecipeGraphSolverTest.java # Multi-tier resolution & reuse tests (JUnit 5)
│   ├── engine/compressor/
│   │   └── BatchCompressorTest.java   # Recursive nugget -> ingot -> block tests
│   ├── engine/inventory/
│   │   └── InventoryDeductionSimulatorTest.java # Overflow & deficit simulation tests
│   ├── engine/transaction/
│   │   └── TransactionPlannerTest.java # Slot mapping & tick pacing verification
│   └── test/
│       └── StandaloneTestRunner.java  # Headless test launcher for native builds
├── build_native_mod.py         # Standalone JDK compiler, test runner, and packager
├── build.gradle                # Fabric Loom build configuration
├── gradle.properties           # Version specifications
└── settings.gradle             # Project definition
```

---

## 🧪 Unit Test Suite (JUnit 5)

SmartCrafter Pro includes 14 unit tests covering core algorithmic mechanics:

1. **`RecipeGraphSolverTest`**:
   - `testSingleTierPlanksResolution`: Validates single-tier log to plank expansion.
   - `testMultiTierPistonCraftingResolution`: Resolves multi-tier Wood $\rightarrow$ Planks $\rightarrow$ Pistons with exact raw material counts.
   - `testObserverCraftingResolution`: Resolves 128 Observers requiring 768 Cobblestone, 256 Redstone, 128 Quartz.
   - `testFireworksCraftingResolution`: Resolves Sugar Cane $\rightarrow$ Paper + Gunpowder $\rightarrow$ Fireworks with ceiling batch calculations.
   - `testIntermediateInventoryReuse`: Verifies that existing inventory stocks are reused without wasteful over-crafting.
   - `testMissingRawIngredientsReporting`: Verifies deficit tracking against empty inventory.
2. **`BatchCompressorTest`**:
   - `testRecursiveNuggetToBlockCompression`: Verifies 81 Iron Nuggets $\rightarrow$ 9 Iron Ingots $\rightarrow$ 1 Iron Block multi-pass compression.
   - `testMixedResourceCompression`: Bulk batch compression of Wheat, Redstone, and Diamonds.
   - `testCategoryFiltering`: Verifies category filtering (e.g. `METALS` only compresses iron/gold/copper).
3. **`InventoryDeductionSimulatorTest`**:
   - `testSuccessfulSimulation`: Simulates item deductions and receipts across all 36 inventory slots.
   - `testInventoryOverflowDetection`: Detects and prevents inventory overflow before clicks are executed.
4. **`TransactionPlannerTest`**:
   - `testContainerSlotMapping`: Verifies vanilla 3x3 Crafting Table and 2x2 Player Inventory slot mappings.
   - `testTransactionSequenceGeneration`: Confirms atomic click sequences (pickup, place, return cursor, collect).
   - `testTransactionExecutorPacing`: Verifies tick delays, cooldown timers, and completion sound triggers.

---

## 🛠️ Building & Compilation

### Option 1: Standalone Native Mod Builder (`build_native_mod.py`)
No Gradle installation required. Automatically compiles all 23 Java classes, executes the full JUnit 5 test suite, packages the JAR, computes SHA-256 checksums, and copies the mod to `%APPDATA%/.minecraft/mods`:

```bash
python build_native_mod.py
```

### Option 2: Building via Gradle
```bash
# Run unit tests
./gradlew test

# Build mod JAR
./gradlew build
```

Compiled JAR will be located in `build/libs/smart-crafter-pro-1.0.0.jar`.

---

## 📜 License

SmartCrafter Pro is released under the **[MIT License](LICENSE)**.
Feel free to include it in modpacks or contribute improvements via pull requests!
