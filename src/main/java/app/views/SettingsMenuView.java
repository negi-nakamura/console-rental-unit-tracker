package app.views;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;

public class SettingsMenuView {
    private final PrintStream output;

    /** Creates a settings-menu view using standard console output. */
    public SettingsMenuView() {
        this(System.out);
    }

    /** Creates a settings-menu view using the supplied output stream. */
    public SettingsMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays table statistics, manual rehashing, and the Back option. */
    public void showMenu() {
        HeaderView.show(output, "TABLE SETTINGS",
                "1. Show load factors and bucket counts",
                "2. Rehash now",
                "0. Back");
    }

    /** Displays the record count, bucket count, and load factor of both tables. */
    public void showStatistics(int unitCount, int unitBuckets, double unitLoadFactor,
                               int tenantCount, int tenantBuckets, double tenantLoadFactor) {
        output.println("===== HASH TABLE STATISTICS =====");
        output.println("Unit records: " + unitCount);
        output.println("Unit buckets: " + unitBuckets);
        output.printf(Locale.ROOT, "Unit load factor: %.2f%n", unitLoadFactor);
        output.println("Tenant records: " + tenantCount);
        output.println("Tenant buckets: " + tenantBuckets);
        output.printf(Locale.ROOT, "Tenant load factor: %.2f%n", tenantLoadFactor);
    }

    /** Explains which custom hash tables will be resized. */
    public void showRehashNotice() {
        output.println("This doubles the buckets in the unit table and tenant-ID index.");
    }

    /** Displays feedback from the controller. */
    public void showMessage(String message) {
        output.println(message);
    }

    /** Displays the constant cost of statistics, cancellation, or empty checks. */
    public void showConstantComplexity() {
        output.println("Time: O(1), Omega(1), Theta(1). Space: Theta(1).");
    }

    /** Displays the cost of rebuilding both tables' bucket arrays. */
    public void showRehashComplexity() {
        output.println("Time: O(n), Omega(n), Theta(n). Space: Theta(n).");
    }
}
