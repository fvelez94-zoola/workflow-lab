package com.hexagonal.workflowlab.infrastructure.adapter.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.ExperimentState;
import com.hexagonal.workflowlab.domain.model.experiment.Instruction;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper.ExperimentEntityMapper;
import com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository.SpringDataExperimentRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaExperimentRepositoryAdapter.class)
class JpaExperimentRepositoryAdapterTest {

    @Autowired
    private JpaExperimentRepositoryAdapter adapter;
    @Autowired
    private SpringDataExperimentRepository springDataRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistExperimentWithInstructionsInOrderAndRunReference() {
        Workflow workflow = Workflow.draft("Assay", null,
                List.of(new IncubateNode(new NodeId("incubate"), "Incubate", 37, 45),
                        new MeasureNode(new NodeId("read"), "Read", MeasurementType.ABSORBANCE)),
                List.of(new Dependency(new NodeId("incubate"), new NodeId("read"))));
        workflow.publish();
        Experiment experiment = Experiment.fromWorkflow(workflow, Instant.parse("2026-10-02T10:00:00Z"));
        experiment.markSubmitted("RUN-9");

        adapter.save(experiment);
        entityManager.flush();
        entityManager.clear();

        Experiment loaded = ExperimentEntityMapper.toDomain(
                springDataRepository.findById(experiment.getId().value()).orElseThrow());
        assertThat(loaded.getState()).isEqualTo(ExperimentState.SUBMITTED);
        assertThat(loaded.getRunReference()).isEqualTo("RUN-9");
        assertThat(loaded.getCreatedAt()).isEqualTo(experiment.getCreatedAt());
        assertThat(loaded.getInstructions()).extracting(Instruction::description)
                .containsExactly("Set temperature to 37 C", "Wait 45 min", "Measure absorbance");
    }
}
