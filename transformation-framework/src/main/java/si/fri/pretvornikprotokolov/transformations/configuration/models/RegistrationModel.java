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
public class RegistrationModel {

    @JsonProperty("tema")
    private String topic;

    @JsonProperty("sporocilo")
    private String message;

    @JsonProperty("izhodne-povezave")
    private List<String> outgoingConnections = new ArrayList<>();
}
