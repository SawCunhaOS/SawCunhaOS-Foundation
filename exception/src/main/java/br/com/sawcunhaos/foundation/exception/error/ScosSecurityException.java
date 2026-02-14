package br.com.sawcunhaos.foundation.exception.error;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScosSecurityException extends RuntimeException {
    private final String code;
	private final Object[] args;

	public ScosSecurityException(ExceptionCode code) {
		super();
		this.code = code.getCode();
		this.args = null;
	}

	public ScosSecurityException(ExceptionCode code, Object... args) {
		super();
		this.code = code.getCode();
		this.args = args;
	}
}
