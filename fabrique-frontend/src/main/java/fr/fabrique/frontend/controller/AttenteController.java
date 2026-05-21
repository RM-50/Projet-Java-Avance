package fr.fabrique.frontend.controller;

import fr.fabrique.frontend.SceneRouter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Contrôleur de l'écran d'attente.
 */
public class AttenteController {

    @FXML private Label             lblTitre;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             lblStatut;
    @FXML private VBox              panneauResultat;
    @FXML private ListView<String>  listSerials;
    @FXML private VBox              panneauErreur;
    @FXML private Label             lblErreurDetail;

    private SceneRouter router;

    @FXML
    public void initialize() {
        panneauResultat.setVisible(false);
        panneauErreur.setVisible(false);
    }

    public void afficherStatut(String statut) {
        Platform.runLater(() -> lblStatut.setText(statut));
    }

    public void afficherResultat(java.util.List<String> serials) {
        Platform.runLater(() -> {
            spinner.setVisible(false);
            lblTitre.setText("Commande livrée !");
            listSerials.getItems().setAll(serials);
            panneauResultat.setVisible(true);
        });
    }

    public void afficherErreur(String detail) {
        Platform.runLater(() -> {
            spinner.setVisible(false);
            lblErreurDetail.setText(detail);
            panneauErreur.setVisible(true);
        });
    }

    @FXML
    private void onNouvelleCommande() {
        router().allerAccueil();
    }

    private SceneRouter router() {
        if (router == null) {
            router = (SceneRouter) lblTitre.getScene().getRoot().getUserData();
        }
        return router;
    }
}
