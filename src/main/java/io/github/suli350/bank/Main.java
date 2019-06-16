package io.github.suli350.bank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path file = args.length > 0
                ? Paths.get(args[0])
                : Paths.get(System.getProperty("user.home"), ".bank-system", "bank.dat");
        BankRepository repository = new BankRepository(file);
        Bank bank = repository.loadOrCreate("Java Community Bank");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new BankCli(bank, repository, in, System.out).run();
    }
}
