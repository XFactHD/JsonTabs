package xfacthd.jsontabs.tabs;

import com.google.common.collect.*;
import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import xfacthd.jsontabs.JsonTabs;
import xfacthd.jsontabs.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class JsonTabManager
{
    public static final Path TABS_PATH = FMLPaths.GAMEDIR.get().resolve("jsontabs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Codec<TabDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(TabDefinition::name),
            Codec.BOOL.optionalFieldOf("use_vanilla").forGetter(TabDefinition::useVanillaOpt),
            ResourceLocation.CODEC.listOf().fieldOf("after").forGetter(TabDefinition::after),
            ResourceLocation.CODEC.listOf().fieldOf("before").forGetter(TabDefinition::before),
            new ExtraCodecs.EitherCodec<>(
                    TabEntry.CODEC_SIMPLE,
                    TabEntry.CODEC_DATA
            ).fieldOf("icon").xmap(TabEntry::crossMapTo, TabEntry::crossMapFrom).forGetter(TabDefinition::icon),
            Codec.BOOL.optionalFieldOf("no_title").forGetter(TabDefinition::noTitleOpt),
            Codec.BOOL.optionalFieldOf("no_scrollbar").forGetter(TabDefinition::noScrollOpt),
            ResourceLocation.CODEC.optionalFieldOf("background").forGetter(TabDefinition::backgroundOpt),
            Utils.FLEXIBLE_INT_CODEC.optionalFieldOf("label_color").forGetter(TabDefinition::labelColorOpt),
            Codec.BOOL.optionalFieldOf("search_bar").forGetter(TabDefinition::searchBarOpt),
            Codec.INT.optionalFieldOf("search_bar_width").forGetter(TabDefinition::searchBarWidthOpt),
            Utils.FLEXIBLE_INT_CODEC.optionalFieldOf("slot_color").forGetter(TabDefinition::labelColorOpt),
            ResourceLocation.CODEC.optionalFieldOf("tab_image").forGetter(TabDefinition::tabImageOpt),
            new ExtraCodecs.EitherCodec<>(
                    TabEntry.CODEC_SIMPLE,
                    TabEntry.CODEC_DATA
            ).listOf().fieldOf("contents").xmap(TabEntry::crossMapTo, TabEntry::crossMapFrom).forGetter(TabDefinition::contents)
    ).apply(instance, TabDefinition::new));

    private static final BiMap<ResourceLocation, CreativeModeTab> TABS = HashBiMap.create();
    private static final Multimap<ResourceLocation, ResourceLocation> EDGES = HashMultimap.create();

    public static void load()
    {
        try
        {
            FileUtils.getOrCreateDirectory(TABS_PATH, "JsonTabs CreativeModeTab definitions");
        }
        catch (Throwable t)
        {
            JsonTabs.LOGGER.error("Failed to create tab definitions directory", t);
            loadVanillaFallback();
            return;
        }

        Map<ResourceLocation, Path> processed = new HashMap<>();
        List<TabDefinition> definitions = new ArrayList<>();
        try (Stream<Path> paths = Files.list(TABS_PATH))
        {
            paths.filter(Files::isRegularFile)
                    .filter(filePath -> filePath.toString().endsWith(".json"))
                    .forEach(filePath ->
                    {
                        JsonObject json = Utils.readJsonFile(filePath, GSON);
                        DataResult<TabDefinition> def = CODEC.parse(JsonOps.INSTANCE, json);
                        if (def.error().isPresent())
                        {
                            throw new JsonParseException(def.error().get().message());
                        }

                        TabDefinition tabDef = def.result().orElseThrow(() -> new JsonParseException(
                                "Unknown error while retrieving parsed tab definition"
                        ));
                        if (processed.containsKey(tabDef.name()))
                        {
                            throw new JsonParseException(String.format(
                                    "Found duplicated tab name '%s' in file '%s', previously found in '%s'",
                                    tabDef.name(),
                                    filePath.toAbsolutePath().normalize(),
                                    processed.get(tabDef.name()).toAbsolutePath().normalize()
                            ));
                        }

                        processed.put(tabDef.name(), filePath);
                        definitions.add(tabDef);
                    });
        }
        catch (IOException | JsonParseException e)
        {
            JsonTabs.LOGGER.error("Encountered an error while loading tab definitions", e);
            loadVanillaFallback();
            return;
        }

        if (definitions.isEmpty())
        {
            JsonTabs.LOGGER.info("No tab definitions loaded, falling back to vanilla");
            loadVanillaFallback();
            return;
        }

        for (TabDefinition def : definitions)
        {
            def.checkIgnoredOptions();

            if (def.useVanilla())
            {
                if (!VanillaTabs.TABS.containsKey(def.name()))
                {
                    JsonTabs.LOGGER.error("Tab definition '{}' is set to use vanilla tab but is not a known vanilla tab, tab will not be added", def.name());
                }
                else
                {
                    if (!def.contents().isEmpty())
                    {
                        JsonTabs.LOGGER.warn("Tab definition '{}' is set to use vanilla tab and specifies contents, contents will be ignored", def.name());
                    }
                    addVanillaTab(def.name(), def.after(), def.before());
                }
                continue;
            }

            CreativeModeTab.Builder tabBuilder = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.jsontabs." + def.name().toString().replace(':', '.')))
                    .icon(def.makeIconSupplier())
                    .displayItems(def.buildDisplayGenerator());

            if (def.noTitle())
            {
                tabBuilder.hideTitle();
            }
            if (def.noScroll())
            {
                tabBuilder.noScrollBar();
            }
            def.backgroundOpt().ifPresent(tabBuilder::withBackgroundLocation);
            def.labelColorOpt().ifPresent(tabBuilder::withLabelColor);
            if (def.searchBar())
            {
                def.searchBarWidthOpt().ifPresentOrElse(
                        tabBuilder::withSearchBar,
                        tabBuilder::withSearchBar
                );
            }
            def.slotColorOpt().ifPresent(tabBuilder::withSlotColor);
            def.tabImageOpt().ifPresent(tabBuilder::withTabsImage);

            addTab(tabBuilder.build(), def.name(), def.after(), def.before());
        }
    }

    private static void loadVanillaFallback()
    {
        addVanillaTab(VanillaTabs.BUILDING_BLOCKS, List.of(), List.of());
        addVanillaTab(VanillaTabs.COLORED_BLOCKS, List.of(VanillaTabs.BUILDING_BLOCKS), List.of(VanillaTabs.NATURAL_BLOCKS));
        addVanillaTab(VanillaTabs.NATURAL_BLOCKS, List.of(VanillaTabs.COLORED_BLOCKS), List.of(VanillaTabs.FUNCTIONAL_BLOCKS));
        addVanillaTab(VanillaTabs.FUNCTIONAL_BLOCKS, List.of(VanillaTabs.NATURAL_BLOCKS), List.of(VanillaTabs.REDSTONE_BLOCKS));
        addVanillaTab(VanillaTabs.REDSTONE_BLOCKS, List.of(VanillaTabs.FUNCTIONAL_BLOCKS), List.of(VanillaTabs.TOOLS_AND_UTILITIES));
        addVanillaTab(VanillaTabs.TOOLS_AND_UTILITIES, List.of(VanillaTabs.REDSTONE_BLOCKS), List.of(VanillaTabs.COMBAT));
        addVanillaTab(VanillaTabs.COMBAT, List.of(VanillaTabs.TOOLS_AND_UTILITIES), List.of(VanillaTabs.FOOD_AND_DRINKS));
        addVanillaTab(VanillaTabs.FOOD_AND_DRINKS, List.of(VanillaTabs.COMBAT), List.of(VanillaTabs.INGREDIENTS));
        addVanillaTab(VanillaTabs.INGREDIENTS, List.of(VanillaTabs.FOOD_AND_DRINKS), List.of(VanillaTabs.SPAWN_EGGS));
        addVanillaTab(VanillaTabs.SPAWN_EGGS, List.of(VanillaTabs.INGREDIENTS), List.of());
    }

    private static void addVanillaTab(ResourceLocation name, List<ResourceLocation> afterEntries, List<ResourceLocation> beforeEntries)
    {
        addTab(VanillaTabs.TABS.get(name), name, afterEntries, beforeEntries);
    }

    /**
     * Add a tab including its sorting edges
     * @param tab The tab to add
     * @param name The name of the tab being added
     * @param afterEntries The tabs by name this tab should be added after
     * @param beforeEntries The tabs by name this tab should be added before
     */
    private static void addTab(CreativeModeTab tab, ResourceLocation name, List<ResourceLocation> afterEntries, List<ResourceLocation> beforeEntries)
    {
        TABS.put(name, tab);
        for (ResourceLocation after : afterEntries)
        {
            EDGES.put(after, name);
        }
        for (ResourceLocation before : beforeEntries)
        {
            EDGES.put(name, before);
        }
    }

    public static Multimap<ResourceLocation, ResourceLocation> getEdges()
    {
        return EDGES;
    }

    public static Map<ResourceLocation, CreativeModeTab> getTabs()
    {
        return TABS;
    }
}
