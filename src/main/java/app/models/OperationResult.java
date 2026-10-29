package app.models;

/** Represents the outcome of a business operation. */
public enum OperationResult {
    SUCCESS,
    DUPLICATE,
    DUPLICATE_TENANT_ID,
    NOT_FOUND,
    OCCUPIED,
    VACANT,
    INVALID_DETAILS,
    INVALID_AMOUNT,
    OVERPAYMENT,
    NO_MONTH_BALANCE,
    EMPTY_TABLE,
    FULL_TABLE,
    HAS_TENANT,
    HAS_BALANCE
}
