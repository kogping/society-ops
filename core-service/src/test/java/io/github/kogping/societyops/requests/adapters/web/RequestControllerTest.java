package io.github.kogping.societyops.requests.adapters.web;

import io.github.kogping.societyops.identity.SecurityConfig;
import io.github.kogping.societyops.requests.application.RequestNotFoundException;
import io.github.kogping.societyops.requests.application.RequestService;
import io.github.kogping.societyops.requests.domain.BankDetails;
import io.github.kogping.societyops.requests.domain.Claimant;
import io.github.kogping.societyops.requests.domain.IllegalTransitionException;
import io.github.kogping.societyops.requests.domain.MemberId;
import io.github.kogping.societyops.requests.domain.Portfolio;
import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.ReimbursementDetails;
import io.github.kogping.societyops.requests.domain.RequestCommand;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.requests.domain.RequestStatus;
import io.github.kogping.societyops.shared.Money;
import io.github.kogping.societyops.shared.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@Import(SecurityConfig.class)
class RequestControllerTest {

    private static final String PROBLEM = "application/problem+json";
    private static final String VALID_BODY = """
            {"firstName":"Alex","lastName":"Nguyen","email":"alex@example.com","bsb":"062-000",
             "accountNumber":"12345678","portfolio":"SOCIALS","eventName":"Welcome BBQ",
             "eventDate":"2026-10-10","amount":84.50,"declarationAccepted":true,
             "declarationSignature":"Alex Nguyen"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RequestService service;

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void submitReturns201WithLocationAndMaskedBankDetails() throws Exception {
        Reimbursement created = sample(RequestStatus.SUBMITTED);
        when(service.submit(any(), any())).thenReturn(created);

        mvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/requests/" + created.id()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.amount").value(84.50))
                .andExpect(jsonPath("$.bank.accountNumber").value("*****678"));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void submitRejectsInvalidBodyWithProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"","lastName":"N","email":"nope","bsb":"1","accountNumber":"2",
                         "eventName":"x","amount":-5,"declarationAccepted":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='bsb')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='amount')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='declarationAccepted')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='portfolio')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void domainRuleViolationsAre400() throws Exception {
        when(service.submit(any(), any())).thenThrow(new IllegalArgumentException("Event date cannot be in the future"));

        mvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.detail").value("Event date cannot be in the future"));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/v1/requests").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void unknownRequestIs404ProblemDetails() throws Exception {
        RequestId id = RequestId.newId();
        when(service.get(any(), any())).thenThrow(new RequestNotFoundException(id));

        mvc.perform(get("/api/v1/requests/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/v1/requests/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    @Test
    @WithMockUser(username = "treasurer", roles = {"TREASURER", "MEMBER"})
    void illegalTransitionIs409() throws Exception {
        RequestId id = RequestId.newId();
        when(service.handle(any(), any(RequestCommand.Approve.class)))
                .thenThrow(new IllegalTransitionException(RequestStatus.PAID, new RequestCommand.Approve(new MemberId("treasurer"))));

        mvc.perform(post("/api/v1/requests/{id}/approve", id))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.currentStatus").value("PAID"));
    }

    @Test
    @WithMockUser(username = "treasurer", roles = {"TREASURER", "MEMBER"})
    void versionConflictIs409() throws Exception {
        when(service.handle(any(), any())).thenThrow(new OptimisticLockingFailureException("stale"));

        mvc.perform(post("/api/v1/requests/{id}/pay", RequestId.newId()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.title").value("Version conflict"));
    }

    @Test
    @WithMockUser(username = "treasurer", roles = {"TREASURER", "MEMBER"})
    void approveReturnsUpdatedRequestWithFullBankDetails() throws Exception {
        when(service.handle(any(), any())).thenReturn(sample(RequestStatus.SUBMITTED));

        mvc.perform(post("/api/v1/requests/{id}/approve", RequestId.newId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bank.accountNumber").value("12345678"));
    }

    @Test
    @WithMockUser(username = "treasurer", roles = {"TREASURER", "MEMBER"})
    void rejectNeedsAReason() throws Exception {
        mvc.perform(post("/api/v1/requests/{id}/reject", RequestId.newId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void membersCannotRunTreasurerActions() throws Exception {
        for (String action : List.of("approve", "reject", "pay")) {
            mvc.perform(post("/api/v1/requests/{id}/" + action, RequestId.newId())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM));
        }
        verifyNoInteractions(service);
    }

    @Test
    void anonymousCallsGet401ProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void listReturnsAPageAndValidatesPaging() throws Exception {
        when(service.list(any(), any())).thenReturn(new PageResult<>(List.of(sample(RequestStatus.SUBMITTED)), 0, 20, 41));

        mvc.perform(get("/api/v1/requests").param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(41))
                .andExpect(jsonPath("$.totalPages").value(3));

        mvc.perform(get("/api/v1/requests").param("size", "1000")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/requests").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/requests").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }

    private static Reimbursement sample(RequestStatus status) {
        ReimbursementDetails details = new ReimbursementDetails(
                new Claimant("Alex", "Nguyen", "alex@example.com"), new BankDetails("062-000", "12345678"),
                Portfolio.SOCIALS, "Welcome BBQ", LocalDate.of(2026, 10, 10),
                Money.aud(new BigDecimal("84.50")), "Alex Nguyen");
        Instant now = Instant.parse("2026-10-14T02:00:00Z");
        return Reimbursement.restore(RequestId.newId(), new MemberId("member"), details, status, now, now,
                null, null, null, null, null);
    }
}
