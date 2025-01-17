package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
public class SslModel {

    @JsonProperty("privzeto")
    private Boolean useDefault = false;
}
