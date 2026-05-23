package fr.fabrique.serveur;

import bernard_flou.Fabricateur;
import fr.fabrique.usine.Dispatcher;
import fr.fabrique.usine.UsineImpl;
import org.eclipse.paho.client.mqttv3.MqttException;
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
            LOG.info("Broker={}, capacité={}, mode={}",
                    config.brokerUrl(), config.fabricateurCapacity(), config.usineMode());

            Fabricateur fabricateur = config.fabricateurCapacity() > 0
                    ? new Fabricateur(config.fabricateurCapacity())
                    : new Fabricateur();

            Dispatcher dispatcher = null;
            UsineImpl usine;

            if ("mutualise".equalsIgnoreCase(config.usineMode())) {
                dispatcher = new Dispatcher(fabricateur);
                usine = new UsineImpl(fabricateur, dispatcher);
                LOG.info("Mode mutualisé activé");
            } else {
                usine = new UsineImpl(fabricateur);
                LOG.info("Mode séquentiel activé");
            }

            MqttGateway gateway = new MqttGateway(config, usine);
            gateway.start();

            final Dispatcher dispatcherFinal = dispatcher;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Signal d'arrêt reçu — fermeture en cours...");
                gateway.stop();
                if (dispatcherFinal != null) dispatcherFinal.arreter();
                LOG.info("=== Serveur Fabrique arrêté ===");
            }, "shutdown-hook"));

            LOG.info("=== Serveur opérationnel — en attente de commandes ===");
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Démarrage impossible", e);
            System.exit(1);
        }
    }
}
