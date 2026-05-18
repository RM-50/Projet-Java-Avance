package fr.fabrique.frontend;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application JavaFX de la fabrique.
 *
 * Squelette minimal : affiche un écran d'accueil.
 */
public class FrontendApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(FrontendApp.class);

    @Override
    public void start(Stage stage) {
        LOG.info("Démarrage du frontend Fabrique");

        Label placeholder = new Label("Fabrique de lunettes — squelette JavaFX");
        Scene scene = new Scene(new StackPane(placeholder), 800, 600);

        stage.setTitle("Fabrique de lunettes");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
