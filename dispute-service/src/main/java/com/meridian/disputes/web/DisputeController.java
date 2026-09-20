package com.meridian.disputes.web;

import com.meridian.disputes.application.DisputeDetails;
import com.meridian.disputes.application.DisputeService;
import com.meridian.disputes.application.FileDisputeCommand;
import com.meridian.disputes.domain.Dispute;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The dispute API. {@code X-Actor} records WHO acted, for the audit trail.
 * Today it is "cardholder:CH-1001" or "analyst:a.shah"; from Part 2 it will also be
 * "agent:intake". Part 9 replaces this trust-me header with real authentication.
 */
@RestController
@RequestMapping("/api/disputes")
class DisputeController {

    private static final String ACTOR = "X-Actor";

    private final DisputeService service;

    DisputeController(DisputeService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<Dispute> file(@Valid @RequestBody CreateDisputeRequest req,
                                 @RequestHeader(value = ACTOR, required = false) String actor) {
        Dispute d = service.fileDispute(new FileDisputeCommand(
                req.cardholderId(), req.transactionId(), req.reasonCode(), req.disputedAmount(),
                req.statement(), req.duplicateOfTransactionId(), req.expectedDeliveryDate(),
                req.cancellationDate(), actorOr(actor, "cardholder:" + req.cardholderId())));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(d.id()).toUri();
        return ResponseEntity.created(location).body(d);
    }

    @GetMapping("/{disputeId}")
    DisputeDetails get(@PathVariable String disputeId) {
        return service.details(disputeId);
    }

    @PostMapping("/{disputeId}/provisional-credits")
    DisputeDetails provisionalCredit(@PathVariable String disputeId,
                                     @Valid @RequestBody ProvisionalCreditRequest req,
                                     @RequestHeader(value = ACTOR, required = false) String actor) {
        return service.issueProvisionalCredit(disputeId, req.amount(), actorOr(actor, "analyst:unknown"));
    }

    @PostMapping("/{disputeId}/chargeback")
    DisputeDetails chargeback(@PathVariable String disputeId,
                              @RequestHeader(value = ACTOR, required = false) String actor) {
        return service.fileChargeback(disputeId, actorOr(actor, "analyst:unknown"));
    }

    @PostMapping("/{disputeId}/resolution")
    DisputeDetails resolve(@PathVariable String disputeId,
                           @Valid @RequestBody ResolveRequest req,
                           @RequestHeader(value = ACTOR, required = false) String actor) {
        return service.resolve(disputeId, req.resolution(), req.note(), actorOr(actor, "analyst:unknown"));
    }

    private static String actorOr(String actor, String fallback) {
        return actor == null || actor.isBlank() ? fallback : actor;
    }
}
