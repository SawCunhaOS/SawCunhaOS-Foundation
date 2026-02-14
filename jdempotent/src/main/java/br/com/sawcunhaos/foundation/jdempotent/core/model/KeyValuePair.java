package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeyValuePair {
    private String key;
    private Object value;
}
