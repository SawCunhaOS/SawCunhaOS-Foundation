package br.com.sawcunhaos.foundation.utils.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalDateAdapterTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();

    @Test
    void shouldSerializeDateSuccessfully() {
        String dateExpected = "\"2023-04-30\"";
        LocalDate date = LocalDate.of(2023, 4, 30);

        String dateResult = gson.toJson(date);

        assertEquals(dateExpected, dateResult);
    }

    @Test
    void shouldDeserializeDateSuccessfully() {
        LocalDate dateExpected = LocalDate.of(2023, 4, 30);
        String date = "\"2023-04-30\"";

        LocalDate dateResult = gson.fromJson(date, LocalDate.class);

        assertEquals(dateExpected, dateResult);
    }
}
