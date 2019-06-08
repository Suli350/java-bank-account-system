package io.github.suli350.bank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Common account behaviour. Subclasses decide how far the balance may go
 * ({@link #availableToWithdraw()}) and what happens at month end.
 */
public abstract class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String number;
    private final String owner;
    private BigDecimal balance = Money.ZERO;
    private final List<Transaction> transactions = new ArrayList<>();
    private long nextTransactionId = 1;

    protected Account(String number, String owner) {
        if (owner == null || owner.isBlank()) {
            throw new BankException("Owner name is required");
        }
        this.number = number;
        this.owner = owner.trim();
    }

    public abstract String getTypeName();

    /** Maximum amount that can be withdrawn right now. */
    public abstract BigDecimal availableToWithdraw();

    /** Apply interest / fees. Called once per month by the bank. */
    public abstract void monthEnd(LocalDateTime when);

    public void deposit(BigDecimal amount, LocalDateTime when, String description) {
        record(TransactionType.DEPOSIT, Money.requirePositive(amount), when, description);
    }

    public void withdraw(BigDecimal amount, LocalDateTime when, String description) {
        BigDecimal value = Money.requirePositive(amount);
        checkCanWithdraw(value);
        record(TransactionType.WITHDRAWAL, value.negate(), when, description);
        afterWithdrawal();
    }

    /** Throws if the withdrawal is not allowed; does not change anything. */
    public void checkCanWithdraw(BigDecimal amount) {
        if (amount.compareTo(availableToWithdraw()) > 0) {
            throw new InsufficientFundsException(number, amount, availableToWithdraw());
        }
    }

    /** Hook for subclasses that count withdrawals. */
    protected void afterWithdrawal() {
    }

    protected final void record(TransactionType type, BigDecimal signedAmount, LocalDateTime when,
                                String description) {
        balance = Money.normalize(balance.add(signedAmount));
        transactions.add(new Transaction(nextTransactionId++, type, Money.normalize(signedAmount),
                balance, when, description == null ? "" : description));
    }

    public String getNumber() { return number; }
    public String getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return String.format("%s  %-9s %-20s %12s", number, getTypeName(), owner, Money.format(balance));
    }
}
