package si.fri.pretvornikprotokolov.common.interfaces;

import si.fri.pretvornikprotokolov.common.exceptions.HandlerException;

import java.time.Duration;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
public interface RequestHandler<T, R> {

    default void publish(T data, String subject) throws HandlerException {
        // Modbus does not support this pattern
    }

    default void subscribe(String subject, Callback<R> callback) {
        // Modbus does not support this pattern
    }

    default void requestStream(T data, String subject, String replyTo, Duration duration, Callback<R> callback) throws HandlerException {
        // Modbus does not support this pattern
        // Implement streaming logic here
    }

    void requestReply(T data, String subject, Callback<R> callback) throws HandlerException;

    default void requestReplyToMultiple(T data, String subject, String replyTo, Callback<byte[]> callback) throws HandlerException {
        // Modbus does not support this pattern
    }

    void disconnect();

    // Interface for callback
    interface Callback<R> {
        void onNext(R response);
    }
}
