package br.com.sawcunhaos.foundation.jdempotent.core.chain;

import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentProperty;

import java.lang.reflect.Field;

public class JdempotentPropertyAnnotationChain extends AnnotationChain {

    @Override
    public KeyValuePair process(ChainData chainData) throws IllegalAccessException {
        Field declaredField = chainData.getDeclaredField();
        declaredField.setAccessible(true);
        JdempotentProperty annotation = declaredField.getAnnotation(JdempotentProperty.class);
        if(annotation != null){
            return new KeyValuePair(annotation.value(),declaredField.get(chainData.getArgs()));
        }
        return super.nextChain.process(chainData);
    }
}
