package si.fri.pretvornikprotokolov.mqtt;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public class Mqtt5Client extends AbstractMqtt5RequestHandler {

    public Mqtt5Client(Mqtt5AsyncClient client) {
        super(client);
    }

    @Override
    public String processReplyRequest(String fromTopic, byte[] data) {
        return "processReplyRequest";
    }

    @Override
    public String processStreamRequest(String fromTopic, byte[] data) {
        return "processStreamRequest";
    }
}
