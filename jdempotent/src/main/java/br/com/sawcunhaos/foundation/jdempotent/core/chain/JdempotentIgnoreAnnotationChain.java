package br.com.sawcunhaos.foundation.jdempotent.core.chain;

import br.com.sawcunhaos.foundation.jdempotent.core.model.ChainData;
import br.com.sawcunhaos.foundation.jdempotent.core.model.KeyValuePair;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentIgnore;

import java.lang.reflect.Field;

public class JdempotentIgnoreAnnotationChain extends AnnotationChain {
    @Override
    public KeyValuePair process(ChainData chainData) throws IllegalAccessException {
        Field declaredField = chainData.getDeclaredField();
        declaredField.setAccessible(true);
        JdempotentIgnore annotation = declaredField.getAnnotation(JdempotentIgnore.class);
        if(annotation != null){
            return new KeyValuePair();
        }
        return super.nextChain.process(chainData);
    }
}
