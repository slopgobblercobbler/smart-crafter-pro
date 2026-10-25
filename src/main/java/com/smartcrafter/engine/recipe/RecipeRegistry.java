package com.smartcrafter.engine.recipe;

import com.smartcrafter.engine.model.ItemIdentifier;
import com.smartcrafter.engine.model.Recipe;

import java.util.*;

/**
 * Client-side registry of known crafting recipes in SmartCrafter Pro.
 * Pre-populated with core vanilla Minecraft recursive recipe trees,
 * compression matrices, and extensible with dynamic runtime recipes.
 */
public class RecipeRegistry {
    private static final RecipeRegistry INSTANCE = new RecipeRegistry();

    private final Map<ItemIdentifier, List<Recipe>> recipesByOutput = new LinkedHashMap<>();
    private final Map<String, Recipe> recipesById = new LinkedHashMap<>();

    public RecipeRegistry() {
        registerVanillaDefaults();
    }

    public static RecipeRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void registerRecipe(Recipe recipe) {
        recipesById.put(recipe.getId(), recipe);
        recipesByOutput.computeIfAbsent(recipe.getOutput(), k -> new ArrayList<>()).add(recipe);
    }

    public Recipe findRecipeFor(ItemIdentifier output) {
        List<Recipe> list = recipesByOutput.get(output);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    public Recipe findRecipeFor(String itemId) {
        return findRecipeFor(ItemIdentifier.of(itemId));
    }

    public List<Recipe> findRecipesFor(ItemIdentifier output) {
        return recipesByOutput.getOrDefault(output, Collections.emptyList());
    }

    public Recipe getRecipeById(String id) {
        return recipesById.get(id);
    }

    public Collection<Recipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipesById.values());
    }

    public boolean hasRecipe(ItemIdentifier item) {
        return recipesByOutput.containsKey(item);
    }

    /**
     * Registers vanilla base recipes for woods, metals, mechanisms, fireworks, and compression.
     */
    private void registerVanillaDefaults() {
        // Items
        ItemIdentifier oakLog = ItemIdentifier.of("minecraft:oak_log", "Oak Log", 64, "WOOD");
        ItemIdentifier oakPlanks = ItemIdentifier.of("minecraft:oak_planks", "Oak Planks", 64, "WOOD");
        ItemIdentifier stick = ItemIdentifier.of("minecraft:stick", "Stick", 64, "WOOD");
        ItemIdentifier cobblestone = ItemIdentifier.of("minecraft:cobblestone", "Cobblestone", 64, "STONE");
        ItemIdentifier ironIngot = ItemIdentifier.of("minecraft:iron_ingot", "Iron Ingot", 64, "METALS");
        ItemIdentifier ironNugget = ItemIdentifier.of("minecraft:iron_nugget", "Iron Nugget", 64, "METALS");
        ItemIdentifier ironBlock = ItemIdentifier.of("minecraft:iron_block", "Block of Iron", 64, "METALS");
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone", "Redstone Dust", 64, "MINERALS");
        ItemIdentifier redstoneBlock = ItemIdentifier.of("minecraft:redstone_block", "Block of Redstone", 64, "MINERALS");
        ItemIdentifier piston = ItemIdentifier.of("minecraft:piston", "Piston", 64, "REDSTONE");
        ItemIdentifier slimeball = ItemIdentifier.of("minecraft:slime_ball", "Slimeball", 64, "MOB_DROPS");
        ItemIdentifier stickyPiston = ItemIdentifier.of("minecraft:sticky_piston", "Sticky Piston", 64, "REDSTONE");
        ItemIdentifier netherQuartz = ItemIdentifier.of("minecraft:quartz", "Nether Quartz", 64, "MINERALS");
        ItemIdentifier observer = ItemIdentifier.of("minecraft:observer", "Observer", 64, "REDSTONE");
        ItemIdentifier sugarCane = ItemIdentifier.of("minecraft:sugar_cane", "Sugar Cane", 64, "CROPS");
        ItemIdentifier paper = ItemIdentifier.of("minecraft:paper", "Paper", 64, "MISC");
        ItemIdentifier gunpowder = ItemIdentifier.of("minecraft:gunpowder", "Gunpowder", 64, "MOB_DROPS");
        ItemIdentifier fireworkRocket = ItemIdentifier.of("minecraft:firework_rocket", "Firework Rocket", 64, "MISC");
        ItemIdentifier craftingTable = ItemIdentifier.of("minecraft:crafting_table", "Crafting Table", 64, "UTILITY");
        ItemIdentifier chest = ItemIdentifier.of("minecraft:chest", "Chest", 64, "UTILITY");
        ItemIdentifier hopper = ItemIdentifier.of("minecraft:hopper", "Hopper", 64, "REDSTONE");
        ItemIdentifier dropper = ItemIdentifier.of("minecraft:dropper", "Dropper", 64, "REDSTONE");
        ItemIdentifier dispenser = ItemIdentifier.of("minecraft:dispenser", "Dispenser", 64, "REDSTONE");
        ItemIdentifier bow = ItemIdentifier.of("minecraft:bow", "Bow", 1, "COMBAT");
        ItemIdentifier string = ItemIdentifier.of("minecraft:string", "String", 64, "MOB_DROPS");
        ItemIdentifier coal = ItemIdentifier.of("minecraft:coal", "Coal", 64, "MINERALS");
        ItemIdentifier torch = ItemIdentifier.of("minecraft:torch", "Torch", 64, "UTILITY");

        // Woods -> Planks (1 log -> 4 planks)
        registerRecipe(Recipe.singleInput("minecraft:oak_planks", oakPlanks, 4, oakLog));

        // Planks -> Sticks (2 planks -> 4 sticks)
        ItemIdentifier[] stickPattern = new ItemIdentifier[] {
            oakPlanks,
            oakPlanks
        };
        registerRecipe(Recipe.shaped("minecraft:stick", stick, 4, 1, 2, stickPattern));

        // Crafting Table (4 planks -> 1 table)
        ItemIdentifier[] tablePattern = new ItemIdentifier[] {
            oakPlanks, oakPlanks,
            oakPlanks, oakPlanks
        };
        registerRecipe(Recipe.shaped2x2("minecraft:crafting_table", craftingTable, 1, tablePattern));

        // Chest (8 planks hollow -> 1 chest)
        ItemIdentifier[] chestPattern = new ItemIdentifier[] {
            oakPlanks, oakPlanks, oakPlanks,
            oakPlanks, null,      oakPlanks,
            oakPlanks, oakPlanks, oakPlanks
        };
        registerRecipe(Recipe.shaped3x3("minecraft:chest", chest, 1, chestPattern));

        // Torches (1 coal, 1 stick -> 4 torches)
        ItemIdentifier[] torchPattern = new ItemIdentifier[] {
            coal,
            stick
        };
        registerRecipe(Recipe.shaped("minecraft:torch", torch, 4, 1, 2, torchPattern));

        // Piston (3 Planks, 4 Cobble, 1 Iron, 1 Redstone -> 1 Piston)
        ItemIdentifier[] pistonPattern = new ItemIdentifier[] {
            oakPlanks,   oakPlanks,   oakPlanks,
            cobblestone, ironIngot,   cobblestone,
            cobblestone, redstone,    cobblestone
        };
        registerRecipe(Recipe.shaped3x3("minecraft:piston", piston, 1, pistonPattern));

        // Sticky Piston (1 Piston, 1 Slimeball -> 1 Sticky Piston)
        ItemIdentifier[] stickyPattern = new ItemIdentifier[] {
            slimeball,
            piston
        };
        registerRecipe(Recipe.shaped("minecraft:sticky_piston", stickyPiston, 1, 1, 2, stickyPattern));

        // Observer (6 Cobblestone, 2 Redstone, 1 Quartz -> 1 Observer)
        ItemIdentifier[] observerPattern = new ItemIdentifier[] {
            cobblestone, cobblestone, cobblestone,
            redstone,    redstone,    netherQuartz,
            cobblestone, cobblestone, cobblestone
        };
        registerRecipe(Recipe.shaped3x3("minecraft:observer", observer, 1, observerPattern));

        // Bow (3 string, 3 sticks)
        ItemIdentifier[] bowPattern = new ItemIdentifier[] {
            null,  stick, string,
            stick, null,  string,
            null,  stick, string
        };
        registerRecipe(Recipe.shaped3x3("minecraft:bow", bow, 1, bowPattern));

        // Dropper (7 Cobble, 1 Redstone)
        ItemIdentifier[] dropperPattern = new ItemIdentifier[] {
            cobblestone, cobblestone, cobblestone,
            cobblestone, null,        cobblestone,
            cobblestone, redstone,    cobblestone
        };
        registerRecipe(Recipe.shaped3x3("minecraft:dropper", dropper, 1, dropperPattern));

        // Dispenser (7 Cobble, 1 Bow, 1 Redstone)
        ItemIdentifier[] dispenserPattern = new ItemIdentifier[] {
            cobblestone, cobblestone, cobblestone,
            cobblestone, bow,         cobblestone,
            cobblestone, redstone,    cobblestone
        };
        registerRecipe(Recipe.shaped3x3("minecraft:dispenser", dispenser, 1, dispenserPattern));

        // Hopper (5 Iron, 1 Chest)
        ItemIdentifier[] hopperPattern = new ItemIdentifier[] {
            ironIngot, null,      ironIngot,
            ironIngot, chest,     ironIngot,
            null,      ironIngot, null
        };
        registerRecipe(Recipe.shaped3x3("minecraft:hopper", hopper, 1, hopperPattern));

        // Paper (3 Sugar Cane -> 3 Paper)
        ItemIdentifier[] paperPattern = new ItemIdentifier[] {
            sugarCane, sugarCane, sugarCane
        };
        registerRecipe(Recipe.shaped("minecraft:paper", paper, 3, 3, 1, paperPattern));

        // Firework Rocket (1 Gunpowder + 1 Paper -> 3 Rockets)
        registerRecipe(Recipe.shapeless("minecraft:firework_rocket", fireworkRocket, 3,
                Arrays.asList(gunpowder, paper)));

        // Compression Registrations (Nuggets <-> Ingots <-> Blocks)
        registerCompressionRecipes();
    }

    private void registerCompressionRecipes() {
        // Iron
        ItemIdentifier ironNugget = ItemIdentifier.of("minecraft:iron_nugget", "Iron Nugget", 64, "METALS");
        ItemIdentifier ironIngot = ItemIdentifier.of("minecraft:iron_ingot", "Iron Ingot", 64, "METALS");
        ItemIdentifier ironBlock = ItemIdentifier.of("minecraft:iron_block", "Block of Iron", 64, "METALS");
        register3x3Compress("minecraft:iron_ingot_from_nuggets", ironIngot, 1, ironNugget);
        register3x3Compress("minecraft:iron_block", ironBlock, 1, ironIngot);
        registerDecompress("minecraft:iron_ingot_from_block", ironIngot, 9, ironBlock);
        registerDecompress("minecraft:iron_nugget_from_ingot", ironNugget, 9, ironIngot);

        // Gold
        ItemIdentifier goldNugget = ItemIdentifier.of("minecraft:gold_nugget", "Gold Nugget", 64, "METALS");
        ItemIdentifier goldIngot = ItemIdentifier.of("minecraft:gold_ingot", "Gold Ingot", 64, "METALS");
        ItemIdentifier goldBlock = ItemIdentifier.of("minecraft:gold_block", "Block of Gold", 64, "METALS");
        register3x3Compress("minecraft:gold_ingot_from_nuggets", goldIngot, 1, goldNugget);
        register3x3Compress("minecraft:gold_block", goldBlock, 1, goldIngot);
        registerDecompress("minecraft:gold_ingot_from_block", goldIngot, 9, goldBlock);
        registerDecompress("minecraft:gold_nugget_from_ingot", goldNugget, 9, goldIngot);

        // Copper
        ItemIdentifier copperIngot = ItemIdentifier.of("minecraft:copper_ingot", "Copper Ingot", 64, "METALS");
        ItemIdentifier copperBlock = ItemIdentifier.of("minecraft:copper_block", "Block of Copper", 64, "METALS");
        register3x3Compress("minecraft:copper_block", copperBlock, 1, copperIngot);
        registerDecompress("minecraft:copper_ingot_from_block", copperIngot, 9, copperBlock);

        // Diamond
        ItemIdentifier diamond = ItemIdentifier.of("minecraft:diamond", "Diamond", 64, "GEMS");
        ItemIdentifier diamondBlock = ItemIdentifier.of("minecraft:diamond_block", "Block of Diamond", 64, "GEMS");
        register3x3Compress("minecraft:diamond_block", diamondBlock, 1, diamond);
        registerDecompress("minecraft:diamond_from_block", diamond, 9, diamondBlock);

        // Emerald
        ItemIdentifier emerald = ItemIdentifier.of("minecraft:emerald", "Emerald", 64, "GEMS");
        ItemIdentifier emeraldBlock = ItemIdentifier.of("minecraft:emerald_block", "Block of Emerald", 64, "GEMS");
        register3x3Compress("minecraft:emerald_block", emeraldBlock, 1, emerald);
        registerDecompress("minecraft:emerald_from_block", emerald, 9, emeraldBlock);

        // Redstone & Lapis & Coal
        ItemIdentifier redstone = ItemIdentifier.of("minecraft:redstone", "Redstone Dust", 64, "MINERALS");
        ItemIdentifier redstoneBlock = ItemIdentifier.of("minecraft:redstone_block", "Block of Redstone", 64, "MINERALS");
        register3x3Compress("minecraft:redstone_block", redstoneBlock, 1, redstone);
        registerDecompress("minecraft:redstone_from_block", redstone, 9, redstoneBlock);

        ItemIdentifier lapis = ItemIdentifier.of("minecraft:lapis_lazuli", "Lapis Lazuli", 64, "MINERALS");
        ItemIdentifier lapisBlock = ItemIdentifier.of("minecraft:lapis_block", "Block of Lapis Lazuli", 64, "MINERALS");
        register3x3Compress("minecraft:lapis_block", lapisBlock, 1, lapis);
        registerDecompress("minecraft:lapis_from_block", lapis, 9, lapisBlock);

        ItemIdentifier coal = ItemIdentifier.of("minecraft:coal", "Coal", 64, "MINERALS");
        ItemIdentifier coalBlock = ItemIdentifier.of("minecraft:coal_block", "Block of Coal", 64, "MINERALS");
        register3x3Compress("minecraft:coal_block", coalBlock, 1, coal);
        registerDecompress("minecraft:coal_from_block", coal, 9, coalBlock);

        // Netherite
        ItemIdentifier netheriteIngot = ItemIdentifier.of("minecraft:netherite_ingot", "Netherite Ingot", 64, "METALS");
        ItemIdentifier netheriteBlock = ItemIdentifier.of("minecraft:netherite_block", "Block of Netherite", 64, "METALS");
        register3x3Compress("minecraft:netherite_block", netheriteBlock, 1, netheriteIngot);
        registerDecompress("minecraft:netherite_from_block", netheriteIngot, 9, netheriteBlock);

        // Raw metals
        ItemIdentifier rawIron = ItemIdentifier.of("minecraft:raw_iron", "Raw Iron", 64, "METALS");
        ItemIdentifier rawIronBlock = ItemIdentifier.of("minecraft:raw_iron_block", "Block of Raw Iron", 64, "METALS");
        register3x3Compress("minecraft:raw_iron_block", rawIronBlock, 1, rawIron);
        registerDecompress("minecraft:raw_iron_from_block", rawIron, 9, rawIronBlock);

        ItemIdentifier rawGold = ItemIdentifier.of("minecraft:raw_gold", "Raw Gold", 64, "METALS");
        ItemIdentifier rawGoldBlock = ItemIdentifier.of("minecraft:raw_gold_block", "Block of Raw Gold", 64, "METALS");
        register3x3Compress("minecraft:raw_gold_block", rawGoldBlock, 1, rawGold);
        registerDecompress("minecraft:raw_gold_from_block", rawGold, 9, rawGoldBlock);

        ItemIdentifier rawCopper = ItemIdentifier.of("minecraft:raw_copper", "Raw Copper", 64, "METALS");
        ItemIdentifier rawCopperBlock = ItemIdentifier.of("minecraft:raw_copper_block", "Block of Raw Copper", 64, "METALS");
        register3x3Compress("minecraft:raw_copper_block", rawCopperBlock, 1, rawCopper);
        registerDecompress("minecraft:raw_copper_from_block", rawCopper, 9, rawCopperBlock);

        // Crops & Farmables: Wheat <-> Hay
        ItemIdentifier wheat = ItemIdentifier.of("minecraft:wheat", "Wheat", 64, "CROPS");
        ItemIdentifier hayBlock = ItemIdentifier.of("minecraft:hay_block", "Hay Bale", 64, "CROPS");
        register3x3Compress("minecraft:hay_block", hayBlock, 1, wheat);
        registerDecompress("minecraft:wheat_from_hay", wheat, 9, hayBlock);

        // Slimeball <-> Slime Block
        ItemIdentifier slimeball = ItemIdentifier.of("minecraft:slime_ball", "Slimeball", 64, "MOB_DROPS");
        ItemIdentifier slimeBlock = ItemIdentifier.of("minecraft:slime_block", "Slime Block", 64, "MOB_DROPS");
        register3x3Compress("minecraft:slime_block", slimeBlock, 1, slimeball);
        registerDecompress("minecraft:slimeball_from_block", slimeball, 9, slimeBlock);

        // Bone -> Bone Meal -> Bone Block
        ItemIdentifier bone = ItemIdentifier.of("minecraft:bone", "Bone", 64, "MOB_DROPS");
        ItemIdentifier boneMeal = ItemIdentifier.of("minecraft:bone_meal", "Bone Meal", 64, "MOB_DROPS");
        ItemIdentifier boneBlock = ItemIdentifier.of("minecraft:bone_block", "Bone Block", 64, "MOB_DROPS");
        registerDecompress("minecraft:bone_meal_from_bone", boneMeal, 3, bone);
        register3x3Compress("minecraft:bone_block", boneBlock, 1, boneMeal);
        registerDecompress("minecraft:bone_meal_from_block", boneMeal, 9, boneBlock);

        // 2x2 Compressions
        ItemIdentifier quartz = ItemIdentifier.of("minecraft:quartz", "Nether Quartz", 64, "MINERALS");
        ItemIdentifier quartzBlock = ItemIdentifier.of("minecraft:quartz_block", "Block of Quartz", 64, "MINERALS");
        register2x2Compress("minecraft:quartz_block", quartzBlock, 1, quartz);

        ItemIdentifier glowstoneDust = ItemIdentifier.of("minecraft:glowstone_dust", "Glowstone Dust", 64, "MINERALS");
        ItemIdentifier glowstoneBlock = ItemIdentifier.of("minecraft:glowstone", "Glowstone", 64, "MINERALS");
        register2x2Compress("minecraft:glowstone", glowstoneBlock, 1, glowstoneDust);

        ItemIdentifier amethystShard = ItemIdentifier.of("minecraft:amethyst_shard", "Amethyst Shard", 64, "GEMS");
        ItemIdentifier amethystBlock = ItemIdentifier.of("minecraft:amethyst_block", "Block of Amethyst", 64, "GEMS");
        register2x2Compress("minecraft:amethyst_block", amethystBlock, 1, amethystShard);

        ItemIdentifier clayBall = ItemIdentifier.of("minecraft:clay_ball", "Clay Ball", 64, "MISC");
        ItemIdentifier clayBlock = ItemIdentifier.of("minecraft:clay", "Clay Block", 64, "MISC");
        register2x2Compress("minecraft:clay", clayBlock, 1, clayBall);

        ItemIdentifier sand = ItemIdentifier.of("minecraft:sand", "Sand", 64, "MISC");
        ItemIdentifier sandstone = ItemIdentifier.of("minecraft:sandstone", "Sandstone", 64, "MISC");
        register2x2Compress("minecraft:sandstone", sandstone, 1, sand);

        ItemIdentifier snowball = ItemIdentifier.of("minecraft:snowball", "Snowball", 16, "MISC");
        ItemIdentifier snowBlock = ItemIdentifier.of("minecraft:snow_block", "Snow Block", 64, "MISC");
        register2x2Compress("minecraft:snow_block", snowBlock, 1, snowball);
    }

    private void register3x3Compress(String id, ItemIdentifier output, int count, ItemIdentifier input) {
        ItemIdentifier[] p = new ItemIdentifier[9];
        Arrays.fill(p, input);
        registerRecipe(Recipe.shaped3x3(id, output, count, p));
    }

    private void register2x2Compress(String id, ItemIdentifier output, int count, ItemIdentifier input) {
        ItemIdentifier[] p = new ItemIdentifier[4];
        Arrays.fill(p, input);
        registerRecipe(Recipe.shaped2x2(id, output, count, p));
    }

    private void registerDecompress(String id, ItemIdentifier output, int count, ItemIdentifier input) {
        registerRecipe(Recipe.singleInput(id, output, count, input));
    }
}
