package br.com.sawcunhaos.foundation.jdempotent.core.datasource;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of the idempotent AbstractIdempotentRepository
 * that uses as a default map
 */
public class InMemoryIdempotentRepository extends AbstractIdempotentRepository {

    private final ConcurrentHashMap<IdempotencyKey, IdempotentRequestResponseWrapper> map;

    public InMemoryIdempotentRepository() {
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    protected Map<IdempotencyKey, IdempotentRequestResponseWrapper> getMap() {
        return map;
    }

}
