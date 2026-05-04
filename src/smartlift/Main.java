package smartlift;

import smartlift.view.AscenseurView;

/**
 * Point d'entrée global de l'application SmartLift.
 * Délègue le lancement à la classe Application JavaFX AscenseurView.
 */
public class Main {
    public static void main(String[] args) {
        AscenseurView.launch(AscenseurView.class, args);
    }
}
