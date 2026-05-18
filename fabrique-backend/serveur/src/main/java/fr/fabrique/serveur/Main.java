package fr.fabrique.serveur;

import fr.fabrique.usine.UsineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée du serveur backend.
 *
 *   charge la configuration ({@link Config})
 *   instancie une {@link UsineImpl}
 *   démarre la passerelle MQTT
 *
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {

    }

    public static void main(String[] args) {
        LOG.info("=== Démarrage du serveur Fabrique ===");
        try {
            Config config = Config.load();
            LOG.info("Configuration chargée : broker={}", config.brokerUrl());

            LOG.warn("Squelette : la passerelle MQTT n'est pas encore branchée.");
        } catch (Exception e) {
            LOG.error("Démarrage impossible", e);
            System.exit(1);
        }
    }
}
