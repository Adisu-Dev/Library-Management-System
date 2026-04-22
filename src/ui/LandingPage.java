package ui;

import java.io.File;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class LandingPage {

    private StackPane view;
    private ScrollPane scroll;
    private VBox aboutSec;
    private VBox howItWorks;
    private HBox featureRow;

    public LandingPage() {
        view = new StackPane();
        view.setStyle("-fx-background-color: #020617;");

        // ── Background + dark overlay ─────────────────────────────────
        StackPane bgLayer = new StackPane();
        try {
            File bgFile = new File("src/images/bdu_bg.jpg");
            Image bgImage = new Image(bgFile.toURI().toString());
            BackgroundSize bs = new BackgroundSize(
                    BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true);
            bgLayer.setBackground(new Background(new BackgroundImage(
                    bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, bs)));
        } catch (Exception ignored) {
            bgLayer.setStyle("-fx-background-color: #020617;");
        }
        Rectangle overlay = new Rectangle();
        overlay.setFill(Color.rgb(2, 6, 23, 0.88));
        overlay.widthProperty().bind(view.widthProperty());
        overlay.heightProperty().bind(view.heightProperty());
        bgLayer.getChildren().add(overlay);

        // ── Floating orbs ─────────────────────────────────────────────
        Pane orbLayer = buildOrbLayer();

        // ── Page content (scrollable) ─────────────────────────────────
        VBox pageContent = new VBox(0);
        pageContent.setStyle("-fx-background-color: transparent;");

        HBox ticker      = buildTickerBar();
        HBox navbar      = buildNavbar();
        VBox hero        = buildHeroSection();
        aboutSec         = buildAboutSection();
        howItWorks       = buildHowItWorksSection();
        featureRow       = buildFeatureRow();
        HBox footer      = buildFooter();

        pageContent.getChildren().addAll(ticker, navbar, hero, aboutSec, howItWorks, featureRow, footer);

        scroll = new ScrollPane(pageContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );

        view.getChildren().addAll(bgLayer, orbLayer, scroll);
        StackPane.setAlignment(scroll, Pos.TOP_LEFT);

        playEntranceAnimations(navbar, hero, aboutSec, howItWorks, featureRow);
    }

    // ═══════════════════════════════════════════════════════════════
    // Orb layer
    // ═══════════════════════════════════════════════════════════════
    private Pane buildOrbLayer() {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        Circle o1 = createOrb(280, "#38bdf8", 0.15);
        Circle o2 = createOrb(200, "#818cf8", 0.12);
        Circle o3 = createOrb(160, "#34d399", 0.10);
        Circle o4 = createOrb(130, "#fbbf24", 0.09);
        pane.getChildren().addAll(o1, o2, o3, o4);
        view.widthProperty().addListener((obs, ov, nv)  -> positionOrbs(o1, o2, o3, o4));
        view.heightProperty().addListener((obs, ov, nv) -> positionOrbs(o1, o2, o3, o4));
        animateOrb(o1, -28,  28, 6.0);
        animateOrb(o2,  22, -22, 8.0);
        animateOrb(o3, -18,  18, 7.0);
        animateOrb(o4,  18, -18, 5.5);
        return pane;
    }

    private void positionOrbs(Circle o1, Circle o2, Circle o3, Circle o4) {
        double w = view.getWidth(), h = view.getHeight();
        if (w == 0 || h == 0) return;
        o1.setCenterX(w * 0.08);  o1.setCenterY(h * 0.18);
        o2.setCenterX(w * 0.90);  o2.setCenterY(h * 0.12);
        o3.setCenterX(w * 0.84);  o3.setCenterY(h * 0.78);
        o4.setCenterX(w * 0.12);  o4.setCenterY(h * 0.82);
    }

    // ═══════════════════════════════════════════════════════════════
    // Ticker bar
    // ═══════════════════════════════════════════════════════════════
    private HBox buildTickerBar() {
        HBox bar = new HBox();
        bar.setMinHeight(34); bar.setMaxHeight(34);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle(
            "-fx-background-color: rgba(56,189,248,0.10);" +
            "-fx-border-color: rgba(56,189,248,0.22);" +
            "-fx-border-width: 0 0 1 0;"
        );
        bar.setClip(new Rectangle(20000, 34));
        String msg = "  📚 Welcome to Smart Library Management System  •  " +
                     "🎓 Bahir Dar University  •  " +
                     "📖 Manage Books, Members & Circulation  •  " +
                     "⚡ Fast, Secure & Modern  •  " +
                     "🔐 Role-Based Access Control  •  " +
                     "📊 Real-Time Analytics  •  🌟 Version 2026  •  ";
        Label lbl = new Label(msg + msg + msg);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web("#38bdf8"));
        bar.getChildren().add(lbl);
        TranslateTransition anim = new TranslateTransition(Duration.seconds(32), lbl);
        anim.setFromX(0); anim.setToX(-2400);
        anim.setCycleCount(Animation.INDEFINITE);
        anim.setInterpolator(Interpolator.LINEAR);
        anim.play();
        return bar;
    }

    // ═══════════════════════════════════════════════════════════════
    // Navbar
    // ═══════════════════════════════════════════════════════════════
    private HBox buildNavbar() {
        HBox nav = new HBox(20);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(16, 50, 16, 50));
        nav.setMinHeight(64); nav.setMaxHeight(64);
        nav.setStyle("-fx-background-color: rgba(2,6,23,0.65);");

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        ImageView logo = new ImageView();
        try {
            logo.setImage(new Image(new File("src/images/logo.png").toURI().toString()));
            logo.setFitWidth(34); logo.setPreserveRatio(true);
        } catch (Exception ignored) {}
        VBox brandText = new VBox(1);
        Label bName = new Label("SmartLMS");
        bName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        bName.setTextFill(Color.WHITE);
        Label bSub = new Label("Bahir Dar University");
        bSub.setFont(Font.font("Segoe UI", 10));
        bSub.setTextFill(Color.web("#64748b"));
        brandText.getChildren().addAll(bName, bSub);
        brand.getChildren().addAll(logo, brandText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnSignIn = createOutlineButton("Sign In", "#38bdf8");
        btnSignIn.setOnAction(e -> navigateToLogin());

        Label navAbout    = createNavLink("About");
        Label navFeatures = createNavLink("Features");
        Label navContact  = createNavLink("Contact");

        navAbout.setOnMouseClicked(e -> scrollToNode(aboutSec));
        navFeatures.setOnMouseClicked(e -> scrollToNode(featureRow));
        navContact.setOnMouseClicked(e -> showContactPopup());

        nav.getChildren().addAll(brand, spacer, navAbout, navFeatures, navContact, btnSignIn);
        return nav;
    }

    // ═══════════════════════════════════════════════════════════════
    // Hero section
    // ═══════════════════════════════════════════════════════════════
    private VBox buildHeroSection() {
        VBox hero = new VBox(24);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(70, 60, 60, 60));

        Label badge = new Label("✨   Next-Gen Library Platform   ✨");
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        badge.setTextFill(Color.web("#38bdf8"));
        badge.setPadding(new Insets(6, 20, 6, 20));
        badge.setStyle(
            "-fx-background-color: rgba(56,189,248,0.12);" +
            "-fx-border-color: rgba(56,189,248,0.35);" +
            "-fx-border-radius: 20; -fx-background-radius: 20;"
        );
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER);

        Label line1 = new Label("Smart Library");
        line1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 62));
        line1.setTextFill(Color.WHITE);
        line1.setTextAlignment(TextAlignment.CENTER);
        line1.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.5)));

        Label line2 = new Label("Management System");
        line2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 62));
        line2.setTextFill(Color.web("#38bdf8"));
        line2.setTextAlignment(TextAlignment.CENTER);

        VBox headline = new VBox(4, line1, line2);
        headline.setAlignment(Pos.CENTER);

        Label subtitle = new Label(
            "A powerful, modern platform for managing books, members, and circulation.\n" +
            "Built for Bahir Dar University — fast, secure, and beautifully designed."
        );
        subtitle.setFont(Font.font("Segoe UI", 15));
        subtitle.setTextFill(Color.web("#94a3b8"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(600);

        Button btnStart = createGradientButton("🚀   Get Started");
        Button btnMore  = createGhostButton("📖   Learn More");
        btnStart.setOnAction(e -> navigateToLogin());
        btnMore.setOnAction(e -> scrollToNode(aboutSec));

        HBox cta = new HBox(16, btnStart, btnMore);
        cta.setAlignment(Pos.CENTER);

        HBox stats = new HBox(0);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
            createStatCard("📚", "10,000+", "Books"),
            createStatDivider(),
            createStatCard("👥", "2,500+",  "Members"),
            createStatDivider(),
            createStatCard("📤", "500+",    "Daily Borrows"),
            createStatDivider(),
            createStatCard("⭐", "99.9%",   "Uptime")
        );

        hero.getChildren().addAll(badgeBox, headline, subtitle, cta, stats);
        return hero;
    }

    // ═══════════════════════════════════════════════════════════════
    // About / Description section
    // ═══════════════════════════════════════════════════════════════
    private VBox buildAboutSection() {
        VBox section = new VBox(50);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(70, 80, 70, 80));
        section.setStyle("-fx-background-color: rgba(15,23,42,0.70);");

        // ── Section header ────────────────────────────────────────────
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label pill = new Label("ABOUT THE SYSTEM");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pill.setTextFill(Color.web("#38bdf8"));
        pill.setPadding(new Insets(4, 14, 4, 14));
        pill.setStyle(
            "-fx-background-color: rgba(56,189,248,0.10);" +
            "-fx-border-color: rgba(56,189,248,0.30);" +
            "-fx-border-radius: 12; -fx-background-radius: 12;"
        );

        Label title = new Label("Everything You Need to Run\nYour Library Efficiently");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
        title.setTextFill(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);
        title.setWrapText(true);

        Label desc = new Label(
            "Smart LMS is a comprehensive desktop application developed for Bahir Dar University " +
            "to digitize and streamline all library operations. From cataloging thousands of books " +
            "to tracking borrowing history and generating overdue reports — everything is handled " +
            "in one elegant, fast, and secure platform."
        );
        desc.setFont(Font.font("Segoe UI", 15));
        desc.setTextFill(Color.web("#94a3b8"));
        desc.setTextAlignment(TextAlignment.CENTER);
        desc.setWrapText(true);
        desc.setMaxWidth(720);

        header.getChildren().addAll(pill, title, desc);

        // ── Two-column detail cards ───────────────────────────────────
        HBox columns = new HBox(24);
        columns.setAlignment(Pos.CENTER);

        columns.getChildren().addAll(
            buildDetailCard("📚", "#38bdf8",
                "Book Catalog Management",
                "Add, edit, search, and categorize thousands of books with ease. " +
                "Track total stock vs available copies in real time. Export the full " +
                "catalog to CSV with one click."),
            buildDetailCard("🔄", "#818cf8",
                "Circulation & Borrowing",
                "Issue books to students and staff with automatic due-date calculation. " +
                "Process returns instantly and compute overdue fines at 5 ETB/day. " +
                "Print transaction receipts on the spot."),
            buildDetailCard("👥", "#34d399",
                "Member Management",
                "Register students, teachers, and staff with role-based access. " +
                "Secure SHA-256 password hashing keeps credentials safe. " +
                "Admins can update or remove accounts at any time."),
            buildDetailCard("📊", "#fbbf24",
                "Analytics & Reports",
                "Live KPI dashboard shows total books, active members, borrowed items, " +
                "and overdue counts at a glance. Monthly circulation bar charts and " +
                "exportable penalty reports keep management informed.")
        );

        // ── Highlight strip ───────────────────────────────────────────
        HBox strip = new HBox(0);
        strip.setAlignment(Pos.CENTER);
        strip.setMaxWidth(900);
        strip.setStyle(
            "-fx-background-color: rgba(56,189,248,0.07);" +
            "-fx-border-color: rgba(56,189,248,0.18);" +
            "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        strip.getChildren().addAll(
            buildStripItem("🏛️", "BDU Poly Campus"),
            buildStripSep(),
            buildStripItem("☕", "Built with JavaFX"),
            buildStripSep(),
            buildStripItem("🗄️", "SQL Server Backend"),
            buildStripSep(),
            buildStripItem("🔒", "SHA-256 Security"),
            buildStripSep(),
            buildStripItem("🌐", "Version 2026")
        );

        section.getChildren().addAll(header, columns, strip);
        return section;
    }

    private VBox buildDetailCard(String icon, String accent, String title, String body) {
        VBox card = new VBox(14);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(26, 24, 26, 24));
        card.setPrefWidth(260);
        String base =
            "-fx-background-color: rgba(2,6,23,0.75);" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 16; -fx-background-radius: 16;";
        card.setStyle(base);
        card.setEffect(new DropShadow(14, Color.rgb(0, 0, 0, 0.45)));

        // Icon circle
        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(26));
        iconLbl.setPadding(new Insets(10, 12, 10, 12));
        iconLbl.setStyle(
            "-fx-background-color: " + hexToRgba(accent, 0.15) + ";" +
            "-fx-background-radius: 12;"
        );

        // Accent line
        Rectangle accentLine = new Rectangle(36, 3);
        accentLine.setFill(Color.web(accent));
        accentLine.setArcWidth(3); accentLine.setArcHeight(3);

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setWrapText(true);

        Label bodyLbl = new Label(body);
        bodyLbl.setFont(Font.font("Segoe UI", 13));
        bodyLbl.setTextFill(Color.web("#94a3b8"));
        bodyLbl.setWrapText(true);
        bodyLbl.setLineSpacing(3);

        card.getChildren().addAll(iconLbl, accentLine, titleLbl, bodyLbl);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: rgba(15,23,42,0.92);" +
                "-fx-border-color: " + accent + ";" +
                "-fx-border-radius: 16; -fx-background-radius: 16; -fx-cursor: hand;"
            );
            card.setEffect(new DropShadow(26, Color.web(accent, 0.25)));
            TranslateTransition liftCard = new TranslateTransition(Duration.millis(140), card);
            liftCard.setToY(-6); liftCard.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(base);
            card.setEffect(new DropShadow(14, Color.rgb(0, 0, 0, 0.45)));
            TranslateTransition dropCard = new TranslateTransition(Duration.millis(140), card);
            dropCard.setToY(0); dropCard.play();
        });

        return card;
    }

    private VBox buildStripItem(String icon, String label) {
        VBox item = new VBox(4);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(18, 30, 18, 30));
        Label li = new Label(icon); li.setFont(Font.font(20));
        Label ll = new Label(label);
        ll.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        ll.setTextFill(Color.web("#94a3b8"));
        item.getChildren().addAll(li, ll);
        return item;
    }

    private Label buildStripSep() {
        Label sep = new Label("|");
        sep.setTextFill(Color.web("#1e293b"));
        sep.setFont(Font.font(22));
        sep.setPadding(new Insets(0, 0, 0, 0));
        return sep;
    }

    // ═══════════════════════════════════════════════════════════════
    // How It Works section
    // ═══════════════════════════════════════════════════════════════
    private VBox buildHowItWorksSection() {
        VBox section = new VBox(40);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(70, 80, 70, 80));

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        Label pill = new Label("HOW IT WORKS");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pill.setTextFill(Color.web("#818cf8"));
        pill.setPadding(new Insets(4, 14, 4, 14));
        pill.setStyle(
            "-fx-background-color: rgba(129,140,248,0.10);" +
            "-fx-border-color: rgba(129,140,248,0.30);" +
            "-fx-border-radius: 12; -fx-background-radius: 12;"
        );

        Label title = new Label("Simple. Fast. Powerful.");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);

        Label sub = new Label("Get up and running in minutes with an intuitive workflow designed for librarians and students alike.");
        sub.setFont(Font.font("Segoe UI", 15));
        sub.setTextFill(Color.web("#94a3b8"));
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);
        sub.setMaxWidth(620);

        header.getChildren().addAll(pill, title, sub);

        // Steps row
        HBox steps = new HBox(0);
        steps.setAlignment(Pos.CENTER);

        steps.getChildren().addAll(
            buildStep("01", "#38bdf8", "Login & Select Role",
                "Choose your role — Admin, Teacher, or Student — and sign in securely with your credentials."),
            buildStepArrow(),
            buildStep("02", "#818cf8", "Manage or Browse",
                "Admins manage the catalog and members. Students browse the digital library and track their borrows."),
            buildStepArrow(),
            buildStep("03", "#34d399", "Issue & Return",
                "Librarians issue books with auto due-dates. Returns are processed instantly with penalty calculation."),
            buildStepArrow(),
            buildStep("04", "#fbbf24", "Reports & Insights",
                "Generate overdue reports, view circulation trends, and export data to CSV with a single click.")
        );

        section.getChildren().addAll(header, steps);
        return section;
    }

    private VBox buildStep(String num, String accent, String title, String body) {
        VBox step = new VBox(14);
        step.setAlignment(Pos.TOP_CENTER);
        step.setPadding(new Insets(28, 22, 28, 22));
        step.setPrefWidth(230);
        step.setStyle(
            "-fx-background-color: rgba(15,23,42,0.75);" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        step.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.4)));

        Label numLbl = new Label(num);
        numLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        numLbl.setTextFill(Color.web(accent));
        numLbl.setPadding(new Insets(8, 16, 8, 16));
        numLbl.setStyle(
            "-fx-background-color: " + hexToRgba(accent, 0.12) + ";" +
            "-fx-background-radius: 12;"
        );

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        titleLbl.setTextFill(Color.WHITE);
        titleLbl.setTextAlignment(TextAlignment.CENTER);
        titleLbl.setWrapText(true);

        Label bodyLbl = new Label(body);
        bodyLbl.setFont(Font.font("Segoe UI", 12));
        bodyLbl.setTextFill(Color.web("#64748b"));
        bodyLbl.setTextAlignment(TextAlignment.CENTER);
        bodyLbl.setWrapText(true);
        bodyLbl.setLineSpacing(3);

        step.getChildren().addAll(numLbl, titleLbl, bodyLbl);
        return step;
    }

    private Label buildStepArrow() {
        Label arrow = new Label("→");
        arrow.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        arrow.setTextFill(Color.web("#334155"));
        arrow.setPadding(new Insets(0, 8, 0, 8));
        return arrow;
    }

    // ═══════════════════════════════════════════════════════════════
    // Feature cards row
    // ═══════════════════════════════════════════════════════════════
    private HBox buildFeatureRow() {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(10, 50, 50, 50));
        row.setStyle("-fx-background-color: rgba(15,23,42,0.70);");
        row.getChildren().addAll(
            createFeatureCard("🔐", "Secure Auth",     "Role-based access\nwith SHA-256 encryption"),
            createFeatureCard("📊", "Live Analytics",  "Real-time dashboards\nand circulation reports"),
            createFeatureCard("📱", "Digital Library", "Browse & download\ne-books instantly"),
            createFeatureCard("⚡", "Fast Search",     "Paginated tables\nwith live filtering"),
            createFeatureCard("🔔", "Smart Alerts",    "Overdue notifications\nand system tray alerts")
        );
        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    // Footer
    // ═══════════════════════════════════════════════════════════════
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(22, 50, 22, 50));
        footer.setStyle(
            "-fx-background-color: rgba(2,6,23,0.90);" +
            "-fx-border-color: rgba(255,255,255,0.06);" +
            "-fx-border-width: 1 0 0 0;"
        );

        Label left = new Label("© 2026 Smart LMS — Bahir Dar University");
        left.setFont(Font.font("Segoe UI", 12));
        left.setTextFill(Color.web("#475569"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnFooterLogin = new Button("Sign In →");
        btnFooterLogin.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        btnFooterLogin.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #38bdf8;" +
            "-fx-cursor: hand;" +
            "-fx-border-color: transparent;"
        );
        btnFooterLogin.setOnAction(e -> navigateToLogin());
        btnFooterLogin.setOnMouseEntered(e -> btnFooterLogin.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #7dd3fc; -fx-cursor: hand; -fx-border-color: transparent;"));
        btnFooterLogin.setOnMouseExited(e -> btnFooterLogin.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #38bdf8; -fx-cursor: hand; -fx-border-color: transparent;"));

        footer.getChildren().addAll(left, spacer, btnFooterLogin);
        return footer;
    }

    // ═══════════════════════════════════════════════════════════════
    // Entrance animations
    // ═══════════════════════════════════════════════════════════════
    private void playEntranceAnimations(HBox navbar, VBox hero, VBox about, VBox howItWorks, HBox features) {
        fadeIn(navbar, 0);
        slideUp(hero,       250, 40);
        slideUp(about,      500, 30);
        slideUp(howItWorks, 700, 30);
        slideUp(features,   900, 30);
    }

    private void fadeIn(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(700), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.play();
    }

    private void slideUp(javafx.scene.Node node, int delayMs, int fromY) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(800), node);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(800), node);
        tt.setFromY(fromY); tt.setToY(0); tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // ═══════════════════════════════════════════════════════════════
    // Scroll to section
    // ═══════════════════════════════════════════════════════════════
    private void scrollToNode(javafx.scene.Node node) {
        if (scroll == null || node == null) return;
        // Layout must be done first; run after current pulse
        javafx.application.Platform.runLater(() -> {
            double contentH = scroll.getContent().getBoundsInLocal().getHeight();
            double nodeY    = node.getBoundsInParent().getMinY();
            double vValue   = nodeY / (contentH - scroll.getViewportBounds().getHeight());
            // Animate scroll
            double start = scroll.getVvalue();
            double end   = Math.min(1.0, Math.max(0.0, vValue));
            javafx.animation.Timeline tl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(600),
                    new javafx.animation.KeyValue(scroll.vvalueProperty(), end,
                        Interpolator.EASE_BOTH))
            );
            tl.play();
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // Contact popup
    // ═══════════════════════════════════════════════════════════════
    private void showContactPopup() {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("Contact Us");
        popup.setResizable(false);

        VBox root = new VBox(22);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(36, 40, 36, 40));
        root.setPrefWidth(460);
        root.setStyle("-fx-background-color: #0f172a;");

        // Header
        Label pill = new Label("GET IN TOUCH");
        pill.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        pill.setTextFill(Color.web("#38bdf8"));
        pill.setPadding(new Insets(4, 14, 4, 14));
        pill.setStyle(
            "-fx-background-color: rgba(56,189,248,0.10);" +
            "-fx-border-color: rgba(56,189,248,0.30);" +
            "-fx-border-radius: 12; -fx-background-radius: 12;"
        );

        Label title = new Label("Contact & Support");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Reach out to the Smart LMS team at Bahir Dar University Poly Campus for support, feedback, or inquiries.");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web("#94a3b8"));
        subtitle.setWrapText(true);

        // Divider
        Rectangle div = new Rectangle(380, 1);
        div.setFill(Color.web("#1e293b"));

        // Contact items
        VBox contacts = new VBox(14);

        // Build a custom phone row with all 3 numbers stacked
        HBox phoneRow = new HBox(14);
        phoneRow.setAlignment(Pos.CENTER_LEFT);
        phoneRow.setPadding(new Insets(10, 16, 10, 16));
        phoneRow.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );
        Label phoneIcon = new Label("📞");
        phoneIcon.setFont(Font.font(18));
        phoneIcon.setMinWidth(28);
        VBox phoneText = new VBox(3);
        Label phoneLabel = new Label("Phone");
        phoneLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        phoneLabel.setTextFill(Color.web("#64748b"));
        Label phone1 = new Label("+251 921 140 482");
        Label phone2 = new Label("+251 905 075 895");
        Label phone3 = new Label("+251 942 120 102");
        for (Label p : new Label[]{phone1, phone2, phone3}) {
            p.setFont(Font.font("Segoe UI", 13));
            p.setTextFill(Color.WHITE);
        }
        phoneText.getChildren().addAll(phoneLabel, phone1, phone2, phone3);
        phoneRow.getChildren().addAll(phoneIcon, phoneText);

        contacts.getChildren().addAll(
            buildContactItem("🏛️", "Institution",   "Bahir Dar University — Poly Campus"),
            buildContactItem("📧", "Email",          "adisudereje3@gmail.com"),
            phoneRow,
            buildContactItem("📍", "Location",       "Bahir Dar, Amhara Region, Ethiopia"),
            buildContactItem("🕐", "Support Hours",  "Mon – Fri, 8:00 AM – 5:00 PM (EAT)")
        );

        // Close button
        Button btnClose = new Button("Close");
        btnClose.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnClose.setPrefWidth(Double.MAX_VALUE);
        btnClose.setPrefHeight(42);
        btnClose.setStyle(
            "-fx-background-color: linear-gradient(to right,#38bdf8,#818cf8);" +
            "-fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnClose.setOnAction(e -> popup.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle(
            "-fx-background-color: linear-gradient(to right,#7dd3fc,#a5b4fc);" +
            "-fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle(
            "-fx-background-color: linear-gradient(to right,#38bdf8,#818cf8);" +
            "-fx-text-fill: #020617; -fx-background-radius: 8; -fx-cursor: hand;"));

        root.getChildren().addAll(pill, title, subtitle, div, contacts, btnClose);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        popup.setScene(scene);
        popup.centerOnScreen();

        // Fade in
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0); ft.setToValue(1);
        popup.setOnShown(e -> ft.play());

        popup.show();
    }

    private HBox buildContactItem(String icon, String label, String value) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle(
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-border-color: rgba(255,255,255,0.07);" +
            "-fx-border-radius: 10; -fx-background-radius: 10;"
        );

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(18));
        iconLbl.setMinWidth(28);

        VBox text = new VBox(2);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web("#64748b"));
        Label val = new Label(value);
        val.setFont(Font.font("Segoe UI", 13));
        val.setTextFill(Color.WHITE);
        text.getChildren().addAll(lbl, val);

        row.getChildren().addAll(iconLbl, text);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    // Navigate to login
    // ═══════════════════════════════════════════════════════════════
    private void navigateToLogin() {
        if (view.getScene() == null) return;
        FadeTransition out = new FadeTransition(Duration.millis(450), view);
        out.setFromValue(1.0); out.setToValue(0.0);
        out.setOnFinished(e -> view.getScene().setRoot(new LoginForm().getView()));
        out.play();
    }

    public StackPane getView() { return view; }

    // ═══════════════════════════════════════════════════════════════
    // Reusable helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Converts a hex color string like "#38bdf8" to a valid JavaFX CSS
     * rgba() string like "rgba(56,189,248,0.15)".
     * JavaFX CSS rgba() requires decimal 0-255 values, not hex strings.
     */
    private String hexToRgba(String hex, double alpha) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return "rgba(" + r + "," + g + "," + b + "," + alpha + ")";
    }

    private Circle createOrb(double r, String color, double opacity) {
        Circle c = new Circle(r);
        c.setFill(Color.web(color, opacity));
        c.setEffect(new GaussianBlur(r * 0.75));
        return c;
    }

    private void animateOrb(Circle orb, double fromY, double toY, double secs) {
        TranslateTransition tt = new TranslateTransition(Duration.seconds(secs), orb);
        tt.setFromY(fromY); tt.setToY(toY);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    private Label createNavLink(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        l.setTextFill(Color.web("#94a3b8"));
        l.setPadding(new Insets(6, 14, 6, 14));
        l.setStyle(
            "-fx-cursor: hand;" +
            "-fx-background-color: transparent;" +
            "-fx-background-radius: 20;"
        );
        l.setOnMouseEntered(e -> {
            l.setTextFill(Color.web("#38bdf8"));
            l.setStyle(
                "-fx-cursor: hand;" +
                "-fx-background-color: rgba(56,189,248,0.12);" +
                "-fx-border-color: rgba(56,189,248,0.35);" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;"
            );
        });
        l.setOnMouseExited(e -> {
            l.setTextFill(Color.web("#94a3b8"));
            l.setStyle(
                "-fx-cursor: hand;" +
                "-fx-background-color: transparent;" +
                "-fx-background-radius: 20;"
            );
        });
        return l;
    }

    private Button createOutlineButton(String text, String color) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        b.setPrefHeight(36);
        String base  = "-fx-background-color:transparent; -fx-border-color:" + color +
                       "; -fx-border-radius:20; -fx-background-radius:20;" +
                       " -fx-text-fill:" + color + "; -fx-padding:5 20; -fx-cursor:hand;";
        String hover = "-fx-background-color:" + color + "; -fx-border-color:" + color +
                       "; -fx-border-radius:20; -fx-background-radius:20;" +
                       " -fx-text-fill:#020617; -fx-font-weight:bold; -fx-padding:5 20; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button createGradientButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        b.setPrefHeight(50); b.setPrefWidth(195);
        String base  = "-fx-background-color:linear-gradient(to right,#38bdf8,#818cf8);" +
                       "-fx-text-fill:#020617; -fx-background-radius:25; -fx-cursor:hand;" +
                       "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.45),18,0.3,0,4);";
        String hover = "-fx-background-color:linear-gradient(to right,#7dd3fc,#a5b4fc);" +
                       "-fx-text-fill:#020617; -fx-background-radius:25; -fx-cursor:hand;" +
                       "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.65),24,0.4,0,6);";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button createGhostButton(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 15));
        b.setPrefHeight(50); b.setPrefWidth(175);
        String base  = "-fx-background-color:rgba(255,255,255,0.07);" +
                       "-fx-border-color:rgba(255,255,255,0.25); -fx-border-radius:25;" +
                       "-fx-background-radius:25; -fx-text-fill:white; -fx-cursor:hand;";
        String hover = "-fx-background-color:rgba(255,255,255,0.14);" +
                       "-fx-border-color:rgba(255,255,255,0.5); -fx-border-radius:25;" +
                       "-fx-background-radius:25; -fx-text-fill:white; -fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private VBox createStatCard(String icon, String value, String label) {
        VBox c = new VBox(3);
        c.setAlignment(Pos.CENTER);
        c.setPadding(new Insets(14, 30, 14, 30));
        Label li = new Label(icon);  li.setFont(Font.font(20));
        Label lv = new Label(value); lv.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lv.setTextFill(Color.WHITE);
        Label ll = new Label(label); ll.setFont(Font.font("Segoe UI", 12));
        ll.setTextFill(Color.web("#64748b"));
        c.getChildren().addAll(li, lv, ll);
        return c;
    }

    private Label createStatDivider() {
        Label d = new Label("|");
        d.setTextFill(Color.web("#1e293b"));
        d.setFont(Font.font(26));
        d.setPadding(new Insets(0, 4, 0, 4));
        return d;
    }

    private VBox createFeatureCard(String icon, String title, String desc) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20, 20, 20, 20));
        card.setPrefWidth(190);
        card.setMinHeight(140);
        String baseStyle =
            "-fx-background-color:rgba(2,6,23,0.80);" +
            "-fx-border-color:rgba(255,255,255,0.08);" +
            "-fx-border-radius:14; -fx-background-radius:14;";
        card.setStyle(baseStyle);
        card.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.4)));

        Label li = new Label(icon);
        li.setFont(Font.font(26));
        li.setStyle("-fx-background-color:rgba(56,189,248,0.12); -fx-background-radius:10; -fx-padding:7 9;");

        Label lt = new Label(title);
        lt.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lt.setTextFill(Color.WHITE);

        Label ld = new Label(desc);
        ld.setFont(Font.font("Segoe UI", 12));
        ld.setTextFill(Color.web("#64748b"));
        ld.setWrapText(true);

        card.getChildren().addAll(li, lt, ld);

        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color:rgba(30,41,59,0.92);" +
                "-fx-border-color:rgba(56,189,248,0.40);" +
                "-fx-border-radius:14; -fx-background-radius:14; -fx-cursor:hand;"
            );
            card.setEffect(new DropShadow(22, Color.rgb(56, 189, 248, 0.28)));
            TranslateTransition liftFeat = new TranslateTransition(Duration.millis(140), card);
            liftFeat.setToY(-5); liftFeat.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
            card.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.4)));
            TranslateTransition dropFeat = new TranslateTransition(Duration.millis(140), card);
            dropFeat.setToY(0); dropFeat.play();
        });

        return card;
    }
}
