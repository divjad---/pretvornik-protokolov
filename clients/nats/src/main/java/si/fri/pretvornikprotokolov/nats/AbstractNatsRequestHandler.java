package si.fri.pretvornikprotokolov.nats;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.AbstractRequestHandler;
import si.fri.pretvornikprotokolov.common.constants.Constants;
import si.fri.pretvornikprotokolov.common.exceptions.HandlerException;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractNatsRequestHandler extends AbstractRequestHandler<NatsConnection, Message> implements RequestHandler<String, byte[]> {

    protected AbstractNatsRequestHandler(NatsConnection client) {
        this.client = client;
    }

    @Override
    public NatsConnection getClient() {
        return this.client;
    }

    @Override
    public void publish(String request, String subject) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .data(request)
                .build();

        client.publish(msg);
    }

    @Override
    public void subscribe(String subject, Callback<byte[]> callback) {
        client.subscribe(subject, message -> {
            if (message.getReplyTo() != null) {
                if (message.hasHeaders() && message.getHeaders().containsKey(Constants.DURATION)) {
                    handleStream(subject, message);
                    callback.onNext("Handled stream".getBytes(StandardCharsets.UTF_8));
                } else {
                    handleRequestReply(subject, message);
                    callback.onNext("Handled request".getBytes(StandardCharsets.UTF_8));
                }
            } else {
                callback.onNext(message.getData());
            }
        });
    }

    @Override
    public void requestStream(String request, String subject, String replyTo, Duration duration, Callback<byte[]> callback) throws HandlerException {
        client.subscribe(replyTo, message -> callback.onNext(message.getData()));

        Headers headers = new Headers();
        headers.add(Constants.DURATION, duration.toString());

        Message message = NatsMessage.builder()
                .subject(subject)
                .replyTo(replyTo)
                .data(request)
                .headers(headers)
                .build();

        client.requestReplyToMultiple(message);
    }

    @Override
    public void requestReplyToMultiple(String request, String subject, String replyTo, Callback<byte[]> callback) throws HandlerException {
        client.subscribe(replyTo, message -> callback.onNext(message.getData()));

        Message msg = NatsMessage.builder()
                .subject(subject)
                .replyTo(replyTo)
                .data(request)
                .build();

        client.requestReplyToMultiple(msg);
    }

    @Override
    public void requestReply(String request, String subject, Callback<byte[]> callback) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .data(request)
                .build();

        callback.onNext(client.requestReply(msg).join().getData());
    }

    @Override
    protected void handleRequestReply(String subject, Message message) {
        String reply = processReplyRequest(subject, message.getData());

        Message msg = NatsMessage.builder()
                .subject(message.getReplyTo())
                .data(reply)
                .build();

        try {
            client.publish(msg);
        } catch (HandlerException e) {
            log.error("Error while publishing reply", e);
        }
    }

    @Override
    protected void handleStream(String subject, Message message) {
        try {
            Headers headers = message.getHeaders();
            if (headers == null || headers.get(Constants.DURATION) == null || headers.get(Constants.DURATION).isEmpty()) {
                throw new IllegalArgumentException("Duration header is missing");
            }

            Duration duration = Duration.parse(headers.get(Constants.DURATION).get(0));

            int numOfMessages = Math.toIntExact(duration.toSeconds());
            for (int iii = 0; iii < numOfMessages; iii++) {
                String reply = processStreamRequest(subject, message.getData());

                Message msg = NatsMessage.builder()
                        .subject(message.getReplyTo())
                        .data(reply)
                        .build();
                client.publish(msg);

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            log.error("Error while publishing stream", e);
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            client.disconnect();
        }
    }
}

