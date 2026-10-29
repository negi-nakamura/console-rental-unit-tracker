package app.models;

import app.datastructures.HashMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RentManager {
    public static final int MAX_UNITS = 50;

    private final HashMap<String, Unit> table;
    private final HashMap<String, Unit> tenantIndex;
    private final HashMap<String, TenantLedger> tenantLedgers;
    private final Clock clock;
    private int nextPaymentId = 1;
    private int nextChargeId = 1;

    /** Creates a manager with an empty custom hash map. */
    public RentManager() {
        this(Clock.system(ZoneId.of("Asia/Manila")));
    }

    /** Creates a manager with an injectable clock for consistent transaction dates. */
    public RentManager(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        table = new HashMap<>();
        tenantIndex = new HashMap<>();
        tenantLedgers = new HashMap<>();
    }

    /** Adds a unit unless its number already exists or the limit is reached. */
    public OperationResult addUnit(Unit unit) {
        return addUnit(unit, null);
    }

    /** Adds a unit and explicitly assigns any opening debt to its billing month. */
    public OperationResult addUnit(Unit unit, YearMonth openingMonth) {
        if (unit == null) {
            return OperationResult.INVALID_DETAILS;
        }

        if (table.containsKey(unit.getUnitNumber())) {
            return OperationResult.DUPLICATE;
        }

        if (!unit.isVacant() && isTenantIdUsed(unit.getTenant().getTenantId())) {
            return OperationResult.DUPLICATE_TENANT_ID;
        }

        if (isFull()) {
            return OperationResult.FULL_TABLE;
        }

        if (unit.getBalance().signum() > 0 && (unit.isVacant() || openingMonth == null)) {
            return OperationResult.INVALID_DETAILS;
        }

        TenantLedger ledger = unit.isVacant() ? null
                : new TenantLedger(unit.getTenant(), unit.getUnitNumber(), unit.getBalance(), openingMonth);
        table.insert(unit.getUnitNumber(), unit);
        if (!unit.isVacant()) {
            tenantIndex.insert(unit.getTenant().getTenantId(), unit);
            registerLedger(ledger);
        }
        return OperationResult.SUCCESS;
    }

    /** Finds a unit by its normalized number, or returns null if missing. */
    public Unit findUnit(String unitNumber) {
        return table.get(normalizeKey(unitNumber));
    }

    /** Finds the existing unit linked to a tenant ID, or returns null if missing. */
    public Unit findUnitByTenantId(String tenantId) {
        return tenantIndex.get(normalizeKey(tenantId));
    }

    /** Finds an active tenant by ID, or returns null if missing. */
    public Tenant findTenant(String tenantId) {
        Unit unit = findUnitByTenantId(tenantId);
        return unit == null ? null : unit.getTenant();
    }

    /** Returns the number of currently assigned tenants. */
    public int tenantCount() {
        return tenantIndex.size();
    }

    /** Returns the billing month according to the application's Philippine clock. */
    public YearMonth currentBillingMonth() {
        return YearMonth.now(clock);
    }

    /** Checks active and archived IDs so unrelated tenants cannot inherit payment history. */
    public boolean isTenantIdUsed(String tenantId) {
        return tenantLedgers.containsKey(normalizeKey(tenantId));
    }

    /** Returns a tenant's ledger, including history retained after move-out. */
    public TenantLedger findTenantLedger(String tenantId) {
        return tenantLedgers.get(normalizeKey(tenantId));
    }

    /** Returns the number of current or archived tenant accounts. */
    public int tenantAccountCount() {
        return tenantLedgers.size();
    }

    /** Returns the unpaid balance for one tenant and billing month. */
    public BigDecimal getBillingBalance(String tenantId, YearMonth month) {
        TenantLedger ledger = findTenantLedger(tenantId);
        return ledger == null ? new BigDecimal("0.00") : ledger.getMonthBalance(month);
    }

    /** Visits units for read-only output. Time: O(n), Ω(n), Θ(n). Space: Θ(1), excluding callback work. */
    public void forEachUnit(Consumer<? super Unit> action) {
        Objects.requireNonNull(action, "Action is required.");
        table.forEach((key, unit) -> action.accept(unit));
    }

    /** Visits assigned tenants with their unit numbers. Time: O(n), Ω(n), Θ(n). Space: Θ(1), excluding callback work. */
    public void forEachTenant(BiConsumer<? super String, ? super Tenant> action) {
        Objects.requireNonNull(action, "Action is required.");
        tenantIndex.forEach((id, unit) -> action.accept(unit.getUnitNumber(), unit.getTenant()));
    }

    /** Updates rent and due day after validating both values. */
    public OperationResult updateUnit(String unitNumber, BigDecimal monthlyRent, int dueDay) {
        return updateUnit(unitNumber, unitNumber, monthlyRent, dueDay);
    }

    /** Validates all edits before changing unit details or its hash-map key. */
    public OperationResult updateUnit(String unitNumber, String newUnitNumber,
                                      BigDecimal monthlyRent, int dueDay) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        String oldKey = normalizeKey(unitNumber);
        String newKey = normalizeKey(newUnitNumber);
        Unit unit = table.get(oldKey);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        BigDecimal rent = normalizePositiveAmount(monthlyRent);

        if (rent == null) {
            return OperationResult.INVALID_AMOUNT;
        }

        if (dueDay < 1 || dueDay > 31) {
            return OperationResult.INVALID_DETAILS;
        }

        if (newKey.isEmpty() || newKey.length() > 10
                || newKey.chars().anyMatch(Character::isWhitespace)) {
            return OperationResult.INVALID_DETAILS;
        }

        if (!oldKey.equals(newKey) && table.containsKey(newKey)) {
            return OperationResult.DUPLICATE;
        }

        if (!oldKey.equals(newKey)) {
            table.delete(oldKey);
            unit.setUnitNumber(newKey);
            table.insert(newKey, unit);
        }

        unit.setMonthlyRent(rent);
        unit.setDueDay(dueDay);
        return OperationResult.SUCCESS;
    }

    /** Renames a unit and updates its hash-map key. */
    public OperationResult renameUnit(String oldUnitNumber, String newUnitNumber) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        String oldKey = normalizeKey(oldUnitNumber);
        String newKey = normalizeKey(newUnitNumber);

        Unit unit = table.get(oldKey);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (newKey.isEmpty() || newKey.length() > 10 || newKey.chars().anyMatch(Character::isWhitespace)) {
            return OperationResult.INVALID_DETAILS;
        }

        if (oldKey.equals(newKey)) {
            return OperationResult.SUCCESS;
        }

        if (table.containsKey(newKey)) {
            return OperationResult.DUPLICATE;
        }

        table.delete(oldKey);
        unit.setUnitNumber(newKey);
        table.insert(newKey, unit);

        return OperationResult.SUCCESS;
    }

    /** Removes a unit only if it is vacant and has no unpaid balance. */
    public OperationResult removeUnit(String unitNumber) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (!unit.isVacant()) {
            return OperationResult.HAS_TENANT;
        }

        if (unit.getBalance().signum() > 0) {
            return OperationResult.HAS_BALANCE;
        }

        table.delete(normalizeKey(unitNumber));
        return OperationResult.SUCCESS;
    }

    /** Assigns a tenant to a vacant unit with no outstanding balance. */
    public OperationResult assignTenant(String unitNumber, Tenant tenant) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (!unit.isVacant()) {
            return OperationResult.OCCUPIED;
        }

        if (tenant == null) {
            return OperationResult.INVALID_DETAILS;
        }

        if (unit.getBalance().signum() > 0) {
            return OperationResult.HAS_BALANCE;
        }

        if (isTenantIdUsed(tenant.getTenantId())) {
            return OperationResult.DUPLICATE_TENANT_ID;
        }

        TenantLedger ledger = new TenantLedger(tenant, unit.getUnitNumber(), unit.getBalance(), null);
        if (!tenantIndex.insert(tenant.getTenantId(), unit)) {
            return OperationResult.DUPLICATE_TENANT_ID;
        }

        unit.setTenant(tenant);
        registerLedger(ledger);
        return OperationResult.SUCCESS;
    }

    /** Replaces an occupied unit's tenant details with a validated Tenant. */
    public OperationResult updateTenant(
            String unitNumber, Tenant updatedTenant) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (unit.isVacant()) {
            return OperationResult.VACANT;
        }

        if (updatedTenant == null
                || !updatedTenant.getTenantId().equals(unit.getTenant().getTenantId())) {
            return OperationResult.INVALID_DETAILS;
        }

        unit.setTenant(updatedTenant);
        findTenantLedger(updatedTenant.getTenantId()).updateTenantName(updatedTenant.getFullName());
        return OperationResult.SUCCESS;
    }

    /** Updates personal details by tenant ID while preserving that ID. */
    public OperationResult updateTenantById(String tenantId, Tenant updatedTenant) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnitByTenantId(tenantId);
        return unit == null ? OperationResult.NOT_FOUND
                : updateTenant(unit.getUnitNumber(), updatedTenant);
    }

    /** Clears the tenant only after the outstanding balance is settled. */
    public OperationResult moveOutTenant(String unitNumber) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (unit.isVacant()) {
            return OperationResult.VACANT;
        }

        if (unit.getBalance().signum() > 0) {
            return OperationResult.HAS_BALANCE;
        }

        tenantIndex.delete(unit.getTenant().getTenantId());
        unit.setTenant(null);
        return OperationResult.SUCCESS;
    }

    /** Moves out the tenant found by ID after checking the existing balance rule. */
    public OperationResult moveOutTenantById(String tenantId) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnitByTenantId(tenantId);
        return unit == null ? OperationResult.NOT_FOUND
                : moveOutTenant(unit.getUnitNumber());
    }

    /** Records a payment against the selected month and synchronizes the unit total. */
    public OperationResult recordPayment(
            String unitNumber, YearMonth month, BigDecimal amount) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (unit.isVacant()) {
            return OperationResult.VACANT;
        }

        if (month == null) {
            return OperationResult.INVALID_DETAILS;
        }

        BigDecimal payment = normalizePositiveAmount(amount);

        if (payment == null) {
            return OperationResult.INVALID_AMOUNT;
        }

        TenantLedger ledger = findTenantLedger(unit.getTenant().getTenantId());
        if (ledger == null) {
            return OperationResult.INVALID_DETAILS;
        }

        OperationResult result = ledger.recordPayment(
                String.format(Locale.ROOT, "P-%03d", nextPaymentId), unit.getUnitNumber(),
                LocalDate.now(clock), month, payment);

        if (result == OperationResult.SUCCESS) {
            nextPaymentId++;
            unit.setBalance(ledger.getTotalBalance());
        }

        return result;
    }

    /** Records a payment for the tenant found through the custom ID index. */
    public OperationResult recordPaymentByTenantId(String tenantId, YearMonth month, BigDecimal amount) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnitByTenantId(tenantId);
        return unit == null ? OperationResult.NOT_FOUND
                : recordPayment(unit.getUnitNumber(), month, amount);
    }

    /** Changes future monthly charges without changing the current balance. */
    public OperationResult adjustRent(String unitNumber, BigDecimal newRent) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnit(unitNumber);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        BigDecimal rent = normalizePositiveAmount(newRent);

        if (rent == null) {
            return OperationResult.INVALID_AMOUNT;
        }

        unit.setMonthlyRent(rent);
        return OperationResult.SUCCESS;
    }

    /** Adds prorated rent only to the tenant found by ID. */
    public OperationResult chargeRentByTenantId(String tenantId, YearMonth month, int rentalDays) {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        Unit unit = findUnitByTenantId(tenantId);

        if (unit == null) {
            return OperationResult.NOT_FOUND;
        }

        if (month == null || rentalDays < 1 || rentalDays > month.lengthOfMonth()) {
            return OperationResult.INVALID_DETAILS;
        }

        BigDecimal charge = unit.calculateRentCharge(month, rentalDays);

        if (charge.signum() == 0) {
            return OperationResult.INVALID_AMOUNT;
        }

        TenantLedger ledger = findTenantLedger(unit.getTenant().getTenantId());
        if (ledger == null) {
            return OperationResult.INVALID_DETAILS;
        }

        RentCharge entry = new RentCharge(String.format(Locale.ROOT, "C-%03d", nextChargeId),
                unit.getTenant().getTenantId(), unit.getUnitNumber(), LocalDate.now(clock),
                month, rentalDays, unit.getMonthlyRent(), charge, false);
        ledger.recordCharge(entry);
        nextChargeId++;
        unit.setBalance(ledger.getTotalBalance());

        return OperationResult.SUCCESS;
    }

    /** Counts units whose balances are greater than zero. */
    public int countUnpaid() {
        int[] count = {0};

        table.forEach((key, unit) -> {
            if (unit.getBalance().signum() > 0) {
                count[0]++;
            }
        });

        return count[0];
    }

    /** Counts units without tenants. */
    public int countVacant() {
        int[] count = {0};

        table.forEach((key, unit) -> {
            if (unit.isVacant()) {
                count[0]++;
            }
        });

        return count[0];
    }

    /** Calculates the total balance owed across all units. */
    public BigDecimal totalBalance() {
        BigDecimal[] total = {new BigDecimal("0.00")};

        table.forEach((key, unit) -> {
            total[0] = total[0].add(unit.getBalance());
        });

        return total[0];
    }

    /** Returns the custom hash map's bucket layout for a view to print. */
    public String getTableLayout() {
        return table.getTableLayout();
    }

    /** Returns the actual tenant-ID index's buckets and chains for a view to print. */
    public String getTenantTableLayout() {
        return tenantIndex.getTableLayout();
    }

    /** Prints the bucket layout through the custom hash map. */
    public void displayTable() {
        table.displayTable();
    }

    /** Returns the number of stored units. */
    public int size() {
        return table.size();
    }

    /** Returns true if no units are stored. */
    public boolean isEmpty() {
        return table.isEmpty();
    }

    /** Returns true if the application limit has been reached. */
    public boolean isFull() {
        return table.size() >= MAX_UNITS;
    }

    /** Returns the number of buckets in the hash map. */
    public int getCapacity() {
        return table.getCapacity();
    }

    /** Returns the number of entries divided by the number of buckets. */
    public double loadFactor() {
        return table.loadFactor();
    }

    /** Returns the bucket count in the custom tenant-ID index. */
    public int getTenantCapacity() {
        return tenantIndex.getCapacity();
    }

    /** Returns assigned tenants divided by the tenant index's bucket count. */
    public double tenantLoadFactor() {
        return tenantIndex.loadFactor();
    }

    /** Doubles both custom hash tables' buckets and redistributes their entries. */
    public OperationResult rehash() {
        if (table.isEmpty()) {
            return OperationResult.EMPTY_TABLE;
        }

        table.rehash();
        tenantIndex.rehash();
        return OperationResult.SUCCESS;
    }

    /** Loads a starting unit using the same duplicate and capacity checks. */
    public OperationResult loadUnit(Unit unit) {
        return addUnit(unit);
    }

    /** Loads a unit with an explicitly identified opening billing month. */
    public OperationResult loadUnit(Unit unit, YearMonth openingMonth) {
        return addUnit(unit, openingMonth);
    }

    /** Registers one account while keeping its custom index's load factor bounded. */
    private void registerLedger(TenantLedger ledger) {
        if (tenantLedgers.size() + 1 > tenantLedgers.getCapacity() * 0.7) {
            tenantLedgers.rehash();
        }
        tenantLedgers.insert(ledger.getTenantId(), ledger);
    }

    /** Trims an identifier and converts it to uppercase for lookup. */
    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }

    /** Rounds a positive amount to two decimals, or returns null if invalid. */
    private BigDecimal normalizePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return null;
        }

        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);

        return rounded.signum() > 0 ? rounded : null;
    }
}
