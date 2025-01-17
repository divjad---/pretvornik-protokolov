package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
public class IntervalRequestModel {

    @JsonProperty("interval")
    private Integer interval;

    @JsonProperty("zahteva")
    private MessageModel request;
}
