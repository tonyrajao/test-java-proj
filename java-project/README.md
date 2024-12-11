# Système de Publication et Abonnement de Contenu

Ce projet est un système de publication et d'abonnement de contenu développé avec Java, Spring Boot, Maven, et d'autres frameworks. Il inclut des fonctionnalités telles que l'intégration avec Kafka, des notifications par email, et une base de données H2 légère.

## Prérequis

Assurez-vous que les logiciels suivants sont installés sur votre système :

1. **Java Development Kit (JDK)**

   - Version : 17 ou supérieure
   - [Télécharger JDK](https://www.oracle.com/java/technologies/javase-downloads.html)
   - **Commande pour vérifier l'installation :**
     ```bash
     $ java -version
     ```

2. **Apache Maven**

   - Version : 3.6.0 ou supérieure
   - [Télécharger Maven](https://maven.apache.org/download.cgi)
   - **Commande pour vérifier l'installation :**
     ```bash
     $ mvn -v
     ```

3. **Docker et Docker Compose** (Optionnel pour un déploiement conteneurisé)

   - [Obtenir Docker](https://www.docker.com/products/docker-desktop/)
   - **Commande pour vérifier l'installation :**
     ```bash
     $ docker --version
     $ docker-compose --version
     ```

4. **Kafka** (Optionnel si vous exécutez Kafka en local)

   - [Installer Kafka](https://kafka.apache.org/quickstart)
   - **Commande pour vérifier l'installation :**
     ```bash
     $ kafka-topics.sh --version
     ```

5. **Git**

   - [Installer Git](https://git-scm.com/)
   - **Commande pour vérifier l'installation :**
     ```bash
     $ git --version
     ```

## Structure du Projet

```
content-pubsub-system/
├── .gitignore
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
└── target/
```

## Étapes pour Exécuter le Projet

### 1. Cloner le Dépôt

```bash
$ git clone <repository_url>
$ cd content-pubsub-system
```

### 2. Configurer l'Environnement

1. Mettez à jour le fichier `application.properties` ou `application-docker.properties` dans le répertoire `src/main/resources/` pour correspondre à vos paramètres d'environnement (par exemple, URL de Kafka, configurations email).
2. Si vous utilisez Docker, aucune modification n'est nécessaire sauf si des personnalisations spécifiques sont requises.

### 3. Compiler le Projet

Avec Maven :

```bash
$ mvn clean install
```

**Ajoutez l'adresse email et le mot de passe de l'application pour les notifications email dans ****************`application.properties`**************** :**
   ```properties
   spring.mail.username=your-email@example.com
   spring.mail.password=your-app-password
   ```


### 4. Exécuter l'Application

#### Option 1 : Avec Maven

Exécutez l'application Spring Boot :

```bash
$ mvn spring-boot:run
```

#### Option 2 : Avec Docker

Construisez et exécutez le conteneur Docker :

```bash
$ docker-compose up --build
```

Vérifiez que l'application est en cours d'exécution :

```bash
$ docker ps
```

Pour accéder au CLI du conteneur en cours d'exécution :

```bash
$ docker exec -it pubsub-app /bin/sh
```

### 5. Accéder à l'Application

- Points d'API : Accédez à l'API via `http://localhost:8080`
- Kafka : Assurez-vous que Kafka est en cours d'exécution localement ou accessible via l'URL configurée.

## Configuration de la Base de Données

L'application utilise par défaut une base de données H2 embarquée. Pour accéder à la console H2 :

1. Rendez-vous sur `http://localhost:8080/h2-console`
2. Utilisez l'URL JDBC, le nom d'utilisateur et le mot de passe spécifiés dans `application.properties`.

## Tests

Exécutez la suite de tests avec Maven :

```bash
$ mvn test
```

## Journalisation

Les journaux sont écrits dans le fichier `pubsub.log` à la racine du projet.

## Notes de Déploiement

1. Utilisez Docker Compose pour un déploiement tout-en-un.
2. Assurez-vous que les dépendances externes comme Kafka et les serveurs email sont correctement configurés dans les environnements de production.
3. Pour la production, envisagez d'utiliser une base de données persistante au lieu de H2.

## Résolution des Problèmes

1. **Erreurs de Compilation** :

   - Assurez-vous que toutes les dépendances dans `pom.xml` sont résolues.
   - Exécutez `mvn dependency:resolve` pour déboguer.

2. **Problèmes avec Kafka** :

   - Vérifiez que Kafka est en cours d'exécution et accessible.
   - Vérifiez l'URL du broker dans `application.properties`.

3. **Problèmes avec Docker** :

   - Assurez-vous que Docker est installé et en cours d'exécution.
   - Vérifiez que le fichier Dockerfile et `docker-compose.yml` sont correctement configurés.

