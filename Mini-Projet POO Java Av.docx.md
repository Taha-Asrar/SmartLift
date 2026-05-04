**Mini-Projet POO Java**

Ce mini-projet vise à mettre en pratique les concepts de la programmation orientée objet en Java à travers deux simulations concrètes avec animations graphiques:

- un système d’ascenseurs intelligents

Le projet devra intégrer :  
une interface graphique (animation) avec JavaFX.  
la gestion du multithreading  
la manipulation des fichiers (flux)  
l’utilisation des collections Java


**Simulation d’ascenseurs** 

🔹 Description  
Simulation d’un immeuble de n étages avec 2 ascenseurs, capables de répondre aux appels des utilisateurs de manière optimisée.  
🔹 Fonctionnalités attendues  
✔️ Gestion des ascenseurs  
Deux ascenseurs indépendants  
Résidents accédant aux ascenseurs ou sortants des ascenseurs  
Demande d'ascenseur (biuton appel par niveau)  
Déplacement entre les étages  
Gestion des appels (monter / descendre)  
Animation graphique  
✔️ Algorithme de décision  
Choisir l’ascenseur le plus proche  
Optimiser les déplacements (éviter les trajets inutiles)  
🔹 Multithreading  
Chaque ascenseur fonctionne dans un thread séparé  
Simulation du déplacement en temps réel  
Gestion des requêtes concurrentes  
🔹 Collections utilisées  
Queue pour les demandes d’étages  
List pour l’état des ascenseurs  
🔹 Fichiers (flux)  
Journalisation des déplacements  
Historique des appels et temps d’attente  
🔹 Interface JavaFX  
Représentation graphique des étages  
Position des ascenseurs en temps réel  
Boutons d’appel par étage  
Affichage des requêtes en cours  
**Contraintes techniques**  
Respect des principes POO :  
Encapsulation  
Héritage  
Polymorphisme  
Séparation claire des couches :  
Modèle  
Interface graphique  
Logique métier  
Utilisation de ExecutorService ou Thread  
Synchronisation (synchronized, Lock)

**Évaluation**  
Le projet sera évalué selon :  
Qualité de conception (UML recommandé)  
Utilisation correcte du multithreading  
Pertinence des structures de données  
Qualité de l’interface graphique  
Gestion des fichiers  
Clarté du code et organisation