package si.fri.pretvornikprotokolov.mqtt;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public class Mqtt3Client extends AbstractMqtt3RequestHandler {

    public Mqtt3Client(Mqtt3AsyncClient client) {
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
