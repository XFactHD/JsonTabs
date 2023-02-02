package xfacthd.jsontabs;

import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;
import xfacthd.jsontabs.tabs.JsonTabManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

@Mod(JsonTabs.MODID)
public final class JsonTabs
{
    public static final String MODID = "jsontabs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JsonTabs()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(JsonTabs::onCreateAttributes);
        bus.addListener(JsonTabs::onAddPackFinders);
    }

    private static void onCreateAttributes(final EntityAttributeCreationEvent event)
    {
        JsonTabManager.load();

        Multimap<ResourceLocation, ResourceLocation> edges = ObfuscationReflectionHelper.getPrivateValue(
                CreativeModeTabRegistry.class, null, "vanillaEdges"
        );
        Map<ResourceLocation, CreativeModeTab> tabs = ObfuscationReflectionHelper.getPrivateValue(
                CreativeModeTabRegistry.class, null, "vanillaCreativeModeTabs"
        );

        Objects.requireNonNull(edges);
        Objects.requireNonNull(tabs);

        edges.clear();
        edges.putAll(JsonTabManager.getEdges());

        tabs.clear();
        tabs.putAll(JsonTabManager.getTabs());
    }

    private static void onAddPackFinders(final AddPackFindersEvent event)
    {
        Path packPath = JsonTabManager.TABS_PATH.resolve("resources");
        try
        {
            FileUtils.getOrCreateDirectory(packPath, "JsonTabs CreativeModeTab resources");
        }
        catch (Throwable t)
        {
            LOGGER.error("Failed to create tab resource pack directory", t);
            return;
        }

        Path metaPath = packPath.resolve("pack.mcmeta");
        if (!Files.exists(metaPath) && !createPackMetadata(metaPath))
        {
            return;
        }

        Pack pack = Pack.readMetaAndCreate(
                "builtin/jsontabs_resources",
                Component.literal("JsonTabs Resources"),
                true,
                path -> new PathPackResources(path, packPath, true),
                PackType.CLIENT_RESOURCES,
                Pack.Position.BOTTOM,
                PackSource.BUILT_IN
        );

        if (pack != null)
        {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
        else
        {
            LOGGER.error("Pack metadata of builtin resource pack is invalid!");
        }
    }

    private static boolean createPackMetadata(Path metaPath)
    {
        try
        {
            Files.writeString(metaPath, """
                    {
                        "pack": {
                            "description": "jsontabs builtin resources",
                            "pack_format": 12,
                            "forge:resource_pack_format": 12,
                            "forge:data_pack_format": 10
                        }
                    }
                    """
            );
            return true;
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to create pack metadata, builtin pack won't be loaded. Create pack.mcmeta manually to fix", e);
            return false;
        }
    }
}
