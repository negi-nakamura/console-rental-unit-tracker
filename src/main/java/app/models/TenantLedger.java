package app.models;

import app.datastructures.HashMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.function.Consumer;

/** Keeps one tenant's monthly balances and chronological history in custom hash maps. */
public final class TenantLedger {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final String tenantId;
    private String tenantName;
    private final HashMap<YearMonth, BigDecimal> monthlyBalances = new HashMap<>();
    private final HashMap<Integer, Payment> payments = new HashMap<>();
    private final HashMap<Integer, RentCharge> charges = new HashMap<>();
    private BigDecimal totalBalance = ZERO;
    private BigDecimal totalPaid = ZERO;
    private BigDecimal totalCharged = ZERO;

    /** Creates an account, optionally importing a dated-by-month opening balance. */
    TenantLedger(Tenant tenant, String unitNumber, BigDecimal openingBalance, YearMonth openingMonth) {
        this.tenantId = tenant.getTenantId();
        this.tenantName = tenant.getFullName();

        if (openingBalance.signum() > 0) {
            Objects.requireNonNull(openingMonth, "Opening billing month is required.");
            recordCharge(new RentCharge("OPEN-" + tenantId, tenantId, unitNumber, null,
                    openingMonth, 0, null, openingBalance, true));
        }
    }

    /** Returns the tenant's fixed ID. */
    public String getTenantId() { return tenantId; }

    /** Returns the tenant's most recently registered name. */
    public String getTenantName() { return tenantName; }

    /** Keeps the account's display name synchronized with validated personal edits. */
    void updateTenantName(String name) { tenantName = name; }

    /** Returns the current balance across all billing months. */
    public BigDecimal getTotalBalance() { return totalBalance; }

    /** Returns all successfully recorded payments added together. */
    public BigDecimal getTotalPaid() { return totalPaid; }

    /** Returns charges and imported opening balances added together. */
    public BigDecimal getTotalCharged() { return totalCharged; }

    /** Returns this month's outstanding balance. Time: O(1) average, O(n) worst. Space: Theta(1). */
    public BigDecimal getMonthBalance(YearMonth month) {
        if (month == null) {
            return ZERO;
        }

        BigDecimal balance = monthlyBalances.get(month);
        return balance == null ? ZERO : balance;
    }

    /** Returns the number of successful payments. */
    public int getPaymentCount() { return payments.size(); }

    /** Returns the number of charge and opening-balance entries. */
    public int getChargeCount() { return charges.size(); }

    /** Returns the latest successful payment, or null if no payment exists. */
    public Payment getLastPayment() {
        return payments.isEmpty() ? null : payments.get(payments.size());
    }

    /** Returns the latest charge entry, or null if no entry exists. */
    public RentCharge getLastCharge() {
        return charges.isEmpty() ? null : charges.get(charges.size());
    }

    /** Records a valid month-specific payment; rejected payments change nothing. */
    OperationResult recordPayment(String paymentId, String unitNumber, LocalDate date,
                                  YearMonth month, BigDecimal amount) {
        BigDecimal monthBalance = getMonthBalance(month);

        if (monthBalance.signum() == 0) {
            return OperationResult.NO_MONTH_BALANCE;
        }

        if (amount.compareTo(monthBalance) > 0) {
            return OperationResult.OVERPAYMENT;
        }

        BigDecimal remainingMonth = monthBalance.subtract(amount);
        BigDecimal remainingTotal = totalBalance.subtract(amount);
        Payment payment = new Payment(paymentId, tenantId, unitNumber, date, month,
                amount, remainingTotal, remainingMonth);

        append(payments, payment);
        putMonthBalance(month, remainingMonth);
        totalBalance = remainingTotal;
        totalPaid = totalPaid.add(amount);
        return OperationResult.SUCCESS;
    }

    /** Records one charge and updates only its billing month's balance. */
    void recordCharge(RentCharge charge) {
        BigDecimal monthBalance = getMonthBalance(charge.getBillingMonth()).add(charge.getAmount());
        append(charges, charge);
        putMonthBalance(charge.getBillingMonth(), monthBalance);
        totalBalance = totalBalance.add(charge.getAmount());
        totalCharged = totalCharged.add(charge.getAmount());
    }

    /** Visits immutable payments in recording order. Time: Theta(n). Space: Theta(1), excluding callback work. */
    public void forEachPayment(Consumer<? super Payment> action) {
        Objects.requireNonNull(action, "Action is required.");
        for (int i = 1; i <= payments.size(); i++) {
            action.accept(payments.get(i));
        }
    }

    /** Visits immutable charge entries in recording order. Time: Theta(n). Space: Theta(1), excluding callback work. */
    public void forEachCharge(Consumer<? super RentCharge> action) {
        Objects.requireNonNull(action, "Action is required.");
        for (int i = 1; i <= charges.size(); i++) {
            action.accept(charges.get(i));
        }
    }

    /** Appends a sequence-keyed history entry and grows the custom map as needed. */
    private <V> void append(HashMap<Integer, V> history, V entry) {
        if (history.size() + 1 > history.getCapacity() * 0.7) {
            history.rehash();
        }
        history.insert(history.size() + 1, entry);
    }

    /** Updates a monthly balance without introducing a built-in collection. */
    private void putMonthBalance(YearMonth month, BigDecimal balance) {
        if (monthlyBalances.containsKey(month)) {
            monthlyBalances.replace(month, balance);
        } else {
            if (monthlyBalances.size() + 1 > monthlyBalances.getCapacity() * 0.7) {
                monthlyBalances.rehash();
            }
            monthlyBalances.insert(month, balance);
        }
    }
}
