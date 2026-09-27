package com.meridian.agent.classify;

/**
 * What we want back from the model. Two fields beyond the code itself matter:
 * <ul>
 *   <li>{@code merchantHint} — what the cardholder called the merchant, in their words.
 *       Part 2 uses it to search transactions.</li>
 *   <li>{@code needsMoreInfo} — the model's own signal that the complaint is too vague
 *       to classify. This is not confidence; it is "I cannot answer yet".</li>
 * </ul>
 */
public record Classification(ReasonCode reasonCode,
                             String merchantHint,
                             boolean needsMoreInfo,
                             String reasoning) {

    public static Classification unknown(String reasoning) {
        return new Classification(ReasonCode.UNKNOWN, null, true, reasoning);
    }
}
