package si.fri.pretvornikprotokolov.transformations.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.transformations.constants.Constants;

import java.util.Arrays;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
public class JSONMapper extends AbstractMapper {

    @SneakyThrows
    public JSONMapper(String mapping) {
        JsonNode root = objectMapper.readTree(mapping);
        root = root.get(Constants.MAPPING_NAME);

        setPath(root.get("pot").asText());

        if (!getPath().startsWith("/")) {
            setPath("/" + getPath());
        }

        setPath(getPath().replace(".", "/"));

        setType(root.get("tip").asText());
        if (root.has("vzorec")) {
            setPattern(root.get("vzorec").asText());
        }

        if (root.has("vrednosti")) {
            String nodeValue = root.get("vrednosti").toString();
            String cleanedString = nodeValue.substring(1, nodeValue.length() - 1);
            cleanedString = cleanedString.replace("\"", "");
            setValues(Arrays.stream(cleanedString.split(",")).map(String::trim).toArray(String[]::new));
        }
    }

    public JSONMapper(String path, String type, String[] values, String pattern) {
        setPath(path);
        setType(type);
        setValues(values);
        setPattern(pattern);
    }
}
