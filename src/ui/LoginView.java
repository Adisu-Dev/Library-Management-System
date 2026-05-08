package ui;

import client.LibraryClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.IOException;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  LoginView — Connect-to-Server Screen                                ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * DISTRIBUTED CONCEPT — Client-Server Model (Chapter 1):
 * This screen is the entry point for the distributed client. Instead of
 * connecting directly to a database, the user specifies the SERVER's
 * IP address and port. The client then establishes a TCP connection to
 * the LibraryServer, which manages all data access.
 *
 * This demonstrates the fundamental principle: clients don't know about
 * the database — they only know about the server's network address.
 */
public class LoginView {

    private StackPane view;

    /** Callback invoked when the user successfully connects to the server. */
    private Runnable onConnected;

    /** The connected client — passed to DashboardView after successful connect. */
    private LibraryClient client;

    /** The user's display name (entered on this screen). */
    private String userName;

    public LoginView() {
        buildUI();
    }

    // ─────────────────────────────────────────────────────────────────
    // UI Construction
    // ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        view = new StackPane();
        view.setStyle("-fx-background-color: #020617;");

        // ── Card ─────────────────────────────────────────────────────
        VBox card = new VBox(20);
        card.setMaxWidth(480);
        card.setMinWidth(380);
        card.setPadding(new Insets(50, 48, 48, 48));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: rgba(8,20,42,0.97);" +
            "-fx-background-radius: 22;" +
            "-fx-border-radius: 22;" +
            "-fx-border-color: rgba(56,189,248,0.50);" +
            "-fx-border-width: 1.8;"
        );
        card.setEffect(new DropShadow(60, Color.rgb(0, 0, 0, 0.85)));

        // ── Title ─────────────────────────────────────────────────────
        Label lblTitle = new Label("Distributed Library");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSub = new Label("Bahir Dar University — Distributed Systems");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web("#94a3b8"));

        Label lblGroup = new Label("Adisu  •  Dawit  •  Dagnachew");
        lblGroup.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblGroup.setTextFill(Color.web("#38bdf8"));

        VBox titleBox = new VBox(6, lblTitle, lblSub, lblGroup);
        titleBox.setAlignment(Pos.CENTER);

        // ── Separator ─────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(56,189,248,0.3);");

        // ── Input fields ──────────────────────────────────────────────
        String inputStyle =
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: #64748b;" +
            "-fx-border-color: rgba(255,255,255,0.14);" +
            "-fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-font-size: 15px; -fx-padding: 0 16;";

        TextField txtHost = new TextField("localhost");
        txtHost.setPromptText("Server IP Address");
        txtHost.setPrefHeight(52);
        txtHost.setStyle(inputStyle);

        TextField txtPort = new TextField("9090");
        txtPort.setPromptText("Port");
        txtPort.setPrefHeight(52);
        txtPort.setStyle(inputStyle);

        TextField txtName = new TextField();
        txtName.setPromptText("Your Name (display name)");
        txtName.setPrefHeight(52);
        txtName.setStyle(inputStyle);

        // ── Labels ────────────────────────────────────────────────────
        Label lblHost = fieldLabel("Server IP Address");
        Label lblPort = fieldLabel("Port");
        Label lblName = fieldLabel("Your Name");

        // ── Status label ──────────────────────────────────────────────
        Label lblStatus = new Label("");
        lblStatus.setFont(Font.font("Segoe UI", 13));
        lblStatus.setTextFill(Color.web("#94a3b8"));
        lblStatus.setWrapText(true);

        // ── Connect button ────────────────────────────────────────────
        Button btnConnect = new Button("CONNECT TO SERVER");
        btnConnect.setMaxWidth(Double.MAX_VALUE);
        btnConnect.setPrefHeight(52);
        btnConnect.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btnConnect.setStyle(
            "-fx-background-color: #3b82f6;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        btnConnect.setOnMouseEntered(e -> btnConnect.setStyle(
            "-fx-background-color: #2563eb;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));
        btnConnect.setOnMouseExited(e -> btnConnect.setStyle(
            "-fx-background-color: #3b82f6;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        ));

        // ── Concepts info box ─────────────────────────────────────────
        VBox infoBox = new VBox(4);
        infoBox.setStyle(
            "-fx-background-color: rgba(56,189,248,0.08);" +
            "-fx-border-color: rgba(56,189,248,0.25);" +
            "-fx-border-radius: 8; -fx-background-radius: 8;" +
            "-fx-padding: 12 14;"
        );
        Label lblInfo = new Label("📡  TCP Socket Communication  •  Multi-Threading\n"
                + "🔒  Mutual Exclusion  •  Shared Data Consistency\n"
                + "📨  Message Passing Protocol  •  Client-Server Model");
        lblInfo.setFont(Font.font("Segoe UI", 11));
        lblInfo.setTextFill(Color.web("#7dd3fc"));
        infoBox.getChildren().add(lblInfo);

        // ── Connect action ────────────────────────────────────────────
        btnConnect.setOnAction(e -> {
            String host = txtHost.getText().trim();
            String portStr = txtPort.getText().trim();
            String name = txtName.getText().trim();

            if (host.isEmpty() || portStr.isEmpty()) {
                setStatus(lblStatus, "⚠ Please enter server IP and port.", "#f59e0b");
                return;
            }
            if (name.isEmpty()) {
                setStatus(lblStatus, "⚠ Please enter your name.", "#f59e0b");
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                setStatus(lblStatus, "⚠ Port must be a number.", "#f59e0b");
                return;
            }

            btnConnect.setDisable(true);
            setStatus(lblStatus, "⏳ Connecting to " + host + ":" + port + "...", "#94a3b8");

            // Connect in background thread — never block the JavaFX thread
            // DISTRIBUTED CONCEPT — Non-blocking I/O pattern
            Thread connectThread = new Thread(() -> {
                LibraryClient newClient = new LibraryClient(host, port);
                try {
                    String welcome = newClient.connect();
                    Platform.runLater(() -> {
                        client = newClient;
                        userName = name;
                        setStatus(lblStatus, "✅ " + welcome.replace("WELCOME:", ""), "#10b981");
                        btnConnect.setDisable(false);
                        if (onConnected != null) onConnected.run();
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        setStatus(lblStatus,
                            "❌ Cannot connect: " + ex.getMessage() +
                            "\n   Is the server running? Start run_server.bat first.",
                            "#ef4444");
                        btnConnect.setDisable(false);
                    });
                }
            });
            connectThread.setDaemon(true);
            connectThread.start();
        });

        // Allow pressing Enter in any field to trigger connect
        txtHost.setOnAction(e -> btnConnect.fire());
        txtPort.setOnAction(e -> btnConnect.fire());
        txtName.setOnAction(e -> btnConnect.fire());

        card.getChildren().addAll(
            titleBox, sep,
            lblHost, txtHost,
            lblPort, txtPort,
            lblName, txtName,
            btnConnect, lblStatus,
            infoBox
        );

        view.getChildren().add(card);
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#94a3b8"));
        return lbl;
    }

    private void setStatus(Label lbl, String text, String color) {
        lbl.setText(text);
        lbl.setTextFill(Color.web(color));
    }

    // ─────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────

    public StackPane getView()                        { return view; }
    public LibraryClient getClient()                  { return client; }
    public String getUserName()                       { return userName; }
    public void setOnConnected(Runnable onConnected)  { this.onConnected = onConnected; }
}
