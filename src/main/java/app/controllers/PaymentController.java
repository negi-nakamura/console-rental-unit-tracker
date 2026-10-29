package app.controllers;

import app.models.OperationResult;
import app.models.TenantLedger;
import app.models.RentManager;
import app.models.Unit;
import app.views.InputReader;
import app.views.PaymentMenuView;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public class PaymentController {
    private final RentManager manager;
    private final InputReader input;
    private final PaymentMenuView view;

    /** Connects payments to the shared manager, input, and view. */
    public PaymentController(RentManager manager, InputReader input, PaymentMenuView view) {
        this.manager = Objects.requireNonNull(manager, "Rent manager is required.");
        this.input = Objects.requireNonNull(input, "Input reader is required.");
        this.view = Objects.requireNonNull(view, "Payment-menu view is required.");
    }

    /** Runs payment operations until Back is selected; the app handles ended input. */
    public void run() {
        while (true) {
            try {
                view.showMenu();
                int choice = input.readMenuChoice(5);

                switch (choice) {
                    case 1 -> recordPayment();
                    case 2 -> checkBalance();
                    case 3 -> chargeRent();
                    case 4 -> {
                        showPaymentHistory();
                        continue;
                    }
                    case 5 -> {
                        showChargeHistory();
                        continue;
                    }
                    case 0 -> {
                        return;
                    }
                }

                if (choice == 1 || choice == 3) {
                    view.showWriteComplexity();
                } else {
                    view.showComplexity();
                }
            } catch (InputReader.NavigationRequest navigation) {
                if (navigation.isExit()) throw navigation;
                if (navigation.isBack()) return;
                view.showMessage("Transaction cancelled.");
            }
        }
    }

    /** Records a positive payment, requesting another amount after overpayment. */
    private void recordPayment() {
        Unit unit = readTenantUnit();

        if (unit == null) {
            return;
        }

        view.showBalance(unit);

        if (unit.getBalance().signum() == 0) {
            view.showMessage("No balance is owed. No payment recorded.");
            return;
        }

        YearMonth currentMonth = manager.currentBillingMonth();
        YearMonth month = input.readBillingMonth(
                "Enter billing month to pay (YYYY-MM, Enter for " + currentMonth + "): ", currentMonth);
        BigDecimal monthBalance = manager.getBillingBalance(unit.getTenant().getTenantId(), month);
        view.showBillingBalance(month, monthBalance);

        if (monthBalance.signum() == 0) {
            view.showMessage("No unpaid charges for this billing month. No payment recorded.");
            return;
        }

        while (true) {
            BigDecimal amount = input.readMoney(
                    "Enter payment amount: ", "Amount must be a number greater than 0.");
            OperationResult result = manager.recordPaymentByTenantId(
                    unit.getTenant().getTenantId(), month, amount);

            if (result == OperationResult.OVERPAYMENT) {
                showResult(result, "");
                continue;
            }

            if (result == OperationResult.SUCCESS) {
                view.showPaymentRecorded(manager.findTenantLedger(
                        unit.getTenant().getTenantId()).getLastPayment());
            } else {
                showResult(result, "");
            }
            return;
        }
    }

    /** Displays the balance for a tenant found by ID. */
    private void checkBalance() {
        Unit unit = readTenantUnit();

        if (unit != null) {
            view.showBalance(unit);
        }
    }

    /** Previews and confirms a rent charge for one tenant and a chosen day count. */
    private void chargeRent() {
        Unit unit = readTenantUnit();

        if (unit == null) {
            return;
        }

        YearMonth currentMonth = manager.currentBillingMonth();
        YearMonth month = input.readBillingMonth("Enter billing month (YYYY-MM, Enter for " + currentMonth + "): ", currentMonth);
        int rentalDays = input.readRentalDays("Enter rental days (1-" + month.lengthOfMonth() + ", Enter for full month): ", month.lengthOfMonth());
        BigDecimal charge = unit.calculateRentCharge(month, rentalDays);

        if (charge.signum() == 0) {
            view.showMessage("Calculated charge must be at least PHP 0.01. Nothing charged.");
            return;
        }

        view.showChargePreview(unit, month, rentalDays, charge);

        if (!input.readYesNo("Confirm this tenant's rent charge? (Y/N): ")) {
            view.showMessage("Rent charge cancelled.");
            return;
        }

        OperationResult result = manager.chargeRentByTenantId(unit.getTenant().getTenantId(), month, rentalDays);

        if (result == OperationResult.SUCCESS) {
            view.showRentCharged(manager.findTenantLedger(
                    unit.getTenant().getTenantId()).getLastCharge(), unit.getBalance());
        } else {
            showResult(result, "");
        }
    }

    /** Displays successful payments, including history retained after move-out. */
    private void showPaymentHistory() {
        TenantLedger ledger = readHistoryAccount();

        if (ledger == null) {
            view.showComplexity();
            return;
        }

        view.showPaymentHistoryHeader(ledger.getTenantId(), ledger.getTenantName());
        if (ledger.getPaymentCount() == 0) {
            view.showMessage("No payment history found for this tenant.");
            view.showPaymentHistoryTotals(0, ledger.getTotalPaid());
            view.showComplexity();
            return;
        }

        ledger.forEachPayment(payment -> view.showPaymentHistoryRow(payment));
        view.showPaymentHistoryTotals(ledger.getPaymentCount(), ledger.getTotalPaid());
        view.showHistoryComplexity();
    }

    /** Displays recorded rent charges and explicitly imported opening balances. */
    private void showChargeHistory() {
        TenantLedger ledger = readHistoryAccount();

        if (ledger == null) {
            view.showComplexity();
            return;
        }

        view.showChargeHistoryHeader(ledger.getTenantId(), ledger.getTenantName());
        if (ledger.getChargeCount() == 0) {
            view.showMessage("No charge history found for this tenant.");
            view.showChargeHistoryTotals(0, ledger.getTotalCharged());
            view.showComplexity();
            return;
        }

        ledger.forEachCharge(charge -> view.showChargeHistoryRow(charge));
        view.showChargeHistoryTotals(ledger.getChargeCount(), ledger.getTotalCharged());
        view.showHistoryComplexity();
    }

    /** Finds a current or archived account without requiring an occupied unit. */
    private TenantLedger readHistoryAccount() {
        if (manager.tenantAccountCount() == 0) {
            view.showMessage("No tenant records are available.");
            return null;
        }

        String tenantId = input.readTenantId("Enter tenant ID: ");
        TenantLedger ledger = manager.findTenantLedger(tenantId);
        if (ledger == null) {
            view.showMessage("Tenant not found.");
        }
        return ledger;
    }

    /** Checks for records before asking for the tenant ID used by payments. */
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
            case EMPTY_TABLE -> "The table is empty.";
            case VACANT -> "Unit has no tenant. No payment recorded.";
            case INVALID_AMOUNT -> "Amount must be greater than 0.";
            case INVALID_DETAILS -> "Billing month or rent charge details are invalid.";
            case NO_MONTH_BALANCE -> "No unpaid charges for this billing month. No payment recorded.";
            case OVERPAYMENT -> "Payment is more than the balance owed for this billing month.";
            default -> "The payment operation could not be completed.";
        };

        view.showMessage(message);
    }
}
