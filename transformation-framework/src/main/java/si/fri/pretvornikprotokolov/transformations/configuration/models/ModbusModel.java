package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class ModbusModel {

    @JsonProperty("naslov-registra")
    private Integer address;

    @JsonProperty("pot")
    private String path;

    @JsonProperty("tip")
    private String type;

    @JsonProperty("vzorec")
    private String pattern;

    @JsonProperty("vrednosti")
    private String[] values;
}
