package com.hexagonal.workflowlab.domain.usecase.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishWorkflowUseCaseTest {

    @Mock
    private WorkflowRepository workflows;

    private PublishWorkflowUseCase service;

    @BeforeEach
    void setUp() {
        service = new PublishWorkflowUseCase(workflows);
    }

    @Test
    void shouldPublishAndSaveValidWorkflow() {
        Workflow workflow = Workflow.draft("Assay", null,
                List.of(new MixNode(new NodeId("a"), "Mix", 300, 30),
                        new MeasureNode(new NodeId("b"), "Read", MeasurementType.ABSORBANCE)),
                List.of(new Dependency(new NodeId("a"), new NodeId("b"))));
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(workflows.save(workflow)).thenReturn(workflow);

        Workflow published = service.publish(workflow.getId());

        assertThat(published.getState()).isEqualTo(WorkflowState.PUBLISHED);
        verify(workflows).save(workflow);
    }

    @Test
    void shouldNotSaveWhenValidationFails() {
        Workflow empty = Workflow.draft("Empty", null, List.of(), List.of());
        when(workflows.findById(empty.getId())).thenReturn(Optional.of(empty));

        assertThatThrownBy(() -> service.publish(empty.getId())).isInstanceOf(InvalidWorkflowException.class);
        verify(workflows, never()).save(any());
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowIsMissing() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(id)).isInstanceOf(WorkflowNotFoundException.class);
    }
}
