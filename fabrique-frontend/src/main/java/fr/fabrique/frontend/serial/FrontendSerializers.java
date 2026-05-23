package fr.fabrique.frontend.serial;

import bernard_flou.Fabricateur.TypeLunette;

import java.util.LinkedHashMap;
import java.util.Map;


public final class FrontendSerializers {

    private static final char SEP_CHAMP = '|';
    private static final char SEP_KV    = ':';

    private FrontendSerializers() { /* utilitaire */ }

    /**
     * Encode une commande.
     * Format : {@code ORDER|id:UUID|CLAUDE:2|BANANA:1}
     */
    public static String encoderCommande(String orderId, Map<TypeLunette, Integer> quantites) {
        StringBuilder sb = new StringBuilder("ORDER");
        sb.append(SEP_CHAMP).append("id").append(SEP_KV).append(orderId);
        for (Map.Entry<TypeLunette, Integer> e : quantites.entrySet()) {
            sb.append(SEP_CHAMP).append(e.getKey().name()).append(SEP_KV).append(e.getValue());
        }
        return sb.toString();
    }


    /**
     * Retourne le type d'un message reçu (premier segment avant '|').
     */
    public static String lireType(String payload) {
        int idx = payload.indexOf(SEP_CHAMP);
        return idx < 0 ? payload : payload.substring(0, idx);
    }

    /**
     * Extrait tous les champs clé:valeur d'un payload.
     * Ignore le premier segment (le type).
     */
    public static Map<String, String> lireChamps(String payload) {
        Map<String, String> champs = new LinkedHashMap<>();
        String[] parties = payload.split("\\" + SEP_CHAMP);
        for (int i = 1; i < parties.length; i++) {
            int idx = parties[i].indexOf(SEP_KV);
            if (idx >= 0) {
                champs.put(parties[i].substring(0, idx), parties[i].substring(idx + 1));
            }
        }
        return champs;
    }

    /**
     * Extrait la liste des numéros de série depuis un payload DELIVERY.
     * Format : {@code DELIVERY|id:UUID|AB-123456-789012:CLAUDE|...}
     *
     * @return Map <serial, TypeLunette> (ou null si type inconnu)
     */
    public static Map<String, TypeLunette> decoderDelivery(String payload) {
        Map<String, TypeLunette> result = new LinkedHashMap<>();
        Map<String, String> champs = lireChamps(payload);
        for (Map.Entry<String, String> e : champs.entrySet()) {
            if ("id".equals(e.getKey())) continue;
            TypeLunette type = null;
            try { type = TypeLunette.valueOf(e.getValue()); } catch (IllegalArgumentException ignored) {}
            result.put(e.getKey(), type);
        }
        return result;
    }

    /**
     * Extrait le motif depuis un payload CANCELLED ou ERROR.
     */
    public static String lireRaison(String payload) {
        return lireChamps(payload).getOrDefault("reason", "Erreur inconnue");
    }

    /**
     * Extrait le statut depuis un payload STATUS.
     */
    public static String lireStatut(String payload) {
        return lireChamps(payload).getOrDefault("status", "");
    }

    /**
     * Extrait le type depuis un payload SERIAL_RESULT.
     * Retourne {@code "invalid"} si le numéro est invalide.
     */
    public static String lireTypeSerial(String payload) {
        return lireChamps(payload).getOrDefault("type", "invalid");
    }
}
