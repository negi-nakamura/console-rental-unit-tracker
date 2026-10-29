package app.utils;

import app.models.OperationResult;
import app.models.RentManager;
import app.models.Tenant;
import app.models.Unit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class DataLoader {
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    /** Prevents creating instances of this utility class. */
    private DataLoader() {
    }

    /** Loads starting units from JSON and returns the number added. */
    public static int load(RentManager manager) {
        Objects.requireNonNull(manager, "Rent manager is required.");

        InputStream input = DataLoader.class.getResourceAsStream("/app/data/data.json");

        if (input == null) {
            System.err.println("Starting data file was not found.");
            return 0;
        }

        int loaded = 0;

        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {

            JsonElement root = GSON.fromJson(reader, JsonElement.class);

            if (root == null || !root.isJsonArray()) {
                System.err.println("Starting data must be a JSON array.");
                return 0;
            }

            JsonArray records = root.getAsJsonArray();

            for (int i = 0; i < records.size(); i++) {
                try {
                    UnitData data = GSON.fromJson(records.get(i), UnitData.class);

                    Unit unit = toUnit(data);
                    OperationResult result = manager.loadUnit(unit, parseOpeningMonth(data));

                    switch (result) {
                        case SUCCESS -> loaded++;

                        case DUPLICATE -> warn(i + 1, "Unit " + unit.getUnitNumber() + " already exists.");

                        case DUPLICATE_TENANT_ID -> warn(i + 1, "Tenant ID already assigned to another unit.");

                        case FULL_TABLE -> warn(i + 1, "Maximum number of units reached (" + RentManager.MAX_UNITS + ").");

                        default -> warn(i + 1, "Could not load unit: " + result);
                    }

                } catch (JsonParseException | IllegalArgumentException exception) {
                    warn(i + 1, exception.getMessage());
                }
            }

        } catch (IOException | JsonParseException exception) {
            System.err.println("Could not read starting data: " + exception.getMessage());
        }

        return loaded;
    }

    /** Creates models through their constructors so validation still runs. */
    private static Unit toUnit(UnitData data) {
        if (data == null) {
            throw new IllegalArgumentException("Unit record cannot be null.");
        }

        if (data.dueDay == null) {
            throw new IllegalArgumentException("Due day is required.");
        }

        Tenant tenant = null;

        if (data.tenant != null) {
            tenant = new Tenant(data.tenant.tenantId, data.tenant.firstName,
                    data.tenant.middleName, data.tenant.lastName, data.tenant.contactNumber);
        }

        return new Unit(data.unitNumber, data.monthlyRent, data.dueDay, data.balance, tenant);
    }

    /** Reads the explicitly supplied month for any imported unpaid balance. */
    private static YearMonth parseOpeningMonth(UnitData data) {
        String text = data.openingBillingMonth == null ? "" : data.openingBillingMonth.trim();
        if (text.isEmpty()) {
            if (data.balance != null && data.balance.signum() > 0) {
                throw new IllegalArgumentException("Opening billing month is required for an unpaid balance.");
            }
            return null;
        }

        try {
            YearMonth month = YearMonth.parse(text);
            if (text.matches("[0-9]{4}-[0-9]{2}") && month.getYear() > 0) {
                return month;
            }
        } catch (DateTimeParseException ignored) {
            // Invalid opening months are reported with the JSON record number.
        }
        throw new IllegalArgumentException("Opening billing month must use YYYY-MM.");
    }

    /** Reports a rejected record using its position in the JSON array. */
    private static void warn(int recordNumber, String message) {
        System.err.println("Starting record " + recordNumber + " skipped: " + message);
    }

    /** Temporarily holds JSON fields before model validation. */
    private static class UnitData {
        String unitNumber;
        BigDecimal monthlyRent;
        Integer dueDay;
        BigDecimal balance;
        String openingBillingMonth;
        TenantData tenant;
    }

    /** Temporarily holds the optional tenant's JSON fields. */
    private static class TenantData {
        String tenantId;
        String firstName;
        String middleName;
        String lastName;
        String contactNumber;
    }
}
