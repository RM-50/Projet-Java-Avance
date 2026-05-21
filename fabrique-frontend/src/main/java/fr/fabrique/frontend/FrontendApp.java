package fr.fabrique.frontend;

import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application JavaFX de la fabrique.
 */
public class FrontendApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(FrontendApp.class);

    @Override
    public void start(Stage stage) {
        LOG.info("Démarrage du frontend Fabrique");
        stage.setTitle("Fabrique de lunettes");

        SceneRouter router = new SceneRouter(stage);
        router.allerAccueil();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
