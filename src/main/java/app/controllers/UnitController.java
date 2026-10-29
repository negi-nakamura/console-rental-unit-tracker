package app.controllers;

import app.models.OperationResult;
import app.models.RentManager;
import app.models.Unit;
import app.views.InputReader;
import app.views.UnitMenuView;

import java.math.BigDecimal;
import java.util.Objects;

public class UnitController {
    private final RentManager manager;
    private final InputReader input;
    private final UnitMenuView view;

    /** Connects unit operations to the shared manager, input, and view. */
    public UnitController(RentManager manager, InputReader input, UnitMenuView view) {
        this.manager = Objects.requireNonNull(manager, "Rent manager is required.");
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Unit-menu view is required.");
    }

    /** Runs unit operations until Back is selected; the app handles ended input. */
    public void run() {
        while (true) {
            try {
                view.showMenu();
                int choice = input.readMenuChoice(7);

                switch (choice) {
                    case 1 -> addUnit();
                    case 2 -> viewUnit();
                    case 3 -> updateUnit();
                    case 4 -> removeUnit();
                    case 5 -> {
                        listAllUnits();
                        continue;
                    }
                    case 6 -> {
                        view.showUnitCount(manager.size());
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

    /** Displays the unit table's current buckets and collision chains. */
    private void displayHashMap() {
        if (manager.isEmpty()) {
            view.showMessage("The table is empty.");
        }

        view.showHashMap(manager.getTableLayout());
        view.showHashMapComplexity();
    }

    /** Lists registered units directly from the custom hash map. */
    private void listAllUnits() {
        if (manager.isEmpty()) {
            view.showMessage("The table is empty.");
            view.showConstantComplexity();
            return;
        }

        view.showUnitListHeader();
        manager.forEachUnit(unit -> view.showUnitListRow(unit));
        view.showUnitCount(manager.size());
        view.showListComplexity();
    }

    /** Reads the details for a new vacant unit and requests insertion. */
    private void addUnit() {
        if (manager.isFull()) {
            showResult(OperationResult.FULL_TABLE, "");
            return;
        }

        String number = input.readUnitNumber("Enter unit number: ");

        if (manager.findUnit(number) != null) {
            view.showMessage("Unit already exists. Nothing added.");
            return;
        }

        BigDecimal rent = input.readMoney("Enter monthly rent: ", "Rent must be a number greater than 0.");
        int dueDay = input.readDueDay("Enter due day (1-31): ");

        OperationResult result = manager.addUnit(new Unit(number, rent, dueDay));
        showResult(result, "Unit added successfully.");
    }

    /** Finds and displays a unit. */
    private void viewUnit() {
        Unit unit = readExistingUnit();

        if (unit != null) {
            view.showUnit(unit);
        }
    }

    /** Reads optional field changes and asks the manager to save them together. */
    private void updateUnit() {
        Unit unit = readExistingUnit();

        if (unit == null) {
            return;
        }

        String number = input.readOptionalUnitNumber("Enter new unit number (Enter to keep): ", unit.getUnitNumber());
        BigDecimal rent = input.readOptionalMoney("Enter new monthly rent (Enter to keep): ", unit.getMonthlyRent(), "Rent must be a number greater than 0.");
        int dueDay = input.readOptionalDueDay("Enter new due day (1-31, Enter to keep): ", unit.getDueDay());

        if (number.equals(unit.getUnitNumber()) && rent.compareTo(unit.getMonthlyRent()) == 0 && dueDay == unit.getDueDay()) {
            view.showMessage("No changes made.");
            return;
        }

        OperationResult result = manager.updateUnit(unit.getUnitNumber(), number, rent, dueDay);
        showResult(result, "Unit updated successfully.");
    }

    /** Confirms removal of an eligible unit before requesting deletion. */
    private void removeUnit() {
        Unit unit = readExistingUnit();

        if (unit == null) {
            return;
        }

        if (!unit.isVacant()) {
            showResult(OperationResult.HAS_TENANT, "");
            return;
        }

        if (unit.getBalance().signum() > 0) {
            showResult(OperationResult.HAS_BALANCE, "");
            return;
        }

        if (!input.readYesNo("Confirm removal? (Y/N): ")) {
            view.showMessage("Removal cancelled.");
            return;
        }

        showResult(manager.removeUnit(unit.getUnitNumber()), "Unit removed successfully.");
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
            showResult(OperationResult.NOT_FOUND, "");
        }

        return unit;
    }

    /** Translates model outcomes into messages for the view. */
    private void showResult(OperationResult result, String successMessage) {
        String message = switch (result) {
            case SUCCESS -> successMessage;
            case DUPLICATE -> "Unit already exists. Nothing changed.";
            case NOT_FOUND -> "Unit not found.";
            case EMPTY_TABLE -> "The table is empty.";
            case FULL_TABLE -> "Maximum number of units reached (" + RentManager.MAX_UNITS + "). Remove a unit first.";
            case HAS_TENANT -> "Unit has a tenant. Move them out first.";
            case HAS_BALANCE -> "Unit has an unpaid balance. Settle it first.";
            case INVALID_AMOUNT -> "Rent must be a number greater than 0.";
            case INVALID_DETAILS -> "Unit details are invalid.";
            default -> "The unit operation could not be completed.";
        };

        view.showMessage(message);
    }
}
