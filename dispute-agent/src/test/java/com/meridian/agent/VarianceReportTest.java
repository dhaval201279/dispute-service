package com.meridian.agent;

import com.meridian.agent.classify.ReasonCode;
import com.meridian.agent.experiment.VarianceReport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The maths behind the numbers in the blog post. No model, no network.
 */
class VarianceReportTest {

    @Test
    void identicalAnswersAreFullyStable() {
        var r = VarianceReport.of("C1", "bounded", 0.0, ReasonCode.DR_101,
                List.of(ReasonCode.DR_101, ReasonCode.DR_101, ReasonCode.DR_101, ReasonCode.DR_101));

        assertThat(r.distinctAnswers()).isEqualTo(1);
        assertThat(r.stability()).isEqualTo(1.0);
        assertThat(r.accuracy()).isEqualTo(1.0);
        assertThat(r.stablyWrong()).isFalse();
    }

    @Test
    void stabilityAndAccuracyAreDifferentThings() {
        // Every run agrees on the wrong answer: the failure mode that looks like success.
        var r = VarianceReport.of("C5", "unbounded", 0.0, ReasonCode.UNKNOWN,
                List.of(ReasonCode.DR_201, ReasonCode.DR_201, ReasonCode.DR_201, ReasonCode.DR_201));

        assertThat(r.stability()).isEqualTo(1.0);
        assertThat(r.accuracy()).isEqualTo(0.0);
        assertThat(r.stablyWrong()).isTrue();
    }

    @Test
    void mixedAnswersReportModalAndShares() {
        var r = VarianceReport.of("C9", "unbounded", 0.7, ReasonCode.DR_107,
                List.of(ReasonCode.DR_107, ReasonCode.DR_107, ReasonCode.DR_201, ReasonCode.UNKNOWN));

        assertThat(r.distinctAnswers()).isEqualTo(3);
        assertThat(r.modalAnswer()).isEqualTo(ReasonCode.DR_107);
        assertThat(r.stability()).isCloseTo(0.5, within(0.001));
        assertThat(r.accuracy()).isCloseTo(0.5, within(0.001));
        assertThat(r.tallyAsText()).startsWith("DR-107x2");
    }

    @Test
    void lenientParseHandlesTheShapesModelsActuallyReturn() {
        assertThat(ReasonCode.parse("DR-101")).isEqualTo(ReasonCode.DR_101);
        assertThat(ReasonCode.parse("  dr_104 ")).isEqualTo(ReasonCode.DR_104);
        assertThat(ReasonCode.parse("This looks like DR-107 to me.")).isEqualTo(ReasonCode.DR_107);
        assertThat(ReasonCode.parse("DR-999")).isEqualTo(ReasonCode.UNKNOWN);
        assertThat(ReasonCode.parse(null)).isEqualTo(ReasonCode.UNKNOWN);
    }
}
