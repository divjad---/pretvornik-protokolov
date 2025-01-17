package si.fri.pretvornikprotokolov.transformations.transformation;

import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.common.exceptions.HandlerException;
import si.fri.pretvornikprotokolov.common.interfaces.RequestHandler;
import si.fri.pretvornikprotokolov.transformations.configuration.Configuration;
import si.fri.pretvornikprotokolov.transformations.configuration.models.ConfigurationModel;
import si.fri.pretvornikprotokolov.transformations.configuration.models.RegistrationModel;
import si.fri.pretvornikprotokolov.transformations.configuration.models.TransformationModel;
import si.fri.pretvornikprotokolov.transformations.connections.Connections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@ApplicationScoped
public class TransformationsHandler {

    @Inject
    private Configuration configuration;

    @Inject
    private ObjectTransformer objectTransformer;

    private final ArrayList<TransformationHandler> transformationHandlers = new ArrayList<>();

    public void startHandling() {
        configuration.setConsumer(this::restart);
        handleTransformations();
    }

    private void handleTransformations() {
        Connections connections = new Connections(configuration);
        for (ConfigurationModel configurationModel : configuration.getConfigurations()) {
            RegistrationModel registration = configurationModel.getRegistration();

            for (String connectionName : registration.getOutgoingConnections()) {
                RequestHandler requestHandler = connections.getConnectionsMap().get(connectionName);
                if (requestHandler != null) {
                    try {
                        String topic = TransformationHandler.replacePlaceholders(registration.getTopic());
                        requestHandler.publish(registration.getMessage(), topic);
                    } catch (HandlerException e) {
                        log.error("Error publishing registration message.", e);
                    }
                }
            }

            for (TransformationModel transformationModel : configurationModel.getTransformations()) {
                TransformationHandler handler = new TransformationHandler(transformationModel, objectTransformer, connections);
                transformationHandlers.add(handler);
                handler.handle();
            }
        }
    }

    private void restart(Boolean b) {
        for (TransformationHandler handler : transformationHandlers) {
            handler.destroy();
        }

        transformationHandlers.clear();
        handleTransformations();
    }
}
