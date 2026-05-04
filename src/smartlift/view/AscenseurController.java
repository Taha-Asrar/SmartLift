package smartlift.view;

import javafx.application.Platform;
import smartlift.metier.GestionnaireAscenseurs;
import smartlift.model.Ascenseur;
import smartlift.model.GenerateurResident;
import smartlift.model.Immeuble;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Contrôleur JavaFX.
 *
 * Correction logique :
 *  - Seul l'étage d'ORIGINE est ajouté comme arrêt immédiatement.
 *  - Quand les portes s'ouvrent à l'étage d'origine (résident monte),
 *    l'étage de DESTINATION est ajouté à ce moment-là.
 *  Cela empêche l'ascenseur de passer par la destination avant de récupérer le résident.
 */
public class AscenseurController {

    public static final int NOMBRE_ETAGES     = 5;
    public static final int NOMBRE_ASCENSEURS = 2;

    private final AscenseurView    view;
    private Immeuble               immeuble;
    private GestionnaireAscenseurs gestionnaire;
    private GenerateurResident     generateur;
    private ExecutorService        executor;

    /**
     * Stocke les destinations en attente par ascenseur et étage d'origine.
     * ascId -> (origFloor -> file de destFloor)
     * Thread-safe : écrit depuis le thread gestionnaire, lu depuis le thread ascenseur.
     */
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Queue<Integer>>> pickupDestinations
            = new ConcurrentHashMap<>();

    public AscenseurController(AscenseurView view) {
        this.view = view;
    }

    public void demarrerSimulation() {
        immeuble     = new Immeuble(NOMBRE_ETAGES);
        gestionnaire = new GestionnaireAscenseurs(immeuble);
        immeuble.creerAscenseurs(NOMBRE_ASCENSEURS);

        setupCallbacks();

        executor = Executors.newFixedThreadPool(NOMBRE_ASCENSEURS + 1);
        for (Ascenseur a : immeuble.getAscenseurs()) {
            executor.submit(a);
        }
        generateur = new GenerateurResident(gestionnaire, NOMBRE_ETAGES);
        executor.submit(generateur);
    }

    private void setupCallbacks() {
        // ── Callback assignation : stocke la destination, anime le résident ──────
        gestionnaire.setAssignmentCallback(data -> {
            final int etageOrig = data[0];
            final int etageDest = data[1];
            final int ascId     = data[2];

            // Stocke la destination de façon thread-safe
            pickupDestinations
                .computeIfAbsent(ascId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(etageOrig, k -> new ConcurrentLinkedQueue<>())
                .add(etageDest);

            Platform.runLater(() -> view.animerNouveauResident(etageOrig, etageDest, ascId));
        });

        for (Ascenseur a : immeuble.getAscenseurs()) {
            final int ascId = a.getId();

            // ── Callback déplacement ─────────────────────────────────────────────
            a.setUiCallback(() -> {
                final int    etage = a.getEtageCourant();
                final String dir   = a.getDirectionActuelle().name();
                Platform.runLater(() -> {
                    view.actualiserPositionAscenseur(ascId, etage);
                    view.actualiserStatut(ascId, etage, dir);
                });
            });

            // ── Callback portes OUVERTES ─────────────────────────────────────────
            a.setPortesOuvertesCallback(() -> {
                // S'exécute sur le thread de l'ascenseur
                final int etage = a.getEtageCourant();

                // Ajoute l'étage DESTINATION quand le résident monte ici
                ConcurrentHashMap<Integer, Queue<Integer>> ascPickups = pickupDestinations.get(ascId);
                if (ascPickups != null) {
                    Queue<Integer> dests = ascPickups.remove(etage); // remove = atomique
                    if (dests != null) {
                        for (int dest : dests) {
                            System.out.println("  [Controleur] Resident monte Asc." + ascId
                                    + " -> destination etage " + dest);
                            a.ajouterArret(dest);
                        }
                    }
                }

                // Mise à jour UI sur le thread JavaFX
                Platform.runLater(() -> {
                    view.animerOuverturePortes(ascId, etage);
                    view.ajouterLog("Portes ouvertes - Asc." + ascId + " Etage " + etage);

                    // Attente ouverture complète (450ms anim portes) PUIS entrée/sortie résidents
                    javafx.animation.PauseTransition waitDoor =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    waitDoor.setOnFinished(e -> {
                        view.animerEntreeResidents(ascId, etage);
                        view.animerSortieResidents(ascId, etage);
                    });
                    waitDoor.play();
                });
            });

            // ── Callback portes FERMÉES ──────────────────────────────────────────
            a.setPortesFermeesCallback(() ->
                Platform.runLater(() -> view.animerFermeturePortes(ascId))
            );
        }
    }

    public void arreter() {
        if (generateur != null) generateur.arreterSimulation();
        if (immeuble   != null) immeuble.getAscenseurs().forEach(Ascenseur::arreter);
        if (executor   != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                    executor.shutdownNow();
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
