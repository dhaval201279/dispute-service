package com.meridian.agent.experiment;

import com.meridian.agent.classify.ReasonCode;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates repeated classifications of ONE complaint at ONE temperature.
 * <p>
 * Pure arithmetic, no model calls, so it is unit-testable — which matters, because
 * these are the numbers the blog post reports.
 */
public record VarianceReport(String complaintId,
                             String mode,
                             double temperature,
                             ReasonCode expected,
                             Map<ReasonCode, Integer> tally,
                             int runs) {

    public static VarianceReport of(String complaintId, String mode, double temperature,
                                    ReasonCode expected, List<ReasonCode> answers) {
        Map<ReasonCode, Integer> tally = new EnumMap<>(ReasonCode.class);
        answers.forEach(a -> tally.merge(a, 1, Integer::sum));
        return new VarianceReport(complaintId, mode, temperature, expected, tally, answers.size());
    }

    /** How many different answers the same input produced. 1 means fully stable. */
    public int distinctAnswers() {
        return tally.size();
    }

    public ReasonCode modalAnswer() {
        return tally.entrySet().stream()
                .max(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .map(Map.Entry::getKey)
                .orElse(ReasonCode.UNKNOWN);
    }

    /** Share of runs that returned the most common answer — how self-consistent the model is. */
    public double stability() {
        return runs == 0 ? 0 : (double) tally.getOrDefault(modalAnswer(), 0) / runs;
    }

    /** Share of runs that returned the analyst's label — how often it is actually right. */
    public double accuracy() {
        return runs == 0 ? 0 : (double) tally.getOrDefault(expected, 0) / runs;
    }

    /**
     * The combination that should worry you: every run agrees, and every run is wrong.
     * Stability is not correctness, and a stable classifier can be confidently useless.
     */
    public boolean stablyWrong() {
        return stability() == 1.0 && accuracy() == 0.0;
    }

    public String tallyAsText() {
        return tally.entrySet().stream()
                .sorted(Map.Entry.<ReasonCode, Integer>comparingByValue().reversed())
                .map(e -> e.getKey().code() + "x" + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
    }
}
