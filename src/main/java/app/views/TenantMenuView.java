package app.views;

import app.models.Tenant;

import java.io.PrintStream;
import java.util.Objects;

public class TenantMenuView {
    private final PrintStream output;

    /** Creates a tenant-menu view using standard console output. */
    public TenantMenuView() {
        this(System.out);
    }

    /** Creates a tenant-menu view using the supplied output stream. */
    public TenantMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays tenant operations and the option to return to the main menu. */
    public void showMenu() {
        HeaderView.show(output, "TENANT MANAGEMENT",
                "1. Assign tenant to unit",
                "2. View tenant by ID",
                "3. Update tenant by ID",
                "4. Move out tenant by ID",
                "5. List all tenants",
                "6. Count all tenants",
                "7. Display hash map",
                "0. Back");
    }

    /** Displays the tenant-ID hash map's actual bucket layout. */
    public void showHashMap(String layout) {
        output.println("===== TENANT ID HASH MAP =====");
        output.print(layout);
    }

    /** Displays headings for currently assigned tenants. */
    public void showTenantListHeader() {
        output.println("===== ALL TENANTS =====");
        output.printf("%-10s | %-10s | %-11s | %s%n", "Tenant ID", "Unit", "Contact", "Name");
        output.println("-".repeat(75));
    }

    /** Displays one tenant row with the unit they currently occupy. */
    public void showTenantListRow(String unitNumber, Tenant tenant) {
        output.printf("%-10s | %-10s | %-11s | %s%n",
                tenant.getTenantId(), unitNumber, tenant.getContactNumber(), tenant.getFullName());
    }

    /** Displays the total number of currently assigned tenants. */
    public void showTenantCount(int count) {
        output.println("Total tenants: " + count);
    }

    /** Displays a unit's tenant details without changing the tenant. */
    public void showTenant(String unitNumber, Tenant tenant) {
        output.println("Tenant ID: " + tenant.getTenantId());
        output.println("Unit number: " + unitNumber);
        output.println("First name: " + tenant.getFirstName());
        output.println("Middle name: " + (tenant.getMiddleName().isEmpty()
                ? "(none)" : tenant.getMiddleName()));
        output.println("Last name: " + tenant.getLastName());
        output.println("Full name: " + tenant.getFullName());
        output.println("Contact number: " + tenant.getContactNumber());
    }

    /** Displays feedback from the controller. */
    public void showMessage(String message) {
        output.println(message);
    }

    /** Displays the complexity of operations targeting one unit's tenant. */
    public void showComplexity() {
        output.println("Time: O(n) worst, Omega(1) best, Theta(1) average. Space: Theta(1).");
    }

    /** Displays the cost of streaming the tenant index's buckets and entries. */
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
