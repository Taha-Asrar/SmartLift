package smartlift.metier;

import smartlift.model.Ascenseur;
import smartlift.model.Direction;
import smartlift.model.Immeuble;
import smartlift.model.Requete;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Cerveau de la simulation. Reçoit toutes les requêtes des résidents
 * et choisit le meilleur ascenseur pour y répondre.
 *
 * Algorithme de dispatching :
 * Pour chaque requête, on calcule un score pour chaque ascenseur.
 * Le score le plus BAS gagne (distance minimale).
 *
 * Priorité de sélection :
 *   1. Ascenseur se déplaçant dans la même direction que la requête
 *      et qui va passer par l'étage d'origine.
 *   2. Ascenseur à l'arrêt (IDLE).
 *   3. N'importe quel ascenseur (le plus proche).
 */
public class GestionnaireAscenseurs {

    private final Queue<Requete> requetesEnAttente;
    private final Immeuble immeuble;

    private static final int PENALITE_MAUVAISE_DIRECTION = 20;

    /** Callback appelé quand une requête est assignée à un ascenseur.
     *  data[] = {etageOrigine, etageDestination, ascenseurId} */
    private Consumer<int[]> assignmentCallback;

    public GestionnaireAscenseurs(Immeuble immeuble) {
        this.immeuble = immeuble;
        this.requetesEnAttente = new LinkedList<>();
    }

    // -------------------------------------------------------------------------
    // RÉCEPTION ET TRAITEMENT DES REQUÊTES
    // -------------------------------------------------------------------------

    /**
     * Point d'entrée des requêtes. Appelé par chaque Resident via demanderAscenseur().
     * Synchronized pour protéger la file d'attente contre les accès concurrents
     * (plusieurs résidents peuvent appuyer sur un bouton en même temps).
     */
    public synchronized void recevoirRequete(Requete r) {
        System.out.println("\n[Gestionnaire] ★ Nouvelle requête : Etage "
                + r.getEtageOrigine() + " → Etage " + r.getEtageDestination()
                + " (" + r.getDirection() + ")");

        assignerRequeteOptimale(r);
        GestionnaireFichier.logRequete(r, 0);
    }

    /**
     * Algorithme de dispatching : sélectionne l'ascenseur optimal et lui
     * assigne les deux arrêts (étage d'appel, puis étage de destination).
     */
    public synchronized void assignerRequeteOptimale(Requete r) {
        List<Ascenseur> ascenseurs = immeuble.getAscenseurs();

        Ascenseur meilleurAscenseur = null;
        int meilleurScore = Integer.MAX_VALUE;

        for (Ascenseur a : ascenseurs) {
            int score = calculerScore(a, r);
            System.out.println("  [Gestionnaire] Ascenseur " + a.getId()
                    + " | Etage courant: " + a.getEtageCourant()
                    + " | Direction: " + a.getDirectionActuelle()
                    + " | Score: " + score);

            if (score < meilleurScore) {
                meilleurScore = score;
                meilleurAscenseur = a;
            }
        }

        if (meilleurAscenseur != null) {
            System.out.println("  [Gestionnaire] Ascenseur " + meilleurAscenseur.getId()
                    + " selectionne (score: " + meilleurScore + ")");
            // Ajoute UNIQUEMENT l'étage d'appel (origine).
            // La destination sera ajoutée quand le résident monte (portes ouvertes à l'origine).
            meilleurAscenseur.ajouterArret(r.getEtageOrigine());
            if (assignmentCallback != null) {
                assignmentCallback.accept(new int[]{
                    r.getEtageOrigine(), r.getEtageDestination(), meilleurAscenseur.getId()
                });
            }
        }
    }

    // -------------------------------------------------------------------------
    // CALCUL DU SCORE (Distance + Contexte directionnel)
    // -------------------------------------------------------------------------

    /**
     * Calcule le score (coût) d'un ascenseur pour répondre à un appel.
     * Score BAS = meilleur candidat.
     *
     * Règles (un seul bouton par étage, pas de UP/DOWN séparé) :
     *
     *  1. Ascenseur à l'ARRET → score = distance pure (meilleur cas)
     *  2. Ascenseur en mouvement VERS le résident (va passer par son étage) → score = distance
     *  3. Ascenseur en mouvement qui s'ÉLOIGNE du résident → score = distance + pénalité
     *
     * Ex: résident en étage 3
     *   - A1 en étage 4 (DESCENDRE vers 3) → headingToward=true  → score = |4-3| = 1
     *   - A2 en étage 1 (MONTER  vers 3) → headingToward=true  → score = |1-3| = 2
     *   - A1 sélectionné (score 1 < 2) ✓
     *
     * Ex: résident en étage 3, égalité de distance :
     *   - A1 en étage 4 (DESCENDRE) → headingToward=true  → score = 1
     *   - A2 en étage 2 (MONTER)    → headingToward=true  → score = 1
     *   - A1 en étage 4 (MONTER, s'éloigne) → score = 1 + 20 = 21
     */
    private int calculerScore(Ascenseur a, Requete r) {
        int distance = Math.abs(a.getEtageCourant() - r.getEtageOrigine());

        // Cas 1 : Ascenseur à l'arrêt — pure distance
        if (a.getDirectionActuelle() == Direction.ARRET) {
            return distance;
        }

        // Cas 2 : Ascenseur se dirige VERS l'étage du résident (va y passer)
        boolean headingToward =
                (a.getDirectionActuelle() == Direction.MONTER
                        && a.getEtageCourant() <= r.getEtageOrigine()) ||
                (a.getDirectionActuelle() == Direction.DESCENDRE
                        && a.getEtageCourant() >= r.getEtageOrigine());

        if (headingToward) {
            return distance; // Bon candidat : passera par cet étage
        }

        // Cas 3 : Ascenseur s'éloigne du résident → pénalité
        return distance + PENALITE_MAUVAISE_DIRECTION;
    }
    public void setAssignmentCallback(Consumer<int[]> callback) {
        this.assignmentCallback = callback;
    }
}
