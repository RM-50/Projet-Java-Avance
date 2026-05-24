package fr.fabrique.frontend.controller;

import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.frontend.SceneRouter;
import fr.fabrique.frontend.model.CatalogueLoader;
import fr.fabrique.frontend.model.Panier;
import fr.fabrique.frontend.model.Produit;
import fr.fabrique.frontend.mqtt.MqttFrontendClient;
import fr.fabrique.frontend.serial.FrontendSerializers;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur de l'écran catalogue.
 */
public class CatalogueController {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueController.class);

    @FXML private FlowPane flowProduits;
    @FXML private Label    lblPanier;
    @FXML private Button   btnCommander;

    private final Panier panier = new Panier();
    private SceneRouter router;

    @FXML
    public void initialize() {
        panier.quantitesProperty().addListener((obs, o, n) -> rafraichirPanier());
        chargerCatalogue();
    }

    /**
     * Méthode chargerCatalogue qui charge le cataloge avec les produits
     */
    private void chargerCatalogue() {
        try {
            List<Produit> produits = CatalogueLoader.charger();
            for (Produit p : produits) {
                flowProduits.getChildren().add(construireCarteVue(p));
            }
        } catch (IOException e) {
            LOG.error("Impossible de charger le catalogue", e);
        }
    }

    /**
     * Construit la carte visuelle d'un produit.
     */
    private VBox construireCarteVue(Produit p) {
        VBox carte = new VBox(8);
        carte.setStyle("-fx-background-color: #16213e; -fx-padding: 20; "
                + "-fx-background-radius: 12; -fx-min-width: 200;");

        String cheminImage = "/images/" + p.id() + ".png";
        var imageUrl = getClass().getResourceAsStream(cheminImage);
        if (imageUrl != null) {
            javafx.scene.image.Image img =
                    new javafx.scene.image.Image(imageUrl);
            javafx.scene.image.ImageView imageView =
                    new javafx.scene.image.ImageView(img);
            imageView.setFitWidth(160);
            imageView.setFitHeight(100);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-alignment: CENTER;");
            carte.getChildren().add(imageView);
        } else {
            LOG.warn("Image introuvable pour le produit : {}", p.id());
        }

        Label nom   = new Label(p.name());
        nom.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");

        Label prix  = new Label(String.format("%.2f €", p.price()));
        prix.setStyle("-fx-text-fill: white; -fx-font-size: 14;");

        Label desc  = new Label(p.description());
        desc.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-wrap-text: true; -fx-max-width: 180;");

        Label badge = new Label(p.badge());
        badge.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; "
                + "-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 10;");
        badge.setVisible(!p.badge().isBlank());

        // Contrôleur de quantité
        Label lblQte  = new Label("0");
        lblQte.setStyle("-fx-text-fill: white; -fx-min-width: 24; -fx-alignment: CENTER;");

        Button moins = new Button("−");
        Button plus  = new Button("+");
        String btnStyle = "-fx-background-color: #444466; -fx-text-fill: white; "
                + "-fx-padding: 4 10; -fx-background-radius: 4;";
        moins.setStyle(btnStyle);
        plus.setStyle(btnStyle);

        plus.setOnAction(e -> {
            panier.incrementer(p.id());
            lblQte.setText(String.valueOf(panier.quantite(p.id())));
        });
        moins.setOnAction(e -> {
            panier.decrementer(p.id());
            lblQte.setText(String.valueOf(panier.quantite(p.id())));
        });

        HBox qteBox = new HBox(6, moins, lblQte, plus);
        qteBox.setStyle("-fx-alignment: CENTER;");

        carte.getChildren().addAll(badge, nom, prix, desc, qteBox);
        return carte;
    }

    private void rafraichirPanier() {
        int total = panier.total();
        lblPanier.setText("Panier : " + total);
        btnCommander.setDisable(panier.estVide());
    }

    @FXML
    private void onRetourAccueil() {
        router().allerAccueil();
    }

    @FXML
    private void onPasserCommande() {
        if (panier.estVide()) return;

        MqttFrontendClient client = router().getMqttClient();
        String orderId = UUID.randomUUID().toString();

        // 1. Naviguer vers l'attente — positionne le callback
        router().allerAttente(orderId);

        // 2. S'abonner
        client.abonner("orders/" + orderId + "/+");

        // 3. Publier en dernier
        Map<TypeLunette, Integer> quantites = new HashMap<>();
        panier.getQuantites().forEach((produitId, qte) -> {
            if (qte > 0) {
                try {
                    TypeLunette type = TypeLunette.valueOf(produitId.toUpperCase());
                    quantites.put(type, qte);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Produit {} ne correspond à aucun TypeLunette", produitId);
                }
            }
        });

        if (quantites.isEmpty()) {
            LOG.warn("Panier non vide mais aucune correspondance TypeLunette trouvée");
            return;
        }

        String payload = FrontendSerializers.encoderCommande(orderId, quantites);
        client.publier("orders/" + orderId, payload);
        LOG.info("Commande {} publiée sur MQTT : {}", orderId, quantites);
    }

    private SceneRouter router() {
        if (router == null) {
            router = (SceneRouter) flowProduits.getScene().getRoot().getUserData();
        }
        return router;
    }
}
