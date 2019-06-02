package io.github.suli350.bank;

import java.math.BigDecimal;

public class InsufficientFundsException extends BankException {

    private static final long serialVersionUID = 1L;
    private final BigDecimal available;

    public InsufficientFundsException(String accountNumber, BigDecimal requested, BigDecimal available) {
        super("Insufficient funds in " + accountNumber + ": requested " + Money.format(requested)
                + ", available " + Money.format(available));
        this.available = available;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}
