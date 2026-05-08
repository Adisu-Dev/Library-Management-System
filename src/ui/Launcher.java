package ui;

/**
 * Launcher — separate entry point class for jpackage/fat-JAR compatibility.
 *
 * When JavaFX is bundled in a fat JAR, the JVM sometimes refuses to launch
 * a class that directly extends Application. This plain Launcher class
 * delegates to Main.main() and avoids that restriction.
 *
 * The build.bat script uses this class as the Main-Class in MANIFEST.MF.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
