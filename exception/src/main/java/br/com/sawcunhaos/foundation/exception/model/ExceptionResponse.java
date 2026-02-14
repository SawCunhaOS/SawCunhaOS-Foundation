package br.com.sawcunhaos.foundation.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public record ExceptionResponse(
        String message,
        String codeError,
        List<AttributeNotValid> validationErrors
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 5688741746859700020L;

}
