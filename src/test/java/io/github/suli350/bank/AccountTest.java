package io.github.suli350.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2019, 6, 1, 9, 30);

    private static BigDecimal m(String s) {
        return Money.of(s);
    }

    @Test
    void depositAndWithdrawUpdateBalanceAndLedger() {
        CheckingAccount a = new CheckingAccount("AC1", "Ann", m("0"));
        a.deposit(m("100.00"), NOW, "cash");
        a.withdraw(m("30.25"), NOW, "atm");
        assertEquals(m("69.75"), a.getBalance());
        assertEquals(2, a.getTransactions().size());
        assertEquals(m("-30.25"), a.getTransactions().get(1).getAmount());
        assertEquals(m("69.75"), a.getTransactions().get(1).getBalanceAfter());
    }

    @Test
    void invalidAmountsAreRejected() {
        CheckingAccount a = new CheckingAccount("AC1", "Ann", m("0"));
        assertThrows(BankException.class, () -> a.deposit(m("0"), NOW, ""));
        assertThrows(BankException.class, () -> a.deposit(m("-5"), NOW, ""));
        assertThrows(BankException.class, () -> a.deposit(new BigDecimal("1.001"), NOW, ""));
        assertThrows(BankException.class, () -> Money.of("abc"));
    }

    @Test
    void checkingCanUseOverdraftButNotBeyond() {
        CheckingAccount a = new CheckingAccount("AC1", "Ann", m("100"));
        a.deposit(m("50"), NOW, "");
        a.withdraw(m("140"), NOW, "");
        assertEquals(m("-90.00"), a.getBalance());
        InsufficientFundsException e = assertThrows(InsufficientFundsException.class,
                () -> a.withdraw(m("20"), NOW, ""));
        assertEquals(m("10.00"), e.getAvailable());
    }

    @Test
    void checkingChargesFeeWhenOverdrawnAtMonthEnd() {
        CheckingAccount a = new CheckingAccount("AC1", "Ann", m("100"));
        a.withdraw(m("10"), NOW, "");
        a.monthEnd(NOW);
        assertEquals(m("-25.00"), a.getBalance());
    }

    @Test
    void savingsHasNoOverdraftAndLimitsWithdrawals() {
        SavingsAccount s = new SavingsAccount("AC2", "Bob", new BigDecimal("0.024"));
        s.deposit(m("1000"), NOW, "");
        assertThrows(InsufficientFundsException.class, () -> s.withdraw(m("1000.01"), NOW, ""));
        s.withdraw(m("1"), NOW, "");
        s.withdraw(m("1"), NOW, "");
        s.withdraw(m("1"), NOW, "");
        assertThrows(BankException.class, () -> s.withdraw(m("1"), NOW, ""));
        s.monthEnd(NOW);                   // resets the counter
        s.withdraw(m("1"), NOW, "");
        assertEquals(1, s.getWithdrawalsThisMonth());
    }

    @Test
    void savingsInterestIsMonthlyShareOfAnnualRate() {
        SavingsAccount s = new SavingsAccount("AC2", "Bob", new BigDecimal("0.024"));
        s.deposit(m("1000"), NOW, "");
        s.monthEnd(NOW);
        assertEquals(m("1002.00"), s.getBalance());
        assertEquals(TransactionType.INTEREST, s.getTransactions().get(1).getType());
    }

    @Test
    void moneyFormatting() {
        assertEquals("$1,234.50", Money.format(m("1234.5")));
        assertEquals(m("2.68"), Money.of("2.675")); // half-even rounding
    }
}
