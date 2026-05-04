# SmartLift 🛗

SmartLift est un simulateur interactif d'ascenseurs intelligents développé en Java avec **JavaFX**. Ce projet met en œuvre des concepts de **programmation orientée objet (POO)**, le **multithreading**, et respecte une architecture **MVC (Modèle-Vue-Contrôleur)** stricte. 

L'application permet de visualiser en temps réel la gestion automatisée de plusieurs ascenseurs dans un immeuble à travers une interface dynamique.

---

## 🎯 Fonctionnalités Principales

- **Algorithme SCAN** : Optimisation des déplacements des ascenseurs pour servir le plus grand nombre de requêtes sans faire de "demi-tours" inefficaces.
- **Système de Dispatching Intelligent** : Assigne l'ascenseur le plus proche et le plus adapté en fonction de sa direction et de la distance avec le résident qui appelle.
- **Multithreading** : Chaque ascenseur fonctionne sur son propre thread, de manière indépendante, permettant des mouvements simultanés et fluides.
- **Interface Graphique Dynamique** : Conçue avec JavaFX, elle offre :
  - Des animations de déplacements des résidents.
  - L'ouverture et la fermeture synchronisée des portes.
  - Des statuts en direct sur la position et la direction de chaque ascenseur.
- **Génération Aléatoire de Requêtes** : Un générateur automatique simule des appels fréquents par des résidents à différents étages.
- **Journalisation Persistante** : Enregistrement de l'historique des déplacements des ascenseurs dans un fichier CSV (`historique_ascenseurs.csv`).

## 🛠️ Technologies Utilisées

- **Langage** : Java 8+
- **Interface Graphique** : JavaFX
- **Concepts clés** : 
  - Threads (`ExecutorService`, `wait()`, `notifyAll()`)
  - Structures de données concurrentes (`ConcurrentHashMap`, `ConcurrentLinkedQueue`)
  - Architecture MVC
  - Callbacks asynchrones

## 📂 Architecture du Projet

```text
src/smartlift/
│
├── metier/
│   └── GestionnaireAscenseurs.java  # Gère l'assignation des requêtes aux ascenseurs
│
├── model/
│   ├── Ascenseur.java               # Modèle threadé d'un ascenseur (Logique SCAN)
│   ├── Direction.java               # Énumération (MONTER, DESCENDRE, ARRET)
│   ├── GenerateurResident.java      # Thread générant des requêtes aléatoires
│   ├── Immeuble.java                # Regroupe les entités du bâtiment
│   ├── Requete.java                 # Représente un appel d'ascenseur
│   └── Resident.java                # Données basiques d'un résident
│
├── view/
│   ├── AscenseurController.java     # Contrôleur MVC, coordonne le modèle et l'UI
│   ├── AscenseurView.java           # L'interface graphique et ses animations (JavaFX)
│   └── style.css                    # Fichier de style pour la Vue
│
└── Main.java                        # Point d'entrée de l'application
```

## 🚀 Comment lancer le projet

Ce projet a été développé sur **IntelliJ IDEA**.

1. **Prérequis** :
   - Assurez-vous d'avoir le [JDK](https://www.oracle.com/java/technologies/downloads/) installé (idéalement 17+).
   - Avoir le SDK JavaFX configuré dans votre IDE si vous n'utilisez pas un JDK incluant JavaFX (comme Liberica ou Zulu FX).
   
2. **Configuration IntelliJ IDEA** :
   - Importez le projet dans IntelliJ.
   - Si vous utilisez une version récente du JDK (11+), ajoutez les options VM de JavaFX dans les configurations d'exécution (Run/Debug Configurations) de `Main.java` :
     ```text
     --module-path "CHEMIN_VERS_VOTRE_JAVAFX_LIB" --add-modules javafx.controls,javafx.fxml
     ```

3. **Exécution** :
   - Lancez la classe `Main.java`. L'interface graphique s'ouvrira et la simulation débutera automatiquement.

## 🤝 Contribution
Sentez-vous libre de "fork" ce projet, de soumettre des "pull requests" ou d'ouvrir des "issues" pour toute amélioration !
