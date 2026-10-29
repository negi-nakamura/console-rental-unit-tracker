package app;

import app.controllers.AppController;
import app.controllers.PaymentController;
import app.controllers.ReportController;
import app.controllers.SettingsController;
import app.controllers.TenantController;
import app.controllers.UnitController;
import app.models.RentManager;
import app.utils.DataLoader;
import app.views.InputReader;
import app.views.MainMenuView;
import app.views.PaymentMenuView;
import app.views.ReportMenuView;
import app.views.SettingsMenuView;
import app.views.TenantMenuView;
import app.views.UnitMenuView;
import app.views.TuiView;
import java.io.PrintStream;
import java.util.Arrays;

public class Main {

    /** Loads starting units and launches the console application. */
    public static void main(String[] args) {
        boolean console = Arrays.asList(args).contains("--console");
        boolean tui = Arrays.asList(args).contains("--tui");
        if (!console && (tui || System.console() != null)) {
            TuiView.run(Main::run);
        } else {
            run(new InputReader(), System.out);
        }
    }

    /** Connects the existing MVC components to either terminal or plain-console views. */
    private static void run(InputReader input, PrintStream output) {
        RentManager manager = new RentManager();
        DataLoader.load(manager);


        MainMenuView view = new MainMenuView(output);
        UnitMenuView unitView = new UnitMenuView(output);
        UnitController unitController = new UnitController(manager, input, unitView);
        TenantMenuView tenantView = new TenantMenuView(output);
        TenantController tenantController = new TenantController(manager, input, tenantView);
        PaymentMenuView paymentView = new PaymentMenuView(output);
        PaymentController paymentController = new PaymentController(manager, input, paymentView);
        ReportMenuView reportView = new ReportMenuView(output);
        ReportController reportController = new ReportController(manager, input, reportView);
        SettingsMenuView settingsView = new SettingsMenuView(output);
        SettingsController settingsController = new SettingsController(manager, input, settingsView);

        AppController controller = new AppController(input, view,
                unitController, tenantController, paymentController, reportController, settingsController);
        controller.run();
    }
}
