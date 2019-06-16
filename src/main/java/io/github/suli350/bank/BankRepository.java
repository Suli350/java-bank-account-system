package io.github.suli350.bank;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Saves the whole bank with Java serialization (atomic write via temp file). */
public class BankRepository {

    private final Path file;

    public BankRepository(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    public Bank loadOrCreate(String bankName) throws IOException {
        if (!Files.exists(file)) {
            return new Bank(bankName);
        }
        try (InputStream in = Files.newInputStream(file); ObjectInputStream ois = new ObjectInputStream(in)) {
            return (Bank) ois.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException("Data file is not a bank file: " + file, e);
        }
    }

    public void save(Bank bank) throws IOException {
        Path dir = file.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "bank", ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp); ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(bank);
        }
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
