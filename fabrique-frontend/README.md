# Fabrique de lunettes — Frontend (JavaFX)

Application graphique qui permet de :
- consulter le catalogue de lunettes ;
- passer commande ;
- suivre la fabrication ;
- vérifier la validité d'un numéro de série.

## Prérequis

- JDK 21
- Maven 3.9+
- Un broker MQTT accessible (par défaut `localhost:1883`)

## Lancement en développement

```bash
mvn javafx:run
```

## Build

```bash
mvn clean package
```

Produit `target/fabrique-frontend.jar`, un jar exécutable.

## Lancement du jar

```bash
java -jar target/fabrique-frontend.jar
```

