package app.views;

import app.models.Unit;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

public class UnitMenuView {
    private final PrintStream output;

    /** Creates a unit-menu view using standard console output. */
    public UnitMenuView() {
        this(System.out);
    }

    /** Creates a unit-menu view using the supplied output stream. */
    public UnitMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays unit operations and the option to return to the main menu. */
    public void showMenu() {
        HeaderView.show(output, "UNIT MANAGEMENT",
                "1. Add unit",
                "2. View unit",
                "3. Update unit details",
                "4. Remove unit",
                "5. List all units",
                "6. Count all units",
                "7. Display hash map",
                "0. Back");
    }


    /** Displays the unit hash map's actual bucket layout. */
    public void showHashMap(String layout) {
        output.println("===== UNIT HASH MAP =====");
        output.print(layout);
    }

    /** Displays the headings for the flat unit list. */
    public void showUnitListHeader() {
        output.println("===== ALL UNITS =====");
        output.printf(Locale.ROOT, "%-10s | %-10s | %14s | %3s | %14s | %s%n",
                "Unit", "Tenant ID", "Rent (PHP)", "Due", "Balance (PHP)", "Tenant");
        output.println("-".repeat(90));
    }

    /** Displays one unit row, including vacant units. */
    public void showUnitListRow(Unit unit) {
        output.printf(Locale.ROOT, "%-10s | %-10s | %14.2f | %3d | %14.2f | %s%n",
                unit.getUnitNumber(), unit.isVacant() ? "-" : unit.getTenant().getTenantId(),
                unit.getMonthlyRent(), unit.getDueDay(), unit.getBalance(),
                unit.isVacant() ? "VACANT" : unit.getTenant().getFullName());
    }

    /** Displays the total number of registered units. */
    public void showUnitCount(int count) {
        output.println("Total units: " + count);
    }

    /** Displays a unit's details without changing the unit. */
    public void showUnit(Unit unit) {
        output.println("Unit number: " + unit.getUnitNumber());
        output.printf(Locale.ROOT, "Monthly rent: PHP %.2f%n", unit.getMonthlyRent());
        output.println("Due day: " + unit.getDueDay());
        output.printf(Locale.ROOT, "Balance owed: PHP %.2f%n", unit.getBalance());
        output.println("Tenant ID: " + (unit.isVacant() ? "(none)" : unit.getTenant().getTenantId()));
        output.println("Tenant: " + (unit.isVacant() ? "VACANT" : unit.getTenant().getFullName()));
    }

    /** Displays feedback from the controller. */
    public void showMessage(String message) {
        output.println(message);
    }

    /** Displays the complexity of operations targeting a single unit. */
    public void showComplexity() {
        output.println("Time: O(n) worst, Omega(1) best, Theta(1) average. " + "Space: Theta(1).");
    }

    /** Displays the cost of streaming all buckets and unit entries. */
    public void showListComplexity() {
        output.println("Time: O(n), Omega(n), Theta(n). Space: Theta(1).");
    }

    /** Displays traversal and formatting costs for the bucket layout. */
    public void showHashMapComplexity() {
        output.println("Time: O(n), Omega(n), Theta(n). Space: Theta(n).");
    }

    /** Displays the cost of stored counts or an empty-list check. */
    public void showConstantComplexity() {
        output.println("Time: O(1), Omega(1), Theta(1). Space: Theta(1).");
    }
}
