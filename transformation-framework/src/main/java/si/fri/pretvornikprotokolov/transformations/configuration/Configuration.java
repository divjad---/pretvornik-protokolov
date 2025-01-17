package si.fri.pretvornikprotokolov.transformations.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import si.fri.pretvornikprotokolov.transformations.configuration.models.ConfigurationModel;
import si.fri.pretvornikprotokolov.transformations.constants.Constants;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author David Trafela, FRI
 * @since 1.0.0
 */
@Slf4j
@Getter
@ApplicationScoped
public class Configuration {

    private final List<ConfigurationModel> configurations = new ArrayList<>();

    private final HashMap<String, Long> lastModified = new HashMap<>();

    @Setter
    private Consumer<Boolean> consumer;

    @PostConstruct
    private void init() {
        readConf();

        scheduleRead();
    }

    public void readConf() {
        try {
            configurations.clear();

            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.findAndRegisterModules();

            File[] files = readFiles();

            for (File file : Objects.requireNonNull(files)) {
                String name = file.getName();
                if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                    log.info("Reading configuration: {}", name);

                    lastModified.put(name, file.lastModified());

                    String fileContent = readFromInputStream(new FileInputStream(file));
                    ConfigurationModel configurationModel = objectMapper.readValue(
                            fileContent,
                            ConfigurationModel.class);


                    this.configurations.add(configurationModel);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }

        return resultStringBuilder.toString();
    }

    private File[] readFiles() {
        String configuration = getConfFolderName();

        File dir = new File(configuration);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new RuntimeException("Configuration directory does not exist");
        }

        return dir.listFiles();
    }

    public static String getConfFolderName() {
        String configuration = System.getenv(Constants.CONFIGURATION_FOLDER);

        if (configuration == null) {
            configuration = System.getProperty(Constants.CONFIGURATION_FOLDER, "conf");
        }

        return configuration;
    }

    private void scheduleRead() {
        String configuration = getConfFolderName();
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.scheduleAtFixedRate(() -> {
            log.info("Checking for changes in the configuration folder {}", configuration);
            try {
                File[] files = readFiles();

                boolean newConf = false;

                for (File file : Objects.requireNonNull(files)) {
                    String name = file.getName();
                    log.debug("Checking file: {}", name);
                    if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                        if (lastModified.containsKey(name)) {
                            if (lastModified.get(name) != file.lastModified()) {
                                newConf = true;
                                log.debug("Configuration file changed: {}", name);
                            }
                        } else {
                            newConf = true;
                            log.debug("New configuration file: {}", name);
                        }

                        lastModified.put(name, file.lastModified());
                        if (newConf) {
                            break;
                        }
                    }
                }

                if (files.length != lastModified.size()) {
                    newConf = true;

                    log.debug("Checking for any configurations that were removed");
                    // Delete keys from lastModified that are not in files array
                    for (String key : new ArrayList<>(lastModified.keySet())) {
                        boolean found = false;
                        for (File file : files) {
                            if (file.getName().equals(key)) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            log.debug("Configuration file removed: {}", key);
                            lastModified.remove(key);
                        }
                    }
                }

                if (newConf) {
                    log.debug("New configuration detected. Reloading configurations...");
                    readConf();
                    consumer.accept(true);
                }
            } catch (Exception e) {
                log.error("Error reading configuration", e);
            }
        }, 60, 30, TimeUnit.SECONDS);
    }
}
