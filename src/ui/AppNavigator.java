package ui;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Central navigation helper.
 *
 * All dashboards call AppNavigator.goToLanding() on logout so the user
 * always returns to the full LandingPage (dark animated background) with
 * the login modal shown on top — exactly the same experience as first launch.
 *
 * This eliminates the "plain white background" bug that occurred when
 * dashboards created a bare Stage with only LoginForm.getView().
 */
public final class AppNavigator {

    private AppNavigator() {}

    /**
     * Closes the given dashboard stage and navigates back to the LandingPage,
     * immediately opening the login modal on top of it.
     *
     * @param dashboardStage the stage to close (the current dashboard)
     */
    public static void goToLanding(Stage dashboardStage) {
        dashboardStage.close();

        // Build the full landing page (dark background, orbs, animations)
        LandingPage landing = new LandingPage();

        Stage landingStage = new Stage();
        landingStage.setTitle("Smart Library | LMS 2026");
        landingStage.setMinWidth(900);
        landingStage.setMinHeight(600);
        landingStage.setMaximized(true);

        Scene scene = new Scene(landing.getView(), 1000, 600);
        landingStage.setScene(scene);
        landingStage.show();

        // Fade in the landing page, then open the login modal
        landing.getView().setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), landing.getView());
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setOnFinished(e -> landing.showLoginModal());
        ft.play();
    }
}
