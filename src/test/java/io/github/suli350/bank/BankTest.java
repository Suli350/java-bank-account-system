package io.github.suli350.bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankTest {

    private Bank bank;
    private Account savings;
    private Account checking;

    @BeforeEach
    void setUp() {
        bank = new Bank("Test Bank");
        bank.setClock(() -> LocalDateTime.of(2019, 6, 15, 12, 0));
        savings = bank.open("Alice Smith", AccountType.SAVINGS, Money.of("500"));
        checking = bank.open("Bob Jones", AccountType.CHECKING, null);
    }

    @Test
    void accountNumbersAreSequentialAndCaseInsensitive() {
        assertEquals("AC1001", savings.getNumber());
        assertEquals("AC1002", checking.getNumber());
        assertEquals(savings, bank.find(" ac1001 "));
        assertThrows(BankException.class, () -> bank.find("AC9999"));
    }

    @Test
    void transferMovesMoneyAndRecordsBothSides() {
        bank.transfer("AC1001", "AC1002", Money.of("120.50"));
        assertEquals(Money.of("379.50"), savings.getBalance());
        assertEquals(Money.of("120.50"), checking.getBalance());
        assertEquals(TransactionType.TRANSFER_OUT, savings.getTransactions().get(1).getType());
        assertEquals(TransactionType.TRANSFER_IN, checking.getTransactions().get(0).getType());
        assertEquals(Money.of("500.00"), bank.totalDeposits());
    }

    @Test
    void failedTransferChangesNothing() {
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer("AC1001", "AC1002", Money.of("600")));
        assertEquals(Money.of("500.00"), savings.getBalance());
        assertEquals(0, checking.getTransactions().size());
        assertThrows(BankException.class, () -> bank.transfer("AC1001", "AC1001", Money.of("1")));
    }

    @Test
    void statementListsTransactions() {
        bank.deposit("AC1001", Money.of("25"));
        String statement = bank.statement("AC1001");
        assertTrue(statement.contains("OPENING_DEPOSIT"));
        assertTrue(statement.contains("$525.00"));
    }

    @Test
    void saveAndLoadRoundTrip() throws IOException {
        Path file = Files.createTempDirectory("bank").resolve("bank.dat");
        BankRepository repo = new BankRepository(file);
        repo.save(bank);
        Bank loaded = repo.loadOrCreate("ignored");
        assertEquals("Test Bank", loaded.getName());
        assertEquals(Money.of("500.00"), loaded.find("AC1001").getBalance());
        loaded.deposit("AC1002", Money.of("1")); // clock is restored after loading
        assertEquals("AC1003", loaded.open("Carol", AccountType.CHECKING, null).getNumber());
    }

    @Test
    void cliScriptedSession() throws IOException {
        String script = String.join("\n",
                "1", "Carol King", "c", "100",      // open AC1003
                "3", "AC1003", "250",               // withdraw into overdraft
                "4", "AC1001", "AC1003", "$1,000",  // too much -> error, loop continues
                "6", "",
                "0") + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Path file = Files.createTempDirectory("bank").resolve("bank.dat");
        new BankCli(bank, new BankRepository(file), new BufferedReader(new StringReader(script)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8)).run();
        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Opened Checking account AC1003"), output);
        assertTrue(output.contains("New balance: -$150.00"), output);
        assertTrue(output.contains("Insufficient funds"), output);
        assertTrue(output.contains("Goodbye!"), output);
        assertTrue(Files.exists(file));
    }
}
