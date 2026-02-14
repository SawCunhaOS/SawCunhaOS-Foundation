package br.com.sawcunhaos.foundation.audit.enums;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode implements ExceptionCode {

    ERROR("INS-001"),
    ERROR_ARGS("INS-002"),
    ERROR_NOT_EXIST("INS-003");

    private final String code;
}
