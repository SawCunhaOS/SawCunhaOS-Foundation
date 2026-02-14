package br.com.sawcunhaos.foundation.utils.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JacksonXmlUtils {

    private static XmlMapper xmlMapper = null;

    public static XmlMapper getInstance() {
        if(Objects.isNull(xmlMapper)) {
            xmlMapper = XmlMapper.xmlBuilder()
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .build();
        }
        return xmlMapper;
    }

}
