package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class IdempotentIgnorableWrapper implements Serializable {

    private final Map<String, Object> nonIgnoredFields;

    public IdempotentIgnorableWrapper() {
        nonIgnoredFields = new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdempotentIgnorableWrapper wrapper = (IdempotentIgnorableWrapper) o;

        return Objects.equals(nonIgnoredFields, wrapper.nonIgnoredFields);
    }

    @Override
    public int hashCode() {
        return nonIgnoredFields.hashCode();
    }

    @Override
    public String toString() {
        return "IdempotentIgnorableWrapper{" +
                "nonIgnoredFields=" + nonIgnoredFields +
                '}';
    }
}
