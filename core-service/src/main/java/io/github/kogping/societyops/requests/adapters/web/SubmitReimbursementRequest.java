package io.github.kogping.societyops.requests.adapters.web;

import io.github.kogping.societyops.requests.domain.Portfolio;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitReimbursementRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\d{3}-?\\d{3}", message = "must be 6 digits, e.g. 062-000") String bsb,
        @NotBlank @Pattern(regexp = "\\d{6,10}", message = "must be 6 to 10 digits") String accountNumber,
        @NotNull Portfolio portfolio,
        @NotBlank @Size(max = 200) String eventName,
        @NotNull LocalDate eventDate,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @AssertTrue(message = "the declaration must be accepted") boolean declarationAccepted,
        @NotBlank @Size(max = 200) String declarationSignature) {
}
