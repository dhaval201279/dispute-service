package com.meridian.disputes.web;

import com.meridian.disputes.domain.Resolution;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveRequest(@NotNull Resolution resolution, @Size(max = 1000) String note) {
}
