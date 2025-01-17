package si.fri.pretvornikprotokolov.transformations.connections;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.serial.*;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import io.nats.client.Options;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;
import si.fri.pretvornikprotokolov.modbus.ModbusClient;
import si.fri.pretvornikprotokolov.mqtt.Mqtt3Client;
import si.fri.pretvornikprotokolov.mqtt.Mqtt5Client;
import si.fri.pretvornikprotokolov.nats.NatsConnection;
import si.fri.pretvornikprotokolov.nats.NatsRequestHandler;
import si.fri.pretvornikprotokolov.transformations.configuration.Configuration;
import si.fri.pretvornikprotokolov.transformations.configuration.models.ConnectionModel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public class Connections {

    private final Configuration configuration;

    @Getter
    private final Map<String, RequestHandler> connectionsMap = new HashMap<>();

    public Connections(Configuration configuration) {
        this.configuration = configuration;
        init();
    }

    public void init() {
        connectionsMap.clear();

        List<ConnectionModel> yamlConnections = configuration.getConfigurations().stream()
                .flatMap(item -> item.getConnections().stream())
                .toList();

        Map<ConnectionModel, RequestHandler> clientMap = new HashMap<>();

        log.debug("Found {} connections", yamlConnections.size());
        for (ConnectionModel connection : yamlConnections) {
            RequestHandler requestHandler = clientMap.get(connection);

            if (requestHandler != null) {
                log.debug("Connection {} already exists under different name", connection.getName());
                this.connectionsMap.put(connection.getName(), requestHandler);
                continue;
            }

            if (connection.getType().equalsIgnoreCase("NATS")) {
                NatsConnection client;
                try {
                    client = buildNatsClient(connection);
                    NatsRequestHandler natsRequestHandler = new NatsRequestHandler(client);
                    this.connectionsMap.put(connection.getName(), natsRequestHandler);
                    clientMap.put(connection, natsRequestHandler);
                } catch (InterruptedException | IOException e) {
                    log.error("Error building NATS client", e);
                }
            } else if (connection.getType().equalsIgnoreCase("MQTT")) {
                if (connection.getVersion() == 3) {
                    Mqtt3Client client = buildMqtt3Client(connection);
                    this.connectionsMap.put(connection.getName(), client);
                    clientMap.put(connection, client);
                } else if (connection.getVersion() == 5) {
                    Mqtt5Client client = buildMqtt5Client(connection);
                    this.connectionsMap.put(connection.getName(), client);
                    clientMap.put(connection, client);
                }
            } else if (connection.getType().equalsIgnoreCase("modbus")) {
                if (connection.getHost() == null && connection.getDevice() == null) {
                    throw new IllegalArgumentException("Host or device is required for modbus connection");
                }

                try {
                    Modbus.setLogLevel(Modbus.LogLevel.LEVEL_VERBOSE);
                    ModbusClient client = buildModbusClient(connection);
                    this.connectionsMap.put(connection.getName(), client);
                    clientMap.put(connection, client);
                } catch (UnknownHostException | SerialPortException e) {
                    log.error("Error building Modbus client", e);
                }
            }
        }
    }

    public Map<String, ModbusClient> getModbusConnections(String... connectionNames) {
        HashMap<String, ModbusClient> modbusConnections = new HashMap<>();
        for (Map.Entry<String, RequestHandler> entry : connectionsMap.entrySet()) {
            if (entry.getValue() instanceof ModbusClient handler) {
                if (connectionNames != null && connectionNames.length > 0
                        && !Arrays.asList(connectionNames).contains(entry.getKey())) {
                    continue;
                }

                modbusConnections.put(entry.getKey(), handler);
            }
        }
        return modbusConnections;
    }

    private NatsConnection buildNatsClient(ConnectionModel connection) throws IOException, InterruptedException {
        NatsConnection client = new NatsConnection();

        client.setReconnect(connection.getReconnect());

        Options.Builder optionsBuilder =
                new Options.Builder()
                        .server(connection.getHost() + ":" + connection.getPort());

        if (connection.getUsername() != null && connection.getPassword() != null) {
            optionsBuilder = optionsBuilder
                    .userInfo(connection.getUsername(),
                            connection.getPassword());
        }

        client.connectSync(optionsBuilder.build(),
                connection.getReconnect());

        return client;
    }

    private Mqtt3Client buildMqtt3Client(ConnectionModel connection) {
        Mqtt3ClientBuilder client = com.hivemq.client.mqtt.mqtt3.Mqtt3Client.builder()
                .identifier(connection.getName())
                .serverHost(connection.getHost())
                .serverPort(connection.getPort())
                .addDisconnectedListener(context -> log.debug("Disconnected from MQTT broker 3: {}", context.getCause()))
                .addConnectedListener(context -> log.debug("Connected to MQTT broker 3"));

        if (Boolean.TRUE.equals(connection.getReconnect())) {
            client = client.automaticReconnectWithDefaultConfig();
        }

        if (connection.getSsl() != null) {
            client = client.sslWithDefaultConfig();
        }

        if (connection.getUsername() != null && connection.getPassword() != null) {
            client = client.simpleAuth()
                    .username(connection.getUsername())
                    .password(connection.getPassword().getBytes())
                    .applySimpleAuth();
        }

        if (connection.getSsl() != null && Boolean.TRUE.equals(connection.getSsl().getUseDefault())) {
            client = client.sslWithDefaultConfig();
        }

        Mqtt3BlockingClient client1 = client.buildBlocking();
        client1.connect();

        return new Mqtt3Client(client1.toAsync());
    }

    private Mqtt5Client buildMqtt5Client(ConnectionModel connection) {
        Mqtt5ClientBuilder client = com.hivemq.client.mqtt.mqtt5.Mqtt5Client.builder()
                .identifier(connection.getName())
                .serverHost(connection.getHost())
                .serverPort(connection.getPort())
                .addDisconnectedListener(context -> log.debug("Disconnected from MQTT broker 5: {}", context.getCause()))
                .addConnectedListener(context -> log.debug("Connected to MQTT broker 5"));

        if (Boolean.TRUE.equals(connection.getReconnect())) {
            client = client.automaticReconnectWithDefaultConfig();
        }

        if (connection.getSsl() != null) {
            client = client.sslWithDefaultConfig();
        }

        if (connection.getUsername() != null && connection.getPassword() != null) {
            client = client.simpleAuth()
                    .username(connection.getUsername())
                    .password(connection.getPassword().getBytes())
                    .applySimpleAuth();
        }

        if (connection.getSsl() != null && Boolean.TRUE.equals(connection.getSsl().getUseDefault())) {
            client = client.sslWithDefaultConfig();
        }

        Mqtt5BlockingClient client1 = client.buildBlocking();
        client1.connect();

        return new Mqtt5Client(client1.toAsync());
    }

    private ModbusClient buildModbusClient(ConnectionModel connectionModel) throws SerialPortException, UnknownHostException {
        if (connectionModel.getHost() != null && connectionModel.getDevice() != null) {
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(connectionModel.getHost()));
            tcpParameters.setPort(connectionModel.getPort());
            SerialUtils.setSerialPortFactory(new SerialPortFactoryTcpServer(tcpParameters));
            SerialParameters serialParameters = getSerialParameters(connectionModel);

            return new ModbusClient(ModbusMasterFactory.createModbusMasterRTU(serialParameters));
        }

        if (connectionModel.getHost() != null) {
            // TCP client
            TcpParameters tcpParameters = new TcpParameters();
            tcpParameters.setHost(InetAddress.getByName(connectionModel.getHost()));
            tcpParameters.setPort(connectionModel.getPort());

            return new ModbusClient(ModbusMasterFactory.createModbusMasterTCP(tcpParameters));
        } else {
            // Serial client
            SerialUtils.setSerialPortFactory(new SerialPortFactoryJSerialComm());
            SerialParameters serialParameters = getSerialParameters(connectionModel);

            return new ModbusClient(ModbusMasterFactory.createModbusMasterRTU(serialParameters));
        }
    }

    private SerialParameters getSerialParameters(ConnectionModel connection) {
        SerialParameters serialParameters = new SerialParameters();
        if (connection.getDevice() == null) {
            throw new IllegalArgumentException("Device is required for serial connection");
        }
        serialParameters.setDevice(connection.getDevice());

        if (connection.getBaudRate() != null) {
            serialParameters.setBaudRate(SerialPort.BaudRate.getBaudRate(connection.getBaudRate()));
        }

        if (connection.getDataBits() != null) {
            serialParameters.setDataBits(connection.getDataBits());
        }

        if (connection.getParity() != null) {
            switch (connection.getParity()) {
                case "even":
                    serialParameters.setParity(SerialPort.Parity.EVEN);
                    break;
                case "odd":
                    serialParameters.setParity(SerialPort.Parity.ODD);
                    break;
                case "mark":
                    serialParameters.setParity(SerialPort.Parity.MARK);
                    break;
                case "space":
                    serialParameters.setParity(SerialPort.Parity.SPACE);
                    break;
                case "none":
                default:
                    serialParameters.setParity(SerialPort.Parity.NONE);
                    break;
            }
        }

        if (connection.getStopBits() != null) {
            serialParameters.setStopBits(connection.getStopBits());
        }
        return serialParameters;
    }
}
