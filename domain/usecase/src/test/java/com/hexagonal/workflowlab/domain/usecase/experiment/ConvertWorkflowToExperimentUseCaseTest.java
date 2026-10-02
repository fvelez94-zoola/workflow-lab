package com.hexagonal.workflowlab.domain.usecase.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.experiment.Experiment;
import com.hexagonal.workflowlab.domain.model.experiment.ExperimentState;
import com.hexagonal.workflowlab.domain.model.port.out.ExperimentRepository;
import com.hexagonal.workflowlab.domain.model.port.out.RunSubmissionGateway;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConvertWorkflowToExperimentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Mock
    private WorkflowRepository workflows;
    @Mock
    private ExperimentRepository experiments;
    @Mock
    private RunSubmissionGateway runSubmission;

    private ConvertWorkflowToExperimentUseCase service;

    @BeforeEach
    void setUp() {
        service = new ConvertWorkflowToExperimentUseCase(workflows, experiments, runSubmission,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Workflow assay() {
        return Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("a"), "Mix", 300, 30),
                        new MeasureNode(new NodeId("b"), "Read", MeasurementType.ABSORBANCE)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b"))));
    }

    @Test
    void shouldSaveThenSubmitThenSaveAgainWithRunReference() {
        Workflow workflow = assay();
        workflow.publish();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(experiments.save(any(Experiment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(runSubmission.submit(any(Experiment.class))).thenReturn("RUN-42");

        Experiment result = service.convert(workflow.getId());

        assertThat(result.getState()).isEqualTo(ExperimentState.SUBMITTED);
        assertThat(result.getRunReference()).isEqualTo("RUN-42");
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        var order = inOrder(experiments, runSubmission);
        order.verify(experiments).save(any(Experiment.class));
        order.verify(runSubmission).submit(any(Experiment.class));
        order.verify(experiments).save(any(Experiment.class));
    }

    @Test
    void shouldNotSubmitAnythingWhenWorkflowIsDraft() {
        Workflow draft = assay();
        when(workflows.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.convert(draft.getId())).isInstanceOf(InvalidWorkflowStateException.class);
        verify(experiments, never()).save(any());
        verifyNoInteractions(runSubmission);
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowIsMissing() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(id)).isInstanceOf(WorkflowNotFoundException.class);
        verifyNoInteractions(experiments, runSubmission);
    }
}
