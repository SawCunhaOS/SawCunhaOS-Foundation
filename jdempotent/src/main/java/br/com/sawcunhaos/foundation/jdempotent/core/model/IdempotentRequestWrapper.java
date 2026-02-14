package br.com.sawcunhaos.foundation.jdempotent.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 *
 * Wraps the incoming event value
 *
 */
@Setter
@Getter
@NoArgsConstructor
@SuppressWarnings("serial")
public class IdempotentRequestWrapper implements Serializable {
    private List<Object> request;

    public IdempotentRequestWrapper(Object request) {
        this.request = Collections.singletonList(request);
    }

    public IdempotentRequestWrapper(List<Object> request) {
        this.request = request;
    }

    @Override
    public int hashCode() {
        return request == null ? 0 : request.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return !Objects.isNull(request) && request.stream().anyMatch(req -> req.equals(obj));
    }

    @Override
    public String toString() {
        StringBuilder requestBuilder = new StringBuilder();
        this.request.stream()
                .map(Object::toString)
                .toList().stream()
                .sorted(String::compareTo)
                .forEach(requestBuilder::append);

        return String.format("IdempotentRequestWrapper [request=%s]", requestBuilder);
    }
}
