package com.meridian.disputes;

import com.meridian.disputes.domain.Queue;
import com.meridian.disputes.domain.ReasonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReasonCodeTest {

    @Test
    void parsesByCodeOrName() {
        assertThat(ReasonCode.fromCode("DR-101")).isEqualTo(ReasonCode.DUPLICATE_CHARGE);
        assertThat(ReasonCode.fromCode("dr-107")).isEqualTo(ReasonCode.CANCELLED_RECURRING);
        assertThat(ReasonCode.fromCode("GOODS_NOT_RECEIVED")).isEqualTo(ReasonCode.GOODS_NOT_RECEIVED);
    }

    @Test
    void rejectsUnknownCodes() {
        assertThatThrownBy(() -> ReasonCode.fromCode("DR-999")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyFraudGoesToFraudQueue() {
        assertThat(ReasonCode.UNRECOGNISED_FRAUD.queue()).isEqualTo(Queue.FRAUD);
        assertThat(ReasonCode.DUPLICATE_CHARGE.queue()).isEqualTo(Queue.DISPUTES);
    }
}
