package br.com.sawcunhaos.foundation.security.keycloak;

import br.com.sawcunhaos.foundation.exception.error.ScosSecurityException;
import br.com.sawcunhaos.foundation.security.service.ScosSecurityService;
import br.com.sawcunhaos.foundation.security.utils.SecurityExceptionCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RequiredArgsConstructor
@AutoConfiguration
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String PREFERRED_USERNAME = "preferred_username";
    private final ScosSecurityService scosSecurityService;

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        String login = getPrincipalClaimName(jwt);

        if (login == null) {
            throw new ScosSecurityException(SecurityExceptionCode.AUTH_001);
        }

        return new JwtAuthenticationToken(
                jwt,
                scosSecurityService.getAllGrantedAuthority(login),
                login
        );
    }

    private String getPrincipalClaimName(Jwt jwt) {
        return jwt.getClaim(PREFERRED_USERNAME);
    }
}
