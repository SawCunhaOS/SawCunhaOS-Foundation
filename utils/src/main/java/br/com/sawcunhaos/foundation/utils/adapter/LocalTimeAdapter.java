package br.com.sawcunhaos.foundation.utils.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class LocalTimeAdapter implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {

    @Override
    public JsonElement serialize(LocalTime time, Type typeOfSrc, JsonSerializationContext context) {
        if(Objects.nonNull(time)) {
            return new JsonPrimitive(time.format(DateTimeFormatter.ISO_TIME));
        }
        return null;
    }

    @Override
    public LocalTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if(Objects.nonNull(jsonElement)) {
            return LocalTime.parse(jsonElement.getAsString(), DateTimeFormatter.ISO_TIME);
        }
        return null;
    }
}
