package si.fri.pretvornikprotokolov.transformations.transformation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import si.fri.pretvornikprotokolov.transformations.configuration.models.ModbusModel;
import si.fri.pretvornikprotokolov.transformations.constants.Constants;
import si.fri.pretvornikprotokolov.transformations.mappers.AbstractMapper;
import si.fri.pretvornikprotokolov.transformations.mappers.JSONMapper;
import si.fri.pretvornikprotokolov.transformations.mappers.XMLMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@ApplicationScoped
public class ObjectTransformer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    public String transform(Object objectInput, String mappingDefinition, String fromFormat, String toFormat) {
        if (mappingDefinition == null) {
            return null;
        }

        long millisecond = System.currentTimeMillis();
        String patternZ = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternZ);
        Date date = new Date(millisecond);
        LocalDateTime ldt = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String timestampZ = String.format("\"%s\"", ldt.format(formatter));

        mappingDefinition = mappingDefinition.replace("\"$timestampZ\"", timestampZ);
        mappingDefinition = mappingDefinition.replace("$timestampZ", timestampZ);
        mappingDefinition = mappingDefinition.replace("\"$timestamp\"", String.valueOf(millisecond));
        mappingDefinition = mappingDefinition.replace("$timestamp", String.valueOf(millisecond));

        try {
            if (objectInput instanceof String input) {
                JsonNode jsonNode = isValidJson(input);

                if (jsonNode != null) {
                    if (fromFormat == null || fromFormat.isEmpty()) {
                        fromFormat = "JSON";
                    }
                    if (fromFormat.equalsIgnoreCase("XML")) {
                        return null;
                    }

                    if (Objects.equals(toFormat, "XML")) {
                        return transformToXML(jsonNode, isValidXml(mappingDefinition));
                    } else if (Objects.equals(toFormat, "JSON")) {
                        return transformToJSON(jsonNode, mappingDefinition);
                    }
                }

                Document document = isValidXml(input);

                if (document != null) {
                    if (fromFormat == null || fromFormat.isEmpty()) {
                        fromFormat = "XML";
                    }
                    if (fromFormat.equalsIgnoreCase("JSON")) {
                        return null;
                    }

                    if (Objects.equals(toFormat, "XML")) {
                        return transformToXML(document, isValidXml(mappingDefinition));
                    } else if (Objects.equals(toFormat, "JSON")) {
                        return transformToJSON(document, mappingDefinition);
                    }
                }
            } else {
                if (Objects.equals(toFormat, "XML")) {
                    return transformToXML(objectInput, isValidXml(mappingDefinition));
                } else if (Objects.equals(toFormat, "JSON")) {
                    return transformToJSON(objectInput, mappingDefinition);
                }
            }
        } catch (Exception e) {
            log.error("Error transforming object", e);
        }

        return null;
    }

    public Map<Integer, Long> transformToModbus(List<ModbusModel> modbusModels, String input, String fromFormat) throws ParseException {
        HashMap<Integer, Long> result = new HashMap<>();

        JsonNode jsonNode = isValidJson(input);

        if (jsonNode != null) {
            if (fromFormat.equalsIgnoreCase("XML")) {
                return new HashMap<>();
            }

            for (ModbusModel modbusModel : modbusModels) {
                JSONMapper jsonMapper = new JSONMapper(modbusModel.getPath(), modbusModel.getType(), modbusModel.getValues(), modbusModel.getPattern());
                String value = jsonMapper.getMappedValueJSON(jsonNode);

                log.debug("Added value for register: {} with value: {}", modbusModel.getAddress(), value);

                if (value == null || value.equals("null")) {
                    result.put(modbusModel.getAddress(), null);
                    continue;
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                result.put(modbusModel.getAddress(), Long.parseLong(value));
            }
        }

        Document document = isValidXml(input);

        if (document != null) {
            if (fromFormat.equalsIgnoreCase("JSON")) {
                return new HashMap<>();
            }

            for (ModbusModel modbusModel : modbusModels) {
                XMLMapper xmlMapper = new XMLMapper(modbusModel.getPath(), modbusModel.getType(), modbusModel.getValues(), modbusModel.getPattern());
                String value = xmlMapper.getMappedValueXML(document);

                if (value == null || value.equals("null")) {
                    result.put(modbusModel.getAddress(), null);
                    continue;
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                result.put(modbusModel.getAddress(), Long.parseLong(value));
            }
        }

        return result;
    }

    private String transformToXML(Object input, Document mappedDocument) throws ParseException {
        NodeList flowList = mappedDocument.getElementsByTagName(Constants.MAPPING_NAME);
        while (flowList.getLength() != 0) {
            for (int i = 0; i < flowList.getLength(); i++) {
                Node parentNode = flowList.item(i).getParentNode();

                Node mappingNode = flowList.item(i);

                XMLMapper mapper = new XMLMapper(mappingNode);

                if (mappingNode.getNodeName().equals(Constants.MAPPING_NAME)) {
                    parentNode.removeChild(mappingNode);
                }

                String value = getValueFromMapper(mapper, input);

                if (value == null || value.equals("null")) {
                    value = "";
                } else if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                log.debug("path: {}, value: {}", mapper.getPath(), value);

                parentNode.setTextContent(value);
            }
            flowList = mappedDocument.getElementsByTagName(Constants.MAPPING_NAME);
        }

        return transformXMLToString(mappedDocument);
    }

    private String transformToJSON(Object input, String mappingDefinition) throws ParseException {
        Pattern mappingPattern = Pattern.compile("\\{\\s*\"" + Constants.MAPPING_NAME + "\":\\s*\\{(.*?)}\\s*}", Pattern.DOTALL);
        Matcher mappingMatcher = mappingPattern.matcher(mappingDefinition);

        StringBuilder modifiedMapping = new StringBuilder();
        while (mappingMatcher.find()) {
            String mappingContent = mappingMatcher.group(0);
            JSONMapper mapper = new JSONMapper(mappingContent);

            String value = getValueFromMapper(mapper, input);

            log.debug("path: {}, value: {}", mapper.getPath(), value);

            if (value == null) {
                value = "null";
            }

            mappingMatcher.appendReplacement(modifiedMapping, value);
        }
        mappingMatcher.appendTail(modifiedMapping);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement je = new JsonParser().parse(modifiedMapping.toString());

        return gson.toJson(je);
    }

    public JsonNode isValidJson(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            return null;
        }
    }

    public Document isValidXml(String xmlString) {
        try {
            if (!xmlString.startsWith("<?xml")) {
                xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + xmlString;
            }

            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new java.io.StringReader(xmlString));

            return builder.parse(inputSource);
        } catch (Exception e) {
            return null;
        }
    }

    private HashMap<Integer, Object> isValidMap(Object object) {
        try {
            return objectMapper.convertValue(object, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String getValueFromMapper(AbstractMapper mapper, Object input) throws ParseException {
        String value = null;
        HashMap<Integer, Object> registersMap = isValidMap(input);
        if (input instanceof JsonNode node) {
            value = mapper.getMappedValueJSON(node);
        } else if (input instanceof Document document) {
            value = mapper.getMappedValueXML(document);
        } else if (registersMap != null && !registersMap.isEmpty()) {
            value = mapper.getMappedValueModbus(registersMap);
        }

        return value;
    }

    private String transformXMLToString(Document document) {
        Transformer transformer;
        try {
            transformerFactory.setAttribute("indent-number", 2);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            String xmlString = writer.getBuffer().toString();
            xmlString = xmlString.replace(">\n", ">");
            int index = xmlString.indexOf("?>");
            if (index != -1) {
                xmlString = xmlString.substring(0, index + 2) + "\n" + xmlString.substring(index + 2);
            }

            return xmlString;
        } catch (TransformerException e) {
            log.error("Error transforming XML", e);
        }

        return null;
    }
}
