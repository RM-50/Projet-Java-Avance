package fr.fabrique.frontend;

import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import javafx.application.Application;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application JavaFX de la fabrique.
 */
public class FrontendApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(FrontendApp.class);

    private final MqttFrontendClient mqttClient = MqttFrontendClient.getInstance();

    @Override
    public void start(Stage stage) {
        LOG.info("Démarrage du frontend Fabrique");

        try {
            mqttClient.connecter();
        } catch ( MqttException e) {
            LOG.warn("Impossible de se connecter au broker MQTT au démarrage : {}", e.getMessage());
        }
        stage.setTitle("Fabrique de lunettes");

        SceneRouter router = new SceneRouter(stage, mqttClient);
        router.allerAccueil();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
