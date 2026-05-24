package fr.fabrique.frontend;

import fr.fabrique.frontend.controller.AttenteController;
import fr.fabrique.frontend.controller.SerialController;
import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Gère la navigation entre les 4 écrans et transmet le client MQTT aux contrôleurs.
 */
public class SceneRouter {

    private static final Logger LOG = LoggerFactory.getLogger(SceneRouter.class);
    private static final String VIEWS = "/fr/fabrique/frontend/views/";

    private final Stage stage;
    private final MqttFrontendClient mqttClient;

    private List<String> derniersSerials;
    private String dernierOrderId;

    public void sauvegarderResultats(String orderId, List<String> serials) {
        this.dernierOrderId  = orderId;
        this.derniersSerials = serials;
    }


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

            AttenteController ctrl = loader.getController();

            // Si on revient sur une commande déjà livrée, restaurer les résultats
            if (orderId.equals(dernierOrderId) && derniersSerials != null) {
                ctrl.initialiserCommande(orderId, mqttClient);
                Platform.runLater(() -> ctrl.afficherResultat(derniersSerials));
            } else {
                ctrl.initialiserCommande(orderId, mqttClient);
            }

            appliquerScene(root);
        } catch (IOException e) {
            LOG.error("Impossible de charger attente.fxml", e);
        }
    }


    public MqttFrontendClient getMqttClient() {
        return mqttClient;
    }


    /**
     * Charger vue fxml
     * @param fxmlNom
     */
    private void charger(String fxmlNom) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(VIEWS + fxmlNom));
            Parent root = loader.load();
            root.setUserData(this);
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root, 800, 800));
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

    /**
     * Route vers la page de vérification
     * @param serialPreRempli
     * @param orderId
     */
    public void allerVerificationSerial(String serialPreRempli, String orderId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEWS + "serial.fxml"));
            Parent root = loader.load();
            root.setUserData(this);

            SerialController ctrl = loader.getController();
            ctrl.preRemplir(serialPreRempli, orderId);

            appliquerScene(root);
        } catch (IOException e) {
            LOG.error("Impossible de charger serial.fxml", e);
        }
    }

    public void allerVerificationSerial() {
        charger("serial.fxml");
    }

    /**
     * Route vers les résultats de la commande
     * @param orderId
     * @param serials
     */
    public void allerResultats(String orderId, List<String> serials) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEWS + "attente.fxml"));
            Parent root = loader.load();
            root.setUserData(this);

            AttenteController ctrl = loader.getController();
            // Injecter directement les résultats sans démarrer le timeout
            ctrl.restaurerResultats(orderId, serials, mqttClient);

            appliquerScene(root);
        } catch (IOException e) {
            LOG.error("Impossible de charger attente.fxml", e);
        }
    }

    public List<String> getDerniersSerials() {
        return derniersSerials;
    }
}
