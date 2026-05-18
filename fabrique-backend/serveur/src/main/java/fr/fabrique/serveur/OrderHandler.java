package fr.fabrique.serveur;

import fr.fabrique.usine.Usine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traite une commande reçue sur {@code orders/xxx}.
 *
 *   désérialisation du payload en {@code Commande}
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
    private final byte[] payload;
    private final Usine usine;

    public OrderHandler(String orderId, byte[] payload, Usine usine) {
        this.orderId = orderId;
        this.payload = payload;
        this.usine = usine;
    }

    public void handle() {
        LOG.info("[order={}] traitement en cours", orderId);
        // TODO Jalon 2.2
        throw new UnsupportedOperationException("À implémenter — Jalon 2.2");
    }
}
