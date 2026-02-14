package br.com.sawcunhaos.foundation.utils.lgpd.model;

import lombok.Builder;

@Builder
public record DataMask(
    String key,
    String newValue,
    boolean isRegex,
    String regex
) {}
