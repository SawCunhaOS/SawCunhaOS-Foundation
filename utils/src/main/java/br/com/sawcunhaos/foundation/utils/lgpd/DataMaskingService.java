package br.com.sawcunhaos.foundation.utils.lgpd;

import br.com.sawcunhaos.foundation.utils.lgpd.model.DataMask;
import br.com.sawcunhaos.foundation.utils.lgpd.specification.DataMaskingValues;
import br.com.sawcunhaos.foundation.utils.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataMaskingService {

    @Autowired(required = false)
    private DataMaskingValues dataMaskingValues;
    private static final Gson gson = GsonUtils.getInstance();

    public String applyDataMaskValueHeader(final String key, final String value) {
        log.debug("Applying a mask to the Header Value");
        if(Objects.isNull(dataMaskingValues) || Objects.isNull(value)) return value;

        Optional<DataMask> dataMaskOptional = dataMaskingValues.headersValue().stream().filter(mask -> mask.key().equalsIgnoreCase(key)).findFirst();

        return applyDataMaskValue(dataMaskOptional, value);
    }

    public JsonElement applyDataMaskValueBody(final String key, final JsonElement value) {
        log.debug("Applying mask to Body Value");
        if(Objects.isNull(value)) return null;

        String valueMaks = applyDataMaskValueBody(key, value.toString());

        return gson.toJsonTree(valueMaks);
    }

    public String applyDataMaskValueBody(final String key, final String value) {
        log.debug("Applying mask to Body Value");
        if(Objects.isNull(dataMaskingValues) || Objects.isNull(value)) return value;

        Optional<DataMask> dataMaskOptional = dataMaskingValues.bodyValue().stream().filter(mask -> mask.key().equalsIgnoreCase(key)).findFirst();

        return applyDataMaskValue(dataMaskOptional, value);
    }



    private String applyDataMaskValue(Optional<DataMask> dataMaskOptional, final String value) {
        log.debug("Applying mask");
        if(dataMaskOptional.isEmpty()) {
            return value;
        }
        DataMask dataMask = dataMaskOptional.get();
        String newValue;
        if(dataMask.isRegex()) {
            newValue = value.replaceAll(dataMask.regex(), dataMask.newValue());
        } else {
            newValue = dataMask.newValue();
        }

        log.debug("Mask applied in value");
        return newValue;
    }

}
