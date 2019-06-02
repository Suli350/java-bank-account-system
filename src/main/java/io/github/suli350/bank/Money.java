package io.github.suli350.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Money helpers. Always BigDecimal with 2 decimals, never double. */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal of(String amount) {
        try {
            return normalize(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new BankException("'" + amount + "' is not a valid amount");
        }
    }

    public static BigDecimal of(double amount) {
        return normalize(BigDecimal.valueOf(amount));
    }

    public static BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN); // banker's rounding
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BankException("Amount must be greater than zero");
        }
        if (amount.scale() > 2 && amount.stripTrailingZeros().scale() > 2) {
            throw new BankException("Amount cannot have more than 2 decimal places");
        }
        return normalize(amount);
    }

    public static String format(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }
}
