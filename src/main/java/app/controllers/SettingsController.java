package app.controllers;

import app.models.OperationResult;
import app.models.RentManager;
import app.views.InputReader;
import app.views.SettingsMenuView;

import java.util.Objects;

public class SettingsController {
    private final RentManager manager;
    private final InputReader input;
    private final SettingsMenuView view;

    /** Connects table settings to the shared manager, input, and view. */
    public SettingsController(RentManager manager, InputReader input, SettingsMenuView view) {
        this.manager = Objects.requireNonNull(manager, "Rent manager is required.");
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Settings-menu view is required.");
    }

    /** Runs table settings until Back is selected; the app handles ended input. */
    public void run() {
        while (true) {
            try {
                view.showMenu();
                int choice = input.readMenuChoice(2);

                switch (choice) {
                    case 1 -> {
                        showStatistics();
                        view.showConstantComplexity();
                    }
                    case 2 -> {
                        if (rehashTables()) {
                            view.showRehashComplexity();
                        } else {
                            view.showConstantComplexity();
                        }
                    }
                    case 0 -> {
                        return;
                    }
                }
            } catch (InputReader.NavigationRequest navigation) {
                if (navigation.isExit()) throw navigation;
                if (navigation.isBack()) return;
                view.showMessage("Transaction cancelled.");
            }
        }
    }

    /** Reads stored counts and capacities without traversing either table. */
    private void showStatistics() {
        view.showStatistics(manager.size(), manager.getCapacity(), manager.loadFactor(),
                manager.tenantCount(), manager.getTenantCapacity(), manager.tenantLoadFactor());
    }

    /** Confirms rehashing and returns true only after the tables are resized. */
    private boolean rehashTables() {
        if (manager.isEmpty()) {
            view.showMessage("The table is empty.");
            return false;
        }

        view.showRehashNotice();

        if (!input.readYesNo("Confirm rehash? (Y/N): ")) {
            view.showMessage("Rehash cancelled.");
            return false;
        }

        OperationResult result = manager.rehash();

        if (result == OperationResult.SUCCESS) {
            view.showMessage("Both hash tables rehashed successfully.");
            showStatistics();
            return true;
        }

        view.showMessage(result == OperationResult.EMPTY_TABLE
                ? "The table is empty." : "Rehash could not be completed.");
        return false;
    }
}
