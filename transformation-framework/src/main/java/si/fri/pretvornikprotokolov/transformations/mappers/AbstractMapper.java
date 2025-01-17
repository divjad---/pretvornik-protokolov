package si.fri.pretvornikprotokolov.transformations.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Data
@Slf4j
public abstract class AbstractMapper {

    protected String path;
    protected String type;
    protected String[] values;
    protected String pattern;

    @ToString.Exclude
    protected DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    @ToString.Exclude
    protected ObjectMapper objectMapper = new ObjectMapper();

    @ToString.Exclude
    private final XPathFactory xpathFactory = XPathFactory.newInstance();

    protected AbstractMapper() {
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getMappedValueXML(String xmlInput) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlInput)));

            return getMappedValueXML(document);
        } catch (Exception e) {
            log.error("Error parsing XML", e);
        }

        return null;
    }

    public String getMappedValueXML(Document xmlInput) {
        try {
            // Parse the input XML string
            XPath xpath = xpathFactory.newXPath();

            // Example: Extract value using XPath expression
            String statusXPath = getPath();
            XPathExpression expr = xpath.compile(statusXPath);
            String value = (String) expr.evaluate(xmlInput, XPathConstants.STRING);

            log.debug("Value at XML path {} : {}", statusXPath, value);

            return getValue(value);
        } catch (Exception e) {
            log.error("Error parsing XML", e);
        }

        return null;
    }

    public String getMappedValueJSON(String jsonInput) throws JsonProcessingException, ParseException {
        JsonNode rootNode = objectMapper.readTree(jsonInput);

        return getMappedValueJSON(rootNode);
    }

    public String getMappedValueJSON(JsonNode jsonInput) throws ParseException {
        if (getPath().startsWith("//")) {
            setPath(getPath().substring(1));
        }
        setPath(getPath().replace(".", "/"));

        JsonNode resultNode = jsonInput.at(getPath());

        // Check if the result node exists
        if (!resultNode.isMissingNode()) {
            log.debug("Value at JSON path {} : {}", getPath(), resultNode);
            return getValue(resultNode.asText());
        } else {
            log.debug("No value found at JSON path {}", getPath());
        }

        return "null";
    }

    public String getMappedValueModbus(Map<Integer, Object> modbusInput) {
        setPath(getPath().replace("/", ""));

        if (!modbusInput.containsKey(Integer.parseInt(getPath()))) {
            return null;
        }

        Object value = modbusInput.get(Integer.parseInt(getPath()));

        if (value instanceof Double d) {
            return String.valueOf(d);
        } else if (value instanceof Integer i) {
            return String.valueOf(i);
        } else if (value instanceof Float f) {
            return String.valueOf(f);
        } else if (value instanceof Long l) {
            return String.valueOf(l);
        }

        return null;
    }

    private String getValue(String cleanedValue) {
        cleanedValue = cleanedValue.trim();
        if (getValues() != null && getValues().length > 0) {
            if (getType().toLowerCase().contains("int")) {
                if (isNumber(cleanedValue)) {
                    return getValues()[Integer.parseInt(cleanedValue)];
                }
                for (int iii = 0; iii < getValues().length; iii++) {
                    if (getValues()[iii].equalsIgnoreCase(cleanedValue.trim())) {
                        return String.valueOf(iii);
                    }
                }
            } else if (getType().toLowerCase().contains("str")) {
                if (isNumber(cleanedValue)) {
                    return "\"" + getValues()[Integer.parseInt(cleanedValue)] + "\"";
                }

                for (int iii = 0; iii < getValues().length; iii++) {
                    if (getValues()[iii].equalsIgnoreCase(cleanedValue.trim())) {
                        return "\"" + iii + "\"";
                    }
                }

                return null;
            }
        } else if (getPattern() != null && (getType().equalsIgnoreCase("date") || getType().equalsIgnoreCase("datetime"))) {
            setPattern(getPattern().replace("\"", "'"));
            log.debug("Pattern: {}", getPattern());
            log.debug("Date value to parse: {}", cleanedValue);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getPattern());

            if (isNumber(cleanedValue)) {
                Date date = new Date(Long.parseLong(cleanedValue));
                LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return "\"" + ldt.format(formatter) + "\"";
            }

            Date date;

            if (getPattern().contains("H") || getPattern().contains("h") || getPattern().contains("K") || getPattern().contains("k")) {
                LocalDateTime ldt = LocalDateTime.parse(cleanedValue, formatter);
                date = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
            } else {
                LocalDate ld = LocalDate.parse(cleanedValue, formatter);
                date = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }

            log.debug("Parsed date: {}", date);

            return "\"" + date.getTime() + "\"";
        }

        if (getType().toLowerCase().contains("str")) {
            return "\"" + cleanedValue + "\"";
        }

        return cleanedValue;
    }

    public static boolean isNumber(String input) {
        try {
            Long.parseLong(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

