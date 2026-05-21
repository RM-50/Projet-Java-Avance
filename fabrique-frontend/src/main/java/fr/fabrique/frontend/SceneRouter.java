package fr.fabrique.frontend;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Gère la navigation entre les 4 écrans de l'application.
 * Chaque vue est définie par un fichier FXML chargé depuis les ressources
 * Le routeur est transmis à chaque contrôleur (via {@code setUserData})
 * pour lui permettre de déclencher des transitions.
 */
public class SceneRouter {

    private static final Logger LOG = LoggerFactory.getLogger(SceneRouter.class);
    private static final String VIEWS = "/fr/fabrique/frontend/views/";

    private final Stage stage;

    public SceneRouter(Stage stage) {
        this.stage = stage;
    }

    public void allerAccueil() {
        charger("accueil.fxml");
    }

    public void allerCatalogue() {
        charger("catalogue.fxml");
    }

    public void allerAttente(String orderId) {
        charger("attente.fxml");
    }

    public void allerVerificationSerial() {
        charger("serial.fxml");
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
}
