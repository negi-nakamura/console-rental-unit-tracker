package app.controllers;

import app.views.InputReader;
import app.views.MainMenuView;

import java.util.NoSuchElementException;
import java.util.Objects;

public class AppController {
    private final InputReader input;
    private final MainMenuView view;
    private final UnitController unitController;
    private final TenantController tenantController;
    private final PaymentController paymentController;
    private final ReportController reportController;
    private final SettingsController settingsController;

    /** Connects shared input and all five main-menu sections. */
    public AppController(InputReader input, MainMenuView view,
                         UnitController unitController, TenantController tenantController,
                         PaymentController paymentController, ReportController reportController,
                         SettingsController settingsController) {
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Main-menu view is required.");
        this.unitController = Objects.requireNonNull(unitController, "Unit controller is required.");
        this.tenantController = Objects.requireNonNull(tenantController, "Tenant controller is required.");
        this.paymentController = Objects.requireNonNull(paymentController, "Payment controller is required.");
        this.reportController = Objects.requireNonNull(reportController, "Report controller is required.");
        this.settingsController = Objects.requireNonNull(settingsController, "Settings controller is required.");
    }

    /** Runs the main menu until Exit is selected or console input ends. */
    public void run() {
        try {
            while (true) {
                try {
                    view.showMenu();
                    int choice = input.readMenuChoice(5);
    
                    switch (choice) {
                        case 1 -> unitController.run();
                        case 2 -> tenantController.run();
                        case 3 -> paymentController.run();
                        case 4 -> reportController.run();
                        case 5 -> settingsController.run();
                        case 0 -> {
                            view.showExit();
                            return;
                        }
                    }
                } catch (InputReader.NavigationRequest navigation) {
                    if (navigation.isExit()) {
                        view.showExit();
                        return;
                    }
                }
            }
        } catch (NoSuchElementException exception) {
            view.showInputEnded();
        }
    }
}
