package com.hexagonal.workflowlab.domain.usecase.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.exception.WorkflowNotFoundException;
import com.hexagonal.workflowlab.domain.model.port.out.ProtocolDocumentWriter;
import com.hexagonal.workflowlab.domain.model.port.out.WorkflowRepository;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolSheet;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportProtocolUseCaseTest {

    @Mock
    private WorkflowRepository workflows;
    @Mock
    private ProtocolDocumentWriter writer;

    private ExportProtocolUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExportProtocolUseCase(workflows, writer,
                Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC));
    }

    private static Workflow published() {
        Workflow workflow = Workflow.draft("Assay", null, List.of(new MixNode(new NodeId("a"), "Mix", 300, 30)),
                List.of());
        workflow.publish();
        return workflow;
    }

    @Test
    void shouldAskTheWriterForTheRequestedFormatAndReturnItsContent() {
        Workflow workflow = published();
        when(workflows.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(writer.write(any(ProtocolSheet.class), Mockito.eq(DocumentFormat.CSV))).thenReturn("csv-content");

        ProtocolDocument document = useCase.export(workflow.getId(), DocumentFormat.CSV);

        assertThat(document).isEqualTo(new ProtocolDocument("Assay", DocumentFormat.CSV, "csv-content"));
        ArgumentCaptor<ProtocolSheet> sheet = ArgumentCaptor.forClass(ProtocolSheet.class);
        Mockito.verify(writer).write(sheet.capture(), Mockito.eq(DocumentFormat.CSV));
        assertThat(sheet.getValue().steps()).hasSize(1);
    }

    @Test
    void shouldNotWriteAnythingWhenWorkflowIsDraft() {
        Workflow draft = Workflow.draft("Assay", null, List.of(new MixNode(new NodeId("a"), "Mix", 300, 30)),
                List.of());
        when(workflows.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> useCase.export(draft.getId(), DocumentFormat.MARKDOWN))
                .isInstanceOf(InvalidWorkflowStateException.class);
        verifyNoInteractions(writer);
    }

    @Test
    void shouldThrowNotFoundWhenWorkflowIsMissing() {
        WorkflowId id = WorkflowId.generate();
        when(workflows.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.export(id, DocumentFormat.MARKDOWN))
                .isInstanceOf(WorkflowNotFoundException.class);
        verifyNoInteractions(writer);
    }
}
