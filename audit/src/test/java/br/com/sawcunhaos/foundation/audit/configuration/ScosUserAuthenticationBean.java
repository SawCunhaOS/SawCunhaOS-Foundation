package br.com.sawcunhaos.foundation.audit.configuration;

import br.com.sawcunhaos.foundation.utils.specification.ScosUserAuthentication;
import org.springframework.stereotype.Service;

@Service("ScosUserAuthentication")
public class ScosUserAuthenticationBean implements ScosUserAuthentication {
    @Override
    public String findUserAuthentication() {
        return "Test";
    }
}
