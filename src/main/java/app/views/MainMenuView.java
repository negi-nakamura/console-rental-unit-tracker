package app.views;

import java.io.PrintStream;
import java.util.Objects;

public class MainMenuView {
    private final PrintStream output;

    /** Creates a main-menu view using standard console output. */
    public MainMenuView() {
        this(System.out);
    }

    /** Creates a main-menu view using the supplied output stream. */
    public MainMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays the main menu and its navigation choices. */
    public void showMenu() {
        HeaderView.show(output, "RENT PAYMENT AND UNIT TRACKER",
                "1. Unit Management",
                "2. Tenant Management",
                "3. Payments",
                "4. Reports",
                "5. Table Settings",
                "0. Exit");
    }

    /** Displays the normal exit message. */
    public void showExit() {
        output.println("Goodbye!");
    }

    /** Displays the exit message when console input ends. */
    public void showInputEnded() {
        output.println();
        output.println("Input ended. Exiting Rent Tracker.");
    }
}
