package io.github.suli350.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Earns monthly interest; no overdraft; limited number of withdrawals per month. */
public class SavingsAccount extends Account {

    private static final long serialVersionUID = 1L;
    public static final int MAX_WITHDRAWALS_PER_MONTH = 3;

    private final BigDecimal annualRate;  // e.g. 0.024 = 2.4 %
    private int withdrawalsThisMonth;

    public SavingsAccount(String number, String owner, BigDecimal annualRate) {
        super(number, owner);
        if (annualRate.signum() < 0) {
            throw new BankException("Interest rate cannot be negative");
        }
        this.annualRate = annualRate;
    }

    @Override
    public String getTypeName() {
        return "Savings";
    }

    @Override
    public BigDecimal availableToWithdraw() {
        return getBalance();
    }

    @Override
    public void checkCanWithdraw(BigDecimal amount) {
        if (withdrawalsThisMonth >= MAX_WITHDRAWALS_PER_MONTH) {
            throw new BankException("Savings accounts allow only " + MAX_WITHDRAWALS_PER_MONTH
                    + " withdrawals per month");
        }
        super.checkCanWithdraw(amount);
    }

    @Override
    protected void afterWithdrawal() {
        withdrawalsThisMonth++;
    }

    @Override
    public void monthEnd(LocalDateTime when) {
        BigDecimal interest = getBalance().multiply(annualRate)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN);
        if (interest.signum() > 0) {
            record(TransactionType.INTEREST, interest, when,
                    "Monthly interest at " + annualRate.movePointRight(2).stripTrailingZeros().toPlainString() + "% p.a.");
        }
        withdrawalsThisMonth = 0;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public int getWithdrawalsThisMonth() {
        return withdrawalsThisMonth;
    }
}
