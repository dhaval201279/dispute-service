package com.meridian.agent.experiment;

import com.meridian.agent.classify.Classification;
import com.meridian.agent.classify.ComplaintClassifier;
import com.meridian.agent.classify.ReasonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Runs every fixture complaint N times per temperature, in both unbounded and bounded
 * modes, and writes the results as CSV plus a markdown summary.
 * <p>
 * Run with: {@code mvn -pl dispute-agent spring-boot:run -Dspring-boot.run.profiles=experiment}
 * <p>
 * Costs real money. 10 complaints x 20 runs x 2 temperatures x 2 modes = 800 calls.
 * Start with {@code --experiment.runs=5} while you are still adjusting prompts.
 */
@Component
@Profile("experiment")
public class VarianceExperiment implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VarianceExperiment.class);
    private static final double[] TEMPERATURES = {0.0, 0.7};

    private final ComplaintClassifier classifier;

    public VarianceExperiment(ComplaintClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        int runs = args.containsOption("experiment.runs")
                ? Integer.parseInt(args.getOptionValues("experiment.runs").getFirst()) : 20;

        List<VarianceReport> reports = new ArrayList<>();
        for (Complaint complaint : Complaint.fixtures()) {
            for (double temperature : TEMPERATURES) {
                reports.add(measure(complaint, "unbounded", temperature, runs, classifier::classifyUnbounded));
                reports.add(measure(complaint, "bounded", temperature, runs, classifier::classifyBounded));
            }
        }

        Path csv = Path.of("target/variance.csv");
        Files.createDirectories(csv.getParent());
        Files.write(csv, asCsv(reports));
        log.info("Wrote {}", csv.toAbsolutePath());
        System.out.println(asMarkdown(reports));
    }

    private VarianceReport measure(Complaint complaint, String mode, double temperature, int runs,
                                   BiFunction<String, Double, Classification> classify) {
        List<ReasonCode> answers = new ArrayList<>(runs);
        for (int i = 0; i < runs; i++) {
            try {
                answers.add(classify.apply(complaint.text(), temperature).reasonCode());
            } catch (RuntimeException ex) {
                log.warn("{} {} t={} run {} failed: {}", complaint.id(), mode, temperature, i, ex.getMessage());
                answers.add(ReasonCode.UNKNOWN);
            }
        }
        VarianceReport report = VarianceReport.of(complaint.id(), mode, temperature, complaint.expected(), answers);
        log.info("{} {} t={} -> {} distinct, stability {}, accuracy {}",
                complaint.id(), mode, temperature, report.distinctAnswers(),
                pct(report.stability()), pct(report.accuracy()));
        return report;
    }

    static List<String> asCsv(List<VarianceReport> reports) {
        List<String> lines = new ArrayList<>();
        lines.add("complaint,mode,temperature,expected,modal,distinct,stability,accuracy,tally");
        reports.forEach(r -> lines.add("%s,%s,%.1f,%s,%s,%d,%.2f,%.2f,\"%s\"".formatted(
                r.complaintId(), r.mode(), r.temperature(), r.expected().code(), r.modalAnswer().code(),
                r.distinctAnswers(), r.stability(), r.accuracy(), r.tallyAsText())));
        return lines;
    }

    static String asMarkdown(List<VarianceReport> reports) {
        StringBuilder sb = new StringBuilder("\n| Complaint | Mode | Temp | Expected | Most common | Distinct | Stability | Accuracy |\n");
        sb.append("|---|---|---|---|---|---|---|---|\n");
        for (VarianceReport r : reports) {
            sb.append("| %s | %s | %.1f | %s | %s | %d | %s | %s |%n".formatted(
                    r.complaintId(), r.mode(), r.temperature(), r.expected().code(), r.modalAnswer().code(),
                    r.distinctAnswers(), pct(r.stability()), pct(r.accuracy())));
        }
        return sb.toString();
    }

    private static String pct(double d) {
        return Math.round(d * 100) + "%";
    }
}
