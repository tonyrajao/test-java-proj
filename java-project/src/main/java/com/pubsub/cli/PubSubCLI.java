package com.pubsub.cli;

import com.pubsub.config.KafkaConfig;
import com.pubsub.model.Content;
import com.pubsub.model.User;
import com.pubsub.service.ContentPublisher;
import com.pubsub.service.ContentSubscriber;
import com.pubsub.service.EmailService;
import com.pubsub.service.SubscriptionService;
import com.pubsub.service.UserService;
import com.pubsub.util.KeywordExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PubSubCLI implements CommandLineRunner {
    private final Map<String, ContentSubscriber> subscribers = new ConcurrentHashMap<>();
    private String currentUser;
    private User.UserType currentUserType;

    private final UserService userService;
    private final ContentPublisher contentPublisher;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaConfig kafkaConfig;
    @Autowired
    private KeywordExtractor keywordExtractor;

    @Autowired
    public PubSubCLI(UserService userService,
            ContentPublisher contentPublisher,
            SubscriptionService subscriptionService,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            KafkaConfig kafkaConfig) {
        this.userService = userService;
        this.contentPublisher = contentPublisher;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.kafkaConfig = kafkaConfig;
    }

    @Override
    public void run(String... args) {
        // Add shutdown hook for proper cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        
        // Set console encoding for Docker
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        try (Scanner scanner = new Scanner(System.in, "UTF-8")) {
            while (true) {
                if (currentUser == null) {
                    handleUserLogin(scanner);
                    continue;
                }

                clearScreen();
                displayMainMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        if (currentUserType == User.UserType.PUBLISHER) {
                            publishContent(scanner);
                        } else {
                            subscribe(scanner);
                        }
                        break;
                    case "2":
                        if (currentUserType == User.UserType.PUBLISHER) {
                            listPublishedContent();
                        } else {
                            unsubscribe(scanner);
                        }
                        break;
                    case "3":
                        if (currentUserType == User.UserType.PUBLISHER) {
                            deleteContent(scanner);
                        } else {
                            listSubscriptions();
                        }
                        break;
                    case "4":
                        if (currentUserType == User.UserType.PUBLISHER) {
                            currentUser = null;
                            System.out.println("Déconnexion réussie !");
                            continue;
                        } else {
                            viewNotifications();
                        }
                        break;
                    case "5":
                        if (currentUserType == User.UserType.PUBLISHER) {
                            closeAllSubscribers();
                            return;
                        } else {
                            viewAllMatchingContent();
                        }
                        break;
                    case "6":
                        if (currentUserType == User.UserType.SUBSCRIBER) {
                            currentUser = null;
                            System.out.println("Déconnexion réussie !");
                            continue;
                        }
                        closeAllSubscribers();
                        return;
                    case "7":
                        if (currentUserType == User.UserType.SUBSCRIBER) {
                            closeAllSubscribers();
                            return;
                        }
                        System.out.println("Choix invalide !");
                        break;
                    default:
                        System.out.println("Choix invalide !");
                }

                if (!choice.equals("6") && !choice.equals("7")) {
                    System.out.println("\nAppuyez sur Entrée pour continuer...");
                    scanner.nextLine();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUserLogin(Scanner scanner) {
        clearScreen();
        System.out.println("=== Bienvenue dans le système PubSub ===");
        System.out.println("1. Connexion");
        System.out.println("2. Inscription");
        System.out.println("3. Lister les utilisateurs existants");
        System.out.println("4. Sortir");
        System.out.print("Choisissez une option : ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                handleLogin(scanner);
                break;
            case "2":
                handleRegistration(scanner);
                break;
            case "3":
                listExistingUsers();
                System.out.println("\nAppuyez sur Entrée pour continuer...");
                scanner.nextLine();
                break;
            case "4":
                closeAllSubscribers();
                System.exit(0);
            default:
                System.out.println("Choix invalide !");
        }
    }

    private void handleLogin(Scanner scanner) {
        System.out.print("Entrez le nom d'utilisateur : ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Entrez le mot de passe : ");
        String password = scanner.nextLine().trim();

        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                currentUser = user.getUsername();
                currentUserType = user.getType();

                if (currentUserType == User.UserType.SUBSCRIBER && !subscribers.containsKey(currentUser)) {
                    subscribers.put(currentUser,
                            new ContentSubscriber(currentUser, subscriptionService, emailService, userService, kafkaConfig));
                }

                System.out.println("Connexion réussie !");
            } else {
                System.out.println("Mot de passe incorrect !");
            }
        } else {
            System.out.println("Utilisateur non trouvé !");
        }
    }

    private void handleRegistration(Scanner scanner) {
        System.out.print("Entrez le nom d'utilisateur : ");
        String username = scanner.nextLine().trim();

        if (userService.existsByUsername(username)) {
            System.out.println("Nom d'utilisateur déjà existant !");
            return;
        }

        System.out.print("Entrez l'email : ");
        String email = scanner.nextLine().trim();

        System.out.print("Entrez le mot de passe : ");
        String password = scanner.nextLine().trim();

        System.out.print("Confirmez le mot de passe : ");
        String confirmPassword = scanner.nextLine().trim();

        if (!password.equals(confirmPassword)) {
            System.out.println("Les mots de passe ne correspondent pas !");
            return;
        }

        System.out.println("Sélectionnez le type d'utilisateur :");
        System.out.println("1. Publisher");
        System.out.println("2. Subscriber");
        System.out.print("Choisissez le type (1/2) : ");
        String typeChoice = scanner.nextLine().trim();

        User.UserType type;
        switch (typeChoice) {
            case "1":
                type = User.UserType.PUBLISHER;
                break;
            case "2":
                type = User.UserType.SUBSCRIBER;
                break;
            default:
                System.out.println("Choix invalide !");
                return;
        }

        try {
            User user = userService.createUser(username, email, password, type);
            System.out.println("Inscription réussie !");

            // Connexion automatique après l'inscription
            currentUser = user.getUsername();
            currentUserType = user.getType();

            if (currentUserType == User.UserType.SUBSCRIBER) {
                subscribers.put(currentUser,
                        new ContentSubscriber(currentUser, subscriptionService, emailService, userService, kafkaConfig));
            }
        } catch (Exception e) {
            System.out.println("Échec de l'inscription : " + e.getMessage());
        }
    }

    private void listExistingUsers() {
        System.out.println("\n=== Lister les utilisateurs existants ===");
        List<User> users = userService.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("Aucun utilisateur n'existe encore.");
            return;
        }

        System.out.println("\nPublishers :");
        users.stream()
                .filter(u -> u.getType() == User.UserType.PUBLISHER)
                .forEach(u -> System.out.println("- " + u.getUsername()));

        System.out.println("\nSubscribers :");
        users.stream()
                .filter(u -> u.getType() == User.UserType.SUBSCRIBER)
                .forEach(u -> System.out.println("- " + u.getUsername()));
    }

    private void displayMainMenu() {
        System.out.println("\nPubSub CLI - " + currentUserType + ": " + currentUser);
        System.out.println("====================================");

        if (currentUserType == User.UserType.PUBLISHER) {
            System.out.println("1. Publier du contenu");
            System.out.println("2. Lister mon contenu publié");
            System.out.println("3. Supprimer un contenu");
            System.out.println("4. Déconnexion");
            System.out.println("5. Sortir");
            System.out.print("Choisissez une option (1-5) : ");
        } else {
            System.out.println("1. S'abonner à des mots-clés");
            System.out.println("2. Se désabonner de mots-clés");
            System.out.println("3. Lister mes abonnements");
            System.out.println("4. Voir les notifications");
            System.out.println("5. Voir tout le contenu correspondant");
            System.out.println("6. Déconnexion");
            System.out.println("7. Sortir");
            System.out.print("Choisissez une option (1-7) : ");
        }
    }

    private void clearScreen() {
        // Docker-friendly clear screen
        if (System.getenv("DOCKER_CONTAINER") != null) {
            // In Docker, just print newlines
            System.out.println("\n\n\n\n\n");
        } else {
            // Regular clear screen for non-Docker environment
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    private void publishContent(Scanner scanner) {
        try {
            System.out.print("Entrez le titre : ");
            String title = scanner.nextLine().trim();
            
            if (title.isEmpty()) {
                System.out.println("Le titre ne peut pas être vide.");
                return;
            }

            System.out.print("Entrez le contenu : ");
            String body = scanner.nextLine().trim();
            
            if (body.isEmpty()) {
                System.out.println("Le contenu ne peut pas être vide.");
                return;
            }

            Set<String> keywords = new HashSet<>();

            System.out.print(
                    "Voulez-vous : \n1. Entrer les mots-clés manuellement\n2. Utiliser les mots-clés extraits automatiquement\nChoix (1 ou 2) : ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                System.out.print("Entrez les mots-clés (séparés par des virgules) : ");
                String[] keywordArray = scanner.nextLine().split(",");
                for (String keyword : keywordArray) {
                    keywords.add(keyword.trim().toLowerCase());
                }
            } else {
                boolean validKeywords = false;
                while (!validKeywords) {
                    // Extraction automatique des mots-clés
                    keywords = keywordExtractor.extractKeywords(title, body);
                    System.out.println("\nMots-clés extraits automatiquement : " + String.join(", ", keywords));
                    
                    // Allow keyword deletion
                    boolean editingKeywords = true;
                    while (editingKeywords) {
                        System.out.println("\nOptions :");
                        System.out.println("1. Supprimer un mot-clé");
                        System.out.println("2. Continuer avec ces mots-clés");
                        System.out.print("Votre choix (1/2) : ");
                        
                        String editChoice = scanner.nextLine().trim();
                        
                        if (editChoice.equals("1")) {
                            if (keywords.isEmpty()) {
                                System.out.println("Aucun mot-clé à supprimer.");
                                continue;
                            }
                            
                            // Convert set to list for indexed access
                            List<String> keywordsList = new ArrayList<>(keywords);
                            System.out.println("\nMots-clés actuels :");
                            for (int i = 0; i < keywordsList.size(); i++) {
                                System.out.println((i + 1) + ". " + keywordsList.get(i));
                            }
                            
                            System.out.print("Entrez le numéro du mot-clé à supprimer (1-" + keywordsList.size() + ") : ");
                            try {
                                int keywordIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
                                if (keywordIndex >= 0 && keywordIndex < keywordsList.size()) {
                                    String removedKeyword = keywordsList.get(keywordIndex);
                                    keywords.remove(removedKeyword);
                                    System.out.println("Mot-clé '" + removedKeyword + "' supprimé.");
                                    System.out.println("Mots-clés restants : " + String.join(", ", keywords));
                                } else {
                                    System.out.println("Numéro invalide.");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Veuillez entrer un numéro valide.");
                            }
                        } else if (editChoice.equals("2")) {
                            editingKeywords = false;
                        } else {
                            System.out.println("Option invalide.");
                        }
                    }
                    
                    if (keywords.isEmpty()) {
                        System.out.println("Au moins un mot-clé est requis.");
                        continue;
                    }
                    
                    System.out.print("Acceptez ces mots-clés ? (o/n) : ");
                    
                    if (scanner.nextLine().trim().toLowerCase().equals("o")) {
                        System.out.print("Voulez-vous ajouter d'autres mots-clés ? (o/n) : ");
                        if (scanner.nextLine().trim().toLowerCase().equals("o")) {
                            System.out.print("Entrez les mots-clés supplémentaires (séparés par des virgules) : ");
                            String[] additionalKeywords = scanner.nextLine().split(",");
                            for (String keyword : additionalKeywords) {
                                keywords.add(keyword.trim().toLowerCase());
                            }
                        }
                        validKeywords = true;
                    } else {
                        System.out.print("Entrez vos propres mots-clés (séparés par des virgules) : ");
                        keywords.clear();
                        String[] manualKeywords = scanner.nextLine().split(",");
                        for (String keyword : manualKeywords) {
                            String trimmedKeyword = keyword.trim().toLowerCase();
                            if (!trimmedKeyword.isEmpty()) {
                                keywords.add(trimmedKeyword);
                            }
                        }
                        
                        if (keywords.isEmpty()) {
                            System.out.println("Au moins un mot-clé est requis.");
                            continue;
                        }
                        
                        validKeywords = true;
                    }
                }
            }

            Content content = new Content(title, body, keywords, currentUser);
            contentPublisher.publish(content);

            System.out.println("\nContenu publié avec succès avec les mots-clés : " + String.join(", ", keywords));
            System.out.println("Les Subscribers seront notifiés automatiquement via Kafka");
        } catch (Exception e) {
            System.err.println("Erreur de publication du contenu : " + e.getMessage());
        }
    }

    private void subscribe(Scanner scanner) {
        try {
            System.out.println(
                    "Entrez le mot-clé pour vous abonner (peut contenir jusqu'à 3 mots, par exemple, 'IA' ou 'apprentissage automatique' ou 'coupe du monde 2024') : ");
            String keyword = scanner.nextLine().trim();

            ContentSubscriber subscriber = subscribers.get(currentUser);
            if (subscriber.addSubscription(keyword)) {
                System.out.println("Abonnement réussi au mot-clé : " + keyword);
            } else {
                System.out.println("Échec de l'abonnement. Les mots-clés doivent contenir 3 mots ou moins.");
            }
        } catch (Exception e) {
            System.out.println("Échec de l'abonnement : " + e.getMessage());
        }
    }

    private void unsubscribe(Scanner scanner) {
        ContentSubscriber subscriber = subscribers.get(currentUser);
        Set<String> activeKeywords = subscriber.getActiveKeywords();

        if (activeKeywords.isEmpty()) {
            System.out.println("Aucun abonnement actif trouvé.");
            return;
        }

        System.out.println("Vos mots-clés actifs : " + String.join(", ", activeKeywords));
        System.out.print("Entrez le mot-clé pour vous désabonner : ");
        String keyword = scanner.nextLine().trim().toLowerCase();

        if (subscriber.removeSubscription(keyword)) {
            System.out.println("Désabonnement réussi du mot-clé : " + keyword);
        } else {
            System.out.println("Échec du désabonnement. Mot-clé non trouvé.");
        }
    }

    private void listSubscriptions() {
        ContentSubscriber subscriber = subscribers.get(currentUser);
        Set<String> keywords = subscriber.getActiveKeywords();

        if (keywords.isEmpty()) {
            System.out.println("Aucun abonnement actif trouvé.");
        } else {
            System.out.println("Vos abonnements actifs :");
            System.out.println(String.join(", ", keywords));
        }
    }

    private void viewNotifications() {
        ContentSubscriber subscriber = subscribers.get(currentUser);
        Map<String, List<Content>> notifications = subscriber.getNotifications();

        if (notifications.isEmpty()) {
            System.out.println("Aucune notification trouvée.");
            return;
        }

        System.out.println("\nVos notifications :");
        System.out.println("====================================");

        for (Map.Entry<String, List<Content>> entry : notifications.entrySet()) {
            String keyword = entry.getKey();
            List<Content> contents = entry.getValue();

            System.out.println("\nNotifications pour le mot-clé '" + keyword + "':");
            System.out.println("------------------------------------");

            for (Content content : contents) {
                System.out.println("\nTitre : " + content.getTitle());
                System.out.println(" : " + content.getPublisherId());
                System.out.println("Contenu : " + content.getBody());
                System.out.println("Mots-clés : " + String.join(", ", content.getKeywords()));
                System.out.println("Publié le : " + content.getCreatedAt());
                System.out.println("------------------------------------");
            }
        }
    }

    private void listPublishedContent() {
        List<Content> contents = contentPublisher.getPublisherContent(currentUser);
        if (contents.isEmpty()) {
            System.out.println("Vous n'avez publié aucun contenu pour le moment.");
            return;
        }

        System.out.println("\nVotre contenu publié :");
        System.out.println("====================================");
        for (Content content : contents) {
            System.out.println("\nTitre : " + content.getTitle());
            System.out.println("Contenu : " + content.getBody());
            System.out.println("Mots-clés : " + String.join(", ", content.getKeywords()));
            System.out.println("Publié le : " + content.getCreatedAt());
            System.out.println("====================================");
        }
    }

    private void viewAllMatchingContent() {
        ContentSubscriber subscriber = subscribers.get(currentUser);
        Set<String> keywords = subscriber.getActiveKeywords();

        if (keywords.isEmpty()) {
            System.out.println("Vous n'avez aucun abonnement actif. Veuillez vous abonner à des mots-clés.");
            return;
        }

        List<Content> allContent = contentPublisher.getAllContent();
        boolean foundMatch = false;

        System.out.println("\nTout le contenu correspondant à vos mots-clés :");
        System.out.println("====================================");

        for (String keyword : keywords) {
            System.out.println("\nContenu correspondant au mot-clé '" + keyword + "':");
            System.out.println("------------------------------------");

            boolean keywordMatch = false;
            String[] keywordParts = keyword.split("\\s+");

            for (Content content : allContent) {
                boolean matches = false;

                // Vérifier uniquement les mots-clés du contenu
                for (String contentKeyword : content.getKeywords()) {
                    if (matchesKeyword(contentKeyword.toLowerCase(), keywordParts)) {
                        matches = true;
                        break;
                    }
                }

                if (matches) {
                    keywordMatch = true;
                    foundMatch = true;
                    System.out.println("\nTitre : " + content.getTitle());
                    System.out.println(" : " + content.getPublisherId());
                    System.out.println("Contenu : " + content.getBody());
                    System.out.println("Mots-clés : " + String.join(", ", content.getKeywords()));
                    System.out.println("Publié le : " + content.getCreatedAt());
                    System.out.println("------------------------------------");
                }
            }

            if (!keywordMatch) {
                System.out.println("Aucun contenu trouvé correspondant à ce mot-clé.");
            }
        }

        if (!foundMatch) {
            System.out.println("\nAucun contenu trouvé correspondant à vos mots-clés.");
        }
    }

    private boolean matchesKeyword(String text, String[] keywordParts) {
        if (keywordParts.length == 1) {
            return text.contains(keywordParts[0].toLowerCase());
        }

        String fullKeyword = String.join(" ", keywordParts).toLowerCase();
        return text.contains(fullKeyword);
    }

    private void deleteContent(Scanner scanner) {
        List<Content> contents = contentPublisher.getPublisherContent(currentUser);
        if (contents.isEmpty()) {
            System.out.println("Vous n'avez publié aucun contenu pour le moment.");
            return;
        }

        System.out.println("\nVotre contenu publié :");
        System.out.println("====================================");
        for (int i = 0; i < contents.size(); i++) {
            Content content = contents.get(i);
            System.out.println("\n" + (i + 1) + ".");
            System.out.println("Titre : " + content.getTitle());
            System.out.println("Contenu : " + content.getBody());
            System.out.println("Mots-clés : " + String.join(", ", content.getKeywords()));
            System.out.println("Publié le : " + content.getCreatedAt());
            System.out.println("====================================");
        }

        System.out.print("\nEntrez le numéro du contenu à supprimer (1-" + contents.size() + ") ou 0 pour annuler : ");
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice == 0) {
                System.out.println("Suppression annulée.");
                return;
            }
            if (choice < 1 || choice > contents.size()) {
                System.out.println("Numéro invalide.");
                return;
            }

            Content selectedContent = contents.get(choice - 1);
            System.out.print("Êtes-vous sûr de vouloir supprimer ce contenu ? (o/n) : ");
            if (scanner.nextLine().trim().toLowerCase().equals("o")) {
                if (contentPublisher.deleteContent(selectedContent.getId(), currentUser)) {
                    System.out.println("Contenu supprimé avec succès.");
                } else {
                    System.out.println("Échec de la suppression du contenu.");
                }
            } else {
                System.out.println("Suppression annulée.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Veuillez entrer un numéro valide.");
        }
    }

    private void closeAllSubscribers() {
        for (ContentSubscriber subscriber : subscribers.values()) {
            try {
                subscriber.close();
            } catch (Exception e) {
                System.err.println("Erreur de fermeture de l'Subscriber : " + e.getMessage());
            }
        }
    }

    private void cleanup() {
        System.out.println("Nettoyage des ressources...");
        closeAllSubscribers();
        // Add any additional cleanup needed
    }
}
