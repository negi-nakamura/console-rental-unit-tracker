package app.controllers;

import app.models.OperationResult;
import app.models.RentManager;
import app.models.Tenant;
import app.models.Unit;
import app.views.InputReader;
import app.views.TenantMenuView;

import java.util.Objects;

public class TenantController {
    private final RentManager manager;
    private final InputReader input;
    private final TenantMenuView view;

    /** Connects tenant operations to the shared manager, input, and view. */
    public TenantController(RentManager manager, InputReader input, TenantMenuView view) {
        this.manager = Objects.requireNonNull(manager, "Rent manager is required.");
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Tenant-menu view is required.");
    }

    /** Runs tenant operations until Back is selected; the app handles ended input. */
    public void run() {
        while (true) {
            try {
                view.showMenu();
                int choice = input.readMenuChoice(7);

                switch (choice) {
                    case 1 -> assignTenant();
                    case 2 -> viewTenant();
                    case 3 -> updateTenant();
                    case 4 -> moveOutTenant();
                    case 5 -> {
                        listAllTenants();
                        continue;
                    }
                    case 6 -> {
                        view.showTenantCount(manager.tenantCount());
                        view.showConstantComplexity();
                        continue;
                    }
                    case 7 -> {
                        displayHashMap();
                        continue;
                    }
                    case 0 -> {
                        return;
                    }
                }

                view.showComplexity();
            } catch (InputReader.NavigationRequest navigation) {
                if (navigation.isExit()) throw navigation;
                if (navigation.isBack()) return;
                view.showMessage("Transaction cancelled.");
            }
        }
    }

    /** Displays the tenant-ID index's current buckets and collision chains. */
    private void displayHashMap() {
        if (manager.tenantCount() == 0) {
            view.showMessage("No tenants are assigned.");
        }

        view.showHashMap(manager.getTenantTableLayout());
        view.showHashMapComplexity();
    }

    /** Lists currently assigned tenants directly from the tenant-ID index. */
    private void listAllTenants() {
        if (manager.tenantCount() == 0) {
            view.showMessage("No tenants are assigned.");
            view.showConstantComplexity();
            return;
        }

        view.showTenantListHeader();
        manager.forEachTenant((unitNumber, tenant) -> view.showTenantListRow(unitNumber, tenant));
        view.showTenantCount(manager.tenantCount());
        view.showListComplexity();
    }

    /** Reads a new tenant only after checking that the unit can accept one. */
    private void assignTenant() {
        Unit unit = readExistingUnit();

        if (unit == null) {
            return;
        }

        if (!unit.isVacant()) {
            showResult(OperationResult.OCCUPIED, "");
            return;
        }

        if (unit.getBalance().signum() > 0) {
            showResult(OperationResult.HAS_BALANCE, "");
            return;
        }

        String tenantId = input.readTenantId("Enter new tenant ID (e.g. T-001): ");

        if (manager.isTenantIdUsed(tenantId)) {
            showResult(OperationResult.DUPLICATE_TENANT_ID, "");
            return;
        }

        String firstName = input.readName("Enter tenant first name: ", "First name");
        String middleName = input.readMiddleName("Enter tenant middle name (Enter to skip): ");
        String lastName = input.readName("Enter tenant last name: ", "Last name");
        String contactNumber = input.readContactNumber("Enter contact number (11 digits): ");

        Tenant tenant = new Tenant(tenantId, firstName, middleName, lastName, contactNumber);
        showResult(manager.assignTenant(unit.getUnitNumber(), tenant),
                "Tenant assigned successfully. Tenant ID: " + tenant.getTenantId());
    }

    /** Finds and displays a tenant by their ID. */
    private void viewTenant() {
        Unit unit = readTenantUnit();

        if (unit != null) {
            view.showTenant(unit.getUnitNumber(), unit.getTenant());
        }
    }

    /** Collects optional edits before replacing the tenant's details together. */
    private void updateTenant() {
        Unit unit = readTenantUnit();

        if (unit == null) {
            return;
        }

        Tenant current = unit.getTenant();
        String firstName = input.readOptionalName(
                "Enter new first name (Enter to keep): ", "First name", current.getFirstName());
        String middleName = input.readOptionalMiddleName(
                "Enter new middle name (Enter to keep, - to clear): ", current.getMiddleName());
        String lastName = input.readOptionalName(
                "Enter new last name (Enter to keep): ", "Last name", current.getLastName());
        String contactNumber = input.readOptionalContactNumber(
                "Enter new contact number (Enter to keep): ", current.getContactNumber());

        if (firstName.equals(current.getFirstName())
                && middleName.equals(current.getMiddleName())
                && lastName.equals(current.getLastName())
                && contactNumber.equals(current.getContactNumber())) {
            view.showMessage("No changes made.");
            return;
        }

        Tenant updated = new Tenant(current.getTenantId(), firstName, middleName, lastName, contactNumber);
        showResult(manager.updateTenantById(current.getTenantId(), updated), "Tenant updated successfully.");
    }

    /** Confirms move-out after checking that the current balance is settled. */
    private void moveOutTenant() {
        Unit unit = readTenantUnit();

        if (unit == null) {
            return;
        }

        if (unit.getBalance().signum() > 0) {
            showResult(OperationResult.HAS_BALANCE, "");
            return;
        }

        if (!input.readYesNo("Confirm move-out? (Y/N): ")) {
            view.showMessage("Move-out cancelled.");
            return;
        }

        showResult(manager.moveOutTenantById(unit.getTenant().getTenantId()), "Tenant moved out successfully.");
    }

    /** Checks for an empty table before asking for an existing unit number. */
    private Unit readExistingUnit() {
        if (manager.isEmpty()) {
            showResult(OperationResult.EMPTY_TABLE, "");
            return null;
        }

        String number = input.readUnitNumber("Enter unit number: ");
        Unit unit = manager.findUnit(number);

        if (unit == null) {
            view.showMessage("Unit not found.");
        }

        return unit;
    }

    /** Finds an assigned tenant by ID and returns the unit linked to that ID. */
    private Unit readTenantUnit() {
        if (manager.isEmpty()) {
            showResult(OperationResult.EMPTY_TABLE, "");
            return null;
        }

        if (manager.tenantCount() == 0) {
            view.showMessage("No tenants are assigned.");
            return null;
        }

        String tenantId = input.readTenantId("Enter tenant ID: ");
        Unit unit = manager.findUnitByTenantId(tenantId);

        if (unit == null) {
            showResult(OperationResult.NOT_FOUND, "");
        }

        return unit;
    }

    /** Translates model outcomes into messages for the view. */
    private void showResult(OperationResult result, String successMessage) {
        String message = switch (result) {
            case SUCCESS -> successMessage;
            case NOT_FOUND -> "Tenant not found.";
            case DUPLICATE_TENANT_ID -> "Tenant ID already exists. Nothing changed.";
            case EMPTY_TABLE -> "The table is empty.";
            case OCCUPIED -> "Unit is already occupied.";
            case VACANT -> "Unit has no tenant.";
            case HAS_BALANCE -> "Unit has an unpaid balance. Settle it first.";
            case INVALID_DETAILS -> "Tenant details are invalid.";
            default -> "The tenant operation could not be completed.";
        };

        view.showMessage(message);
    }
}
