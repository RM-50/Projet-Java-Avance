package fr.fabrique.serveur.serial;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Format de sérialisation maison.
 *
 * <h2>Grammaire</h2>
 * <pre>
 * message   ::= type "|" paire ("|" paire)*
 * paire     ::= cle ":" valeur
 * type      ::= identifiant ASCII sans "|" ni ":"
 * cle       ::= identifiant ASCII sans "|" ni ":"
 * valeur    ::= texte ASCII sans "|" ni ":"   (peut être vide)
 * </pre>
 *
 * <h2>Exemples de payloads MQTT</h2>
 * <pre>
 * ORDER|id:550e8400-e29b-41d4-a716|CLAUDE:2|BANANA:1
 * VALIDATED|id:550e8400-e29b-41d4-a716
 * CANCELLED|id:550e8400-e29b-41d4-a716|reason:Type inconnu FOOBAR
 * DELIVERY|id:550e8400-e29b-41d4-a716|AB-123456-789012:CLAUDE|CD-654321-210987:BANANA
 * STATUS|id:550e8400-e29b-41d4-a716|status:processing
 * ERROR|id:550e8400-e29b-41d4-a716|reason:Erreur machine interne
 * SERIAL_CHECK|serial:AB-123456-789012
 * SERIAL_RESULT|serial:AB-123456-789012|type:CLAUDE
 * SERIAL_RESULT|serial:XX-000000-000000|type:invalid
 * </pre>
 *
 * <h2>Règles d'encodage</h2>
 * <ul>
 *   <li>Encodage : UTF-8</li>
 *   <li>Les caractères {@code |} et {@code :} sont réservés et interdits dans les valeurs.
 *       Si une valeur doit en contenir (ex. un message d'erreur), ils sont remplacés par
 *       leur séquence d'échappement : {@code \|} et {@code \:}.</li>
 *   <li>Un message vide ou ne respectant pas la grammaire lève une {@link SerialException}.</li>
 * </ul>
 */
public final class MessageFormat {

    public static final char SEP_CHAMP = '|';
    public static final char SEP_KV    = ':';
    static final String ESC_PIPE  = "\\|";
    static final String ESC_COLON = "\\:";

    private MessageFormat() { /* utilitaire */ }


    /**
     * Sérialise un message.
     *
     * @param type   identifiant du type de message (ex. "ORDER")
     * @param champs paires clé-valeur dans l'ordre
     * @return la chaîne sérialisée
     */
    public static String encoder(String type, Map<String, String> champs) {
        StringBuilder sb = new StringBuilder(valeurEchappee(type));
        for (Map.Entry<String, String> entry : champs.entrySet()) {
            sb.append(SEP_CHAMP)
              .append(valeurEchappee(entry.getKey()))
              .append(SEP_KV)
              .append(valeurEchappee(entry.getValue()));
        }
        return sb.toString();
    }

    /** Raccourci pour un message sans champs (ex. VALIDATED). */
    public static String encoder(String type, String... kvPairs) {
        if (kvPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Les paires clé-valeur doivent être en nombre pair");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put(kvPairs[i], kvPairs[i + 1]);
        }
        return encoder(type, map);
    }


    /**
     * Désérialise un message.
     *
     * @param payload la chaîne brute
     * @return un {@link Message} contenant le type et les champs
     * @throws SerialException si le payload est malformé
     */
    public static Message decoder(String payload) throws SerialException {
        if (payload == null || payload.isBlank()) {
            throw new SerialException("Payload vide");
        }
        // On split sur '|' non échappé : caractère non précédé d'un '\'
        String[] parties = payload.split("(?<!\\\\)\\|", -1);
        if (parties.length < 1) {
            throw new SerialException("Payload invalide : " + payload);
        }

        String type = valeurDesechappee(parties[0].trim());
        if (type.isBlank()) {
            throw new SerialException("Type de message absent dans : " + payload);
        }

        Map<String, String> champs = new LinkedHashMap<>();
        for (int i = 1; i < parties.length; i++) {
            String partie = parties[i];
            // Split sur ':' non échappé, limité à 2 pour tolérer ':' dans la valeur une fois déséchappé
            int idx = indexColonNonEchappe(partie);
            if (idx < 0) {
                throw new SerialException("Paire clé-valeur sans ':' dans : " + partie);
            }
            String cle   = valeurDesechappee(partie.substring(0, idx).trim());
            String valeur = valeurDesechappee(partie.substring(idx + 1));
            champs.put(cle, valeur);
        }
        return new Message(type, champs);
    }

    static String valeurEchappee(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(String.valueOf(SEP_KV), ESC_COLON)
                .replace(String.valueOf(SEP_CHAMP), ESC_PIPE);
    }

    static String valeurDesechappee(String s) {
        if (s == null) return "";
        return s.replace(ESC_PIPE, String.valueOf(SEP_CHAMP))
                .replace(ESC_COLON, String.valueOf(SEP_KV))
                .replace("\\\\", "\\");
    }

    // Retourne l'index du premier ':' non précédé d'un '\'.
    private static int indexColonNonEchappe(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == SEP_KV && (i == 0 || s.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }
}
