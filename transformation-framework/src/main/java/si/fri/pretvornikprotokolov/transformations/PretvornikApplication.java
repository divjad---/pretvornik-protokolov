package si.fri.pretvornikprotokolov.transformations;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import si.fri.pretvornikprotokolov.transformations.transformation.TransformationsHandler;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@ApplicationPath("/")
public class PretvornikApplication extends ResourceConfig {

    @Inject
    public PretvornikApplication(TransformationsHandler handler) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jersey.config.server.wadl.disableWadl", "true");
        setProperties(properties);
        packages("si.fri.pretvornikprotokolov.transformations");
        register(MultiPartFeature.class);

        try {
            handler.startHandling();
        } catch (Exception e) {
            log.error("Error starting transformations.", e);
            System.exit(1);
        }
    }
}
