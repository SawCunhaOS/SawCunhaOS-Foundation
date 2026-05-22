package br.com.sawcunhaos.foundation.security.utils;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum SecurityExceptionCode implements ExceptionCode {

    AUTH_001("AUTH-001"),
    AUTH_002("AUTH-002"),
    AUTH_003("AUTH-003"),
    AUTH_004("AUTH-004"),
    AUTH_005("AUTH-005"),
    AUTH_006("AUTH-006"),
    AUTH_007("AUTH-007"),
    AUTH_008("AUTH-008"),

    ;

    private final String code;

}
