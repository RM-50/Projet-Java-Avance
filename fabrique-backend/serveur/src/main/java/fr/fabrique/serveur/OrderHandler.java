package fr.fabrique.serveur;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.model.Commande;
import fr.fabrique.serveur.model.OrderValidator;
import fr.fabrique.serveur.model.ValidationResult;
import fr.fabrique.serveur.serial.SerialException;
import fr.fabrique.serveur.serial.Serializers;
import fr.fabrique.usine.Usine;
import fr.fabrique.usine.UsineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Traite une commande reçue sur {@code orders/xxx}.
 *
 *   Désérialisation du payload en {@code Commande}
 *   validation (cf. OrderValidator)
 *   publication de {@code orders/xxx/validated} ou {@code orders/xxx/cancelled}
 *   appel à {@link Usine#produire(java.util.Map)}
 *   publication de {@code orders/xxx/delivery}
 *   en cas d'exception : publication de {@code orders/xxx/error}
 *
 * Un handler par commande, exécuté dans un thread dédié pour permettre
 * la prise en charge simultanée de plusieurs commandes.
 */
public class OrderHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OrderHandler.class);

    private final String orderId;
    private final byte[] rawPayload;
    private final Usine usine;
    private final MqttGateway gateway;

    public OrderHandler(String orderId, byte[] rawPayload, Usine usine, MqttGateway gateway) {
        this.orderId    = orderId;
        this.rawPayload = rawPayload;
        this.usine      = usine;
        this.gateway    = gateway;
    }

    public void handle() {
        LOG.info("[order={}] Début du traitement", orderId);
        try {

            String payloadStr = new String(rawPayload, StandardCharsets.UTF_8);
            Commande commande;
            try {
                commande = Serializers.decoderCommande(payloadStr);
            } catch (SerialException e) {
                LOG.warn("[order={}] Payload illisible : {}", orderId, e.getMessage());
                publierErreur("Payload illisible : " + e.getMessage());
                return;
            }

            ValidationResult validation = OrderValidator.valider(commande);
            if (!validation.valide()) {
                LOG.info("[order={}] Commande invalide : {}", orderId, validation.erreur());
                publierCancelled(validation.erreur());
                return;
            }

            publierValidated();

            List<Lunette> lunettes;
            try {
                lunettes = usine.produire(
                        commande.typesQuantites()
                );
            } catch (UsineException e) {
                LOG.error("[order={}] Erreur de fabrication", orderId, e);
                publierErreur("Erreur de fabrication : " + e.getMessage());
                return;
            }

            publierDelivery(lunettes);
            LOG.info("[order={}] Traitement terminé — {} lunettes livrées",
                    orderId, lunettes.size());

        } catch (Exception e) {
            LOG.error("[order={}] Erreur inattendue", orderId, e);
            publierErreur("Erreur interne : " + e.getMessage());
        }
    }

    private void publierValidated() {
        gateway.publier("orders/" + orderId + "/validated",
                Serializers.encoderValidated(orderId));
    }

    private void publierCancelled(String raison) {
        gateway.publier("orders/" + orderId + "/cancelled",
                Serializers.encoderCancelled(orderId, raison));
    }

    private void publierStatus(String statut) {
        gateway.publier("orders/" + orderId + "/status",
                Serializers.encoderStatus(orderId, statut));
    }

    private void publierDelivery(List<Lunette> lunettes) {
        gateway.publier("orders/" + orderId + "/delivery",
                Serializers.encoderDelivery(orderId, lunettes));
    }

    private void publierErreur(String detail) {
        gateway.publier("orders/" + orderId + "/error",
                Serializers.encoderError(orderId, detail));
    }

}
