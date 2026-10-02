package com.hexagonal.workflowlab.infrastructure.entrypoint.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hexagonal.workflowlab.domain.model.exception.InvalidWorkflowStateException;
import com.hexagonal.workflowlab.domain.model.port.in.ExportProtocol;
import com.hexagonal.workflowlab.domain.model.protocol.DocumentFormat;
import com.hexagonal.workflowlab.domain.model.protocol.ProtocolDocument;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.infrastructure.entrypoint.rest.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProtocolsControllerTest {

    @Mock
    private ExportProtocol exportProtocol;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProtocolsController(exportProtocol))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnCsvAsAnAttachmentWithTheRightContentType() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(exportProtocol.export(id, DocumentFormat.CSV))
                .thenReturn(new ProtocolDocument("Absorbance assay", DocumentFormat.CSV, "sequence,node\n1,mix\n"));

        mvc.perform(get("/workflows/{id}/protocol", id.value()).param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"absorbance-assay-protocol.csv\""))
                .andExpect(content().string("sequence,node\n1,mix\n"));
    }

    @Test
    void shouldDefaultToMarkdownWhenNoFormatIsGiven() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(exportProtocol.export(id, DocumentFormat.MARKDOWN))
                .thenReturn(new ProtocolDocument("Assay", DocumentFormat.MARKDOWN, "# Protocol"));

        mvc.perform(get("/workflows/{id}/protocol", id.value()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown;charset=UTF-8"))
                .andExpect(content().string("# Protocol"));
        verify(exportProtocol).export(id, DocumentFormat.MARKDOWN);
    }

    @Test
    void shouldReturn409AsJsonEvenWhenClientAsksForCsv() throws Exception {
        WorkflowId id = WorkflowId.generate();
        when(exportProtocol.export(id, DocumentFormat.CSV)).thenThrow(new InvalidWorkflowStateException("draft"));

        mvc.perform(get("/workflows/{id}/protocol", id.value()).param("format", "CSV").accept("text/csv"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_WORKFLOW_STATE"));
    }

    @Test
    void shouldReturn400WhenFormatIsUnknown() throws Exception {
        mvc.perform(get("/workflows/{id}/protocol", WorkflowId.generate().value()).param("format", "PDF"))
                .andExpect(status().isBadRequest());
    }
}
