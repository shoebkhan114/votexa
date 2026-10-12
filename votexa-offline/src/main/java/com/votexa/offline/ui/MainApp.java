package com.votexa.offline.ui;

import com.votexa.offline.crypto.PackageVerifier;
import com.votexa.offline.crypto.PackageVerifier.OpenedPackage;
import com.votexa.offline.database.LocalDb;
import com.votexa.offline.security.PackageRegistry;
import com.votexa.offline.sync.SyncExporter;
import com.votexa.offline.validator.OfflineValidator;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;

/** VOTEXA offline polling validator. Works with no Internet: everything comes from the signed, encrypted package. */
public class MainApp extends Application {
    private static final Path HOME = Path.of(System.getProperty("user.home"), ".votexa-offline");

    private final TextField pkgField = new TextField();
    private final TextField keyField = new TextField();
    private final PasswordField passField = new PasswordField();
    private final ChoiceBox<String> resumeBox = new ChoiceBox<>();
    private final Label sessionLabel = new Label("No session loaded");
    private final TextField voterField = new TextField();
    private final CheckBox identityBox = new CheckBox("Identity confirmed by polling officer");
    private final Label resultLabel = new Label();
    private final TextArea checksArea = new TextArea();
    private final Label eligibleLabel = new Label("-");
    private final Label authorizedLabel = new Label("-");
    private final Label remainingLabel = new Label("-");
    private final TextArea logArea = new TextArea();
    private final Button validateBtn = new Button("Validate");
    private final Button authorizeBtn = new Button("Authorize voting");
    private final Button exportBtn = new Button("Export sync file");

    private LocalDb db;
    private OfflineValidator validator;

    @Override
    public void start(Stage stage) {
        stage.setTitle("VOTEXA Offline Polling Validator (academic prototype)");

        Label banner = new Label("OFFLINE MODE: no network required. Dummy data only. Offline operation reduces network attack surface; it does not remove physical or insider threats.");
        banner.setWrapText(true);
        banner.setStyle("-fx-background-color:#fef3c7;-fx-padding:8;-fx-text-fill:#92400e;");

        // --- package section ---
        Button browsePkg = new Button("Browse…");
        browsePkg.setOnAction(e -> choose(pkgField, "Election package (*.vxpkg)", "*.vxpkg"));
        Button browseKey = new Button("Browse…");
        browseKey.setOnAction(e -> choose(keyField, "Authority public key (*.b64)", "*.b64", "*.txt"));
        Button loadBtn = new Button("Verify signature, decrypt & load");
        loadBtn.setOnAction(e -> loadPackage());
        Button resumeBtn = new Button("Resume session");
        resumeBtn.setOnAction(e -> resume());
        resumeBox.getItems().setAll(LocalDb.existingStations(HOME));

        GridPane pkg = new GridPane();
        pkg.setHgap(8); pkg.setVgap(6);
        pkg.addRow(0, new Label("Election package"), pkgField, browsePkg);
        pkg.addRow(1, new Label("Authority public key"), keyField, browseKey);
        pkg.addRow(2, new Label("Passphrase"), passField, loadBtn);
        pkg.addRow(3, new Label("Existing sessions"), resumeBox, resumeBtn);
        GridPane.setHgrow(pkgField, Priority.ALWAYS);
        GridPane.setHgrow(keyField, Priority.ALWAYS);
        GridPane.setHgrow(passField, Priority.ALWAYS);
        TitledPane pkgPane = new TitledPane("1. Election package (signed + encrypted)", new VBox(8, pkg, sessionLabel));
        pkgPane.setCollapsible(false);

        // --- validation section ---
        voterField.setPromptText("Voter ID, e.g. VTX0000001");
        validateBtn.setOnAction(e -> run(false));
        authorizeBtn.setOnAction(e -> run(true));
        identityBox.setSelected(true);
        resultLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;");
        resultLabel.setWrapText(true);
        checksArea.setEditable(false);
        checksArea.setPrefRowCount(8);
        HBox row = new HBox(8, voterField, identityBox, validateBtn, authorizeBtn);
        HBox.setHgrow(voterField, Priority.ALWAYS);
        TitledPane valPane = new TitledPane("2. Voter validation & one-time authorization", new VBox(8, row, resultLabel, checksArea));
        valPane.setCollapsible(false);

        // --- stats + sync ---
        exportBtn.setOnAction(e -> export());
        HBox stats = new HBox(24, stat("Eligible", eligibleLabel), stat("Authorized", authorizedLabel), stat("Remaining", remainingLabel), exportBtn);
        TitledPane statPane = new TitledPane("3. Polling status & secure synchronization", new VBox(8, stats,
                new Label("Export the sync file and upload it in the web portal (Polling Dashboard → Offline validator sync).")));
        statPane.setCollapsible(false);

        logArea.setEditable(false);
        logArea.setPrefRowCount(6);

        VBox root = new VBox(10, banner, pkgPane, valPane, statPane, new Label("Activity log"), logArea);
        root.setPadding(new Insets(12));
        setSessionActive(false);
        stage.setScene(new Scene(new ScrollPane(root) {{ setFitToWidth(true); }}, 900, 820));
        stage.show();
    }

    private VBox stat(String name, Label value) {
        value.setStyle("-fx-font-size:24px;-fx-font-weight:bold;");
        return new VBox(2, new Label(name), value);
    }

    private void choose(TextField target, String desc, String... patterns) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, patterns));
        File f = fc.showOpenDialog(null);
        if (f != null) target.setText(f.getAbsolutePath());
    }

    private void log(String s) { logArea.appendText("[" + LocalTime.now().withNano(0) + "] " + s + "\n"); }

    private void setSessionActive(boolean on) {
        validateBtn.setDisable(!on); authorizeBtn.setDisable(!on); exportBtn.setDisable(!on); voterField.setDisable(!on);
    }

    private void closeDb() { try { if (db != null) db.close(); } catch (Exception ignored) { } db = null; }

    private void loadPackage() {
        try {
            if (pkgField.getText().isBlank() || keyField.getText().isBlank() || passField.getText().isEmpty())
                throw new IllegalArgumentException("Choose the package, the authority public key and enter the passphrase");
            String pub = Files.readString(Path.of(keyField.getText())).trim();
            OpenedPackage p = PackageVerifier.open(Path.of(pkgField.getText()), pub, passField.getText());
            PackageRegistry reg = new PackageRegistry(HOME);
            if (reg.seen(p.packageId()))
                throw new SecurityException("This package was already loaded on this device (replay protection). Use 'Resume session'.");
            closeDb();
            db = LocalDb.create(HOME, p, passField.getText());
            reg.add(p.packageId());
            log("Signature VALID, package decrypted and loaded: " + p.electionCode() + " / " + p.stationCode());
            afterOpen();
        } catch (Exception ex) {
            log("LOAD FAILED: " + ex.getMessage());
            alert(ex.getMessage());
        }
    }

    private void resume() {
        try {
            String st = resumeBox.getValue();
            if (st == null) throw new IllegalArgumentException("Pick an existing session");
            closeDb();
            db = LocalDb.open(HOME, st, passField.getText());
            log("Resumed local session " + st);
            afterOpen();
        } catch (Exception ex) {
            log("RESUME FAILED: " + ex.getMessage());
            alert(ex.getMessage());
        }
    }

    private void afterOpen() throws Exception {
        validator = new OfflineValidator(db);
        sessionLabel.setText("Station " + db.config("station_code") + " (" + db.config("station_name") + ") · election "
                + db.config("election_code") + " · device " + db.device("device_id") + " · expires " + db.config("expires_at"));
        resumeBox.getItems().setAll(LocalDb.existingStations(HOME));
        setSessionActive(true);
        refreshStats();
    }

    private void refreshStats() {
        try {
            int total = db.count(null), voted = db.count("voting_status = 'VOTED'");
            eligibleLabel.setText(String.valueOf(total));
            authorizedLabel.setText(String.valueOf(voted));
            remainingLabel.setText(String.valueOf(total - voted));
        } catch (Exception e) { log("Stats error: " + e.getMessage()); }
    }

    private void run(boolean authorize) {
        try {
            if (voterField.getText().isBlank()) return;
            OfflineValidator.Result r = validator.run(voterField.getText(), identityBox.isSelected(), authorize);
            boolean good = "ELIGIBLE".equals(r.result()) || "AUTHORIZED".equals(r.result());
            resultLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:" + (good ? "#166534" : "#991b1b") + ";");
            resultLabel.setText((good ? "✓ " : "✗ ") + r.result().replace('_', ' ') + ": " + r.message() + (r.voterName() != null ? "  (" + r.voterName() + ")" : ""));
            StringBuilder sb = new StringBuilder();
            r.checks().forEach(c -> sb.append(c.passed() ? "✓ " : "✗ ").append(c.name()).append('\n'));
            checksArea.setText(sb.toString());
            log(voterField.getText().trim() + " → " + r.result());
            refreshStats();
        } catch (Exception ex) {
            log("ERROR: " + ex.getMessage());
            alert(ex.getMessage());
        }
    }

    private void export() {
        try {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName("sync-" + db.config("station_code") + "-" + System.currentTimeMillis() + ".json");
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            int n = SyncExporter.export(db, f.toPath());
            log("Exported " + n + " authorization(s) to " + f.getName() + ". Keep the file until the central system confirms it.");
        } catch (Exception ex) {
            log("EXPORT FAILED: " + ex.getMessage());
            alert(ex.getMessage());
        }
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg == null ? "Unexpected error" : msg, ButtonType.OK);
        a.setHeaderText("VOTEXA offline validator");
        a.showAndWait();
    }

    @Override
    public void stop() { closeDb(); }

    public static void main(String[] args) { launch(args); }
}
