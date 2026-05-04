package smartlift.view;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

/**
 * Vue principale JavaFX — SmartLift.
 *
 * Comportement des résidents :
 *  1. Apparaît au bord de son étage et marche vers la gaine de l'ascenseur assigné.
 *  2. S'arrête devant la gaine et ATTEND (animation de balancement) que l'ascenseur arrive.
 *  3. Quand les portes s'ouvrent à son étage → entre dans l'ascenseur.
 *  4. Quand les portes s'ouvrent à sa destination → sort et marche vers la droite.
 *
 * Comportement des portes :
 *  - Ouvertes : quand l'ascenseur s'arrête à un étage.
 *  - Fermées : pendant le déplacement entre étages.
 */
public class AscenseurView extends Application {

    // ── Constantes de layout ──────────────────────────────────────────────────
    private static final int NB_ETAGES   = AscenseurController.NOMBRE_ETAGES;
    private static final int FLOOR_H     = 110;
    private static final int SHAFT_W     = 100;
    private static final int CABIN_W     = 74;
    private static final int CABIN_H     = 80;
    private static final int CABIN_PAD_X = (SHAFT_W - CABIN_W) / 2;
    private static final int CABIN_PAD_Y = (FLOOR_H - CABIN_H) / 2;
    private static final int LABEL_W     = 120;
    private static final int SHAFT1_X    = LABEL_W + 10;
    private static final int SHAFT2_X    = SHAFT1_X + SHAFT_W + 20;
    private static final int BUILDING_W  = SHAFT2_X + SHAFT_W + 15;
    private static final int BUILDING_H  = NB_ETAGES * FLOOR_H;

    // ── Palette couleurs résidents ────────────────────────────────────────────
    private static final Color[] COLORS = {
        Color.web("#34d399"), Color.web("#60a5fa"), Color.web("#f472b6"),
        Color.web("#fbbf24"), Color.web("#a78bfa"), Color.web("#fb7185"),
        Color.web("#2dd4bf"), Color.web("#f87171")
    };
    private int colorIdx = 0;

    // ── Structure de données pour les résidents ───────────────────────────────
    /** Encapsule le visuel d'un résident et sa destination. */
    private static class ResidentVisual {
        final Group  figure;
        final int    destFloor;
        ScaleTransition idleAnim;
        ResidentVisual(Group figure, int destFloor) {
            this.figure    = figure;
            this.destFloor = destFloor;
        }
    }

    // ── Composants graphiques ─────────────────────────────────────────────────
    private Pane buildingPane;
    private final Map<Integer, Group>     elevatorGroups = new HashMap<>();
    private final Map<Integer, Rectangle> leftDoors      = new HashMap<>();
    private final Map<Integer, Rectangle> rightDoors     = new HashMap<>();
    private final Map<Integer, Label>     statuts        = new HashMap<>();

    /** ascId → (origFloor → file de résidents qui attendent) */
    private final Map<Integer, Map<Integer, Queue<ResidentVisual>>> residentsWaiting    = new HashMap<>();
    /** ascId → (destFloor → file de résidents dans l'ascenseur) */
    private final Map<Integer, Map<Integer, Queue<ResidentVisual>>> residentsInElevator = new HashMap<>();
    /** ascId → étage où les portes sont ouvertes (-1 = fermées) */
    private final Map<Integer, Integer> portesOuvertesEtage = new HashMap<>();

    private TextArea logArea;
    private AscenseurController controller;

    // =========================================================================
    // POINT D'ENTRÉE JAVAFX
    // =========================================================================
    @Override
    public void start(Stage stage) {
        controller = new AscenseurController(this);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;");
        root.setTop(buildHeader());
        root.setCenter(buildCenterPane());

        Scene scene = new Scene(root, 670, BUILDING_H + 70);
        java.net.URL css = getClass().getResource("/smartlift/view/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("SmartLift — Simulation d'ascenseurs");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> controller.arreter());
        stage.show();

        controller.demarrerSimulation();
        ajouterLog("Simulation démarrée automatiquement.");
    }

    // =========================================================================
    // CONSTRUCTION UI
    // =========================================================================
    private HBox buildHeader() {
        Label title = new Label("SmartLift — Simulation d'ascenseurs");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.setTextFill(Color.WHITE);
        HBox h = new HBox(title);
        h.setAlignment(Pos.CENTER);
        h.setPadding(new Insets(14, 20, 10, 20));
        h.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        return h;
    }

    private HBox buildCenterPane() {
        HBox center = new HBox(16);
        center.setPadding(new Insets(16));
        center.setAlignment(Pos.TOP_CENTER);
        buildingPane = createBuildingPane();
        center.getChildren().addAll(buildingPane, buildStatusPane());
        return center;
    }

    private Pane createBuildingPane() {
        Pane pane = new Pane();
        pane.setPrefSize(BUILDING_W, BUILDING_H);
        pane.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;"
                + " -fx-border-color: #334155; -fx-border-radius: 12; -fx-border-width: 1;");

        pane.getChildren().addAll(shaftRect(SHAFT1_X), shaftRect(SHAFT2_X));
        pane.getChildren().addAll(shaftLabel("Asc. 1", SHAFT1_X, "#818cf8"),
                                   shaftLabel("Asc. 2", SHAFT2_X, "#fb923c"));

        for (int f = 0; f < NB_ETAGES; f++) {
            int y = floorY(f);
            Line line = new Line(0, y, BUILDING_W, y);
            line.setStroke(Color.web("#334155"));
            Label lbl = new Label(f == 0 ? "RDC" : "Etage " + f);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            lbl.setTextFill(Color.web("#e2e8f0"));
            lbl.setLayoutX(8);
            lbl.setLayoutY(y + (FLOOR_H - 18) / 2.0);
            pane.getChildren().addAll(line, lbl);
        }
        Line bottom = new Line(0, BUILDING_H, BUILDING_W, BUILDING_H);
        bottom.setStroke(Color.web("#334155"));
        pane.getChildren().add(bottom);
        pane.getChildren().addAll(buildElevatorGroup(1, SHAFT1_X), buildElevatorGroup(2, SHAFT2_X));
        return pane;
    }

    private Rectangle shaftRect(int sx) {
        Rectangle r = new Rectangle(sx, 0, SHAFT_W, BUILDING_H);
        r.setFill(Color.web("#0d1526"));
        r.setStroke(Color.web("#334155"));
        return r;
    }

    private Label shaftLabel(String text, int sx, String accent) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(accent));
        l.setLayoutX(sx + (SHAFT_W - 45) / 2.0);
        l.setLayoutY(BUILDING_H - 22);
        return l;
    }

    private Group buildElevatorGroup(int ascId, int sx) {
        boolean isA1 = (ascId == 1);
        LinearGradient grad = isA1
                ? new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#818cf8")), new Stop(1, Color.web("#6366f1")))
                : new LinearGradient(0,0,0,1,true,CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#fb923c")), new Stop(1, Color.web("#f97316")));

        Rectangle cabinBg = new Rectangle(0, 0, CABIN_W, CABIN_H);
        cabinBg.setFill(grad);
        cabinBg.setArcWidth(10); cabinBg.setArcHeight(10);
        cabinBg.setEffect(new DropShadow(10, isA1 ? Color.web("#6366f1aa") : Color.web("#f97316aa")));

        Color doorColor = isA1 ? Color.web("#4338ca") : Color.web("#c2410c");
        Rectangle lDoor = new Rectangle(0,           0, CABIN_W / 2.0, CABIN_H);
        Rectangle rDoor = new Rectangle(CABIN_W/2.0, 0, CABIN_W / 2.0, CABIN_H);
        for (Rectangle d : new Rectangle[]{lDoor, rDoor}) {
            d.setFill(doorColor); d.setArcWidth(6); d.setArcHeight(6);
            d.setStroke(Color.web("#ffffff22")); d.setStrokeWidth(0.5);
        }

        Label num = new Label(String.valueOf(ascId));
        num.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        num.setTextFill(Color.WHITE);
        num.setLayoutX(CABIN_W / 2.0 - 6); num.setLayoutY(CABIN_H / 2.0 - 10);

        Group group = new Group(cabinBg, lDoor, rDoor, num);
        group.setLayoutX(sx + CABIN_PAD_X);
        group.setLayoutY((NB_ETAGES - 1) * FLOOR_H + CABIN_PAD_Y);

        elevatorGroups.put(ascId, group);
        leftDoors.put(ascId, lDoor);
        rightDoors.put(ascId, rDoor);
        return group;
    }

    private VBox buildStatusPane() {
        VBox panel = new VBox(12);
        panel.setPrefWidth(258);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 12;"
                + " -fx-border-color: #334155; -fx-border-radius: 12; -fx-border-width: 1;");
        panel.getChildren().add(styledLabel("Etat des ascenseurs", 14, FontWeight.BOLD, "#e2e8f0"));
        for (int i = 1; i <= AscenseurController.NOMBRE_ASCENSEURS; i++)
            panel.getChildren().add(buildElevatorCard(i));
        Region sep = new Region(); sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #334155;");
        panel.getChildren().addAll(sep, styledLabel("Journal", 13, FontWeight.BOLD, "#e2e8f0"));
        logArea = new TextArea();
        logArea.setEditable(false); logArea.setWrapText(true);
        logArea.setPrefHeight(BUILDING_H - 200);
        logArea.setStyle("-fx-control-inner-background: #0f172a; -fx-text-fill: #94a3b8;"
                + " -fx-font-family: Consolas; -fx-font-size: 11px;"
                + " -fx-border-color: #334155; -fx-border-radius: 6;");
        panel.getChildren().add(logArea);
        return panel;
    }

    private VBox buildElevatorCard(int ascId) {
        String accent = (ascId == 1) ? "#818cf8" : "#fb923c";
        VBox card = new VBox(4);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #0f172a; -fx-background-radius: 8;"
                + " -fx-border-color: " + accent + "; -fx-border-radius: 8; -fx-border-width: 1;");
        Label stat = styledLabel("En attente...", 12, FontWeight.NORMAL, "#64748b");
        statuts.put(ascId, stat);
        card.getChildren().addAll(styledLabel("Ascenseur " + ascId, 13, FontWeight.BOLD, accent), stat);
        return card;
    }

    // =========================================================================
    // FIGURE HUMAINE
    // =========================================================================
    /**
     * Crée une silhouette humaine stylisée (tête + corps + bras + jambes).
     * Origine du Group : centre de la taille (milieu du corps).
     */
    private Group createHumanFigure(Color color) {
        // Tête
        Circle head = new Circle(0, -16, 6);
        head.setFill(color); head.setStroke(Color.WHITE); head.setStrokeWidth(0.8);

        // Corps
        Line body = new Line(0, -10, 0, 4);
        body.setStroke(color); body.setStrokeWidth(2.5);

        // Bras gauche / droit
        Line armL = new Line(0, -7, -7,  1);
        Line armR = new Line(0, -7,  7,  1);
        armL.setStroke(color); armL.setStrokeWidth(2);
        armR.setStroke(color); armR.setStrokeWidth(2);

        // Jambe gauche / droite
        Line legL = new Line(0,  4, -6, 16);
        Line legR = new Line(0,  4,  6, 16);
        legL.setStroke(color); legL.setStrokeWidth(2);
        legR.setStroke(color); legR.setStrokeWidth(2);

        return new Group(head, body, armL, armR, legL, legR);
    }

    // =========================================================================
    // MÉTHODES D'ANIMATION APPELÉES VIA Platform.runLater
    // =========================================================================

    /** Déplace la cabine de l'ascenseur vers l'étage cible (animation fluide). */
    public void actualiserPositionAscenseur(int ascId, int etage) {
        Group g = elevatorGroups.get(ascId);
        if (g == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(1200), g);
        tt.setToY(-etage * FLOOR_H);
        tt.play();
    }

    /** Met à jour le label de statut d'un ascenseur. */
    public void actualiserStatut(int ascId, int etage, String direction) {
        Label lbl = statuts.get(ascId);
        if (lbl == null) return;
        String arrow;
        if ("MONTER".equals(direction))         arrow = "⬆ ";
        else if ("DESCENDRE".equals(direction)) arrow = "⬇ ";
        else                                    arrow = "⏸ ";
        lbl.setText(arrow + (etage == 0 ? "RDC" : "Etage " + etage) + "  [" + direction + "]");
    }

    /** Ajoute un message dans le journal. */
    public void ajouterLog(String msg) {
        if (logArea != null) logArea.appendText(msg + "\n");
    }

    // ── RÉSIDENT : apparition et attente ────────────────────────────────────

    /**
     * Un résident apparaît à son étage, marche vers la gaine assignée,
     * puis attend (balancement) que l'ascenseur arrive.
     */
    public void animerNouveauResident(int etageOrig, int etageDest, int ascId) {
        Color color = COLORS[colorIdx++ % COLORS.length];
        Group figure = createHumanFigure(color);

        double startX = 10;
        double startY = floorY(etageOrig) + FLOOR_H / 2.0;
        figure.setLayoutX(startX);
        figure.setLayoutY(startY);
        buildingPane.getChildren().add(figure);

        int sx = (ascId == 1) ? SHAFT1_X : SHAFT2_X;
        // S'arrête juste devant la gaine
        double walkTargetX = sx - 22 - startX;

        ResidentVisual rv = new ResidentVisual(figure, etageDest);

        TranslateTransition walk = new TranslateTransition(Duration.millis(900), figure);
        walk.setToX(walkTargetX);
        walk.setOnFinished(e -> {
            // Si les portes de l'ascenseur assigné sont déjà ouvertes à cet étage
            // (race condition : ascenseur arrivé avant que le résident finisse de marcher)
            // → le résident monte immédiatement
            int etageOuvert = portesOuvertesEtage.getOrDefault(ascId, -1);
            if (etageOuvert == etageOrig) {
                embarquerResident(rv, ascId);
            } else {
                // Attendre l'ascenseur : animation de balancement
                ScaleTransition idle = new ScaleTransition(Duration.millis(600), figure);
                idle.setFromY(1.0); idle.setToY(1.08);
                idle.setCycleCount(Animation.INDEFINITE);
                idle.setAutoReverse(true);
                idle.play();
                rv.idleAnim = idle;

                residentsWaiting
                    .computeIfAbsent(ascId, k -> new HashMap<>())
                    .computeIfAbsent(etageOrig, k -> new LinkedList<>())
                    .add(rv);
            }
        });
        walk.play();
        ajouterLog("Resident etage " + etageOrig + " -> etage " + etageDest + " (Asc." + ascId + ")");
    }

    /**
     * Anime l'entrée d'un résident dans l'ascenseur (marché jusqu'à la cabine + fondu).
     * Partagé par animerEntreeResidents() et le cas "portes déjà ouvertes".
     */
    private void embarquerResident(ResidentVisual rv, int ascId) {
        if (rv.idleAnim != null) { rv.idleAnim.stop(); rv.idleAnim = null; }
        int sx = (ascId == 1) ? SHAFT1_X : SHAFT2_X;
        double cabinCenterX = sx + CABIN_PAD_X + CABIN_W / 2.0;
        double enterTargetX = cabinCenterX - rv.figure.getLayoutX();

        TranslateTransition enter = new TranslateTransition(Duration.millis(400), rv.figure);
        enter.setToX(enterTargetX);
        FadeTransition fade = new FadeTransition(Duration.millis(250), rv.figure);
        fade.setToValue(0);
        SequentialTransition seq = new SequentialTransition(enter, fade);
        seq.setOnFinished(e -> {
            buildingPane.getChildren().remove(rv.figure);
            rv.figure.setOpacity(1.0);
            residentsInElevator
                .computeIfAbsent(ascId, k -> new HashMap<>())
                .computeIfAbsent(rv.destFloor, k -> new LinkedList<>())
                .add(rv);
        });
        seq.play();
    }

    // ── RÉSIDENT : entrée dans l'ascenseur (quand les portes s'ouvrent à l'étage d'origine) ──

    public void animerEntreeResidents(int ascId, int etage) {
        Map<Integer, Queue<ResidentVisual>> waitingMap = residentsWaiting.get(ascId);
        if (waitingMap == null) return;
        Queue<ResidentVisual> queue = waitingMap.remove(etage);
        if (queue == null || queue.isEmpty()) return;

        for (ResidentVisual rv : queue) {
            embarquerResident(rv, ascId);
        }
    }

    // ── RÉSIDENT : sortie de l'ascenseur (quand les portes s'ouvrent à destination) ──

    public void animerSortieResidents(int ascId, int etage) {
        Map<Integer, Queue<ResidentVisual>> elevMap = residentsInElevator.get(ascId);
        if (elevMap == null) return;
        Queue<ResidentVisual> exitList = elevMap.remove(etage);
        if (exitList == null || exitList.isEmpty()) return;

        int sx = (ascId == 1) ? SHAFT1_X : SHAFT2_X;
        double exitStartX = sx + CABIN_PAD_X + CABIN_W / 2.0;
        double exitStartY = floorY(etage) + FLOOR_H / 2.0;

        for (ResidentVisual rv : exitList) {
            Group fig = rv.figure;
            fig.setTranslateX(0); fig.setTranslateY(0);
            fig.setLayoutX(exitStartX); fig.setLayoutY(exitStartY);
            fig.setOpacity(1);
            buildingPane.getChildren().add(fig);

            TranslateTransition exit = new TranslateTransition(Duration.millis(900), fig);
            exit.setToX(BUILDING_W - exitStartX + 30);
            FadeTransition fade = new FadeTransition(Duration.millis(250), fig);
            fade.setToValue(0);
            SequentialTransition seq = new SequentialTransition(exit, fade);
            seq.setOnFinished(e -> buildingPane.getChildren().remove(fig));
            seq.play();
        }
    }

    // ── PORTES ───────────────────────────────────────────────────────────────

    public void animerOuverturePortes(int ascId, int etage) {
        portesOuvertesEtage.put(ascId, etage); // portes ouvertes à cet étage
        Rectangle lD = leftDoors.get(ascId), rD = rightDoors.get(ascId);
        if (lD == null) return;
        TranslateTransition l = new TranslateTransition(Duration.millis(450), lD);
        l.setToX(-CABIN_W / 2.0);
        TranslateTransition r = new TranslateTransition(Duration.millis(450), rD);
        r.setToX(CABIN_W / 2.0);
        new ParallelTransition(l, r).play();
    }

    public void animerFermeturePortes(int ascId) {
        portesOuvertesEtage.put(ascId, -1); // portes fermées
        Rectangle lD = leftDoors.get(ascId), rD = rightDoors.get(ascId);
        if (lD == null) return;
        TranslateTransition l = new TranslateTransition(Duration.millis(450), lD);
        l.setToX(0);
        TranslateTransition r = new TranslateTransition(Duration.millis(450), rD);
        r.setToX(0);
        new ParallelTransition(l, r).play();
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================
    private int floorY(int floor) {
        return (NB_ETAGES - 1 - floor) * FLOOR_H;
    }

    private Label styledLabel(String text, int size, FontWeight w, String color) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", w, size));
        l.setTextFill(Color.web(color));
        return l;
    }

    public static void main(String[] args) { launch(args); }
}
