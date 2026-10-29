package app.controllers;

import app.models.RentManager;
import app.views.InputReader;
import app.views.ReportMenuView;

import java.util.Objects;

public class ReportController {
    private final RentManager manager;
    private final InputReader input;
    private final ReportMenuView view;

    /** Connects reports to the shared manager, input, and view. */
    public ReportController(RentManager manager, InputReader input, ReportMenuView view) {
        this.manager = Objects.requireNonNull(manager, "Rent manager is required.");
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Report-menu view is required.");
    }

    /** Runs read-only reports until Back is selected; the app handles ended input. */
    public void run() {
        while (true) {
            try {
                view.showMenu();
                int choice = input.readMenuChoice(4);

                if (choice == 0) {
                    return;
                }

                if (manager.isEmpty()) {
                    view.showMessage("The table is empty.");
                    view.showEmptyComplexity();
                    continue;
                }

                switch (choice) {
                    case 1 -> view.showTable(manager.getTableLayout());
                    case 2 -> view.showUnpaidCount(manager.countUnpaid());
                    case 3 -> view.showVacantCount(manager.countVacant());
                    case 4 -> view.showTotalBalance(manager.totalBalance());
                }

                view.showComplexity(choice == 1);
            } catch (InputReader.NavigationRequest navigation) {
                if (navigation.isExit()) throw navigation;
                if (navigation.isBack()) return;
                view.showMessage("Transaction cancelled.");
            }
        }
    }
}
