package smartlift.model;

import smartlift.metier.GestionnaireAscenseurs;

/**
 * Mise à jour de Resident : le gestionnaire est injecté via le constructeur.
 * La méthode demanderAscenseur() crée une Requete et la soumet au gestionnaire.
 */
public class Resident {
    private final String nom;
    private final int etageActuel;
    private final GestionnaireAscenseurs gestionnaire;

    public Resident(String nom, int etageActuel, GestionnaireAscenseurs gestionnaire) {
        this.nom = nom;
        this.etageActuel = etageActuel;
        this.gestionnaire = gestionnaire;
    }

    /**
     * Simule le clic du résident sur le bouton d'appel de l'ascenseur.
     * Crée une Requete et l'envoie au gestionnaire central.
     *
     * @param destination L'étage où le résident souhaite se rendre.
     */
    public void demanderAscenseur(int destination) {
        System.out.println("[Résident " + nom + "] à l'étage " + etageActuel
                + " appuie sur le bouton → destination : étage " + destination);
        Requete requete = new Requete(this.etageActuel, destination);
        gestionnaire.recevoirRequete(requete);
    }

    // Getters
    public String getNom()      { return nom; }
    public int getEtageActuel() { return etageActuel; }
}
