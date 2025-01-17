package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
public class TransformationConnectionsModel {

    @JsonProperty("vhodna-tema")
    private String incomingTopic;

    @JsonProperty("izhodna-tema")
    private String outgoingTopic;

    @JsonProperty("vhodni-format")
    private String incomingFormat;

    @JsonProperty("izhodni-format")
    private String outgoingFormat;

    @JsonProperty("vhodne-povezave")
    private String[] incomingConnections;

    @JsonProperty("izhodne-povezave")
    private String[] outgoingConnections;
}
