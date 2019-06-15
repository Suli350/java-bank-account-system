package io.github.suli350.bank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Service layer: all operations go through here so rules are enforced in one place. */
public class Bank implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final BigDecimal DEFAULT_SAVINGS_RATE = new BigDecimal("0.024");
    public static final BigDecimal DEFAULT_OVERDRAFT = Money.of("500.00");

    private final String name;
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private int nextAccountNumber = 1001;
    private transient Supplier<LocalDateTime> clock = LocalDateTime::now;

    public Bank(String name) {
        this.name = name;
    }

    public void setClock(Supplier<LocalDateTime> clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        if (clock == null) {
            clock = LocalDateTime::now; // transient field after deserialization
        }
        return clock.get();
    }

    public Account open(String owner, AccountType type, BigDecimal initialDeposit) {
        String number = "AC" + nextAccountNumber;
        Account account = type == AccountType.SAVINGS
                ? new SavingsAccount(number, owner, DEFAULT_SAVINGS_RATE)
                : new CheckingAccount(number, owner, DEFAULT_OVERDRAFT);
        if (initialDeposit != null && initialDeposit.signum() != 0) {
            BigDecimal amount = Money.requirePositive(initialDeposit);
            account.record(TransactionType.OPENING_DEPOSIT, amount, now(), "Opening deposit");
        }
        nextAccountNumber++;
        accounts.put(number, account);
        return account;
    }

    public Account find(String number) {
        Account account = accounts.get(number == null ? null : number.trim().toUpperCase());
        if (account == null) {
            throw new BankException("No account with number " + number);
        }
        return account;
    }

    public void deposit(String number, BigDecimal amount) {
        find(number).deposit(amount, now(), "Cash deposit");
    }

    public void withdraw(String number, BigDecimal amount) {
        find(number).withdraw(amount, now(), "Cash withdrawal");
    }

    /** All checks happen before any money moves, so a failed transfer changes nothing. */
    public void transfer(String fromNumber, String toNumber, BigDecimal amount) {
        Account from = find(fromNumber);
        Account to = find(toNumber);
        if (from == to) {
            throw new BankException("Cannot transfer to the same account");
        }
        BigDecimal value = Money.requirePositive(amount);
        from.checkCanWithdraw(value);
        LocalDateTime when = now();
        from.record(TransactionType.TRANSFER_OUT, value.negate(), when, "Transfer to " + to.getNumber());
        from.afterWithdrawal();
        to.record(TransactionType.TRANSFER_IN, value, when, "Transfer from " + from.getNumber());
    }

    public void runMonthEnd() {
        LocalDateTime when = now();
        for (Account a : accounts.values()) {
            a.monthEnd(when);
        }
    }

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public List<Account> findByOwner(String query) {
        String q = query.toLowerCase().trim();
        return accounts.values().stream()
                .filter(a -> a.getOwner().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public BigDecimal totalDeposits() {
        return accounts.values().stream().map(Account::getBalance)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    public String getName() {
        return name;
    }

    public String statement(String number) {
        Account a = find(number);
        List<String> lines = new ArrayList<>();
        lines.add(name + " — statement for " + a.getNumber() + " (" + a.getTypeName() + ", " + a.getOwner() + ")");
        lines.add(String.format("%-16s %-15s %12s %12s  %s", "Date", "Type", "Amount", "Balance", "Description"));
        lines.add("-".repeat(80));
        for (Transaction t : a.getTransactions()) {
            lines.add(t.toString());
        }
        lines.add("-".repeat(80));
        lines.add("Current balance: " + Money.format(a.getBalance())
                + "   Available: " + Money.format(a.availableToWithdraw()));
        return String.join(System.lineSeparator(), lines);
    }
}
