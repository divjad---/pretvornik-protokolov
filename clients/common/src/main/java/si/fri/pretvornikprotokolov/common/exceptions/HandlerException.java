package si.fri.pretvornikprotokolov.common.exceptions;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
public class HandlerException extends Exception {
    public HandlerException(String message) {
        super(message);
    }

    public HandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
