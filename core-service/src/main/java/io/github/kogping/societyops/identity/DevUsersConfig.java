package io.github.kogping.societyops.identity;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableConfigurationProperties(DevUsersProperties.class)
class DevUsersConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(DevUsersProperties props, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("member").password(encoder.encode(props.memberPassword())).roles("MEMBER").build(),
                User.withUsername("treasurer").password(encoder.encode(props.treasurerPassword()))
                        .roles("TREASURER", "MEMBER").build());
    }
}
