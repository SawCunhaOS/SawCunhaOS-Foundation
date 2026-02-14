package br.com.sawcunhaos.foundation.audit.service;

import br.com.sawcunhaos.foundation.audit.enums.ErrorCode;
import br.com.sawcunhaos.foundation.exception.error.ScosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class InsideAuditExampleService {

    public BigDecimal multiplication(BigDecimal valueOne, BigDecimal valueTwo) {
        return valueOne.multiply(valueTwo);
    }

    public BigDecimal divide(BigDecimal valueOne, BigDecimal valueTwo) {
        return valueOne.divide(valueTwo);
    }

    public void showLog() {
        log.info("Show Log");
    }

    public void showException(final ErrorCode code, final Object... args) {
        throw new ScosException(code, args);
    }

}
