package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** An immutable snapshot of a successfully recorded payment. */
public final class Payment {
    private final String paymentId;
    private final String tenantId;
    private final String unitNumber;
    private final LocalDate paymentDate;
    private final YearMonth billingMonth;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final BigDecimal monthBalanceAfter;

    /** Creates a payment after its amount and billing month have been validated. */
    Payment(String paymentId, String tenantId, String unitNumber, LocalDate paymentDate,
            YearMonth billingMonth, BigDecimal amount, BigDecimal balanceAfter,
            BigDecimal monthBalanceAfter) {
        this.paymentId = paymentId;
        this.tenantId = tenantId;
        this.unitNumber = unitNumber;
        this.paymentDate = paymentDate;
        this.billingMonth = billingMonth;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.monthBalanceAfter = monthBalanceAfter;
    }

    /** Returns the generated payment ID. */
    public String getPaymentId() { return paymentId; }

    /** Returns the tenant ID attached to this payment. */
    public String getTenantId() { return tenantId; }

    /** Returns the unit number as it was when payment was recorded. */
    public String getUnitNumber() { return unitNumber; }

    /** Returns the date the payment was recorded. */
    public LocalDate getPaymentDate() { return paymentDate; }

    /** Returns the billing month covered by the payment. */
    public YearMonth getBillingMonth() { return billingMonth; }

    /** Returns the amount paid. */
    public BigDecimal getAmount() { return amount; }

    /** Returns the tenant's total balance immediately after this payment. */
    public BigDecimal getBalanceAfter() { return balanceAfter; }

    /** Returns the selected month's balance immediately after this payment. */
    public BigDecimal getMonthBalanceAfter() { return monthBalanceAfter; }
}
