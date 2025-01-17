package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
public class TransformationModel {

    @JsonProperty("ime")
    private String name = null;

    @JsonProperty("opis")
    private String description;

    @JsonProperty("povezave")
    private TransformationConnectionsModel connections;

    @JsonProperty("vhodno-sporocilo")
    private MessageModel toIncoming;

    @JsonProperty("intervalna-zahteva")
    private IntervalRequestModel intervalRequest;

    @JsonProperty("izhodno-sporocilo")
    private MessageModel toOutgoing;
}
