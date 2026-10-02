package com.hexagonal.workflowlab;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Black-box test through the whole stack: controller, use case, domain, JPA and the H2 database. */
@SpringBootTest(properties = "workflowlab.sample-data.enabled=false")
@AutoConfigureMockMvc
class WorkflowLabFlowTest {

    private static final String ASSAY = """
            {
              "name": "Absorbance assay",
              "nodes": [
                {"id": "read", "name": "Read plate", "type": "MEASURE", "measurementType": "ABSORBANCE"},
                {"id": "incubate", "name": "Incubate", "type": "INCUBATE", "temperatureCelsius": 37, "durationMinutes": 45},
                {"id": "mix", "name": "Mix", "type": "MIX", "speedRpm": 300, "durationSeconds": 30}
              ],
              "dependencies": [
                {"from": "mix", "to": "incubate"},
                {"from": "incubate", "to": "read"}
              ]
            }
            """;

    private static final String CYCLIC = """
            {
              "name": "Cyclic",
              "nodes": [
                {"id": "a", "name": "A", "type": "MIX", "speedRpm": 100, "durationSeconds": 5},
                {"id": "b", "name": "B", "type": "MEASURE", "measurementType": "ABSORBANCE"}
              ],
              "dependencies": [{"from": "a", "to": "b"}, {"from": "b", "to": "a"}]
            }
            """;

    @Autowired
    private MockMvc mvc;

    private String create(String body) throws Exception {
        String response = mvc.perform(post("/workflows").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void shouldRunFullFlowFromCreationToSubmittedExperiment() throws Exception {
        String id = create(ASSAY);

        mvc.perform(get("/workflows/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.nodes.length()").value(3));

        mvc.perform(post("/workflows/{id}/publish", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(post("/workflows/{id}/experiments", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.runReference").isNotEmpty())
                .andExpect(jsonPath("$.instructions.length()").value(4))
                .andExpect(jsonPath("$.instructions[0].sourceNodeId").value("mix"))
                .andExpect(jsonPath("$.instructions[1].type").value("SET_TEMPERATURE"))
                .andExpect(jsonPath("$.instructions[2].type").value("WAIT"))
                .andExpect(jsonPath("$.instructions[3].sourceNodeId").value("read"));
    }

    @Test
    void shouldUpdateAndDeleteDraftWorkflow() throws Exception {
        String id = create(ASSAY);

        mvc.perform(put("/workflows/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"nodes\":[],\"dependencies\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.nodes.length()").value(0));

        mvc.perform(delete("/workflows/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/workflows/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectConversionOfDraftWorkflow() throws Exception {
        String id = create(ASSAY);

        mvc.perform(post("/workflows/{id}/experiments", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_WORKFLOW_STATE"));
    }

    @Test
    void shouldRejectPublishingCyclicWorkflowAndKeepItAsDraft() throws Exception {
        String id = create(CYCLIC);

        mvc.perform(post("/workflows/{id}/publish", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.failures[0].code").value("CYCLE_DETECTED"));

        mvc.perform(get("/workflows/{id}", id)).andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void shouldProtectPublishedWorkflowFromUpdatesAndDeletion() throws Exception {
        String id = create(ASSAY);
        mvc.perform(post("/workflows/{id}/publish", id)).andExpect(status().isOk());

        mvc.perform(put("/workflows/{id}", id).contentType(MediaType.APPLICATION_JSON).content(ASSAY))
                .andExpect(status().isConflict());
        mvc.perform(delete("/workflows/{id}", id)).andExpect(status().isConflict());
    }

    @Test
    void shouldAnalyzeDurationAndCriticalPathOfADraft() throws Exception {
        String id = create(ASSAY);

        mvc.perform(get("/workflows/{id}/analysis", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeCount").value(3))
                .andExpect(jsonPath("$.estimatedDurationSeconds").value(2760))
                .andExpect(jsonPath("$.estimatedDuration").value("46m"))
                .andExpect(jsonPath("$.criticalPath[0].nodeId").value("mix"))
                .andExpect(jsonPath("$.criticalPath[1].nodeId").value("incubate"))
                .andExpect(jsonPath("$.criticalPath[2].nodeId").value("read"));
    }

    @Test
    void shouldRejectAnalysisOfCyclicWorkflow() throws Exception {
        String id = create(CYCLIC);

        mvc.perform(get("/workflows/{id}/analysis", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.failures[0].code").value("CYCLE_DETECTED"));
    }

    @Test
    void shouldExportPublishedWorkflowAsMarkdownByDefault() throws Exception {
        String id = create(ASSAY);
        mvc.perform(post("/workflows/{id}/publish", id)).andExpect(status().isOk());

        mvc.perform(get("/workflows/{id}/protocol", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("absorbance-assay-protocol.md")))
                .andExpect(content().string(containsString("# Protocol: Absorbance assay")))
                .andExpect(content().string(containsString("| 1 | mix | MIX | Mix at 300 rpm for 30 s |")))
                .andExpect(content().string(containsString("| 4 | read | MEASURE | Measure absorbance |")));
    }

    @Test
    void shouldExportPublishedWorkflowAsCsv() throws Exception {
        String id = create(ASSAY);
        mvc.perform(post("/workflows/{id}/publish", id)).andExpect(status().isOk());

        mvc.perform(get("/workflows/{id}/protocol", id).param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(content().string(startsWith("sequence,node,type,description\n1,mix,MIX,")));
    }

    @Test
    void shouldRejectExportOfDraftWorkflowWithJsonError() throws Exception {
        String id = create(ASSAY);

        mvc.perform(get("/workflows/{id}/protocol", id).param("format", "CSV").accept("text/csv"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_WORKFLOW_STATE"));
    }
}
