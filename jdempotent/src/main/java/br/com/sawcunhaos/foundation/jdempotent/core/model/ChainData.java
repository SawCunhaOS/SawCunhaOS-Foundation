package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainData {

    private Field declaredField;
    private Object args;

}
