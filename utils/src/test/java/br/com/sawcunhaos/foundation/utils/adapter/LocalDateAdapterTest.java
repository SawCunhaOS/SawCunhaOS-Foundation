
/*
 *
 *  * Copyright 2026 SawCunha Open System - SawCunhaOS-Foundation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */

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
