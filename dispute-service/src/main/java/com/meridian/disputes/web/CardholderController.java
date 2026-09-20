package com.meridian.disputes.web;

import com.meridian.disputes.application.CardholderQueries;
import com.meridian.disputes.application.TransactionView;
import com.meridian.disputes.domain.Cardholder;
import com.meridian.disputes.domain.Dispute;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cardholders/{cardholderId}")
@Validated
class CardholderController {

    private final CardholderQueries queries;

    CardholderController(CardholderQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    Cardholder cardholder(@PathVariable String cardholderId) {
        return queries.cardholder(cardholderId);
    }

    @GetMapping("/transactions")
    List<TransactionView> transactions(@PathVariable String cardholderId,
                                       @RequestParam(defaultValue = "90") @Min(1) @Max(365) int days) {
        return queries.recentTransactions(cardholderId, days);
    }

    @GetMapping("/transactions/{transactionId}")
    TransactionView transaction(@PathVariable String cardholderId, @PathVariable String transactionId) {
        return queries.transaction(cardholderId, transactionId);
    }

    @GetMapping("/disputes")
    List<Dispute> disputes(@PathVariable String cardholderId) {
        return queries.disputes(cardholderId);
    }
}
