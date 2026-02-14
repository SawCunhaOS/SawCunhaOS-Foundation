package br.com.sawcunhaos.foundation.exception.model;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record AttributeNotValid(
        String attribute,
        String message
) implements Serializable {
    @Serial
    private static final long serialVersionUID = -3648581516760025926L;
}
