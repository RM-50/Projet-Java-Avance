# Fabrique de lunettes — Backend

Projet Maven multi-module qui implémente le backend de la fabrique de lunettes connectées.

## Modules

```
fabrique-backend/
├── pom.xml          # POM parent
├── usine/           # Pilote le fabricateur (lib privée bernard-flou)
│   └── ...
└── serveur/         # Passerelle MQTT (Eclipse Paho), point d'entrée
    └── ...
```

- **`usine`** : encapsule le `Fabricateur`. Expose une API thread-safe
  `produire(Map<TypeLunette, Integer>)`. Publié en tant que dépendance
  Maven sur GitHub Packages sur création d'une release.
- **`serveur`** : se connecte au broker MQTT, reçoit les commandes, les
  valide, délègue à l'usine et publie les réponses. Empaqueté en jar.

## Prérequis

- JDK 21
- Maven 3.9+
- Un broker [Mosquitto](https://mosquitto.org) accessible (par défaut sur `localhost:1883`)
- Accès au dépôt Maven privé GitHub hébergeant `bernard-flou:fabricateur:0.0.1`

## Configuration de l'accès au dépôt privé

Dans `~/.m2/settings.xml`, ajoutez :

```xml
<settings>
  <servers>
    <server>
      <id>github-fabricateur</id>
      <username>le-prof-de-raizo</username>
      <password>VOTRE_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

## Build

```bash
mvn clean verify
```

Le jar exécutable est généré dans `serveur/target/fabrique-serveur.jar`.

## Lancement

Avec la configuration par défaut (broker sur `localhost:1883`) :

```bash
java -jar serveur/target/fabrique-serveur.jar
```

## Configuration

Voir `serveur/src/main/resources/config.properties` pour les options.

| Clé | Défaut | Description |
| --- | --- | --- |
| `mqtt.broker.url` | `tcp://localhost:1883` | URL du broker MQTT |
| `mqtt.client.id.prefix` | `fabrique-serveur` | Préfixe du clientId MQTT |
| `mqtt.qos` | `1` | Qualité de service MQTT |
| `usine.capacity` | `0` | Capacité du fabricateur (0 = aléatoire) |

## CI/CD

- **CI** (`.github/workflows/ci.yml`) : build + tests à chaque push/PR.
- **Publication usine** (`.github/workflows/release-usine.yml`) :
  publie le `.jar` de l'usine sur GitHub Packages quand une release est créée.
  La version est extraite du tag (ex. tag `v1.0.0` → version Maven `1.0.0`).
