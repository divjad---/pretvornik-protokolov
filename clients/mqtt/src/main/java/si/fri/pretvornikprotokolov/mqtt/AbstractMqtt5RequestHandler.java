package si.fri.pretvornikprotokolov.mqtt;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.connect.connack.Mqtt5ConnAck;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.AbstractRequestHandler;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;
import si.fri.pretvornikprotokolov.common.models.MqttMessage;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractMqtt5RequestHandler extends AbstractRequestHandler<Mqtt5AsyncClient, Mqtt5Publish> implements RequestHandler<String, byte[]> {

    protected AbstractMqtt5RequestHandler(Mqtt5AsyncClient client) {
        this.client = client;
    }

    @Override
    public Mqtt5AsyncClient getClient() {
        return this.client;
    }

    public CompletableFuture<Mqtt5ConnAck> connect() {
        return client.connect();
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

        Mqtt5Publish publish = Mqtt5Publish.builder()
                .topic(subject)
                .payload(data.getBytes())
                .build();

        Mqtt5PublishResult publishResult = client.publish(publish).join();

        log.debug("Published message: {}", publishResult);
    }

    @Override
    public void subscribe(String subject, Callback<byte[]> callback) {
        log.debug("Subscribing to topic: {}", subject);

        Mqtt5SubAck ack = client.subscribeWith()
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
        Mqtt5SubAck ack = client.subscribeWith()
                .topicFilter(replyTo)
                .callback(message -> {
                    MqttMessage mqttMessage = MqttMessage.fromJson(message.getPayloadAsBytes());
                    callback.onNext(mqttMessage.getContent().getBytes());
                }).send().join();

        log.debug("Subscribed to stream topic: {}", ack);

        MqttMessage mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                .content(data)
                .replyTo(replyTo)
                .duration(duration)
                .build();

        Mqtt5PublishResult publishResult = client.publishWith()
                .topic(subject)

                .payload(mqttMessage.toJsonString().getBytes())
                .send().join();

        log.debug("Published message: {}", publishResult);
    }

    @Override
    public void requestReplyToMultiple(String data, String subject, String replyTo, Callback<byte[]> callback) {
        Mqtt5SubAck ack = client.subscribeWith()
                .topicFilter(replyTo)
                .callback(message -> {
                    MqttMessage mqttMessage = MqttMessage.fromJson(message.getPayloadAsBytes());
                    callback.onNext(mqttMessage.getContent().getBytes());
                }).send().join();

        log.debug("Subscribed to reply topic: {}", ack);

        MqttMessage mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                .content(data)
                .replyTo(replyTo)
                .build();

        Mqtt5PublishResult publishResult = client.publishWith()
                .topic(subject)
                .payload(mqttMessage.toJsonString().getBytes())
                .send().join();

        log.debug("Published message: {}", publishResult);
    }

    @Override
    public void requestReply(String data, String subject, Callback<byte[]> callback) {
        String replyTopic = subject + System.currentTimeMillis() + client.getConfig().getClientIdentifier().orElse(null);

        requestReplyToMultiple(data, subject, replyTopic, callback);
    }

    @Override
    protected void handleRequestReply(String subject, Mqtt5Publish mqtt5Publish) {
        MqttMessage mqttMessage = MqttMessage.fromJson(mqtt5Publish.getPayloadAsBytes());

        String replyTo = mqttMessage.getReplyTo();
        String reply = processReplyRequest(subject, mqttMessage.getContent().getBytes());

        mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                .content(reply)
                .build();

        client.publishWith()
                .topic(replyTo)
                .payload(mqttMessage.toJsonString().getBytes())
                .send().join();
    }

    @Override
    protected void handleStream(String subject, Mqtt5Publish mqtt5Publish) {
        MqttMessage mqttMessage = MqttMessage.fromJson(mqtt5Publish.getPayloadAsBytes());
        if (mqttMessage == null || mqttMessage.getDuration() == null) {
            throw new IllegalStateException("Duration header is missing");
        }

        String replyTo = mqttMessage.getReplyTo();
        String content = mqttMessage.getContent();

        Duration duration = mqttMessage.getDuration();

        int numOfMessages = Math.toIntExact(duration.toSeconds());
        for (int iii = 0; iii < numOfMessages; iii++) {
            log.debug("Sending message {} of {}", iii + 1, numOfMessages);

            String reply = processStreamRequest(subject, content.getBytes());

            mqttMessage = MqttMessage.MqttMessageBuilder.builder()
                    .content(reply)
                    .build();

            client.publishWith()
                    .topic(replyTo)
                    .payload(mqttMessage.toJsonString().getBytes())
                    .send().join();

            log.debug("Sent message {} of {}", iii + 1, numOfMessages);

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Error while sleeping", e);
            }
        }

        log.debug("Finished sending {} messages", numOfMessages);
    }
}

