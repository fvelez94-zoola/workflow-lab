package com.hexagonal.workflowlab.infrastructure.adapter.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.hexagonal.workflowlab.domain.model.workflow.Dependency;
import com.hexagonal.workflowlab.domain.model.workflow.IncubateNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasureNode;
import com.hexagonal.workflowlab.domain.model.workflow.MeasurementType;
import com.hexagonal.workflowlab.domain.model.workflow.MixNode;
import com.hexagonal.workflowlab.domain.model.workflow.NodeId;
import com.hexagonal.workflowlab.domain.model.workflow.Workflow;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowId;
import com.hexagonal.workflowlab.domain.model.workflow.WorkflowState;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Adapter test against a real (in-memory) database: proves the mapping domain <-> tables round-trips. */
@DataJpaTest
@Import(JpaWorkflowRepositoryAdapter.class)
class JpaWorkflowRepositoryAdapterTest {

    @Autowired
    private JpaWorkflowRepositoryAdapter adapter;
    @Autowired
    private EntityManager entityManager;

    private static Workflow assay() {
        return Workflow.draft("Assay", "description",
                List.of(new MixNode(new NodeId("mix"), "Mix", 300, 30),
                        new IncubateNode(new NodeId("incubate"), "Incubate", 37, 45),
                        new MeasureNode(new NodeId("read"), "Read", MeasurementType.LUMINESCENCE)),
                List.of(new Dependency(new NodeId("mix"), new NodeId("incubate")),
                        new Dependency(new NodeId("incubate"), new NodeId("read"))));
    }

    private void flushAndForgetEverything() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldSaveAndLoadWorkflowPreservingNodesAndDependencies() {
        Workflow workflow = assay();
        adapter.save(workflow);
        flushAndForgetEverything();

        Workflow loaded = adapter.findById(workflow.getId()).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Assay");
        assertThat(loaded.getDescription()).isEqualTo("description");
        assertThat(loaded.getState()).isEqualTo(WorkflowState.DRAFT);
        assertThat(loaded.getNodes()).containsExactlyElementsOf(workflow.getNodes());
        assertThat(loaded.getDependencies()).containsExactlyElementsOf(workflow.getDependencies());
    }

    @Test
    void shouldReturnEmptyWhenWorkflowDoesNotExist() {
        assertThat(adapter.findById(WorkflowId.generate())).isEmpty();
    }

    @Test
    void shouldPersistPublishedState() {
        Workflow workflow = assay();
        adapter.save(workflow);
        workflow.publish();
        adapter.save(workflow);
        flushAndForgetEverything();

        assertThat(adapter.findById(workflow.getId()).orElseThrow().getState()).isEqualTo(WorkflowState.PUBLISHED);
    }

    @Test
    void shouldReplaceNodesAndDependenciesWhenWorkflowIsUpdated() {
        Workflow workflow = assay();
        adapter.save(workflow);
        flushAndForgetEverything();

        workflow.redefine("Smaller", null, List.of(new MixNode(new NodeId("only"), "Only", 100, 10)), List.of());
        adapter.save(workflow);
        flushAndForgetEverything();

        Workflow loaded = adapter.findById(workflow.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Smaller");
        assertThat(loaded.getNodes()).extracting(node -> node.id().value()).containsExactly("only");
        assertThat(loaded.getDependencies()).isEmpty();
    }

    @Test
    void shouldListAllSavedWorkflows() {
        adapter.save(assay());
        adapter.save(assay());
        flushAndForgetEverything();

        assertThat(adapter.findAll()).hasSize(2);
    }

    @Test
    void shouldDeleteWorkflow() {
        Workflow workflow = assay();
        adapter.save(workflow);
        flushAndForgetEverything();

        adapter.deleteById(workflow.getId());
        flushAndForgetEverything();

        assertThat(adapter.findById(workflow.getId())).isEmpty();
    }
}
