package smartlift.model;

public class Requete {
    private int etageOrigine;
    private int etageDestination;
    private Direction directionDemande;

    public Requete(int etageOrigine, int etageDestination) {
        this.etageOrigine = etageOrigine;
        this.etageDestination = etageDestination;
        // Détermine automatiquement la direction en fonction des étages
        if (etageDestination > etageOrigine) {
            this.directionDemande = Direction.MONTER;
        } else if (etageDestination < etageOrigine) {
            this.directionDemande = Direction.DESCENDRE;
        } else {
            this.directionDemande = Direction.ARRET;
        }
    }

    public int getEtageOrigine() {
        return etageOrigine;
    }

    public int getEtageDestination() {
        return etageDestination;
    }

    public Direction getDirection() {
        return directionDemande;
    }

    @Override
    public String toString() {
        return "Requete{" +
                "origine=" + etageOrigine +
                ", destination=" + etageDestination +
                ", direction=" + directionDemande +
                '}';
    }
}
