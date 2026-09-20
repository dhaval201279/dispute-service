package com.meridian.disputes.domain;

import java.time.LocalDate;

public record Cardholder(String id,
                         String fullName,
                         String email,
                         String segment,
                         String preferredLanguage,
                         LocalDate customerSince) {
}
