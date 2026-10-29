package app.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

public class Unit {
    private String unitNumber;
    private BigDecimal monthlyRent;
    private int dueDay;
    private BigDecimal balance;
    private Tenant tenant;

    /** Creates a vacant unit with a zero balance. */
    public Unit(String unitNumber, BigDecimal monthlyRent, int dueDay) {
        this(unitNumber, monthlyRent, dueDay, BigDecimal.ZERO, null);
    }

    /** Creates a unit with validated details and an optional tenant. */
    public Unit(String unitNumber, BigDecimal monthlyRent, int dueDay, BigDecimal balance, Tenant tenant) {
        setUnitNumber(unitNumber);
        setMonthlyRent(monthlyRent);
        setDueDay(dueDay);
        setBalance(balance);
        setTenant(tenant);
    }

    /** Returns the unit number. */
    public String getUnitNumber() {
        return unitNumber;
    }

    /** Validates and updates the unit number in uppercase. */
    public void setUnitNumber(String unitNumber) {
        String number = unitNumber == null ? "" : unitNumber.trim().toUpperCase(Locale.ROOT);

        if (number.isEmpty()) {
            throw new IllegalArgumentException("Unit number is required.");
        }

        if (number.length() > 10) {
            throw new IllegalArgumentException("Unit number must not exceed 10 characters.");
        }

        if (number.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Unit number must not contain spaces.");
        }

        this.unitNumber = number;
    }

    /** Returns the monthly rent. */
    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    /** Updates the monthly rent without changing the existing balance. */
    public void setMonthlyRent(BigDecimal monthlyRent) {
        if (monthlyRent == null) {
            throw new IllegalArgumentException("Monthly rent is required.");
        }

        if (monthlyRent.signum() <= 0) {
            throw new IllegalArgumentException("Rent must be greater than 0.");
        }

        BigDecimal rounded = monthlyRent.setScale(2, RoundingMode.HALF_UP);

        if (rounded.signum() == 0) {
            throw new IllegalArgumentException("Rent must be at least 0.01 after rounding.");
        }

        this.monthlyRent = rounded;
    }

    /** Returns the day of the month when rent is due. */
    public int getDueDay() {
        return dueDay;
    }

    /** Updates the due day, which must be from 1 to 31. */
    public void setDueDay(int dueDay) {
        if (dueDay < 1 || dueDay > 31) {
            throw new IllegalArgumentException("Due day must be from 1 to 31.");
        }

        this.dueDay = dueDay;
    }

    /** Returns the month's due date, using its last day when necessary. */
    public LocalDate getDueDate(YearMonth month) {
        if (month == null) {
            throw new IllegalArgumentException("Month is required.");
        }

        int day = Math.min(dueDay, month.lengthOfMonth());
        return month.atDay(day);
    }

    /** Calculates rent for the selected days, rounding only the final amount. */
    public BigDecimal calculateRentCharge(YearMonth month, int rentalDays) {
        if (month == null) {
            throw new IllegalArgumentException("Billing month is required.");
        }

        if (rentalDays < 1 || rentalDays > month.lengthOfMonth()) {
            throw new IllegalArgumentException(
                    "Rental days must be from 1 to " + month.lengthOfMonth() + ".");
        }

        return monthlyRent.multiply(BigDecimal.valueOf(rentalDays))
                .divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP);
    }

    /** Returns the amount currently owed. */
    public BigDecimal getBalance() {
        return balance;
    }

    /** Updates the balance, which cannot be negative. */
    public void setBalance(BigDecimal balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required.");
        }

        if (balance.signum() < 0) {
            throw new IllegalArgumentException("Balance must be greater than or equal to 0.");
        }

        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the assigned tenant, or null if vacant. */
    public Tenant getTenant() {
        return tenant;
    }

    /** Updates the tenant; null marks the unit as vacant. */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /** Returns true if no tenant is assigned. */
    public boolean isVacant() {
        return tenant == null;
    }

    /** Returns the unit details for display. */
    @Override
    public String toString() {
        String tenantName = isVacant() ? "VACANT"
                : tenant.getTenantId() + " - " + tenant.getFullName();

        return String.format(
                Locale.ROOT,
                "%s | Rent: %.2f | Due day: %d | Balance: %.2f | Tenant: %s",
                unitNumber, monthlyRent, dueDay, balance, tenantName);
    }
}
