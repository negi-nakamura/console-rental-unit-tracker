package app.views;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public class ReportMenuView {
    private final PrintStream output;

    /** Creates a report-menu view using standard console output. */
    public ReportMenuView() {
        this(System.out);
    }

    /** Creates a report-menu view using the supplied output stream. */
    public ReportMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays report operations and the option to return to the main menu. */
    public void showMenu() {
        HeaderView.show(output, "REPORTS",
                "1. Display table",
                "2. Count unpaid units",
                "3. Count vacant units",
                "4. Total balance owed",
                "0. Back");
    }

    /** Displays the custom unit table's buckets and collision chains. */
    public void showTable(String layout) {
        output.println("===== UNIT HASH TABLE =====");
        output.print(layout);
    }

    /** Displays the number of units with a positive balance. */
    public void showUnpaidCount(int count) {
        output.println("Unpaid units: " + count);
    }

    /** Displays the number of units without tenants. */
    public void showVacantCount(int count) {
        output.println("Vacant units: " + count);
    }

    /** Displays the total money currently owed across all units. */
    public void showTotalBalance(BigDecimal total) {
        output.printf(Locale.ROOT, "Total balance owed: PHP %.2f%n", total);
    }

    /** Displays feedback from the controller. */
    public void showMessage(String message) {
        output.println(message);
    }

    /** Displays traversal cost; table text uses additional space for its output. */
    public void showComplexity(boolean tableDisplay) {
        output.println("Time: O(n), Omega(n), Theta(n). Space: "
                + (tableDisplay ? "Theta(n)." : "Theta(1)."));
    }

    /** Displays the constant cost of checking an empty table. */
    public void showEmptyComplexity() {
        output.println("Time: O(1), Omega(1), Theta(1). Space: Theta(1).");
    }
}
