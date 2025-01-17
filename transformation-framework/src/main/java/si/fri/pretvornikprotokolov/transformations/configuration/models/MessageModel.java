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
public class MessageModel {

    @JsonProperty("poslji-na-temo")
    private String toTopic;

    @JsonProperty("prejmi-iz-teme")
    private String fromTopic;

    @JsonProperty("sporocilo")
    private String message;

    @JsonProperty("modbus-funkcijska-koda")
    private Integer functionCode;

    @JsonProperty("modbus-id-naprave")
    private Integer deviceId;

    @JsonProperty("modbus-registri")
    private List<ModbusModel> modbusRegisters = new ArrayList<>();

    @JsonProperty("ponovi-sporocilo")
    private Integer retryCount = 0;
}
