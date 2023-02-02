package xfacthd.jsontabs.tabs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.Map;

public final class VanillaTabs
{
    public static final ResourceLocation BUILDING_BLOCKS = new ResourceLocation("building_blocks");
    public static final ResourceLocation COLORED_BLOCKS = new ResourceLocation("colored_blocks");
    public static final ResourceLocation NATURAL_BLOCKS = new ResourceLocation("natural_blocks");
    public static final ResourceLocation FUNCTIONAL_BLOCKS = new ResourceLocation("functional_blocks");
    public static final ResourceLocation REDSTONE_BLOCKS = new ResourceLocation("redstone_blocks");
    public static final ResourceLocation TOOLS_AND_UTILITIES = new ResourceLocation("tools_and_utilities");
    public static final ResourceLocation COMBAT = new ResourceLocation("combat");
    public static final ResourceLocation FOOD_AND_DRINKS = new ResourceLocation("food_and_drinks");
    public static final ResourceLocation INGREDIENTS = new ResourceLocation("ingredients");
    public static final ResourceLocation SPAWN_EGGS = new ResourceLocation("spawn_eggs");

    public static final Map<ResourceLocation, CreativeModeTab> TABS = Map.of(
            BUILDING_BLOCKS, CreativeModeTabs.BUILDING_BLOCKS,
            COLORED_BLOCKS, CreativeModeTabs.COLORED_BLOCKS,
            NATURAL_BLOCKS, CreativeModeTabs.NATURAL_BLOCKS,
            FUNCTIONAL_BLOCKS, CreativeModeTabs.FUNCTIONAL_BLOCKS,
            REDSTONE_BLOCKS, CreativeModeTabs.REDSTONE_BLOCKS,
            TOOLS_AND_UTILITIES, CreativeModeTabs.TOOLS_AND_UTILITIES,
            COMBAT, CreativeModeTabs.COMBAT,
            FOOD_AND_DRINKS, CreativeModeTabs.FOOD_AND_DRINKS,
            INGREDIENTS, CreativeModeTabs.INGREDIENTS,
            SPAWN_EGGS, CreativeModeTabs.SPAWN_EGGS
    );
}
