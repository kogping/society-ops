package io.github.kogping.societyops.identity;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("societyops.dev-users")
@Validated
record DevUsersProperties(@NotBlank String memberPassword, @NotBlank String treasurerPassword) {
}
