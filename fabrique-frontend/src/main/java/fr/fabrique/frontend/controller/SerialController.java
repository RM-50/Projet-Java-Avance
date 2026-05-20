package fr.fabrique.frontend.controller;

import fr.fabrique.frontend.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Contrôleur de l'écran de vérification de numéro de série.
 */
public class SerialController {

    @FXML private TextField txtSerial;
    @FXML private Label     lblResultat;

    private SceneRouter router;

    @FXML
    private void onVerifier() {
        String serial = txtSerial.getText().trim().toUpperCase();

        // Validation du format XX-XXXXXX-XXXXXX
        if (!serial.matches("[A-Z0-9]{2}-[A-Z0-9]{6}-[A-Z0-9]{6}")) {
            afficherResultat(" Format invalide — attendu : XX-XXXXXX-XXXXXX", false);
            return;
        }

        afficherResultat("Vérification en cours… (MQTT non connecté)", false);
    }

    public void afficherResultat(String message, boolean valide) {
        Platform.runLater(() -> {
            lblResultat.setText(message);
            lblResultat.setStyle("-fx-font-size: 15; -fx-text-fill: "
                    + (valide ? "#6cff9f" : "#ff9f6c") + ";");
        });
    }

    @FXML
    private void onRetourAccueil() {
        router().allerAccueil();
    }

    private SceneRouter router() {
        if (router == null) {
            router = (SceneRouter) txtSerial.getScene().getRoot().getUserData();
        }
        return router;
    }
}
