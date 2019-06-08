package io.github.suli350.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Everyday account with an overdraft limit and a fee when overdrawn at month end. */
public class CheckingAccount extends Account {

    private static final long serialVersionUID = 1L;
    public static final BigDecimal OVERDRAWN_FEE = Money.of("15.00");

    private final BigDecimal overdraftLimit;

    public CheckingAccount(String number, String owner, BigDecimal overdraftLimit) {
        super(number, owner);
        if (overdraftLimit.signum() < 0) {
            throw new BankException("Overdraft limit cannot be negative");
        }
        this.overdraftLimit = Money.normalize(overdraftLimit);
    }

    @Override
    public String getTypeName() {
        return "Checking";
    }

    @Override
    public BigDecimal availableToWithdraw() {
        return getBalance().add(overdraftLimit);
    }

    @Override
    public void monthEnd(LocalDateTime when) {
        if (getBalance().signum() < 0) {
            record(TransactionType.FEE, OVERDRAWN_FEE.negate(), when, "Overdrawn account fee");
        }
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }
}
