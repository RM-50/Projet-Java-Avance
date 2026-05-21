package fr.fabrique.serveur.serial;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Représente un message désérialisé : son type et ses champs.
 */
public record Message(String type, Map<String, String> champs) {

    public Message {
        champs = Collections.unmodifiableMap(champs);
    }

    /**
     * Retourne la valeur d'un champ obligatoire.
     *
     * @throws SerialException si le champ est absent
     */
    public String champObligatoire(String cle) throws SerialException {
        String valeur = champs.get(cle);
        if (valeur == null) {
            throw new SerialException("Champ obligatoire absent : '" + cle + "' dans message " + type);
        }
        return valeur;
    }

    // Retourne la valeur d'un champ optionnel.
    public Optional<String> champOptional(String cle) {
        return Optional.ofNullable(champs.get(cle));
    }
}
