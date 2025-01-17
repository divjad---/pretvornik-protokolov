package si.fri.pretvornikprotokolov.transformations.configuration.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
public class ConnectionModel {

    // Common
    @EqualsAndHashCode.Exclude
    @JsonProperty("ime")
    private String name;

    @JsonProperty("tip")
    private String type;

    @JsonProperty("naslov")
    private String host;

    @JsonProperty("vrata")
    private Integer port;

    @JsonProperty("uporabnik")
    private String username;

    @JsonProperty("geslo")
    private String password;

    private SslModel ssl = new SslModel();

    @EqualsAndHashCode.Exclude
    @JsonProperty("ponovno-povezi")
    private Boolean reconnect = false;

    // MQTT
    @JsonProperty("verzija")
    private Integer version = 5;

    // Modbus
    @JsonProperty("naprava")
    private String device;

    @JsonProperty("baudna-hitrost")
    private Integer baudRate;

    @JsonProperty("podatkovni-biti")
    private Integer dataBits;

    @JsonProperty("parnost")
    private String parity;

    @JsonProperty("koncni-biti")
    private Integer stopBits;
}
