package fr.fabrique.frontend.controller;

import fr.fabrique.frontend.SceneRouter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Contrôleur de l'écran d'accueil.
 * Récupère le {@link SceneRouter}
 */
public class AccueilController {

    @FXML private Button btnCommander;
    @FXML private Button btnVerifier;

    private SceneRouter router;

    @FXML
    public void initialize() {
    }

    @FXML
    private void onCommander() {
        router().allerCatalogue();
    }

    @FXML
    private void onVerifier() {
        router().allerVerificationSerial();
    }

    private SceneRouter router() {
        if (router == null) {
            router = (SceneRouter) btnCommander.getScene().getRoot().getUserData();
        }
        return router;
    }
}
