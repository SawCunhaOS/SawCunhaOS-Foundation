package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 *
 *  That is a container for idempotent requests and responses
 *
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("serial")
public class IdempotentRequestResponseWrapper implements Serializable {
    private IdempotentRequestWrapper request;
    private IdempotentResponseWrapper response = null;

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request) {
        this.request = request;
    }

    public IdempotentRequestResponseWrapper(IdempotentRequestWrapper request, IdempotentResponseWrapper response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("IdempotentRequestResponseWrapper [request=%s, response=%s]", request, response);
    }
}
