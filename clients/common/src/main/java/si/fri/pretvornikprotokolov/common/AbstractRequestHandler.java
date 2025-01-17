package si.fri.pretvornikprotokolov.common;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
public abstract class AbstractRequestHandler<E, M> {

    protected E client;

    public abstract E getClient();

    public abstract String processStreamRequest(String fromTopic, byte[] data);

    public abstract String processReplyRequest(String fromTopic, byte[] data);

    protected void handleRequestReply(String subject, M message) {
        // Modbus does not support this pattern
    }

    protected void handleStream(String subject, M message) {
        // Modbus does not support this pattern
    }
}
