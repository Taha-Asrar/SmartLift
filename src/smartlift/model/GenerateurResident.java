package smartlift.model;

import smartlift.metier.GestionnaireAscenseurs;
import java.util.Random;

/**
 * Thread d'arrière-plan qui génère aléatoirement des résidents.
 * Chaque résident apparaît à un étage aléatoire et demande à aller
 * vers un autre étage aléatoire (différent de son étage de départ).
 *
 * Simule l'arrivée naturelle de personnes dans l'immeuble.
 */
public class GenerateurResident implements Runnable {

    /** Intervalle minimum entre deux apparitions de résidents (ms). */
    private static final int DELAI_MIN_MS = 4000;
    /** Intervalle maximum entre deux apparitions de résidents (ms). */
    private static final int DELAI_MAX_MS = 9000;

    private final GestionnaireAscenseurs gestionnaire;
    private final int nombreEtages;
    private final Random random;
    private volatile boolean simulationActive;
    private int compteurResidents;

    public GenerateurResident(GestionnaireAscenseurs gestionnaire, int nombreEtages) {
        this.gestionnaire        = gestionnaire;
        this.nombreEtages        = nombreEtages;
        this.random              = new Random();
        this.simulationActive    = true;
        this.compteurResidents   = 1;
    }

    public void arreterSimulation() {
        this.simulationActive = false;
    }

    @Override
    public void run() {
        System.out.println("[Générateur] Démarré — résidents automatiques activés.");

        while (simulationActive) {
            try {
                // Délai aléatoire avant l'apparition du prochain résident
                int delai = DELAI_MIN_MS + random.nextInt(DELAI_MAX_MS - DELAI_MIN_MS);
                Thread.sleep(delai);

                if (!simulationActive) break;

                // Génère un étage de départ et de destination différents
                int etageDepart      = random.nextInt(nombreEtages);
                int etageDestination;
                do {
                    etageDestination = random.nextInt(nombreEtages);
                } while (etageDestination == etageDepart);

                String nom = "R" + compteurResidents++;
                Resident nouveauResident = new Resident(nom, etageDepart, gestionnaire);
                nouveauResident.demanderAscenseur(etageDestination);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Générateur] Interrompu.");
                break;
            }
        }

        System.out.println("[Générateur] Arrêté.");
    }
}
