package br.com.sawcunhaos.foundation.jdempotent.core.utils;

import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentIgnore;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentProperty;
import lombok.Data;

@Data
public class IdempotentTestPayload {
    private String name;
    @JdempotentIgnore
    private Long age;

    @JdempotentProperty("transactionId")
    private Long eventId;

    public IdempotentTestPayload() {
    }

    public IdempotentTestPayload(String name) {
        this.name = name;
    }
}
