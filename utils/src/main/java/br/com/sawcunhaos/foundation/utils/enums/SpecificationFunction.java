package br.com.sawcunhaos.foundation.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpecificationFunction {

    DAY("DAY", null),
    MONTH("MONTH", null),
    YEAR("YEAR", null),
    DATE_PART_DAY("DATE_PART", "DAY"),
    DATE_PART_MONTH("DATE_PART", "MONTH"),
    DATE_PART_YEAR("DATE_PART", "YEAR");

    private final String function;
    private final String param;

}
