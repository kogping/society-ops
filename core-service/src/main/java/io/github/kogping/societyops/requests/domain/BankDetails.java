package io.github.kogping.societyops.requests.domain;

import java.util.regex.Pattern;

public record BankDetails(String bsb, String accountNumber) {

    private static final Pattern BSB = Pattern.compile("\\d{6}");
    private static final Pattern ACCOUNT = Pattern.compile("\\d{6,10}");

    public BankDetails {
        bsb = bsb == null ? "" : bsb.replaceAll("[\\s-]", "");
        accountNumber = accountNumber == null ? "" : accountNumber.replaceAll("\\s", "");
        if (!BSB.matcher(bsb).matches()) {
            throw new IllegalArgumentException("BSB must be 6 digits");
        }
        if (!ACCOUNT.matcher(accountNumber).matches()) {
            throw new IllegalArgumentException("Account number must be 6 to 10 digits");
        }
    }

    public String maskedAccountNumber() {
        return "*".repeat(accountNumber.length() - 3) + accountNumber.substring(accountNumber.length() - 3);
    }

    @Override
    public String toString() {
        return "BankDetails[bsb=***, accountNumber=" + maskedAccountNumber() + "]";
    }
}
