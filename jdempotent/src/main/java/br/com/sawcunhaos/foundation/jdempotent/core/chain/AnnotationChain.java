package br.com.sawcunhaos.foundation.jdempotent.core.chain;


import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;

public abstract class AnnotationChain {
    protected AnnotationChain nextChain;

    public abstract KeyValuePair process(ChainData chainData) throws IllegalAccessException;

    public void next(AnnotationChain nextChain) {
        this.nextChain = nextChain;
    }
}
