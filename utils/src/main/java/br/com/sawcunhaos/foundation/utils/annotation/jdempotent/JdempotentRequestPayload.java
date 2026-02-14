package br.com.sawcunhaos.foundation.utils.annotation.jdempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Add to the methods arguments that represents the idempotent request payload.
 *
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JdempotentRequestPayload {
}
