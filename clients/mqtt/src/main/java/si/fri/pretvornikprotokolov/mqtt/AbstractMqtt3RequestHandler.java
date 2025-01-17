package si.fri.pretvornikprotokolov.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.AbstractRequestHandler;
import si.fri.pretvornikprotokolov.common.constants.Constants;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;
import si.fri.pretvornikprotokolov.common.models.MqttMessage;

import javax.annotation.PreDestroy;
import javax.json.Json;
import javax.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractMqtt3RequestHandler extends AbstractRequestHandler<Mqtt3AsyncClient, Mqtt3Publish> implements RequestHandler<String, byte[]> {

    protected AbstractMqtt3RequestHandler(Mqtt3AsyncClient client) {
        this.client = client;
    }

    @Override
    public Mqtt3AsyncClient getClient() {
        return this.client;
    }

    @Override
    public void disconnect() {
        if (client.getState().isConnected()) {
            log.debug("Disconnecting MQTT client");
            client.disconnect();
        }
    }

    @Override
    public void publish(String data, String subject) {
        log.debug("Publishing message: {} to topic: {}", data, subject);

        if (data == null || data.isEmpty()) {
            log.error("Data is empty, topic: {}", subject);
            return;
        }

        Mqtt3Publish publish = client.publishWith()
                .topic(subject)
                .payload(data.getBytes())
                .send().join();

        log.debug("Published message: {}", publish);
    }

    @Override
    public void subscribe(String subject, Callback<byte[]> callback) {
        log.debug("Subscribing to topic: {}", subject);

        Mqtt3SubAck ack = client.subscribeWith()
                .topicFilter(subject)
                .callback(message -> {
                    MqttMessage mqttMessage = MqttMessage.fromJson(message.getPayloadAsBytes());

                    if (mqttMessage.getReplyTo() != null) {
                        if (mqttMessage.getDuration() != null) {
                            handleStream(subject, message);
                            callback.onNext("Handled stream".getBytes(StandardCharsets.UTF_8));
                        } else {
                            handleRequestReply(subject, message);
                            callback.onNext("Handled request".getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        callback.onNext(mqttMessage.getContent().getBytes());
                    }
                }).send().join();

        log.debug("Subscribed to topic: {}", ack);
    }

    @Override
    public void requestStream(String data, String subject, String replyTo, Duration duration, Callback<byte[]> callback) {
        Mqtt3SubAck ack = client.subscribeWith()
                .topicFilter(replyTo)
                .qos(MqttQos.EXACTLY_ONCE)
                .callback(message -> {
                    MqttMessage mqttMessage = MqttMessage.fromJson(message.getPayloadAsBytes());
                    callback.onNext(mqttMessage.getContent().getBytes());
                }).send().join();

        log.debug("Subscribed to stream topic: {}", ack);

        JsonObject json = Json.createObjectBuilder()
                .add(Constants.DURATION, duration.toMillis())
                .add(Constants.CONTENT, data)
                .add(Constants.REPLY_TO, replyTo)
                .build();

        Mqtt3Publish result = client.publishWith()
                .topic(subject)
                .payload(json.toString().getBytes())
                .send().join();

        log.debug("Published message: {}", result);
    }

    @Override
    public void requestReplyToMultiple(String data, String subject, String replyTo, Callback<byte[]> callback) {
        Mqtt3SubAck ack = client.subscribeWith()
                .topicFilter(replyTo)
                .callback(message -> {
                    MqttMessage mqttMessage = MqttMessage.fromJson(message.getPayloadAsBytes());
                    callback.onNext(mqttMessage.getContent().getBytes());
                }).send().join();

        log.debug("Subscribed to reply topic: {}", ack);

        JsonObject json = Json.createObjectBuilder()
                .add(Constants.REPLY_TO, replyTo)
                .add(Constants.CONTENT, data)
                .build();

        Mqtt3Publish result = client.publishWith()
                .topic(subject)
                .payload(json.toString().getBytes())
                .send().join();

        log.debug("Published message: {}", result);
    }

    @Override
    public void requestReply(String data, String subject, Callback<byte[]> callback) {
        String replyTopic = subject + System.currentTimeMillis() + client.getConfig().getClientIdentifier().orElse(null);

        requestReplyToMultiple(data, subject, replyTopic, callback);
    }

    @Override
    protected void handleRequestReply(String subject, Mqtt3Publish mqtt3Publish) {
        MqttMessage mqttMessage = MqttMessage.fromJson(mqtt3Publish.getPayloadAsBytes());

        String replyTo = mqttMessage.getReplyTo();
        String reply = processReplyRequest(subject, mqttMessage.getContent().getBytes());

        mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                .content(reply)
                .build();

        Mqtt3Publish publish = Mqtt3Publish.builder()
                .topic(replyTo)
                .payload(mqttMessage.toJsonString().getBytes())
                .build();

        client.publish(publish).join();
    }

    @Override
    protected void handleStream(String subject, Mqtt3Publish mqtt3Publish) {
        MqttMessage mqttMessage = MqttMessage.fromJson(mqtt3Publish.getPayloadAsBytes());
        if (mqttMessage == null || mqttMessage.getDuration() == null) {
            throw new IllegalStateException("Duration header is missing");
        }

        String replyTo = mqttMessage.getReplyTo();
        String content = mqttMessage.getContent();

        Duration duration = mqttMessage.getDuration();

        int numOfMessages = Math.toIntExact(duration.toSeconds());
        for (int iii = 0; iii < numOfMessages; iii++) {
            String reply = processStreamRequest(subject, content.getBytes());

            mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                    .content(reply)
                    .build();

            Mqtt3Publish publish = Mqtt3Publish.builder()
                    .topic(replyTo)
                    .payload(mqttMessage.toJsonString().getBytes())
                    .build();

            client.publish(publish).join();

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Error while sleeping", e);
            }
        }
    }
}

