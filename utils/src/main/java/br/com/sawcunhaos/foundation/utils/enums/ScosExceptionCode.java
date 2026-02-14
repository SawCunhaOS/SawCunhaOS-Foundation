package br.com.sawcunhaos.foundation.utils.enums;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ScosExceptionCode implements ExceptionCode {
    ATTRIBUTE_NOT_VALID("INS-001"),
    ENUM_ERROR("INS-002"),
    GENERIC("INS-003"),
    ACCESS_DENIED("INS-004"),
    TOKEN_NOT_PROVIDED("INS-005");

    private final String code;
}
