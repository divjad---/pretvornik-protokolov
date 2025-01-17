package si.fri.pretvornikprotokolov.nats;

import io.nats.client.*;
import io.nats.client.impl.NatsMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.exceptions.HandlerException;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@ApplicationScoped
public class NatsConnection implements ConnectionListener, MessageHandler {

    @Getter
    @Setter
    private Boolean reconnect = true;

    private Connection connection;

    private Dispatcher dispatcher;

    public void disconnect() {
        log.debug("Destroying NATS connection");

        if (getConnection().isPresent()) {
            getConnection().get().removeConnectionListener(this);
            getConnection().get().closeDispatcher(dispatcher);

            try {
                getConnection().get().close();
            } catch (InterruptedException e) {
                log.error("Error closing NATS connection", e);
            } finally {
                this.connection = null;
                this.dispatcher = null;
            }
        }
    }

    // Connect methods
    public void connectAsync(Options options, boolean reconnect) throws InterruptedException {
        Nats.connectAsynchronously(options, reconnect);
    }

    public void connectAsync(Options options) throws InterruptedException {
        connectAsync(options, reconnect);
    }

    public void connectAsync() throws InterruptedException {
        Options options = Options.builder()
                .connectionListener(this)
                .build();

        connectAsync(options);
    }

    public Connection connectSync(Options options, boolean reconnect) throws IOException, InterruptedException {
        if (reconnect) {
            this.connection = Nats.connectReconnectOnConnect(options);
        } else {
            this.connection = Nats.connect(options);
        }

        if (dispatcher == null) {
            this.dispatcher = this.connection.createDispatcher();
        }

        return this.connection;
    }

    public Connection connectSync(Options options) throws IOException, InterruptedException {
        return connectSync(options, reconnect);
    }

    public Connection connectSync() throws IOException, InterruptedException {
        Options options = Options.builder().build();

        return connectSync(options);
    }

    public Optional<Connection> getConnection() {
        return Optional.ofNullable(this.connection);
    }

    public Optional<Dispatcher> getDispatcher() {
        return Optional.ofNullable(this.dispatcher);
    }

    @Override
    public void connectionEvent(Connection connection, Events events) {
        log.debug("Connection event: {}", events);
        log.debug("Connection: {}", connection);

        if (events == Events.CONNECTED) {
            log.debug("Connected to NATS");

            this.connection = connection;

            if (dispatcher == null) {
                this.dispatcher = this.connection.createDispatcher();
            }
        } else {
            this.connection = null;
            this.dispatcher = null;
        }
    }

    public CompletableFuture<Message> requestReply(Message message) throws HandlerException {
        return getConnection().orElseThrow(() -> new HandlerException("Connection is null")).request(message);
    }

    public CompletableFuture<Message> requestReply(String subject, byte[] data) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .data(data)
                .build();

        return requestReply(msg);
    }

    public CompletableFuture<Message> requestReply(String subject, String data) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .data(data)
                .build();

        return requestReply(msg);
    }

    public void requestReplyToMultiple(Message message) throws HandlerException {
        getConnection().orElseThrow(() -> new HandlerException("Connection is null")).publish(message);
    }

    public CompletableFuture<Message> requestStream(Message message) throws HandlerException {
        return getConnection().orElseThrow(() -> new HandlerException("Connection is null")).request(message);
    }

    public Subscription subscribe(String subject, MessageHandler handler, Dispatcher dispatcher) {
        return dispatcher.subscribe(subject, handler);
    }

    public Subscription subscribe(String subject, MessageHandler handler) {
        return subscribe(subject, handler, this.dispatcher);
    }

    public Subscription subscribe(String subject) {
        return subscribe(subject, this);
    }

    public void publish(Message msg) throws HandlerException {
        getConnection().orElseThrow(() -> new HandlerException("Connection is null")).publish(msg);
    }

    public void publish(String subject, String replyTo, byte[] data) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .replyTo(replyTo)
                .data(data)
                .build();

        publish(msg);
    }

    public void publish(String subject, String replyTo, String data) throws HandlerException {
        Message msg = NatsMessage.builder()
                .subject(subject)
                .replyTo(replyTo)
                .data(data)
                .build();

        publish(msg);
    }

    @Override
    public void onMessage(Message message) {
        log.debug("Received message: {}", message);

        message.ack();
    }
}
