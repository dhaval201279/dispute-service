package com.meridian.disputes.web;

import com.meridian.disputes.application.DisputeRuleException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Business-rule violations as RFC 9457 problem details with a stable {@code errorCode}.
 * Framework errors (validation, malformed JSON) use Spring's built-in problem details
 * via {@code spring.mvc.problemdetails.enabled=true}.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(DisputeRuleException.class)
    ProblemDetail onRuleViolation(DisputeRuleException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.code().status(), ex.getMessage());
        pd.setType(URI.create("https://disputedesk.example/errors/" + ex.code().name().toLowerCase().replace('_', '-')));
        pd.setTitle(ex.code().name());
        pd.setProperty("errorCode", ex.code().name());
        return pd;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail onConcurrentModification(OptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("CONCURRENT_MODIFICATION");
        pd.setProperty("errorCode", "CONCURRENT_MODIFICATION");
        return pd;
    }

    /**
     * Two concurrent requests can both pass the "no active dispute" check; the partial
     * unique index in V1__schema.sql rejects the loser. Parallel tool calls from an agent
     * (Part 3) make this race very real.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    ProblemDetail onDuplicate(DuplicateKeyException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This transaction already has an active dispute");
        pd.setTitle("DISPUTE_ALREADY_ACTIVE");
        pd.setProperty("errorCode", "DISPUTE_ALREADY_ACTIVE");
        return pd;
    }
}
