package fr.fabrique.frontend.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Charge le catalogue de lunettes depuis {@code products.json}
 */
public final class CatalogueLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueLoader.class);

    private static final String RESOURCE = "products.json";

    private static final Pattern P_STR   = Pattern.compile("\"(%s)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern P_NUM   = Pattern.compile("\"price\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private CatalogueLoader() {}

    /**
     * Charge et retourne la liste des produits du catalogue
     *
     * @throws IOException si la ressource est introuvable ou illisible
     */
    public static List<Produit> charger() throws IOException {
        String json = lireRessource();
        return parser(json);
    }


    private static String lireRessource() throws IOException {
        InputStream in = CatalogueLoader.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (in == null) {
            throw new IOException("Ressource introuvable : " + RESOURCE);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Parser JSON : extrait les objets du tableau et construit{@link Produit}.
     */
    static List<Produit> parser(String json) {
        List<Produit> produits = new ArrayList<>();

        int debut = json.indexOf('{');
        while (debut >= 0) {
            int fin = json.indexOf('}', debut);
            if (fin < 0) break;
            String objet = json.substring(debut, fin + 1);
            try {
                produits.add(parseProduit(objet));
            } catch (Exception e) {
                LOG.warn("Objet ignoré (format inattendu) : {}", objet, e);
            }
            debut = json.indexOf('{', fin + 1);
        }

        LOG.info("{} produits chargés depuis {}", produits.size(), RESOURCE);
        return produits;
    }

    /**
     * Méthode parseProduit qui récupère les informations d'un produit
     * @param obj le produit
     * @return produit instancié
     */
    private static Produit parseProduit(String obj) {
        String id          = extraireStr(obj, "id");
        String name        = extraireStr(obj, "name");
        String badge       = extraireStr(obj, "badge");
        String description = extraireStr(obj, "description");
        double price       = extraireDouble(obj);
        return new Produit(id, name, price, badge, description);
    }

    private static String extraireStr(String obj, String cle) {
        Matcher m = Pattern.compile(
                "\"" + cle + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(obj);
        return m.find() ? m.group(1) : "";
    }

    private static double extraireDouble(String obj) {
        Matcher m = P_NUM.matcher(obj);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }
}
