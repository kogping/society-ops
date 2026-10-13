package io.github.kogping.societyops;

import io.github.kogping.societyops.requests.application.RequestRepository;
import io.github.kogping.societyops.requests.domain.RequestCommand;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.requests.domain.MemberId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestFlowIntegrationTest {

    private static final String BODY = """
            {"firstName":"Alex","lastName":"Nguyen","email":"alex@example.com","bsb":"062-000",
             "accountNumber":"12345678","portfolio":"SOCIALS","eventName":"Welcome BBQ",
             "eventDate":"2026-01-10","amount":84.50,"declarationAccepted":true,
             "declarationSignature":"Alex Nguyen"}""";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired RequestRepository repository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JsonMapper json;

    @Test
    void submitApprovePay() throws Exception {
        String id = submit("member", "test-member-pw");

        mvc.perform(get("/api/v1/requests/{id}", id).with(httpBasic("member", "test-member-pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.bank.accountNumber").value("*****678"));

        mvc.perform(post("/api/v1/requests/{id}/pay", id).with(treasurer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("SUBMITTED"));

        mvc.perform(post("/api/v1/requests/{id}/approve", id).with(treasurer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").value("treasurer"))
                .andExpect(jsonPath("$.bank.accountNumber").value("12345678"));

        mvc.perform(post("/api/v1/requests/{id}/approve", id).with(treasurer()))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/requests/{id}/pay", id).with(treasurer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidBy").value("treasurer"));

        mvc.perform(get("/api/v1/requests/{id}", id).with(httpBasic("member", "test-member-pw")))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void rejectStoresTheReason() throws Exception {
        String id = submit("member", "test-member-pw");

        mvc.perform(post("/api/v1/requests/{id}/reject", id).with(treasurer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"No receipt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("No receipt"));
    }

    @Test
    void membersOnlySeeTheirOwnRequests() throws Exception {
        String treasurersOwn = submit("treasurer", "test-treasurer-pw");

        mvc.perform(get("/api/v1/requests/{id}", treasurersOwn).with(httpBasic("member", "test-member-pw")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/requests").param("size", "100").with(httpBasic("member", "test-member-pw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + treasurersOwn + "')]").isEmpty());

        mvc.perform(get("/api/v1/requests").param("size", "100").with(treasurer()))
                .andExpect(jsonPath("$.content[?(@.id=='" + treasurersOwn + "')]").isNotEmpty());
    }

    @Test
    void queueIsFilteredByStatusAndPaged() throws Exception {
        for (int i = 0; i < 3; i++) {
            submit("member", "test-member-pw");
        }
        mvc.perform(get("/api/v1/requests").param("status", "SUBMITTED").param("size", "2")
                        .param("order", "oldest").with(treasurer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void meReportsTheLoggedInUserAndRoles() throws Exception {
        mvc.perform(get("/api/v1/me").with(treasurer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("treasurer"))
                .andExpect(jsonPath("$.roles[0]").value("MEMBER"))
                .andExpect(jsonPath("$.roles[1]").value("TREASURER"));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(get("/api/v1/requests").with(httpBasic("member", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void databaseRejectsInvalidRowsEvenIfTheApplicationWouldNot() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO requests (id, requester, status, claimant_first_name, claimant_last_name, claimant_email,
                    bank_bsb, bank_account_number, portfolio, event_name, event_date, amount, currency,
                    declaration_signature, created_at, updated_at, version)
                VALUES (?, 'm', 'SUBMITTED', 'A', 'B', 'a@b.co', '062000', '12345678', 'IT', 'e',
                    '2026-01-01', 0, 'AUD', 'A B', now(), now(), 0)""", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("requests_amount_positive");
    }

    @Test
    void concurrentUpdatesAreCaughtByOptimisticLocking() throws Exception {
        String id = submit("member", "test-member-pw");
        RequestId requestId = RequestId.of(id);
        MemberId by = new MemberId("treasurer");
        TransactionTemplate outer = new TransactionTemplate(txManager);
        TransactionTemplate inner = new TransactionTemplate(txManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            var first = repository.findById(requestId).orElseThrow();
            // A second treasurer approves and commits while the first still holds the old version.
            inner.executeWithoutResult(s -> {
                var second = repository.findById(requestId).orElseThrow();
                second.handle(new RequestCommand.Approve(by), java.time.Instant.now());
                repository.save(second);
            });
            first.handle(new RequestCommand.Reject("Too late", by), java.time.Instant.now());
            repository.save(first);
        })).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(jdbc.queryForObject("SELECT status FROM requests WHERE id = ?", String.class, UUID.fromString(id)))
                .isEqualTo("APPROVED");
    }

    /**
     * The committed web/openapi.json is what the front end's TypeScript types are generated from.
     * Fails when the API changes without regenerating it. Regenerate with:
     * {@code UPDATE_OPENAPI=true ./gradlew :core-service:test --tests '*RequestFlowIntegrationTest'}.
     */
    @Test
    void committedOpenApiSpecMatchesTheRunningApi() throws Exception {
        MvcResult result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode actual = json.readTree(result.getResponse().getContentAsString());
        Path file = Path.of("..", "web", "openapi.json");

        if ("true".equals(System.getenv("UPDATE_OPENAPI"))) {
            Files.writeString(file, json.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n");
        }
        assertThat(file).as("web/openapi.json exists; run with UPDATE_OPENAPI=true to create it").exists();
        assertThat(json.readTree(Files.readString(file)))
                .as("web/openapi.json is stale; run with UPDATE_OPENAPI=true to regenerate")
                .isEqualTo(actual);
    }

    private String submit(String user, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/requests").with(httpBasic(user, password))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(location).contains("/api/v1/requests/");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor treasurer() {
        return httpBasic("treasurer", "test-treasurer-pw");
    }
}
