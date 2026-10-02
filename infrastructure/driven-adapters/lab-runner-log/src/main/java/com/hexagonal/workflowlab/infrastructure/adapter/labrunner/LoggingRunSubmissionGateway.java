package com.hexagonal.workflowlab.infrastructure.adapter.labrunner;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.port.out.RunSubmissionGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pretends to send the experiment to a lab by logging it. Replacing this class (for example with an
 * HTTP client to a real lab) requires zero changes in the domain, the use cases or the REST layer.
 */
@Slf4j
@Component
public class LoggingRunSubmissionGateway implements RunSubmissionGateway {

    @Override
    public String submit(Experiment experiment) {
        String runReference = "LAB-RUN-" + experiment.getId().value();
        log.info("Submitting experiment {} with {} instructions to the lab. Run reference: {}",
                experiment.getId().value(), experiment.getInstructions().size(), runReference);
        return runReference;
    }
}
