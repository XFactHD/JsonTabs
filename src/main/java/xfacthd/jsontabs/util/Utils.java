package xfacthd.jsontabs.util;

import com.google.gson.*;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Utils
{
    public static JsonObject readJsonFile(Path path, Gson gson)
    {
        try (Reader reader = Files.newBufferedReader(path))
        {
            return GsonHelper.fromJson(gson, reader, JsonObject.class);
        }
        catch (IOException e)
        {
            throw new JsonSyntaxException(e);
        }
    }
}
