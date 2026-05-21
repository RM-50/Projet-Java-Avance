package fr.fabrique.serveur.serial;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import fr.fabrique.serveur.model.Commande;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convertisseurs métier utilisant {@link MessageFormat}.
 * <p>
 * Chaque méthode statique correspond à un topic MQTT :
 * <ul>
 *   <li>{@link #encoderCommande} / {@link #decoderCommande} → {@code orders/xxx}</li>
 *   <li>{@link #encoderValidated}                           → {@code orders/xxx/validated}</li>
 *   <li>{@link #encoderCancelled}                          → {@code orders/xxx/cancelled}</li>
 *   <li>{@link #encoderDelivery}                           → {@code orders/xxx/delivery}</li>
 *   <li>{@link #encoderStatus}                             → {@code orders/xxx/status}</li>
 *   <li>{@link #encoderError}                              → {@code orders/xxx/error}</li>
 *   <li>{@link #encoderSerialResult}                       → {@code serials/xxx}</li>
 * </ul>
 */
public final class Serializers {

    public static final String T_ORDER         = "ORDER";
    public static final String T_VALIDATED     = "VALIDATED";
    public static final String T_CANCELLED     = "CANCELLED";
    public static final String T_DELIVERY      = "DELIVERY";
    public static final String T_STATUS        = "STATUS";
    public static final String T_ERROR         = "ERROR";
    public static final String T_SERIAL_RESULT = "SERIAL_RESULT";

    static final String K_ID     = "id";
    static final String K_REASON = "reason";
    static final String K_STATUS = "status";
    static final String K_TYPE   = "type";

    private Serializers() { /* utilitaire */ }


    /**
     * Encode une commande.
     * Format : {@code ORDER|id:UUID|CLAUDE:2|BANANA:1}
     */
    public static String encoderCommande(Commande commande) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put(K_ID, commande.orderId());
        for (Map.Entry<TypeLunette, Integer> e : commande.typesQuantites().entrySet()) {
            champs.put(e.getKey().name(), String.valueOf(e.getValue()));
        }
        return MessageFormat.encoder(T_ORDER, champs);
    }

    /**
     * Décode un payload {@code ORDER}.
     *
     * @throws SerialException si le payload est mal formé ou si un type est inconnu
     */
    public static Commande decoderCommande(String payload) throws SerialException {
        Message msg = MessageFormat.decoder(payload);
        assertType(msg, T_ORDER);

        String orderId = msg.champObligatoire(K_ID);
        Map<TypeLunette, Integer> quantites = new LinkedHashMap<>();

        for (Map.Entry<String, String> e : msg.champs().entrySet()) {
            if (K_ID.equals(e.getKey())) continue;
            TypeLunette type = parseType(e.getKey());
            int qte = parseQte(e.getValue(), e.getKey());
            quantites.put(type, qte);
        }
        return new Commande(orderId, quantites);
    }


    /** {@code VALIDATED|id:UUID} */
    public static String encoderValidated(String orderId) {
        return MessageFormat.encoder(T_VALIDATED, K_ID, orderId);
    }


    /** {@code CANCELLED|id:UUID|reason:...} */
    public static String encoderCancelled(String orderId, String reason) {
        return MessageFormat.encoder(T_CANCELLED, K_ID, orderId, K_REASON, reason);
    }

    /** Extrait le motif d'annulation depuis un payload CANCELLED. */
    public static String decoderCancelledReason(String payload) throws SerialException {
        Message msg = MessageFormat.decoder(payload);
        assertType(msg, T_CANCELLED);
        return msg.champObligatoire(K_REASON);
    }

    /**
     * Encode la livraison.
     * Format : {@code DELIVERY|id:UUID|AB-123456-789012:CLAUDE|CD-654321-210987:BANANA}
     * (clé = numéro de série, valeur = type)
     */
    public static String encoderDelivery(String orderId, List<Lunette> lunettes) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put(K_ID, orderId);
        for (Lunette l : lunettes) {
            champs.put(l.serial, l.type.name());
        }
        return MessageFormat.encoder(T_DELIVERY, champs);
    }

    /**
     * Décode un payload DELIVERY.
     *
     * @return Map&lt;serial, TypeLunette&gt;
     */
    public static Map<String, TypeLunette> decoderDelivery(String payload) throws SerialException {
        Message msg = MessageFormat.decoder(payload);
        assertType(msg, T_DELIVERY);
        Map<String, TypeLunette> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : msg.champs().entrySet()) {
            if (K_ID.equals(e.getKey())) continue;
            result.put(e.getKey(), parseType(e.getValue()));
        }
        return result;
    }


    /** {@code STATUS|id:UUID|status:processing} */
    public static String encoderStatus(String orderId, String status) {
        return MessageFormat.encoder(T_STATUS, K_ID, orderId, K_STATUS, status);
    }


    /** {@code ERROR|id:UUID|reason:...} */
    public static String encoderError(String orderId, String reason) {
        return MessageFormat.encoder(T_ERROR, K_ID, orderId, K_REASON, reason);
    }


    /**
     * Encode la réponse à une vérification de numéro de série.
     * {@code SERIAL_RESULT|serial:AB-123456-789012|type:CLAUDE}
     * ou {@code SERIAL_RESULT|serial:XX-...|type:invalid}
     */
    public static String encoderSerialResult(String serial, TypeLunette type) {
        String typeStr = (type != null) ? type.name() : "invalid";
        return MessageFormat.encoder(T_SERIAL_RESULT, "serial", serial, K_TYPE, typeStr);
    }


    private static void assertType(Message msg, String expected) throws SerialException {
        if (!expected.equals(msg.type())) {
            throw new SerialException(
                    "Type de message inattendu : attendu '" + expected + "', reçu '" + msg.type() + "'");
        }
    }

    private static TypeLunette parseType(String name) throws SerialException {
        try {
            return TypeLunette.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new SerialException("Type de lunette inconnu : '" + name + "'", e);
        }
    }

    private static int parseQte(String valeur, String cle) throws SerialException {
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new SerialException("Quantité non entière pour '" + cle + "' : '" + valeur + "'", e);
        }
    }
}
