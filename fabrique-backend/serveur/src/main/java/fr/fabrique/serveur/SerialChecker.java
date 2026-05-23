package fr.fabrique.serveur;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.serial.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vérifie la validité d'un numéro de série
 * <p>
 * Déclenché par un message sur {@code serials/<serial>/check}.
 * Utilise la méthode statique {@link Fabricateur#validateSerial(String)}
 * et publie la réponse sur {@code serials/<serial>}.
 * <p>
 * Réponse :
 * <ul>
 *   <li>S/N valide -> {@code SERIAL_RESULT|serial:...|type:CLAUDE} (ou autre type)</li>
 *   <li>S/N invalide -> {@code SERIAL_RESULT|serial:...|type:invalid}</li>
 * </ul>
 */
public class SerialChecker {

    private static final Logger LOG = LoggerFactory.getLogger(SerialChecker.class);

    private final String serial;
    private final MqttGateway gateway;

    public SerialChecker(String serial, MqttGateway gateway) {
        this.serial  = serial;
        this.gateway = gateway;
    }

    public void handle() {
        LOG.info("[serial={}] Vérification", serial);

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
