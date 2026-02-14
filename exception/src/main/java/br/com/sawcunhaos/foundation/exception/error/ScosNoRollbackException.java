package br.com.sawcunhaos.foundation.exception.error;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.Getter;

@Getter
public class ScosNoRollbackException extends RuntimeException {
    private final String code;
	public ScosNoRollbackException(ExceptionCode code) {
		super();
		this.code = code.getCode();
	}
}
