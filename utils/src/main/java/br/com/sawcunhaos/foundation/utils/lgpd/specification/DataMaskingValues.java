package br.com.sawcunhaos.foundation.utils.lgpd.specification;

import br.com.sawcunhaos.foundation.utils.lgpd.model.DataMask;

import java.util.Set;

public interface DataMaskingValues {

    default Set<DataMask> headersValue() {
        return Set.of();
    }
    default Set<DataMask> bodyValue() {
        return Set.of();
    }

}
