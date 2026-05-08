package ui;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  MainApp — JavaFX Entry Point for Distributed Library Client        ║
 * ║  Bahir Dar University — Fundamentals of Distributed Systems         ║
 * ║  Group Project — Adisu, Dawit, and Dagnachew                        ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * This is the entry point for the DISTRIBUTED CLIENT application.
 * It is separate from the existing Main.java (which launches the
 * standalone LMS with direct DB access).
 *
 * Flow:
 *   1. Show LoginView  → user enters server IP, port, and name
 *   2. On connect      → switch to DashboardView
 *   3. DashboardView   → all operations go through TCP to LibraryServer
 *
 * To run: java -cp ... ui.MainApp
 * Or use: run_client.bat
 */
public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        stage.setTitle("Distributed Library Client — BDU");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        // Graceful shutdown — disconnect from server when window is closed
        stage.setOnCloseRequest(e -> Platform.exit());

        showLoginView();
        stage.show();
    }

    /**
     * Shows the connect-to-server screen.
     */
    private void showLoginView() {
        LoginView loginView = new LoginView();

        // When the user successfully connects, switch to the dashboard
        loginView.setOnConnected(() -> showDashboard(loginView));

        Scene scene = new Scene(loginView.getView(), 1000, 700);
        primaryStage.setScene(scene);
    }

    /**
     * Switches to the main dashboard after a successful server connection.
     *
     * @param loginView the LoginView that holds the connected client
     */
    private void showDashboard(LoginView loginView) {
        DashboardView dashboard = new DashboardView(
            loginView.getClient(),
            loginView.getUserName()
        );

        Scene scene = new Scene(dashboard.getView(), 1200, 750);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        // Fade in the dashboard
        dashboard.getView().setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(400), dashboard.getView());
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
