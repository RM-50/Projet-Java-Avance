package fr.fabrique.frontend.controller;

import fr.fabrique.frontend.SceneRouter;
import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import fr.fabrique.frontend.serial.FrontendSerializers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Contrôleur de l'écran de vérification de numéro de série.
 */
public class SerialController {

    private static final Logger LOG = LoggerFactory.getLogger(SerialController.class);

    @FXML private TextField txtSerial;
    @FXML private Label     lblResultat;
    @FXML private Button btnRetourResultats;

    private SceneRouter router;
    private String dernierSerial;
    private String orderId;



    @FXML
    private void onVerifier() {
        String serial = txtSerial.getText().trim().toUpperCase();

        if (!serial.matches("[A-Za-z0-9]{2}-[A-Za-z0-9]+-[A-Za-z0-9]+")) {
            afficherResultat("⚠️ Format invalide — attendu : XX-XXXXXX-XXXXXX", false);
            return;
        }

        SceneRouter r = router();
        if (r == null) return;

        MqttFrontendClient client = Objects.requireNonNull(router()).getMqttClient();

        if (dernierSerial != null) {
            client.desabonner("serials/" + dernierSerial);
        }
        dernierSerial = serial;

        client.abonner("serials/" + serial);
        client.setMessageCallback((topic, payload) -> {
            if (("serials/" + serial).equals(topic)) {
                String type = FrontendSerializers.lireTypeSerial(payload);
                boolean valide = !"invalid".equals(type);
                String message = valide
                        ? "Numéro valide — type : " + type
                        : "Numéro de série invalide";
                Platform.runLater(() -> afficherResultat(message, valide));
                client.desabonner("serials/" + serial);
                client.setMessageCallback(null);
            }
        });

        // Publier la demande (pas de payload nécessaire selon le README)
        client.publier("serials/" + serial + "/check", "");
        LOG.info("Vérification demandée pour le serial {}", serial);
        afficherResultat("⏳ Vérification en cours...", false);
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
        if (dernierSerial != null) {
            Objects.requireNonNull(router()).getMqttClient().desabonner("serials/" + dernierSerial);
        }
        Objects.requireNonNull(router()).allerAccueil();
    }

    private SceneRouter router() {
        if (router == null) {
            if (txtSerial.getScene() == null) return null;
            router = (SceneRouter) txtSerial.getScene().getRoot().getUserData();
        }
        return router;
    }

    public void preRemplir(String serial, String orderId) {
        this.orderId = orderId;
        txtSerial.setText(serial);
        btnRetourResultats.setVisible(true);
        btnRetourResultats.setManaged(true);
        Platform.runLater(this::onVerifier);
    }

    @FXML
    private void onRetourResultats() {
        if (orderId != null) {
            List<String> serials = router().getDerniersSerials();
            if (serials != null) {
                router().allerResultats(orderId, serials);
            } else {
                router().allerAccueil();
            }
        }
    }
}
