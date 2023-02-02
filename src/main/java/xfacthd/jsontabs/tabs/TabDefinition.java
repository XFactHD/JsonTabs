package xfacthd.jsontabs.tabs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import xfacthd.jsontabs.JsonTabs;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record TabDefinition(
        ResourceLocation name,
        Optional<Boolean> useVanillaOpt,
        List<ResourceLocation> after,
        List<ResourceLocation> before,
        TabEntry icon,
        Optional<Boolean> noTitleOpt,
        Optional<Boolean> noScrollOpt,
        Optional<ResourceLocation> backgroundOpt,
        Optional<Integer> labelColorOpt,
        Optional<Boolean> searchBarOpt,
        Optional<Integer> searchBarWidthOpt,
        Optional<Integer> slotColorOpt,
        Optional<ResourceLocation> tabImageOpt,
        List<TabEntry> contents
)
{
    private static final CreativeModeTab.DisplayItemsGenerator EMPTY_GENERATOR = (flags, output, bool) -> { };

    public boolean useVanilla()
    {
        return useVanillaOpt.orElse(false);
    }

    public boolean noTitle()
    {
        return noTitleOpt.orElse(false);
    }

    public boolean noScroll()
    {
        return noScrollOpt.orElse(false);
    }

    public boolean searchBar()
    {
        return searchBarOpt.orElse(false);
    }

    public Supplier<ItemStack> makeIconSupplier()
    {
        return () -> icon.toStack(name, () ->
                String.format("Invalid icon '%s' in tab definition '%s', ignoring", icon.name(), name)
        );
    }

    public CreativeModeTab.DisplayItemsGenerator buildDisplayGenerator()
    {
        if (contents.isEmpty())
        {
            JsonTabs.LOGGER.warn("Tab definition '{}' doesn't specify any contents", name);
            return EMPTY_GENERATOR;
        }

        return (flags, output, bool) ->
        {
            for (TabEntry entry : contents)
            {
                ItemStack stack = entry.toStack(name, () ->
                        String.format("Found invalid entry '%s' in tab definition '%s', ignoring", entry.name(), name)
                );

                if (!stack.isEmpty() && stack.isItemEnabled(flags))
                {
                    output.accept(stack);
                }
            }
        };
    }

    public void checkIgnoredOptions()
    {
        if (useVanilla())
        {
            String vanillaMsg = "Tab definition '{}' is set to use vanilla tab, specified {} will be ignored";
            noTitleOpt.ifPresent(title -> JsonTabs.LOGGER.warn(vanillaMsg, name, "'no title' setting"));
            noScrollOpt.ifPresent(scroll -> JsonTabs.LOGGER.warn(vanillaMsg, name, "'no scrollbar' setting"));
            backgroundOpt.ifPresent(bg -> JsonTabs.LOGGER.warn(vanillaMsg, name, "background"));
            labelColorOpt.ifPresent(col -> JsonTabs.LOGGER.warn(vanillaMsg, name, "label color"));
            searchBarOpt.ifPresent(search -> JsonTabs.LOGGER.warn(vanillaMsg, name, "'search bar' option"));
            searchBarWidthOpt.ifPresent(w -> JsonTabs.LOGGER.warn(vanillaMsg, name, "search bar width"));
            slotColorOpt.ifPresent(col -> JsonTabs.LOGGER.warn(vanillaMsg, name, "slot color"));
            tabImageOpt.ifPresent(img -> JsonTabs.LOGGER.warn(vanillaMsg, name, "tab image"));
        }

        if (!searchBar())
        {
            searchBarWidthOpt.ifPresent(w ->
                    JsonTabs.LOGGER.warn("Tab definition '{}' has no search bar, specified search bar width will be ignored", name)
            );
        }
    }
}
