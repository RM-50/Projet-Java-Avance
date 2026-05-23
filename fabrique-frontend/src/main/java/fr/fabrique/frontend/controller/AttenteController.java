package fr.fabrique.frontend.controller;

import fr.fabrique.frontend.SceneRouter;
import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import fr.fabrique.frontend.serial.FrontendSerializers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/**
 * Contrôleur de l'écran d'attente.
 */
public class AttenteController {

    private static final Logger LOG = LoggerFactory.getLogger(AttenteController.class);
    private static final int TIMEOUT_DEFAUT_MS = 30_000;

    @FXML private Label             lblTitre;
    @FXML private ProgressIndicator spinner;
    @FXML private ProgressBar       progressBar;
    @FXML private Label             lblStatut;
    @FXML private VBox              panneauResultat;
    @FXML private ListView<String>  listSerials;
    @FXML private VBox              panneauErreur;
    @FXML private Label             lblErreurDetail;

    private SceneRouter router;
    private MqttFrontendClient mqttClient;
    private String orderId;
    private String topicBase;
    private String topicSub;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timeoutTask;

    @FXML
    public void initialize() {
        panneauResultat.setVisible(false);
        panneauErreur.setVisible(false);
        progressBar.setProgress(0);
    }

    public void initialiserCommande(String orderId, MqttFrontendClient client) {
        this.orderId    = orderId;
        this.mqttClient = client;
        this.topicBase  = "orders/" + orderId;
        this.topicSub   = topicBase + "/+";

        mqttClient.setMessageCallback(this::onMessageRecu);
        mqttClient.abonner(topicSub);

        demarrerTimeout(lireTimeoutMs());
        LOG.info("Attente commande {} (timeout {}ms)", orderId, lireTimeoutMs());
    }

    private void onMessageRecu(String topic, String payload) {
        if (!topic.startsWith(topicBase)) return;

        annulerTimeout();

        String type = FrontendSerializers.lireType(payload);
        LOG.debug("[order={}] Message reçu type={}", orderId, type);

        switch (type) {
            case "VALIDATED" ->
                    Platform.runLater(() -> {
                        lblStatut.setText("Commande validée ✓");
                        progressBar.setProgress(0.1);
                    });

            case "CANCELLED" -> {
                String raison = FrontendSerializers.lireRaison(payload);
                Platform.runLater(() -> afficherErreur("Commande annulée : " + raison));
                seDesabonner();
            }

            case "STATUS" -> {
                String statut = FrontendSerializers.lireStatut(payload);
                Platform.runLater(() -> {
                    if ("processing".equals(statut)) {
                        lblStatut.setText("Fabrication en cours...");
                        progressBar.setProgress(0.5);
                    } else if ("processed".equals(statut)) {
                        lblStatut.setText("Fabrication terminée ✓");
                        progressBar.setProgress(0.9);
                    }
                });
            }

            case "DELIVERY" -> {
                Map<String, ?> livraison = FrontendSerializers.decoderDelivery(payload);
                List<String> serials = new ArrayList<>(livraison.keySet());
                Platform.runLater(() -> afficherResultat(serials));
                seDesabonner();
            }

            case "ERROR" -> {
                String raison = FrontendSerializers.lireRaison(payload);
                Platform.runLater(() -> afficherErreur("Erreur : " + raison));
                seDesabonner();
            }

            default -> LOG.warn("Type inattendu : {}", type);
        }
    }

    private void demarrerTimeout(int delaiMs) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutTask = scheduler.schedule(() ->
                        Platform.runLater(() ->
                                afficherErreur("L'usine ne répond pas (timeout " + delaiMs / 1000 + "s).\n"
                                        + "Vérifiez que le backend est démarré et connecté au broker MQTT.")
                        ),
                delaiMs, TimeUnit.MILLISECONDS
        );
    }

    private void annulerTimeout() {
        if (timeoutTask != null) timeoutTask.cancel(false);
        if (scheduler != null)  scheduler.shutdownNow();
    }

    private int lireTimeoutMs() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                return Integer.parseInt(props.getProperty("order.timeout.ms",
                        String.valueOf(TIMEOUT_DEFAUT_MS)));
            }
        } catch (IOException | NumberFormatException e) {
            LOG.warn("Impossible de lire order.timeout.ms, valeur par défaut : {}ms", TIMEOUT_DEFAUT_MS);
        }
        return TIMEOUT_DEFAUT_MS;
    }

    public void afficherStatut(String statut) {
        Platform.runLater(() -> lblStatut.setText(statut));
    }

    public void afficherResultat(java.util.List<String> serials) {
        annulerTimeout();
        spinner.setVisible(false);
        progressBar.setProgress(1.0);
        lblTitre.setText("Commande livrée !");
        listSerials.getItems().setAll(serials);
        panneauResultat.setVisible(true);
    }

    public void afficherErreur(String detail) {
        annulerTimeout();
        seDesabonner();
        spinner.setVisible(false);
        progressBar.setProgress(0);
        lblErreurDetail.setText(detail);
        panneauErreur.setVisible(true);
    }

    private void seDesabonner() {
        if (mqttClient != null && topicSub != null) {
            mqttClient.desabonner(topicSub);
            mqttClient.setMessageCallback(null);
        }
    }

    @FXML
    private void onNouvelleCommande() {
        annulerTimeout();
        seDesabonner();
        router().allerAccueil();
    }

    private SceneRouter router() {
        if (router == null) {
            router = (SceneRouter) lblTitre.getScene().getRoot().getUserData();
        }
        return router;
    }
}
