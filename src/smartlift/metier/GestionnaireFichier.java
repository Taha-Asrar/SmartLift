package smartlift.metier;

import smartlift.model.Requete;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe utilitaire statique pour la journalisation des événements
 * de la simulation dans un fichier CSV.
 * Toutes les méthodes sont synchronisées pour garantir l'intégrité
 * du fichier lors d'accès concurrents depuis plusieurs threads.
 */
public class GestionnaireFichier {

    private static final String FICHIER_LOGS = "historique_ascenseurs.csv";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Initialise le fichier CSV avec un en-tête au premier appel
    static {
        initialiserFichier();
    }

    private static void initialiserFichier() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FICHIER_LOGS, false))) {
            pw.println("timestamp,type_evenement,ascenseur_id,etage,details");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'initialisation du fichier de log : " + e.getMessage());
        }
    }

    /**
     * Journalise un déplacement d'un ascenseur vers un étage.
     *
     * @param idAscenseur L'identifiant de l'ascenseur qui se déplace.
     * @param etage       L'étage atteint par l'ascenseur.
     */
    public static synchronized void logDeplacement(int idAscenseur, int etage) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String ligne = String.format("%s,DEPLACEMENT,%d,%d,\"Ascenseur %d arrive à l'étage %d\"",
                timestamp, idAscenseur, etage, idAscenseur, etage);
        ecrireLigne(ligne);
    }

    /**
     * Journalise une requête traitée avec son temps d'attente.
     *
     * @param r            La requête traitée.
     * @param tempsAttente Le temps d'attente du résident en secondes.
     */
    public static synchronized void logRequete(Requete r, double tempsAttente) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String ligne = String.format("%s,REQUETE,-,%d,\"Etage %d -> Etage %d | Attente: %.2fs\"",
                timestamp, r.getEtageOrigine(),
                r.getEtageOrigine(), r.getEtageDestination(), tempsAttente);
        ecrireLigne(ligne);
    }

    /**
     * Journalise l'ouverture des portes d'un ascenseur.
     *
     * @param idAscenseur L'identifiant de l'ascenseur.
     * @param etage       L'étage où les portes s'ouvrent.
     */
    public static synchronized void logOuverturePortes(int idAscenseur, int etage) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String ligne = String.format("%s,PORTES,%d,%d,\"Portes ouvertes à l'étage %d\"",
                timestamp, idAscenseur, etage, etage);
        ecrireLigne(ligne);
    }

    private static void ecrireLigne(String ligne) {
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(FICHIER_LOGS, true)))) {
            pw.println(ligne);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture dans le fichier de log : " + e.getMessage());
        }
    }
}
