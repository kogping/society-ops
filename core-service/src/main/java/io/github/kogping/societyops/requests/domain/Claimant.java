package io.github.kogping.societyops.requests.domain;

import java.util.regex.Pattern;

public record Claimant(String firstName, String lastName, String email) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Claimant {
        firstName = requireText(firstName, "First name", 100);
        lastName = requireText(lastName, "Last name", 100);
        email = requireText(email, "Email", 254);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Email is not a valid address");
        }
    }

    static String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
