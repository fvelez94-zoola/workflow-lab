package com.hexagonal.workflowlab.config;

import com.hexagonal.workflowlab.domain.model.port.in.WorkflowCrud;
import com.hexagonal.workflowlab.domain.model.port.in.WorkflowDraft;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Seeds one DRAFT workflow through the input port, so the data goes through the domain rules too. */
@Slf4j
@Configuration
class SampleDataConfig {

    @Bean
    @ConditionalOnProperty(name = "workflowlab.sample-data.enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner sampleWorkflow(WorkflowCrud workflowCrud) {
        return args -> {
            var workflow = workflowCrud.create(new WorkflowDraft(
                    "Absorbance assay",
                    "Mix, incubate and read absorbance",
                    List.of(
                            new MixNode(new NodeId("mix-1"), "Mix reagents", 300, 30),
                            new IncubateNode(new NodeId("incubate-1"), "Incubate sample", 37, 45),
                            new MeasureNode(new NodeId("measure-1"), "Read plate", MeasurementType.ABSORBANCE)),
                    List.of(
                            new Dependency(new NodeId("mix-1"), new NodeId("incubate-1")),
                            new Dependency(new NodeId("incubate-1"), new NodeId("measure-1")))));
            log.info("Sample workflow created with id {}", workflow.getId().value());
        };
    }
}
