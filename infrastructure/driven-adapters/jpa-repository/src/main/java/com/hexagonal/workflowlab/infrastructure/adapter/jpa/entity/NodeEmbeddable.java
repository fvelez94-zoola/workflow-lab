package com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row per node. A single flat shape for every node type; unused parameters stay null. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeEmbeddable {

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Column(nullable = false)
    private String name;

    @Column(name = "node_type", nullable = false)
    private String nodeType;

    private Integer speedRpm;
    private Integer durationSeconds;
    private Integer temperatureCelsius;
    private Integer durationMinutes;
    private String measurementType;
}
