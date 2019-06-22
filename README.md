# Bank Account System (Java, console)

A menu-driven banking application that shows object-oriented design in Java:
inheritance, abstract classes, custom exceptions, `BigDecimal` money handling,
and persistence with serialization.

## Features

- Open **savings** accounts (2.4 % p.a. interest, max 3 withdrawals per month, no overdraft)
  and **checking** accounts ($500 overdraft, $15 fee if overdrawn at month end)
- Deposit, withdraw and transfer between accounts (transfers are all-or-nothing)
- Full transaction ledger and printable statements
- Month-end processing for interest and fees
- Search accounts by owner name, bank-wide total
- Automatic save to `~/.bank-system/bank.dat`

```
  1) Open account        5) Account statement
  2) Deposit             6) List / search accounts
  3) Withdraw            7) Run month end (interest & fees)
  4) Transfer            8) Save
  0) Save and exit
```

## Design

```
Account (abstract)                  Bank                BankCli
 ├─ SavingsAccount                   ├─ open/find        (menu loop, reads any
 └─ CheckingAccount                  ├─ deposit/withdraw   BufferedReader, so it
Transaction (immutable)              ├─ transfer           is testable)
Money (BigDecimal helpers)           └─ runMonthEnd     BankRepository (save/load)
BankException ← InsufficientFundsException
```

Money is always `BigDecimal` with two decimals and banker's rounding. Never use
`double` for money: `0.1 + 0.2 != 0.3`.

## Build and run

```bash
mvn test
mvn package
java -jar target/bank-account-system-1.0.0.jar
java -jar target/bank-account-system-1.0.0.jar /tmp/demo-bank.dat   # separate data file
```

## Ideas for extending it

- PIN-protected customer login
- Recurring payments / standing orders
- Export statements to CSV
