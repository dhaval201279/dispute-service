package com.meridian.agent.classify;

/**
 * The agent's own copy of Meridian's reason codes.
 * <p>
 * Deliberately NOT shared as a jar with dispute-service (ADR-001). The agent is a
 * separate deployable that talks to the dispute API over HTTP; sharing domain classes
 * would couple their release cycles and quietly let the agent reach past the API.
 * <p>
 * UNKNOWN is the escape hatch. A classifier that cannot say "I don't know" will
 * always guess, and a guess here becomes a wrongly routed case.
 */
public enum ReasonCode {
    DR_101("DR-101", "The same purchase was charged more than once"),
    DR_104("DR-104", "Goods or services were paid for but never received"),
    DR_107("DR-107", "A recurring payment continued after the cardholder cancelled"),
    DR_201("DR-201", "The cardholder does not recognise the transaction at all"),
    UNKNOWN("UNKNOWN", "The complaint does not clearly match any reason code");

    private final String code;
    private final String description;

    ReasonCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String code() { return code; }

    public String description() { return description; }

    /** Lenient parse: free-text model output rarely comes back exactly as an enum constant. */
    public static ReasonCode parse(String raw) {
        if (raw == null) return UNKNOWN;
        String normalised = raw.trim().toUpperCase().replace('-', '_').replace(" ", "");
        for (ReasonCode rc : values()) {
            if (normalised.contains(rc.name())) return rc;
        }
        return UNKNOWN;
    }

    public static String promptCatalogue() {
        StringBuilder sb = new StringBuilder();
        for (ReasonCode rc : values()) {
            sb.append(rc.code).append(" - ").append(rc.description).append('\n');
        }
        return sb.toString();
    }
}
