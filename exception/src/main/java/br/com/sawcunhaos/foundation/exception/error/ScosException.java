package br.com.sawcunhaos.foundation.exception.error;

import br.com.sawcunhaos.foundation.utils.specification.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScosException extends RuntimeException {
    private final String code;
	private final Object[] args;

	public ScosException(ExceptionCode code) {
		super();
		this.code = code.getCode();
		this.args = null;
	}

	public ScosException(ExceptionCode code, Object... args) {
		super();
		this.code = code.getCode();
		this.args = args;
	}
}
