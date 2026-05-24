package fr.fabrique.serveur;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.serial.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vérifie la validité d'un numéro de série
 */
public class SerialChecker {

    private static final Logger LOG = LoggerFactory.getLogger(SerialChecker.class);

    private final String serial;
    private final MqttGateway gateway;

    public SerialChecker(String serial, MqttGateway gateway) {
        this.serial  = serial;
        this.gateway = gateway;
    }

    /**
     * Méthode handle qui valide un numéro de série
     */
    public void handle() {
        LOG.info("[serial={}] Verification", serial);

        // Utilisation de la méthode fournie dans le Fabricateur
        TypeLunette type = Fabricateur.validateSerial(serial);

        if (type != null) {
            LOG.info("[serial={}] Valide → type={}", serial, type);
        } else {
            LOG.info("[serial={}] Invalide", serial);
        }

        gateway.publier(
                "serials/" + serial,
                Serializers.encoderSerialResult(serial, type)
        );
    }
}
