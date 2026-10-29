package app.views;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

public class InputReader {
    private final Function<String, String> lineReader;
    private final PrintStream output;

    /** Creates a reader for standard console input and output. */
    public InputReader() {
        this(new Scanner(System.in), System.out);
    }

    /** Creates a reader with a shared scanner and output stream. */
    public InputReader(Scanner scanner, PrintStream output) {
        Objects.requireNonNull(scanner, "Scanner is required.");
        this.output = Objects.requireNonNull(output, "Output stream is required.");
        this.lineReader = prompt -> {
            output.print(prompt);
            output.flush();
            if (!scanner.hasNextLine()) throw new NoSuchElementException("Console input ended.");
            return scanner.nextLine();
        };
    }

    /** Creates a reader using a terminal input adapter while retaining all validation. */
    public InputReader(Function<String, String> lineReader, PrintStream output) {
        this.lineReader = Objects.requireNonNull(lineReader, "Line reader is required.");
        this.output = Objects.requireNonNull(output, "Output stream is required.");
    }

    /** Reads a trimmed line; throws NoSuchElementException when input ends. */
    public String readText(String prompt) {
        String value = lineReader.apply(Objects.requireNonNull(prompt, "Prompt is required.")).trim();
        String command = value.toLowerCase(Locale.ROOT);
        if (command.equals("/back") || command.equals("/cancel") || command.equals("/exit")) {
            throw new NavigationRequest(command);
        }
        return value;
    }

    /** Requests navigation so controllers can abandon incomplete input without saving it. */
    public static final class NavigationRequest extends RuntimeException {
        private final String command;

        /** Carries a navigation command without creating a stack trace for normal navigation. */
        private NavigationRequest(String command) {
            super(null, null, false, false);
            this.command = command;
        }

        /** Returns whether the application should exit instead of showing the main menu. */
        public boolean isExit() { return command.equals("/exit"); }

        /** Returns whether the current section should follow its Back option. */
        public boolean isBack() { return command.equals("/back"); }

        /** Returns whether only the current unfinished transaction should be cancelled. */
        public boolean isCancel() { return command.equals("/cancel"); }
    }
    /** Reads nonblank text, repeating the prompt when the value is empty. */
    public String readRequiredText(String prompt) {
        return readRequiredText(prompt, "Value is required.");
    }

    /** Reads nonblank text using the supplied validation message. */
    public String readRequiredText(String prompt, String errorMessage) {
        while (true) {
            String value = readText(prompt);

            if (!value.isEmpty()) {
                return value;
            }

            output.println(errorMessage);
        }
    }

    /** Reads a numbered menu choice from zero to the supplied maximum. */
    public int readMenuChoice(int maxChoice) {
        while (true) {
            try {
                return readInt("Choice: ", 0, maxChoice, "Invalid choice. Enter a number from 0 to " + maxChoice + ".");
            } catch (NavigationRequest navigation) {
                if (navigation.isBack()) return 0;
                if (navigation.isExit()) throw navigation;
                output.println("No transaction is in progress.");
            }
        }
    }

    /** Reads a whole number within an inclusive range. */
    public int readInt(String prompt, int min, int max) {
        return readInt(prompt, min, max, "Enter a whole number from " + min + " to " + max + ".");
    }

    /** Reads a due day from 1 to 31. */
    public int readDueDay(String prompt) {
        return readInt(prompt, 1, 31, "Due day must be from 1 to 31.");
    }

    /** Reads a due day, keeping the current value when Enter is pressed. */
    public int readOptionalDueDay(String prompt, int currentValue) {
        return readInt(prompt, 1, 31, "Due day must be from 1 to 31.", currentValue);
    }

    /** Reads a billing month, accepting Enter to use the supplied current month. */
    public YearMonth readBillingMonth(String prompt, YearMonth currentMonth) {
        Objects.requireNonNull(currentMonth, "Current month is required.");

        while (true) {
            String value = readText(prompt);

            if (value.isEmpty()) {
                return currentMonth;
            }

            try {
                if (value.matches("[0-9]{4}-[0-9]{2}")) {
                    YearMonth month = YearMonth.parse(value);

                    if (month.getYear() > 0) {
                        return month;
                    }
                }
            } catch (DateTimeParseException ignored) {
                // Invalid calendar input is reported below and requested again.
            }

            output.println("Enter a valid billing month in YYYY-MM format.");
        }
    }

    /** Reads rental days, accepting Enter for the whole selected month. */
    public int readRentalDays(String prompt, int daysInMonth) {
        return readInt(prompt, 1, daysInMonth,
                "Rental days must be from 1 to " + daysInMonth + ".", daysInMonth);
    }

    /** Reads positive money and rounds it to two decimal places. */
    public BigDecimal readMoney(String prompt) {
        return readMoney(prompt, "Amount must be a number greater than 0.");
    }

    /** Reads positive money using the supplied validation message. */
    public BigDecimal readMoney(String prompt, String errorMessage) {
        return readMoney(prompt, errorMessage, null);
    }

    /** Reads positive money, keeping the current value when Enter is pressed. */
    public BigDecimal readOptionalMoney(String prompt, BigDecimal currentValue, String errorMessage) {
        Objects.requireNonNull(currentValue, "Current amount is required.");
        return readMoney(prompt, errorMessage, currentValue);
    }

    /** Repeats money input, optionally accepting Enter to keep a current value. */
    private BigDecimal readMoney(String prompt, String errorMessage, BigDecimal currentValue) {
        while (true) {
            String value = readText(prompt);

            if (value.isEmpty() && currentValue != null) {
                return currentValue;
            }

            try {
                BigDecimal amount = new BigDecimal(value);

                if (amount.signum() > 0) {
                    BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);

                    if (rounded.signum() > 0) {
                        return rounded;
                    }
                }
            } catch (NumberFormatException ignored) {
                // Invalid numeric input is reported below and requested again.
            }

            output.println(errorMessage);
        }
    }

    /** Reads a valid unit number and converts it to uppercase. */
    public String readUnitNumber(String prompt) {
        return readUnitNumber(prompt, null);
    }

    /** Reads a unit number, keeping the current value when Enter is pressed. */
    public String readOptionalUnitNumber(String prompt, String currentValue) {
        Objects.requireNonNull(currentValue, "Current unit number is required.");
        return readUnitNumber(prompt, currentValue);
    }

    /** Repeats unit-number input, optionally accepting Enter to keep its value. */
    private String readUnitNumber(String prompt, String currentValue) {
        while (true) {
            String number = readText(prompt).toUpperCase(Locale.ROOT);

            if (number.isEmpty() && currentValue != null) {
                return currentValue;
            }

            if (number.isEmpty()) {
                output.println("Unit number is required.");
            } else if (number.length() > 10) {
                output.println("Unit number must not exceed 10 characters.");
            } else if (number.chars().anyMatch(Character::isWhitespace)) {
                output.println("Unit number must not contain spaces.");
            } else {
                return number;
            }
        }
    }

    /** Reads a nonblank tenant ID and converts it to uppercase. */
    public String readTenantId(String prompt) {
        while (true) {
            String id = readText(prompt).toUpperCase(Locale.ROOT);

            if (id.isEmpty()) {
                output.println("Tenant ID is required.");
            } else if (id.length() > 10 || id.chars().anyMatch(Character::isWhitespace)) {
                output.println("Tenant ID must have at most 10 characters and no spaces.");
            } else {
                return id;
            }
        }
    }

    /** Reads a required first or last name with at most 30 characters. */
    public String readName(String prompt, String label) {
        return readName(prompt, label, false, null);
    }

    /** Reads a middle name, accepting Enter when no middle name is provided. */
    public String readMiddleName(String prompt) {
        return readName(prompt, "Middle name", true, null);
    }

    /** Reads a first or last name, keeping its current value on Enter. */
    public String readOptionalName(String prompt, String label, String currentValue) {
        Objects.requireNonNull(currentValue, "Current name is required.");
        return readName(prompt, label, false, currentValue);
    }

    /** Keeps the middle name on Enter, or clears it when a single dash is entered. */
    public String readOptionalMiddleName(String prompt, String currentValue) {
        Objects.requireNonNull(currentValue, "Current middle name is required.");
        return readName(prompt, "Middle name", true, currentValue);
    }

    /** Reads an 11-digit contact number starting with 09. */
    public String readContactNumber(String prompt) {
        return readContactNumber(prompt, null);
    }

    /** Reads a contact number, keeping its current value on Enter. */
    public String readOptionalContactNumber(String prompt, String currentValue) {
        Objects.requireNonNull(currentValue, "Current contact number is required.");
        return readContactNumber(prompt, currentValue);
    }

    /** Reads Y or N; returns true for Y and false for N. */
    public boolean readYesNo(String prompt) {
        while (true) {
            String answer = readText(prompt);

            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }

            if (answer.equalsIgnoreCase("N")) {
                return false;
            }

            output.println("Enter Y or N.");
        }
    }

    /** Validates names and handles optional middle names and skipped updates. */
    private String readName(String prompt, String label, boolean middleName, String currentValue) {
        while (true) {
            String name = readText(prompt);

            if (name.isEmpty() && currentValue != null) {
                return currentValue;
            }

            if (middleName && currentValue != null && name.equals("-")) {
                return "";
            }

            if (name.isEmpty()) {
                if (middleName) {
                    return "";
                }
                output.println(label + " is required.");
            } else if (!middleName && name.length() > 30) {
                output.println(label + " must not exceed 30 characters.");
            } else if (!name.matches("[\\p{L} .]+")) {
                output.println(middleName
                        ? "Middle name contains invalid characters."
                        : label + " must contain only letters, spaces, or periods.");
            } else {
                return name;
            }
        }
    }

    /** Validates contact-number text without losing its leading zero. */
    private String readContactNumber(String prompt, String currentValue) {
        while (true) {
            String number = readText(prompt);

            if (number.isEmpty() && currentValue != null) {
                return currentValue;
            }

            if (number.matches("09[0-9]{9}")) {
                return number;
            }

            output.println("Contact number must be 11 digits starting with 09.");
        }
    }

    /** Repeats integer input until it is valid and within the given range. */
    private int readInt(String prompt, int min, int max, String errorMessage) {
        return readInt(prompt, min, max, errorMessage, null);
    }

    /** Repeats integer input, optionally accepting Enter to keep its value. */
    private int readInt(String prompt, int min, int max, String errorMessage, Integer currentValue) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum must not exceed maximum.");
        }

        if (currentValue != null && (currentValue < min || currentValue > max)) {
            throw new IllegalArgumentException("Current value must be within the allowed range.");
        }

        while (true) {
            String value = readText(prompt);

            if (value.isEmpty() && currentValue != null) {
                return currentValue;
            }

            try {
                int number = Integer.parseInt(value);

                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Invalid integer input is reported below and requested again.
            }

            output.println(errorMessage);
        }
    }
}
