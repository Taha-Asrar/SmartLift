package smartlift.model;

import smartlift.metier.GestionnaireFichier;

/**
 * Représente un ascenseur physique.
 * Implémente Runnable car chaque ascenseur fonctionne dans son propre thread.
 *
 * Algorithme interne : SCAN (Elevator Algorithm)
 * L'ascenseur balaye les étages dans une direction jusqu'à ce qu'il n'y ait
 * plus d'arrêts dans ce sens, puis il inverse sa direction ou s'arrête.
 * Ce comportement imite un ascenseur réel et minimise les trajets inutiles.
 */
public class Ascenseur implements Runnable {

    // -------------------------------------------------------------------------
    // CONSTANTES
    // -------------------------------------------------------------------------
    /** Durée simulée pour se déplacer d'un étage à l'autre (ms). */
    private static final int TEMPS_DEPLACEMENT_MS = 1500;
    /** Durée simulée d'ouverture/fermeture des portes (ms). */
    private static final int TEMPS_PORTES_MS = 2000;

    // -------------------------------------------------------------------------
    // ATTRIBUTS
    // -------------------------------------------------------------------------
    private final int id;
    private int etageCourant;
    private Direction directionActuelle;

    /**
     * Tableau de booléens indexé par numéro d'étage.
     * arretsDemandes[2] == true signifie que l'ascenseur doit s'arrêter à l'étage 2.
     * Taille fixée au nombre d'étages de l'immeuble (ex: 5).
     */
    private final boolean[] arretsDemandes;
    private final int nombreEtages;
    private volatile boolean simulationActive;

    /**
     * Direction mémorisée : permet à l'ascenseur de privilégier sa direction courante
     * avant de faire demi-tour (algorithme SCAN complet).
     * Ex: montait vers étage 4, passe étage 3 → doit continuer à monter vers 4
     *     même si une destination vers 0 a été ajoutée à l'étage 3.
     */
    private Direction directionPrecedente = Direction.ARRET;

    /**
     * Callback exécuté sur le thread JavaFX (via Platform.runLater) à chaque
     * changement de position, pour mettre à jour l'interface graphique.
     */
    private Runnable uiCallback;
    private Runnable portesOuvertesCallback;
    private Runnable portesFermeesCallback;

    // -------------------------------------------------------------------------
    // CONSTRUCTEUR
    // -------------------------------------------------------------------------
    public Ascenseur(int id, int nombreEtages) {
        this.id = id;
        this.nombreEtages = nombreEtages;
        this.etageCourant = 0; // Démarre au rez-de-chaussée
        this.directionActuelle = Direction.ARRET;
        this.arretsDemandes = new boolean[nombreEtages];
        this.simulationActive = true;
    }

    // -------------------------------------------------------------------------
    // GESTION DES ARRÊTS
    // -------------------------------------------------------------------------

    /**
     * Ajoute un étage à la liste des arrêts demandés.
     * Cette méthode est synchronized car elle peut être appelée depuis le thread
     * du GestionnaireAscenseurs pendant que le thread de l'ascenseur s'exécute.
     * notifyAll() réveille l'ascenseur s'il était en attente.
     *
     * @param etage L'étage où l'ascenseur doit s'arrêter.
     */
    public synchronized void ajouterArret(int etage) {
        if (etage >= 0 && etage < nombreEtages) {
            arretsDemandes[etage] = true;
            System.out.println("  [Ascenseur " + id + "] Arrêt ajouté à l'étage " + etage);
            notifyAll(); // Réveille le thread de l'ascenseur s'il attend
        }
    }

    /**
     * Vérifie si l'ascenseur a au moins un arrêt planifié.
     */
    public synchronized boolean aDesArrets() {
        for (boolean arret : arretsDemandes) {
            if (arret) return true;
        }
        return false;
    }

    /**
     * Vérifie s'il reste des arrêts à faire AU-DESSUS de l'étage courant.
     */
    private boolean aDesArretEnHaut() {
        for (int i = etageCourant + 1; i < nombreEtages; i++) {
            if (arretsDemandes[i]) return true;
        }
        return false;
    }

    /**
     * Vérifie s'il reste des arrêts à faire EN-DESSOUS de l'étage courant.
     */
    private boolean aDesArretEnBas() {
        for (int i = 0; i < etageCourant; i++) {
            if (arretsDemandes[i]) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // BOUCLE PRINCIPALE DU THREAD (Algorithme SCAN)
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        System.out.println("[Ascenseur " + id + "] Thread demarre a l'etage " + etageCourant);

        while (simulationActive) {
            // 1. Attente tant qu'il n'y a aucun arrêt planifié
            synchronized (this) {
                while (!aDesArrets() && simulationActive) {
                    directionActuelle = Direction.ARRET;
                    notifierVue();
                    try {
                        System.out.println("[Ascenseur " + id + "] En attente de requetes...");
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (!simulationActive) break;

            // 2. CORRECTION BUG : vérifier si l'ascenseur est DÉJÀ à un étage demandé
            //    (ex: ascenseur au RDC, demande reçue pour le RDC → ouvrir portes immédiatement)
            synchronized (this) {
                if (arretsDemandes[etageCourant]) {
                    arretsDemandes[etageCourant] = false;
                    ouvrirPortes(); // le callback peut ajouter l'étage destination
                }
            }

            // Si plus d'arrêts après cette ouverture, retourner en attente
            if (!aDesArrets()) continue;

            // 3. Détermine la direction (SCAN : favorise la direction précédente en cas d'égalité)
            synchronized (this) {
                if (directionActuelle == Direction.ARRET) {
                    boolean hausse = aDesArretEnHaut();
                    boolean baisse = aDesArretEnBas();
                    if (hausse && baisse) {
                        // Égalité : continue dans la même direction qu'avant
                        directionActuelle = (directionPrecedente == Direction.DESCENDRE)
                                ? Direction.DESCENDRE : Direction.MONTER;
                    } else if (hausse) {
                        directionActuelle = Direction.MONTER;
                    } else if (baisse) {
                        directionActuelle = Direction.DESCENDRE;
                    }
                }
            }

            // 4. Déplacement d'un étage dans la direction courante
            deplacer();
        }

        System.out.println("[Ascenseur " + id + "] Thread arrete.");
    }

    /**
     * Déplace l'ascenseur d'un étage dans la direction actuelle (Algorithme SCAN).
     * Si un arrêt est demandé à l'étage courant, ouvre les portes.
     * Réévalue la direction à chaque mouvement.
     */
    private void deplacer() {
        try {
            // Pause simulant le temps de déplacement
            Thread.sleep(TEMPS_DEPLACEMENT_MS);

            // Se déplace physiquement d'un étage
            synchronized (this) {
                if (directionActuelle == Direction.MONTER) {
                    etageCourant++;
                } else if (directionActuelle == Direction.DESCENDRE) {
                    etageCourant--;
                }

                // Sécurité : bornes de l'immeuble
                etageCourant = Math.max(0, Math.min(etageCourant, nombreEtages - 1));
            }

            System.out.println("[Ascenseur " + id + "] -> Etage " + etageCourant
                    + " (direction: " + directionActuelle + ")");
            GestionnaireFichier.logDeplacement(id, etageCourant);
            notifierVue();

            synchronized (this) {
                if (arretsDemandes[etageCourant]) {
                    arretsDemandes[etageCourant] = false;
                    ouvrirPortes();
                }

                // SCAN : on ne change de direction que s'il n'y a PLUS d'arrêts
                // dans le sens actuel. On se souvient de la direction précédente
                // pour ne pas faire demi-tour à tort.
                if (directionActuelle == Direction.MONTER) {
                    if (!aDesArretEnHaut()) {
                        directionPrecedente = Direction.MONTER;
                        directionActuelle = aDesArretEnBas() ? Direction.DESCENDRE : Direction.ARRET;
                    }
                } else if (directionActuelle == Direction.DESCENDRE) {
                    if (!aDesArretEnBas()) {
                        directionPrecedente = Direction.DESCENDRE;
                        directionActuelle = aDesArretEnHaut() ? Direction.MONTER : Direction.ARRET;
                    }
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Simule l'ouverture et la fermeture des portes à un étage d'arrêt.
     */
    private void ouvrirPortes() {
        System.out.println("[Ascenseur " + id + "] Portes ouvertes a l'etage " + etageCourant);
        GestionnaireFichier.logOuverturePortes(id, etageCourant);
        if (portesOuvertesCallback != null) portesOuvertesCallback.run();
        notifierVue();
        try {
            Thread.sleep(TEMPS_PORTES_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (portesFermeesCallback != null) portesFermeesCallback.run();
        System.out.println("[Ascenseur " + id + "] Portes fermees a l'etage " + etageCourant);
    }

    // -------------------------------------------------------------------------
    // COMMUNICATION AVEC LA VUE (JavaFX)
    // -------------------------------------------------------------------------

    /**
     * Enregistre le callback qui sera exécuté via Platform.runLater()
     * pour notifier la vue JavaFX de chaque changement d'état.
     */
    public void setUiCallback(Runnable callback) {
        this.uiCallback = callback;
    }

    public void setPortesOuvertesCallback(Runnable callback) {
        this.portesOuvertesCallback = callback;
    }

    public void setPortesFermeesCallback(Runnable callback) {
        this.portesFermeesCallback = callback;
    }

    private void notifierVue() {
        if (uiCallback != null) {
            uiCallback.run();
        }
    }

    // -------------------------------------------------------------------------
    // CONTRÔLE DU THREAD
    // -------------------------------------------------------------------------
    public synchronized void arreter() {
        this.simulationActive = false;
        notifyAll(); // Débloquer le wait() pour permettre une sortie propre
    }

    // -------------------------------------------------------------------------
    // GETTERS
    // -------------------------------------------------------------------------
    public int getId() { return id; }
    public synchronized int getEtageCourant() { return etageCourant; }
    public synchronized Direction getDirectionActuelle() { return directionActuelle; }
    public int getNombreEtages() { return nombreEtages; }
}
