package br.com.sawcunhaos.foundation.jdempotent.core.datasource;

import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestResponseWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentResponseWrapper;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Includes all the methods of IdempotentRequestStore
 */
public abstract class AbstractIdempotentRepository implements IdempotentRepository {

    @Override
    public boolean contains(IdempotencyKey key) {
        return getMap().containsKey(key);
    }

    @Override
    public IdempotentResponseWrapper getResponse(IdempotencyKey key) {
        return getMap().containsKey(key) ? getMap().get(key).getResponse() : null;
    }

    @Override
    public void store(IdempotencyKey key, IdempotentRequestWrapper request,Long ttl, TimeUnit timeUnit) {
        getMap().put(key, new IdempotentRequestResponseWrapper(request));
    }

    @Override
    public void setResponse(IdempotencyKey key, IdempotentRequestWrapper request,
                            IdempotentResponseWrapper idempotentResponse, Long ttl, TimeUnit timeUnit) {
        if (getMap().containsKey(key)) {
            IdempotentRequestResponseWrapper requestResponseWrapper = getMap().get(key);
            requestResponseWrapper.setResponse(idempotentResponse);
            getMap().put(key, requestResponseWrapper);
        }
    }

    @Override
    public void remove(IdempotencyKey key) {
        getMap().remove(key);
    }


    /**
     * @return
     */
    protected abstract Map<IdempotencyKey, IdempotentRequestResponseWrapper> getMap();
}
