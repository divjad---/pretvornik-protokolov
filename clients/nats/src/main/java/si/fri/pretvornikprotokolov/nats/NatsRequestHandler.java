package si.fri.pretvornikprotokolov.nats;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@ApplicationScoped
public class NatsRequestHandler extends AbstractNatsRequestHandler {

    @Inject
    public NatsRequestHandler(NatsConnection client1) {
        super(client1);
    }

    @Override
    public String processReplyRequest(String subject, byte[] data) {
        return "processReplyRequest";
    }

    @Override
    public String processStreamRequest(String subject, byte[] data) {
        return "processStreamRequest";
    }
}
