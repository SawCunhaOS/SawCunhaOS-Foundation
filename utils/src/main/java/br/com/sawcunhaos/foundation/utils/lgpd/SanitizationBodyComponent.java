
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

package br.com.sawcunhaos.foundation.utils.lgpd;

import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SanitizationBodyComponent {

    private final DataMaskingService dataMaskingService;
    private static final String BODY_IS_EMPTY = "Does not have Body";
    private final Gson gson = GsonUtils.getInstance();

    public String sanitizeBody(final InputStream bodyInputStream){
        log.debug("Starting sanitize body");
        String bodyValue = new BufferedReader(
                new InputStreamReader(bodyInputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        return sanitizeBody(bodyValue);
    }

    public String sanitizeBody(final String bodyValue){
        log.debug("Starting sanitize body");
        if(!bodyValue.isEmpty()) {
            try {
                JsonElement json = gson.fromJson(bodyValue, JsonElement.class);
                if(json.isJsonObject() && json instanceof JsonObject bodyJson) {
                    return processBody(bodyJson);
                }
                if(json.isJsonArray() && json instanceof JsonArray arrayJson) {
                    JsonArray objects = new JsonArray();
                    arrayJson.forEach(jsonElement -> {
                        if(jsonElement.isJsonObject() || jsonElement.isJsonArray()) {
                            String jsonSanitized = sanitizeBody(jsonElement.toString());
                            objects.add(gson.fromJson(jsonSanitized, jsonElement.getClass()));
                        } else {
                            objects.add(jsonElement);
                        }
                    });
                    return gson.toJson(objects);
                }
                return bodyValue;
            } catch (Exception exception) {
                log.error("Erro format Body", exception);
                return bodyValue;
            }
        }
        return BODY_IS_EMPTY;
    }

    private String processBody(JsonObject jsonObject) {
        if(jsonObject.isJsonArray()) {
            return sanitizeBody(jsonObject.getAsJsonArray().toString());
        }

        jsonObject.keySet().forEach( key -> {
                JsonElement jsonElementResponse = jsonObject.get(key);
                if(jsonElementResponse.isJsonPrimitive() || jsonElementResponse.isJsonNull()) {
                    jsonObject.add(key, dataMaskingService.applyDataMaskValueBody(key, jsonElementResponse));
                } else {
                    if(jsonElementResponse.isJsonObject() || jsonElementResponse.isJsonArray()) {
                        String jsonResponse = sanitizeBody(jsonElementResponse.toString());
                        jsonObject.add(key, gson.fromJson(jsonResponse, jsonElementResponse.getClass()));
                    }
                }
            }
        );

        return gson.toJson(jsonObject);
    }

}
