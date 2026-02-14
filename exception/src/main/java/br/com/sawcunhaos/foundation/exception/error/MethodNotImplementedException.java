package br.com.sawcunhaos.foundation.exception.error;

public class MethodNotImplementedException extends RuntimeException {

    public MethodNotImplementedException() {
        super("Method not implemented");
    }

    public MethodNotImplementedException(String message) {
        super(message);
    }

    public MethodNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotImplementedException(Throwable cause) {
        super(cause);
    }

    protected MethodNotImplementedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
