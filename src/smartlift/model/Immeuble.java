package smartlift.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Représente le bâtiment physique.
 * Contient les ascenseurs et connaît le nombre d'étages.
 * Les ascenseurs sont initialisés séparément après la création du gestionnaire
 * pour éviter toute référence circulaire entre constructeurs.
 */
public class Immeuble {

    private final int nombreEtages;
    private final List<Ascenseur> ascenseurs;

    public Immeuble(int nombreEtages) {
        this.nombreEtages = nombreEtages;
        this.ascenseurs = new ArrayList<>();
    }

    /**
     * Crée et ajoute les ascenseurs à l'immeuble.
     * Appelé APRÈS la création du GestionnaireAscenseurs pour éviter une
     * dépendance circulaire dans les constructeurs.
     *
     * @param nbAscenseurs Le nombre d'ascenseurs à créer (ex: 2).
     */
    public void creerAscenseurs(int nbAscenseurs) {
        for (int i = 1; i <= nbAscenseurs; i++) {
            ascenseurs.add(new Ascenseur(i, nombreEtages));
        }
    }

    public List<Ascenseur> getAscenseurs() {
        return Collections.unmodifiableList(ascenseurs);
    }

    public int getNombreEtages() {
        return nombreEtages;
    }
}
