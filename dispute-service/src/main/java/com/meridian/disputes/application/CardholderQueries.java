package com.meridian.disputes.application;

import com.meridian.disputes.domain.Cardholder;
import com.meridian.disputes.domain.Dispute;
import com.meridian.disputes.persistence.CardholderRepository;
import com.meridian.disputes.persistence.DisputeRepository;
import com.meridian.disputes.persistence.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CardholderQueries {

    private final CardholderRepository cardholders;
    private final TransactionRepository transactions;
    private final DisputeRepository disputes;
    private final Clock clock;

    public CardholderQueries(CardholderRepository cardholders, TransactionRepository transactions,
                             DisputeRepository disputes, Clock clock) {
        this.cardholders = cardholders;
        this.transactions = transactions;
        this.disputes = disputes;
        this.clock = clock;
    }

    public Cardholder cardholder(String id) {
        return cardholders.findById(id)
                .orElseThrow(() -> new DisputeRuleException(ErrorCode.CARDHOLDER_NOT_FOUND,
                        "No cardholder with id " + id));
    }

    public List<TransactionView> recentTransactions(String cardholderId, int days) {
        cardholder(cardholderId); // 404 if unknown
        var since = clock.instant().minus(Duration.ofDays(days));
        return transactions.findByCardholderSince(cardholderId, since).stream()
                .map(t -> TransactionView.of(t, transactions.sumPostedRefunds(t.id())))
                .toList();
    }

    public TransactionView transaction(String cardholderId, String transactionId) {
        var t = transactions.findById(transactionId)
                .filter(tx -> tx.cardholderId().equals(cardholderId))
                .orElseThrow(() -> new DisputeRuleException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "No transaction " + transactionId + " for cardholder " + cardholderId));
        return TransactionView.of(t, transactions.sumPostedRefunds(t.id()));
    }

    public List<Dispute> disputes(String cardholderId) {
        cardholder(cardholderId);
        return disputes.findByCardholder(cardholderId);
    }
}
