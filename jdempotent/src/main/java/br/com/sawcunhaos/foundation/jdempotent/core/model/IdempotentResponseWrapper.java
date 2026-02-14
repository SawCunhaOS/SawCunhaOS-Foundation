package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Wraps the incoming event response
 *
 */
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
public class IdempotentResponseWrapper implements Serializable {

    private Object response;

    public IdempotentResponseWrapper(Object response) {
        this.response = response;
    }

    @Override
    public int hashCode() {
        return response == null ? 0 : response.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return response != null && response.equals(obj);
    }

    @Override
    public String toString() {
        return String.format("IdempotentResponseWrapper [response=%s]", response);
    }
}
