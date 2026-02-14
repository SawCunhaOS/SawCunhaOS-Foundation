package br.com.sawcunhaos.foundation.jdempotent.core.utils;


import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentRequestPayload;
import br.com.sawcunhaos.foundation.utils.annotation.jdempotent.JdempotentResource;
import org.springframework.stereotype.Component;

@Component
public class TestIdempotentResource {
    @JdempotentResource
    public void idempotentMethod(IdempotentTestPayload testObject) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodThrowingARuntimeException(IdempotentTestPayload testObject) {
        throw new TestException();
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithThreeParameter(@JdempotentRequestPayload IdempotentTestPayload testObject, IdempotentTestPayload anotherObject, IdempotentTestPayload anotherObject2) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithThreeParamaterAndMultipleJdempotentRequestPayloadAnnotation(IdempotentTestPayload testObject, @JdempotentRequestPayload IdempotentTestPayload anotherObject, @JdempotentRequestPayload Object anotherObject2) {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void idempotentMethodWithZeroParamater() {
    }

    @JdempotentResource(cachePrefix = "TestIdempotentResource")
    public void methodWithTwoParamater(IdempotentTestPayload testObject, IdempotentTestPayload anotherObject) {
    }

    @JdempotentResource
    public IdempotentTestPayload idempotentMethodReturnArg(IdempotentTestPayload testObject) {
        return testObject;
    }

    @JdempotentResource
    public String idempotencyKeyAsString(@JdempotentRequestPayload String idempotencyKey) {
        return idempotencyKey;
    }
}
