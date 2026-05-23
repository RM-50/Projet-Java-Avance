package fr.fabrique.frontend;

import fr.fabrique.frontend.controller.AttenteController;
import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Gère la navigation entre les 4 écrans et transmet le client MQTT aux contrôleurs.
 */
public class SceneRouter {

    private static final Logger LOG = LoggerFactory.getLogger(SceneRouter.class);
    private static final String VIEWS = "/fr/fabrique/frontend/views/";

    private final Stage stage;
    private final MqttFrontendClient mqttClient;


    public SceneRouter(Stage stage, MqttFrontendClient mqttClient) {
        this.stage      = stage;
        this.mqttClient = mqttClient;
    }

    public void allerAccueil() {
        charger("accueil.fxml");
    }

    public void allerCatalogue() {
        charger("catalogue.fxml");
    }

    /**
     * Navigue vers l'écran d'attente et y injecte l'orderId + le client MQTT.
     *
     * @param orderId UUID de la commande en cours
     */
    public void allerAttente(String orderId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEWS + "attente.fxml"));
            Parent root = loader.load();
            root.setUserData(this);

            // Injection de l'orderId et du client MQTT dans le contrôleur
            AttenteController ctrl = loader.getController();
            ctrl.initialiserCommande(orderId, mqttClient);

            appliquerScene(root);
            LOG.debug("Navigation vers attente.fxml (orderId={})", orderId);
        } catch (IOException e) {
            LOG.error("Impossible de charger attente.fxml", e);
        }
    }

    public void allerVerificationSerial() {
        charger("serial.fxml");
    }

    public MqttFrontendClient getMqttClient() {
        return mqttClient;
    }


    private void charger(String fxmlNom) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(VIEWS + fxmlNom));
            Parent root = loader.load();
            root.setUserData(this);
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root, 800, 600));
            } else {
                stage.getScene().setRoot(root);
            }
            LOG.debug("Navigation vers {}", fxmlNom);
        } catch (IOException e) {
            LOG.error("Impossible de charger la vue {}", fxmlNom, e);
        }
    }

    private void appliquerScene(Parent root) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(root, 800, 600));
        } else {
            stage.getScene().setRoot(root);
        }
    }
}
