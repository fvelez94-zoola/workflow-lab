package com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistence shape of a workflow. It is NOT the domain object and the domain never sees it. */
@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String state;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_node", joinColumns = @JoinColumn(name = "workflow_id"))
    @OrderColumn(name = "node_order")
    private List<NodeEmbeddable> nodes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_dependency", joinColumns = @JoinColumn(name = "workflow_id"))
    @OrderColumn(name = "dependency_order")
    private List<DependencyEmbeddable> dependencies = new ArrayList<>();
}
