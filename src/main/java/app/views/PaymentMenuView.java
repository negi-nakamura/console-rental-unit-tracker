package app.views;

import app.models.Unit;
import app.models.Payment;
import app.models.RentCharge;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Objects;

public class PaymentMenuView {
    private final PrintStream output;

    /** Creates a payment-menu view using standard console output. */
    public PaymentMenuView() {
        this(System.out);
    }

    /** Creates a payment-menu view using the supplied output stream. */
    public PaymentMenuView(PrintStream output) {
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Displays payment operations and the option to return to the main menu. */
    public void showMenu() {
        HeaderView.show(output, "PAYMENTS",
                "1. Record payment by tenant ID",
                "2. Check balance by tenant ID",
                "3. Charge rent by tenant ID",
                "4. View payment history",
                "5. View charge history",
                "0. Back");
    }

    /** Displays the tenant, their unit, and the balance currently owed. */
    public void showBalance(Unit unit) {
        output.println("Tenant ID: " + unit.getTenant().getTenantId());
        output.println("Tenant: " + unit.getTenant().getFullName());
        output.println("Unit number: " + unit.getUnitNumber());
        output.printf(Locale.ROOT, "Balance owed: PHP %.2f%n", unit.getBalance());
    }

    /** Displays the selected billing month's unpaid balance before payment. */
    public void showBillingBalance(YearMonth month, BigDecimal balance) {
        output.println("Billing month: " + month);
        output.printf(Locale.ROOT, "Unpaid for this month: PHP %.2f%n", balance);
    }

    /** Displays a committed payment with its date, billing month, and remaining balances. */
    public void showPaymentRecorded(Payment payment) {
        output.println("Payment recorded successfully.");
        output.println("Payment ID: " + payment.getPaymentId());
        output.println("Payment date: " + payment.getPaymentDate());
        output.println("Billing month: " + payment.getBillingMonth());
        output.printf(Locale.ROOT, "Payment amount: PHP %.2f%n", payment.getAmount());
        output.printf(Locale.ROOT, "Month balance: PHP %.2f%n", payment.getMonthBalanceAfter());
        output.printf(Locale.ROOT, "Remaining balance: PHP %.2f%n", payment.getBalanceAfter());
    }

    /** Displays the selected tenant and prorated amount before confirmation. */
    public void showChargePreview(Unit unit, YearMonth month, int rentalDays, BigDecimal charge) {
        showBalance(unit);
        output.println("Billing month: " + month);
        output.println("Rental days: " + rentalDays + " of " + month.lengthOfMonth());
        output.printf(Locale.ROOT, "Monthly rent: PHP %.2f%n", unit.getMonthlyRent());
        output.printf(Locale.ROOT, "Rent charge: PHP %.2f%n", charge);
    }

    /** Displays the rent added to this tenant's balance. */
    public void showRentCharged(RentCharge charge, BigDecimal newBalance) {
        output.println("Rent charged successfully.");
        output.println("Charge ID: " + charge.getChargeId());
        output.println("Recorded date: " + charge.getRecordedDate());
        output.println("Billing month: " + charge.getBillingMonth());
        output.printf(Locale.ROOT, "Rent charge: PHP %.2f%n", charge.getAmount());
        output.printf(Locale.ROOT, "New balance: PHP %.2f%n", newBalance);
    }

    /** Displays payment history headings; both balance columns are snapshots after payment. */
    public void showPaymentHistoryHeader(String tenantId, String tenantName) {
        output.println("===== PAYMENT HISTORY =====");
        output.println("Tenant ID: " + tenantId);
        output.println("Tenant: " + tenantName);
        output.println("Balances shown are immediately after each payment.");
        output.printf("%-12s | %-10s | %-10s | %-10s | %14s | %14s | %14s%n",
                "Payment ID", "Paid date", "Bill month", "Unit", "Paid (PHP)", "Month balance", "Total balance");
        output.println("-".repeat(110));
    }

    /** Displays one immutable payment, including payment date and billing month. */
    public void showPaymentHistoryRow(Payment payment) {
        output.printf(Locale.ROOT, "%-12s | %-10s | %-10s | %-10s | %14.2f | %14.2f | %14.2f%n",
                payment.getPaymentId(), payment.getPaymentDate(), payment.getBillingMonth(),
                payment.getUnitNumber(), payment.getAmount(), payment.getMonthBalanceAfter(),
                payment.getBalanceAfter());
    }

    /** Displays successful payment count and total paid, excluding opening balances. */
    public void showPaymentHistoryTotals(int count, BigDecimal total) {
        output.println("Total payments: " + count);
        output.printf(Locale.ROOT, "Total paid: PHP %.2f%n", total);
    }

    /** Displays charge-history headings without inventing dates for opening balances. */
    public void showChargeHistoryHeader(String tenantId, String tenantName) {
        output.println("===== CHARGE HISTORY =====");
        output.println("Tenant ID: " + tenantId);
        output.println("Tenant: " + tenantName);
        output.println("Opening balances have no known original charge date or rental-day count.");
        output.printf("%-15s | %-10s | %-10s | %-10s | %4s | %12s | %14s | %s%n",
                "Charge ID", "Recorded", "Bill month", "Unit", "Days", "Rate (PHP)", "Amount (PHP)", "Kind");
        output.println("-".repeat(115));
    }

    /** Displays a recorded charge or a clearly marked imported opening balance. */
    public void showChargeHistoryRow(RentCharge charge) {
        output.printf(Locale.ROOT, "%-15s | %-10s | %-10s | %-10s | %4s | %12s | %14.2f | %s%n",
                charge.getChargeId(), charge.getRecordedDate() == null ? "--" : charge.getRecordedDate(),
                charge.getBillingMonth(), charge.getUnitNumber(),
                charge.isOpeningBalance() ? "--" : charge.getRentalDays(),
                charge.getMonthlyRent() == null ? "--" : charge.getMonthlyRent().toPlainString(),
                charge.getAmount(), charge.isOpeningBalance() ? "Opening balance" : "Rent charge");
    }

    /** Displays all charge entries and opening balances added together. */
    public void showChargeHistoryTotals(int count, BigDecimal total) {
        output.println("Total charge entries: " + count);
        output.printf(Locale.ROOT, "Total billed: PHP %.2f%n", total);
    }

    /** Displays feedback from the controller. */
    public void showMessage(String message) {
        output.println(message);
    }

    /** Displays the complexity of an operation targeting one unit or tenant. */
    public void showComplexity() {
        output.println("Time: O(n) worst, Omega(1) best, Theta(1) average. Space: Theta(1).");
    }

    /** Displays append cost, including occasional custom-map resizing. */
    public void showWriteComplexity() {
        output.println("Time: O(n) worst, Omega(1) best, Theta(1) amortized. Space: O(n) during resizing.");
    }

    /** Displays the cost of streaming chronological history entries. */
    public void showHistoryComplexity() {
        output.println("Time: O(n), Omega(n), Theta(n). Space: Theta(1).");
    }

}
