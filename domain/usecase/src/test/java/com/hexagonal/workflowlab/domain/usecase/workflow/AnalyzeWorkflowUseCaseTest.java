package com.hexagonal.workflowlab.domain.usecase.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.analysis.CriticalPathCalculator;
import com.hexagonal.workflowlab.domain.model.analysis.NodeDurationPolicy;
import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalysis;
import com.hexagonal.workflowlab.domain.model.analysis.WorkflowAnalyzer;
import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Only the output port is mocked: the analyzer is pure domain logic, so the real one is used. */
@ExtendWith(MockitoExtension.class)
class AnalyzeWorkflowUseCaseTest {

    @Mock
    private WorkflowRepository workflows;

    private AnalyzeWorkflowUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AnalyzeWorkflowUseCase(workflows,
                new WorkflowAnalyzer(new CriticalPathCalculator(new NodeDurationPolicy())));
    }

    @Test
    void shouldAnalyzeDraftWorkflow() {
        Workflow workflow = Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("a"), "A", 100, 10), new MixNode(new NodeId("b"), "B", 100, 20)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b"))));
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        WorkflowAnalysis analysis = useCase.analyze(workflow.getId());

        assertThat(analysis.estimatedDuration()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowIsMissing() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.analyze(id)).isInstanceOf(WorkflowNotFoundException.class);
    }

    @Test
    void shouldRejectCyclicWorkflow() {
        Workflow cyclic = Workflow.draft("Cycle", null,
                List.of(new MixNode(new NodeId("a"), "A", 100, 10), new MixNode(new NodeId("b"), "B", 100, 10)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b")), new Dependency(new NodeId("b"), new NodeId("a"))));
        when(workflows.findById(cyclic.getId())).thenReturn(Optional.of(cyclic));

        assertThatThrownBy(() -> useCase.analyze(cyclic.getId())).isInstanceOf(InvalidWorkflowException.class);
    }
}
