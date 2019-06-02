package io.github.suli350.bank;

/** Base class for every business-rule violation (bad amount, unknown account, no funds...). */
public class BankException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BankException(String message) {
        super(message);
    }
}
