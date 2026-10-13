package io.github.kogping.societyops.requests.adapters.web;

import io.github.kogping.societyops.requests.application.RequestQuery;
import io.github.kogping.societyops.requests.application.RequestService;
import io.github.kogping.societyops.requests.application.Viewer;
import io.github.kogping.societyops.requests.domain.BankDetails;
import io.github.kogping.societyops.requests.domain.Claimant;
import io.github.kogping.societyops.requests.domain.MemberId;
import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.ReimbursementDetails;
import io.github.kogping.societyops.requests.domain.RequestCommand;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.requests.domain.RequestStatus;
import io.github.kogping.societyops.shared.Money;
import io.github.kogping.societyops.shared.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
class RequestController {

    private static final String TREASURER_ROLE = "ROLE_TREASURER";

    private final RequestService service;

    RequestController(RequestService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<RequestResponse> submit(@Valid @RequestBody SubmitReimbursementRequest body, Authentication auth) {
        ReimbursementDetails details = new ReimbursementDetails(
                new Claimant(body.firstName(), body.lastName(), body.email()),
                new BankDetails(body.bsb(), body.accountNumber()),
                body.portfolio(), body.eventName(), body.eventDate(),
                Money.aud(body.amount()), body.declarationSignature());
        Reimbursement created = service.submit(new MemberId(auth.getName()), details);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(RequestResponse.from(created, isTreasurer(auth)));
    }

    @GetMapping("/{id}")
    RequestResponse get(@PathVariable UUID id, Authentication auth) {
        return RequestResponse.from(service.get(new RequestId(id), viewer(auth)), isTreasurer(auth));
    }

    /** {@code mine=true} restricts a treasurer to their own submissions; members are always restricted. */
    @GetMapping
    PageResponse<RequestResponse> list(
            @RequestParam Optional<RequestStatus> status,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "newest") SortOrder order,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication auth) {
        Optional<MemberId> requester = mine ? Optional.of(new MemberId(auth.getName())) : Optional.empty();
        RequestQuery query = new RequestQuery(status, requester, order == SortOrder.newest, page, size);
        boolean showBank = isTreasurer(auth);
        PageResult<Reimbursement> result = service.list(query, viewer(auth));
        return PageResponse.of(result.items().stream().map(r -> RequestResponse.from(r, showBank)).toList(),
                result.page(), result.size(), result.totalElements());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('TREASURER')")
    RequestResponse approve(@PathVariable UUID id, Authentication auth) {
        return act(id, new RequestCommand.Approve(new MemberId(auth.getName())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('TREASURER')")
    RequestResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest body, Authentication auth) {
        return act(id, new RequestCommand.Reject(body.reason(), new MemberId(auth.getName())));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('TREASURER')")
    RequestResponse pay(@PathVariable UUID id, Authentication auth) {
        return act(id, new RequestCommand.MarkPaid(new MemberId(auth.getName())));
    }

    private RequestResponse act(UUID id, RequestCommand command) {
        return RequestResponse.from(service.handle(new RequestId(id), command), true);
    }

    private static Viewer viewer(Authentication auth) {
        return new Viewer(new MemberId(auth.getName()), isTreasurer(auth));
    }

    private static boolean isTreasurer(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(TREASURER_ROLE));
    }

    enum SortOrder { newest, oldest }
}
