package br.com.sawcunhaos.foundation.utils.configuration.feign;

import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class JacksonDecoderCustom implements Decoder {
    private final ObjectMapper mapper;

    public JacksonDecoderCustom(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Object decode(Response response, Type type) throws IOException {
        if (response.status() != 404 && response.status() != 204) {
            if (response.body() == null) {
                return null;
            } else {
                Reader reader = response.body().asReader(response.charset());
                if (!reader.markSupported()) {
                    reader = new BufferedReader(reader, 1);
                }

                try {
                    reader.mark(1);
                    if (reader.read() == -1) {
                        return null;
                    } else {
                        reader.reset();
                        return this.mapper.readValue(reader, this.mapper.constructType(type));
                    }
                } catch (JacksonException var5) {
                    if (var5.getCause() != null && var5.getCause() instanceof IOException) {
                        throw (IOException) var5.getCause();
                    } else {
                        throw var5;
                    }
                }
            }
        } else {
            return Util.emptyValueOf(type);
        }
    }
}
