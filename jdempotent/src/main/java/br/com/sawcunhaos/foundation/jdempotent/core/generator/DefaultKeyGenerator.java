package br.com.sawcunhaos.foundation.jdempotent.core.generator;

import br.com.sawcunhaos.foundation.jdempotent.core.constant.EnvironmentVariableUtils;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotencyKey;
import br.com.sawcunhaos.foundation.jdempotent.core.model.IdempotentRequestWrapper;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;

/**
 *
 *
 */
public class DefaultKeyGenerator implements KeyGenerator {

    private final String appName;

    public DefaultKeyGenerator() {
        appName = System.getenv(EnvironmentVariableUtils.APP_NAME);
    }

    /**
     *
     * Generates a idempotent key for incoming event
     *
     * @param requestObject
     * @param listenerName
     * @param builder
     * @param messageDigest
     * @return
     */
    public IdempotencyKey generateIdempotentKey(IdempotentRequestWrapper requestObject, String listenerName, StringBuilder builder, MessageDigest messageDigest) {
        messageDigest.update(requestObject.toString().getBytes());
        byte[] digest = messageDigest.digest();

        if (!StringUtils.isBlank(appName)) {
            builder.append(appName);
            builder.append("-");
        }

        if (!StringUtils.isBlank(listenerName)) {
            builder.append(listenerName);
            builder.append("-");
        }

        for (byte b : digest) {
            builder.append(Integer.toHexString(0xFF & b));
        }

        return new IdempotencyKey(builder.toString());
    }
}
