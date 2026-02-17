package br.com.sawcunhaos.foundation.security.domain.cache;

import lombok.Builder;

import java.io.Serializable;
import java.util.Set;

@Builder
public record PermissionCacheDTO(
        String feature,
        Set<String> permissions
) implements Serializable {

}
