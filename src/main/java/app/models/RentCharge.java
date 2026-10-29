package app.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** An immutable rent-charge or imported opening-balance entry. */
public final class RentCharge {
    private final String chargeId;
    private final String tenantId;
    private final String unitNumber;
    private final LocalDate recordedDate;
    private final YearMonth billingMonth;
    private final int rentalDays;
    private final BigDecimal monthlyRent;
    private final BigDecimal amount;
    private final boolean openingBalance;

    /** Creates a validated charge snapshot; imported opening entries have no original date or rate. */
    RentCharge(String chargeId, String tenantId, String unitNumber, LocalDate recordedDate,
               YearMonth billingMonth, int rentalDays, BigDecimal monthlyRent,
               BigDecimal amount, boolean openingBalance) {
        this.chargeId = chargeId;
        this.tenantId = tenantId;
        this.unitNumber = unitNumber;
        this.recordedDate = recordedDate;
        this.billingMonth = billingMonth;
        this.rentalDays = rentalDays;
        this.monthlyRent = monthlyRent;
        this.amount = amount;
        this.openingBalance = openingBalance;
    }

    /** Returns the generated charge ID or imported opening-entry ID. */
    public String getChargeId() { return chargeId; }

    /** Returns the tenant ID attached to this entry. */
    public String getTenantId() { return tenantId; }

    /** Returns the unit number as it was when the entry was created. */
    public String getUnitNumber() { return unitNumber; }

    /** Returns the recorded date, or null for an opening balance whose original date is unknown. */
    public LocalDate getRecordedDate() { return recordedDate; }

    /** Returns the billing month covered by this entry. */
    public YearMonth getBillingMonth() { return billingMonth; }

    /** Returns charged rental days, or zero for an imported opening balance. */
    public int getRentalDays() { return rentalDays; }

    /** Returns the rate used to calculate rent, or null for an imported opening balance. */
    public BigDecimal getMonthlyRent() { return monthlyRent; }

    /** Returns the amount charged or imported. */
    public BigDecimal getAmount() { return amount; }

    /** Identifies an imported opening balance without inventing earlier transactions. */
    public boolean isOpeningBalance() { return openingBalance; }
}
