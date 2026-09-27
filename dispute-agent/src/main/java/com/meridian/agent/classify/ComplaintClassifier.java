package com.meridian.agent.classify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The first LLM call of the series: turn a sentence a human typed into a reason code.
 * <p>
 * Two implementations of the same task, on purpose:
 * <ul>
 *   <li>{@link #classifyUnbounded} — ask in plain language, parse whatever comes back.
 *       This is how most first attempts look.</li>
 *   <li>{@link #classifyBounded} — constrain the output to a schema the code owns.</li>
 * </ul>
 * Part 1 measures the difference between them instead of asserting it.
 */
@Component
public class ComplaintClassifier {

    private static final Logger log = LoggerFactory.getLogger(ComplaintClassifier.class);

    private static final String UNBOUNDED_SYSTEM = """
            You are a dispute intake assistant at Meridian Bank.
            Read the cardholder's complaint and tell me which dispute reason code applies.
            The reason codes are:
            %s
            """.formatted(ReasonCode.promptCatalogue());

    private static final String BOUNDED_SYSTEM = """
            You classify cardholder complaints at Meridian Bank into exactly one dispute reason code.

            Reason codes:
            %s

            Rules:
            - Choose exactly one code from the list above. Never invent a code.
            - If the cardholder explicitly denies making the transaction ("I never made this",
              "I did not buy anything from them"), that is DR-201. Do not use UNKNOWN.
            - If the cardholder cannot place a merchant name but has NOT denied making the
              purchase, that is not DR-201: use UNKNOWN and set needsMoreInfo to true.
            - A subscription that keeps billing after cancellation is DR-107, not DR-101,
              even when the amounts are identical.
            - If the complaint clearly describes one situation, choose that code even when
              details are missing. Missing dates and amounts are the rules engine's problem,
              not yours.
            - Use UNKNOWN only when you genuinely cannot choose between codes.
            - merchantHint: the merchant as the cardholder described it, or null.
            - reasoning: one short sentence.

            Respond with JSON only. No prose, no code fences.
            """.formatted(ReasonCode.promptCatalogue());

    private final ChatClient chatClient;

    public ComplaintClassifier(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    /** Free-form prompt, free-form answer, best-effort parse. */
    public Classification classifyUnbounded(String complaint, double temperature) {
        String answer = chatClient.prompt()
                .system(UNBOUNDED_SYSTEM)
                .user(complaint)
                .options(ChatOptions.builder().temperature(temperature))
                .call()
                .content();
        log.debug("unbounded raw answer: {}", answer);
        return new Classification(ReasonCode.parse(answer), null, false, answer);
    }

    /** Constrained prompt, schema-mapped answer. */
    public Classification classifyBounded(String complaint, double temperature) {
        try {
            Classification result = chatClient.prompt()
                    .system(BOUNDED_SYSTEM)
                    .user(complaint)
                    .options(ChatOptions.builder().temperature(temperature))
                    .call()
                    .entity(Classification.class);
            return result == null ? Classification.unknown("no response") : result;
        } catch (RuntimeException ex) {
            // A model that returns malformed JSON is a real experimental outcome, and it
            // gets counted. A 401, a connection refused or a missing model is NOT — that is
            // a broken setup, and swallowing it produces a CSV full of confident UNKNOWNs
            // that look like data.
            if (!isParseFailure(ex)) {
                throw ex;
            }
            log.warn("bounded classification could not be parsed: {}", ex.getMessage());
            return Classification.unknown("unparseable response: " + ex.getMessage());
        }
    }

    /**
     * True when the failure came from mapping the model's text into {@link Classification},
     * rather than from the call itself. Matched on the exception chain's class names because
     * the JSON library sitting under Spring AI differs by version, and this check should not
     * break when that changes.
     */
    private static boolean isParseFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String type = t.getClass().getName().toLowerCase();
            if (type.contains("jackson") || type.contains("json") || t instanceof IllegalArgumentException) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }
}
