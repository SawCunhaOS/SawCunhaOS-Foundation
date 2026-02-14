package br.com.sawcunhaos.foundation.utils.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDateTimeAdapterTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();

    @Test
    void shouldSerializeDateSuccessfully() {
        String dateExpected = "\"2023-04-30T13:12:32\"";
        LocalDateTime date = LocalDateTime.of(2023, 4, 30, 13, 12, 32);

        String dateResult = gson.toJson(date);

        assertEquals(dateExpected, dateResult);
    }

    @Test
    void shouldDeserializeDateSuccessfully() {
        LocalDateTime dateExpected = LocalDateTime.of(2023, 4, 30, 13, 12, 32);
        String date = "\"2023-04-30T13:12:32\"";

        LocalDateTime dateResult = gson.fromJson(date, LocalDateTime.class);

        assertEquals(dateExpected, dateResult);
    }

}
