package br.com.sawcunhaos.foundation.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Constant {

    REQUEST_ID_HEADER("X-Request-ID");

    private final String value;
}
