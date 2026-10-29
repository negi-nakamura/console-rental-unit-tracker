package app.models;

import java.util.Locale;

public class Tenant {
    private final String tenantId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String contactNumber;

    /** Creates a tenant with a fixed ID and validated personal details. */
    public Tenant(String tenantId, String firstName, String middleName, String lastName, String contactNumber) {
        String id = tenantId == null ? "" : tenantId.trim().toUpperCase(Locale.ROOT);

        if (id.isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required.");
        }

        if (id.length() > 10 || id.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "Tenant ID must have at most 10 characters and no spaces.");
        }

        this.tenantId = id;
        setFirstName(firstName);
        setMiddleName(middleName);
        setLastName(lastName);
        setContactNumber(contactNumber);
    }

    /** Returns the tenant ID, which cannot be changed after creation. */
    public String getTenantId() {
        return tenantId;
    }

    /** Returns the first name. */
    public String getFirstName() {
        return firstName;
    }

    /** Validates and updates the first name. */
    public void setFirstName(String firstName) {
        this.firstName = validateRequiredName(firstName, "First name");
    }

    /** Returns the middle name, which may be blank. */
    public String getMiddleName() {
        return middleName;
    }

    /** Updates the optional middle name. */
    public void setMiddleName(String middleName) {
        String name = middleName == null ? "" : middleName.trim();

        if (!name.isEmpty() && !name.matches("[\\p{L} .]+")) {
            throw new IllegalArgumentException("Middle name contains invalid characters.");
        }

        this.middleName = name;
    }

    /** Returns the last name. */
    public String getLastName() {
        return lastName;
    }

    /** Validates and updates the last name. */
    public void setLastName(String lastName) {
        this.lastName = validateRequiredName(lastName, "Last name");
    }

    /** Returns the contact number. */
    public String getContactNumber() {
        return contactNumber;
    }

    /** Requires an 11-digit contact number starting with 09. */
    public void setContactNumber(String contactNumber) {
        String number = contactNumber == null ? "" : contactNumber.trim();

        if (!number.matches("09[0-9]{9}")) {
            throw new IllegalArgumentException("Contact number must be 11 digits starting with 09.");
        }

        this.contactNumber = number;
    }

    /** Returns the name in the format Last, First M. */
    public String getFullName() {
        String name = lastName + ", " + firstName;

        if (!middleName.isEmpty()) {
            name += " " + Character.toUpperCase(middleName.charAt(0)) + ".";
        }

        return name;
    }

    /** Checks that a required name contains valid characters and at most 30 characters. */
    private String validateRequiredName(String value, String label) {
        String name = value == null ? "" : value.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }

        if (name.length() > 30) {
            throw new IllegalArgumentException(label + " must not exceed 30 characters.");
        }

        if (!name.matches("[\\p{L} .]+")) {
            throw new IllegalArgumentException(label + " must contain only letters, spaces, or periods.");
        }

        return name;
    }

    /** Returns the tenant's ID, name, and contact number for display. */
    @Override
    public String toString() {
        return tenantId + " | " + getFullName() + " | Contact: " + contactNumber;
    }
}
