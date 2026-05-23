package fr.fabrique.serveur;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration du serveur, lue depuis un fichier {@code .properties}.
 *
 * Stratégie de résolution, dans l'ordre :
 *
 *   chemin passé via la propriété système {@code -Dfabrique.config=/chemin/vers/config.properties}
 *   fichier {@code config.properties} dans le répertoire courant
 *   fichier {@code config.properties} embarqué dans le jar (ressources)
 *
 */
public record Config(
        String brokerUrl,
        String clientIdPrefix,
        int qos,
        int fabricateurCapacity,
        String usineMode          // "autonome" ou "mutualise"
) {

    private static final Logger LOG = LoggerFactory.getLogger(Config.class);
    private static final String DEFAULT_RESOURCE = "config.properties";
    private static final String SYS_PROP = "fabrique.config";

    public static Config load() throws IOException {
        Properties props = new Properties();

        String sysPath = System.getProperty(SYS_PROP);
        if (sysPath != null) {
            LOG.info("Chargement de la config depuis -D{} = {}", SYS_PROP, sysPath);
            try (InputStream in = Files.newInputStream(Path.of(sysPath))) {
                props.load(in);
            }
        } else if (Files.exists(Path.of(DEFAULT_RESOURCE))) {
            LOG.info("Chargement de la config depuis ./{}", DEFAULT_RESOURCE);
            try (InputStream in = Files.newInputStream(Path.of(DEFAULT_RESOURCE))) {
                props.load(in);
            }
        } else {
            LOG.info("Chargement de la config embarquée dans le jar : {}", DEFAULT_RESOURCE);
            try (InputStream in = Config.class.getClassLoader().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Aucun fichier de configuration trouvé");
                }
                props.load(in);
            }
        }

        return new Config(
                props.getProperty("mqtt.broker.url", "tcp://localhost:1883"),
                props.getProperty("mqtt.client.id.prefix", "fabrique-serveur"),
                Integer.parseInt(props.getProperty("mqtt.qos", "1")),
                Integer.parseInt(props.getProperty("usine.capacity", "0")),
                props.getProperty("usine.mode", "autonome")
        );
    }
}

