package br.com.sawcunhaos.foundation.security.domain.cache;

import lombok.Builder;

import java.io.Serializable;
import java.util.Set;

@Builder
public record LoginCacheDTO(
        String login,
        Long profileId,
        String status,
        boolean active,
        Set<String> features
) implements Serializable {


}
