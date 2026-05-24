# Fabrique de Lunettes

Système distribué de commande et fabrication de lunettes connectées.  
Un client JavaFX permet de passer commande, suivre la fabrication en temps réel et recevoir et vérifier les numéros de série.
 Un backend Java gère la production via un bus de messages MQTT.

---

## Structure du projet

```
fabrique/
├── fabrique-backend/     # Serveur MQTT + moteur de production
│   ├── usine/            # Pilote le Fabricateur (publié sur GitHub Packages)
│   └── serveur/          # Passerelle MQTT (Paho) — jar exécutable
└── fabrique-frontend/    # Application graphique JavaFX — jar exécutable
```

---

## Démarrage rapide

### Prérequis

| Outil | Version minimale |
|-------|-----------------|
| JDK | 21 |
| Maven | 3.9 |
| [Mosquitto](https://mosquitto.org) | 2.0 |

Mosquitto doit être démarré **avant** le backend et le frontend :

```bash
# Linux / macOS
mosquitto -v

# Windows
mosquitto.exe -v
```

Par défaut Mosquitto écoute sur `localhost:1883` sans authentification ni TLS.

---

### 1. Lancer le backend

Télécharger `fabrique-serveur.jar` depuis la dernière [release](../../releases) puis :

```bash
java -jar fabrique-serveur.jar
```

Le serveur affiche dans les logs :
```
=== Serveur opérationnel — en attente de commandes ===
```

### 2. Lancer le frontend

Télécharger `fabrique-frontend.jar` depuis la dernière [release](../../releases) puis :

```bash
java -jar fabrique-frontend.jar
```

---

## Configuration

Par défaut la configuration embarquée dans chaque jar est utilisée.  
Pour la surcharger, placer un fichier `config.properties` dans le répertoire de lancement :

```bash
java -Dfabrique.config=/chemin/vers/config.properties -jar fabrique-serveur.jar
```

### Backend (`config.properties`)

```properties
# Broker MQTT
mqtt.broker.url=tcp://localhost:1883
mqtt.client.id.prefix=fabrique-serveur
mqtt.qos=1

# Fabricateur
# 0 = capacité aléatoire entre 3 et 5
usine.capacity=0

# Mode de production : séquentiel ou mutualise
usine.mode=sequentiel
```

| Clé | Défaut | Description |
|-----|--------|-------------|
| `mqtt.broker.url` | `tcp://localhost:1883` | URL du broker MQTT |
| `mqtt.qos` | `1` | Qualité de service MQTT (0, 1 ou 2) |
| `usine.capacity` | `0` | Capacité du Fabricateur (0 = aléatoire) |
| `usine.mode` | `sequentiel` | `sequentiel` ou `mutualise` |

### Frontend (`config.properties`)

```properties
mqtt.broker.url=tcp://localhost:1883
mqtt.client.id.prefix=fabrique-frontend
mqtt.qos=1

# Délai avant message "usine indisponible" (ms)
order.timeout.ms=30000
```

| Clé | Défaut | Description |
|-----|--------|-------------|
| `mqtt.broker.url` | `tcp://localhost:1883` | URL du broker MQTT |
| `order.timeout.ms` | `30000` | Timeout si l'usine ne répond pas |

---

## Écrans de l'application

| Écran | Description |
|-------|-------------|
| **Accueil** | Présentation et navigation |
| **Catalogue** | Liste des lunettes, gestion du panier, passage de commande |
| **Attente** | Suivi en temps réel (barre de progression + statuts) |
| **Vérification S/N** | Vérification de la validité d'un numéro de série |

---

## Modes de production

### Mode séquentiel (défaut)

Chaque commande est traitée indépendamment. Les lunettes d'un même lot sont fabriquées en parallèle.

```
Commande [CLAUDE×3]
  └── Lot 1 : configurer([C,C,C]) → fabriquer×3 en parallèle
```

### Mode mutualisé

Plusieurs commandes simultanées sont regroupées dans un seul lot machine pour maximiser l'utilisation de la capacité.

```
Commande A [CLAUDE×2] ──┐
Commande B [BANANA×1] ──┼── configurer([C,C,B]) → fabriquer×3 en parallèle
```

Activer dans `config.properties` :
```properties
usine.mode=mutualise
```

---

## Topics MQTT

| Topic | Sens | Description |
|-------|------|-------------|
| `orders/{uuid}` | Client → Usine | Passage d'une commande |
| `orders/{uuid}/validated` | Usine → Client | Commande valide |
| `orders/{uuid}/cancelled` | Usine → Client | Commande invalide (avec motif) |
| `orders/{uuid}/status` | Usine → Client | `processing` ou `processed` |
| `orders/{uuid}/delivery` | Usine → Client | Livraison avec les numéros de série |
| `orders/{uuid}/error` | Usine → Client | Erreur de fabrication |
| `serials/{serial}/check` | Client → Usine | Vérification d'un numéro de série |
| `serials/{serial}` | Usine → Client | Résultat : type de lunette ou `invalid` |

---

## Règles de validation d'une commande

Une commande est valide si :
- Chaque type de lunette est connu (`CLAUDE`, `CHATGPT`, `BANANA`, `LE_CHAT`)
- La quantité de chaque type est comprise entre **0 inclus** et **10 exclu**
- La quantité **totale** est strictement supérieure à **0**

---

## Format de sérialisation

Le protocole utilise un format texte maison (UTF-8) :

```
TYPE|cle:valeur|cle:valeur|...
```

Exemples :
```
ORDER|id:550e8400-e29b-41d4-a716|CLAUDE:2|BANANA:1
DELIVERY|id:550e8400-...|AB-3DMYIN-D3C7E5:CLAUDE|CD-4XKPOL-A1B2C3:BANANA
CANCELLED|id:550e8400-...|reason:Quantite invalide pour CLAUDE : 11
```

Les caractères `|` et `:` présents dans les valeurs sont échappés avec `\`.

---

## Tests

```bash
# Tous les tests (y compris les tests lents ~1-2 min)
mvn test

# Tests rapides uniquement (sans fabrication réelle)
mvn test -DskipSlowTests=slow
```

Les tests lents utilisent un vrai `Fabricateur` (2-3s par lunette produite).

---

## Build depuis les sources

### Backend

```bash
cd fabrique-backend

# Configurer l'accès au dépôt privé dans ~/.m2/settings.xml
# <servers>
#    <server>
#      <id>github-fabricateur</id>
#      <username>le-prof-de-raizo</username>
#      <password>TOKEN</password>
#    </server>
#  </servers>


mvn clean package -DskipSlowTests=slow
java -jar serveur/target/fabrique-serveur.jar
```

### Frontend

```bash
cd fabrique-frontend
mvn clean package
java -jar target/fabrique-frontend.jar

# Ou en développement
mvn javafx:run
```

---

## Équipe

| Membre | Domaine |
|--------|---------|
| [Victor-Nervy IRAZIGAMA] | Usine|
| [Patrick-Pierre HABONIMANA] | Serveur|
| [Raphaël Mangin] | Squelette du projet, GitHub Action, Sérialisation, README |
| [Mélissandre Los] | JavaFx, navigation, client MQTT côté UI |
