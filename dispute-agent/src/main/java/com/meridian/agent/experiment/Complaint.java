package com.meridian.agent.experiment;

import com.meridian.agent.classify.ReasonCode;

import java.util.List;

/**
 * The ten fixture complaints, written the way people actually write them:
 * lower case, run-on, hedged, occasionally wrong about their own problem.
 * {@code expected} is the label a senior dispute analyst would give.
 * <p>
 * C7 and C9 have no clean answer on the text alone. That is intentional — they measure
 * whether the classifier can say "I don't know" instead of guessing.
 */
public record Complaint(String id, String text, ReasonCode expected, String note) {

    public static List<Complaint> fixtures() {
        return List.of(
                new Complaint("C1",
                        "i got charged twice for the same order at that furniture place last week",
                        ReasonCode.DR_101, "S1 - textbook duplicate"),
                new Complaint("C2",
                        "streamflix is still taking 649 every month even though i cancelled ages ago",
                        ReasonCode.DR_107, "S2 - cancelled recurring"),
                new Complaint("C3",
                        "ordered a chair from quickcart 25 days back, it never came and support has gone quiet",
                        ReasonCode.DR_104, "S4 - goods not received"),
                new Complaint("C4",
                        "there are two charges of 59.99 dollars to some games company i have never heard of "
                                + "and i definitely did not buy anything from them, my card is in my wallet",
                        ReasonCode.DR_201, "S8 - genuine fraud, explicit denial"),
                new Complaint("C5",
                        "what is SQ *TPR HSPTLTY on my statement? i dont recognise it at all",
                        ReasonCode.UNKNOWN, "S7 - cryptic descriptor, NOT fraud. Needs lookup first"),
                new Complaint("C6",
                        "the gym charged me again after i cancelled my membership in august",
                        ReasonCode.DR_107, "recurring, past tense cancellation"),
                new Complaint("C7",
                        "there's a wrong charge on my card please help",
                        ReasonCode.UNKNOWN, "too vague - must ask, not guess"),
                new Complaint("C8",
                        "paid for express delivery on 2 orders and both are showing the same amount deducted "
                                + "twice from my account on the same day, its the same shop",
                        ReasonCode.DR_101, "duplicate, verbose and slightly confused"),
                new Complaint("C9",
                        "i cancelled my subscription and they charged me again, its basically fraud",
                        ReasonCode.DR_107, "cardholder says 'fraud' but means cancelled recurring"),
                new Complaint("C10",
                        "my flight was cancelled by the airline months ago and i still havent got my money back",
                        ReasonCode.DR_104, "services not received; filing window is the rules engine's problem")
        );
    }
}
