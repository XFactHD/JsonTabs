package xfacthd.jsontabs.util;

import com.google.gson.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
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
            throw new JsonParseException(e);
        }
    }

    public static final Codec<Integer> FLEXIBLE_INT_CODEC = new PrimitiveCodec<>()
    {
        @Override
        public <T> DataResult<Integer> read(DynamicOps<T> ops, T input)
        {
            DataResult<Integer> intResult = ops.getNumberValue(input).map(Number::intValue);
            if (intResult.result().isPresent())
            {
                return intResult;
            }

            DataResult<String> stringResult = ops.getStringValue(input);
            if (stringResult.result().isPresent())
            {
                String value = stringResult.result().get();
                try
                {
                    return DataResult.success(Long.decode(value).intValue());
                }
                catch (NumberFormatException e)
                {
                    return DataResult.error(e::getMessage);
                }
            }
            else if (stringResult.error().isPresent())
            {
                return DataResult.error(stringResult.error().get()::message);
            }
            return DataResult.error(() -> "Not a number: " + input);
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Integer value)
        {
            return ops.createString(Integer.toHexString(value));
        }
    };
}
