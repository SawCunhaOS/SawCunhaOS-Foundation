package br.com.sawcunhaos.foundation.utils.utils;

import br.com.sawcunhaos.foundation.utils.adapter.LocalDateAdapter;
import br.com.sawcunhaos.foundation.utils.adapter.LocalDateTimeAdapter;
import br.com.sawcunhaos.foundation.utils.adapter.LocalTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GsonUtils {

    private static Gson gson = null;

    public static Gson getInstance() {
        if(Objects.isNull(gson)) {
            gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                    .create();
        }
        return gson;
    }



}
