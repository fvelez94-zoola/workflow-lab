package com.hexagonal.workflowlab.domain.model.analysis;

import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowNode;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Business assumption: how long does each kind of node take? Mixing and incubating carry their own
 * duration; reading a plate takes a fixed time that depends on the measurement.
 *
 * <p>It lives in the domain because it is a business rule. It returns a {@link Duration}, never text:
 * turning that into "1h 15m" is presentation and belongs to the entry point.
 */
@Component
public final class NodeDurationPolicy {

    public Duration durationOf(WorkflowNode node) {
        return switch (node) {
            case MixNode mix -> Duration.ofSeconds(mix.durationSeconds());
            case IncubateNode incubate -> Duration.ofMinutes(incubate.durationMinutes());
            case MeasureNode measure -> switch (measure.measurementType()) {
                case ABSORBANCE -> Duration.ofSeconds(30);
                case FLUORESCENCE -> Duration.ofSeconds(45);
                case LUMINESCENCE -> Duration.ofSeconds(60);
            };
        };
    }
}
