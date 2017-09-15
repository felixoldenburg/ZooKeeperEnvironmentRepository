package com.foldenburg.jones;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by f.oldenburg on 9/16/15.
 */
public class JsonHelper
{
    /**
     * Converts (nested) Json object to flat, dot chained java properties:
     * Example:
     * <p/>
     * { "A": "B",
     * "C": {
     * "D": "E",
     * "F": "G"
     * }
     * }
     * <p/>
     * to:
     * A   -> B
     * C.D -> E
     * C.F -> G
     */
    public static Map<String, String> flattenJson(final JsonElement json)
    {
        return flattenJson(json, "");
    }


    /**
     * Recursively crunches the json input
     *
     * @param json
     * @param keyPrefix
     * @return
     */
    @SuppressWarnings("unchecked")
    private static Map<String, String> flattenJson(final JsonElement json, final String keyPrefix)
    {
        final HashMap<String, String> result = Maps.newHashMap();

        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet())
        {
            final String key = keyPrefix.isEmpty() ? entry.getKey() : keyPrefix + "." + entry.getKey();

            JsonElement value = entry.getValue();
            if (value.isJsonPrimitive())
            {
                result.put(key, value.getAsString());
            }
            else if (value.isJsonArray())
            {
                List<String> values = Lists.newArrayList();
                for (JsonElement jsonElement : value.getAsJsonArray())
                {
                    if (jsonElement.isJsonPrimitive())
                    {
                        values.add(jsonElement.getAsString());
                    }
                    else
                    {
                        throw new IllegalArgumentException("Only primitives in arrays are supported. Skipping " + jsonElement.toString());
                    }
                }
                result.put(key, Joiner.on(",").join(values));
            }
            else
            {
                result.putAll(flattenJson(value, key));
            }
        }

        return result;
    }
}
