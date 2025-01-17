package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
public class ConfigurationModel {

    private String version = "1.0.0";
    private String timestamp = null;

    @JsonProperty("transformacije")
    private List<TransformationModel> transformations = new ArrayList<>();

    @JsonProperty("povezave")
    private List<ConnectionModel> connections = new ArrayList<>();

    @JsonProperty("registracija")
    private RegistrationModel registration = new RegistrationModel();
}
