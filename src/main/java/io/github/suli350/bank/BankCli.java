package io.github.suli350.bank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

/** Menu-driven console interface. Takes reader/printer so it can be tested with scripted input. */
public class BankCli {

    private final Bank bank;
    private final BankRepository repository;
    private final BufferedReader in;
    private final PrintStream out;

    public BankCli(Bank bank, BankRepository repository, BufferedReader in, PrintStream out) {
        this.bank = bank;
        this.repository = repository;
        this.in = in;
        this.out = out;
    }

    public void run() throws IOException {
        out.println("Welcome to " + bank.getName());
        while (true) {
            printMenu();
            String choice = prompt("Choose an option");
            if (choice == null || choice.equals("0")) {
                save();
                out.println("Goodbye!");
                return;
            }
            try {
                handle(choice);
            } catch (BankException e) {
                out.println("  ✗ " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        out.println();
        out.println("  1) Open account        5) Account statement");
        out.println("  2) Deposit             6) List / search accounts");
        out.println("  3) Withdraw            7) Run month end (interest & fees)");
        out.println("  4) Transfer            8) Save");
        out.println("  0) Save and exit");
    }

    private void handle(String choice) throws IOException {
        switch (choice) {
            case "1": openAccount(); break;
            case "2":
                bank.deposit(prompt("Account number"), amount("Amount"));
                out.println("  ✓ Deposited. New balance: " + balanceOf(lastAccount));
                break;
            case "3":
                bank.withdraw(prompt("Account number"), amount("Amount"));
                out.println("  ✓ Withdrawn. New balance: " + balanceOf(lastAccount));
                break;
            case "4": {
                String from = prompt("From account");
                String to = prompt("To account");
                bank.transfer(from, to, amount("Amount"));
                out.println("  ✓ Transferred. " + from.toUpperCase() + " balance: " + balanceOf(from));
                break;
            }
            case "5": out.println(bank.statement(prompt("Account number"))); break;
            case "6": listAccounts(); break;
            case "7":
                bank.runMonthEnd();
                out.println("  ✓ Month end processed for " + bank.getAccounts().size() + " account(s)");
                break;
            case "8": save(); break;
            default: out.println("  Unknown option '" + choice + "'");
        }
    }

    private String lastAccount;

    private void openAccount() throws IOException {
        String owner = prompt("Owner name");
        String typeText = prompt("Type (s = savings, c = checking)");
        AccountType type;
        if (typeText != null && typeText.toLowerCase().startsWith("s")) {
            type = AccountType.SAVINGS;
        } else if (typeText != null && typeText.toLowerCase().startsWith("c")) {
            type = AccountType.CHECKING;
        } else {
            throw new BankException("Type must be 's' or 'c'");
        }
        String initial = prompt("Initial deposit (Enter for none)");
        BigDecimal deposit = initial == null || initial.isBlank() ? null : Money.of(initial);
        Account a = bank.open(owner, type, deposit);
        out.println("  ✓ Opened " + a.getTypeName() + " account " + a.getNumber()
                + " for " + a.getOwner() + " with balance " + Money.format(a.getBalance()));
    }

    private void listAccounts() throws IOException {
        String query = prompt("Owner name contains (Enter for all)");
        List<Account> accounts = query == null || query.isBlank()
                ? List.copyOf(bank.getAccounts()) : bank.findByOwner(query);
        if (accounts.isEmpty()) {
            out.println("  No accounts found");
            return;
        }
        accounts.forEach(a -> out.println("  " + a));
        out.println("  Total held by the bank: " + Money.format(bank.totalDeposits()));
    }

    private void save() throws IOException {
        repository.save(bank);
        out.println("  ✓ Saved to " + repository.getFile());
    }

    private String balanceOf(String number) {
        return Money.format(bank.find(number).getBalance());
    }

    private BigDecimal amount(String label) throws IOException {
        String text = prompt(label);
        if (text == null) {
            throw new BankException("No amount given");
        }
        return Money.of(text.replace("$", "").replace(",", ""));
    }

    private String prompt(String label) throws IOException {
        out.print(label + ": ");
        out.flush();
        String line = in.readLine();
        if (line != null && (label.startsWith("Account number") || label.startsWith("From"))) {
            lastAccount = line.trim();
        }
        return line == null ? null : line.trim();
    }
}
