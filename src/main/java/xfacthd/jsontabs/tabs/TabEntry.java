package xfacthd.jsontabs.tabs;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import xfacthd.jsontabs.JsonTabs;

import java.util.List;
import java.util.function.Supplier;

public record TabEntry(ResourceLocation name, String nbt)
{
    private TabEntry(ResourceLocation item) { this(item, ""); }

    public ItemStack toStack(ResourceLocation tabName, Supplier<String> errorSupplier)
    {
        Item item = ForgeRegistries.ITEMS.getValue(name);
        if (item == null || item == Items.AIR)
        {
            JsonTabs.LOGGER.warn(errorSupplier.get());
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(item);
        if (!nbt.isEmpty())
        {
            try
            {
                result.setTag(TagParser.parseTag(nbt));
            }
            catch (CommandSyntaxException e)
            {
                JsonTabs.LOGGER.error(
                        "Found invalid NBT data on item '{}' in tab '{}', tag will not be attached",
                        name, tabName, e
                );
            }
        }
        return result;
    }



    public static final Codec<TabEntry> CODEC_SIMPLE = ResourceLocation.CODEC.xmap(TabEntry::new, TabEntry::name);

    public static final Codec<TabEntry> CODEC_DATA = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(TabEntry::name),
            Codec.STRING.fieldOf("data").forGetter(TabEntry::nbt)
    ).apply(instance, TabEntry::new));

    public static TabEntry crossMapTo(Either<TabEntry, TabEntry> either)
    {
        return either.left().orElseGet(() -> either.right().orElseThrow());
    }

    public static Either<TabEntry, TabEntry> crossMapFrom(TabEntry entry)
    {
        return entry.nbt.isEmpty() ? Either.left(entry) : Either.right(entry);
    }

    public static List<TabEntry> crossMapTo(List<Either<TabEntry, TabEntry>> list)
    {
        return list.stream().map(TabEntry::crossMapTo).toList();
    }

    public static List<Either<TabEntry, TabEntry>> crossMapFrom(List<TabEntry> list)
    {
        return list.stream().map(TabEntry::crossMapFrom).toList();
    }
}
