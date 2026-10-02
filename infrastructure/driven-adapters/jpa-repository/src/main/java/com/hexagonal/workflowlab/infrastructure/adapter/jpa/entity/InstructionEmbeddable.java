package com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructionEmbeddable {

    @Column(name = "source_node_key", nullable = false)
    private String sourceNodeKey;

    @Column(name = "instruction_type", nullable = false)
    private String instructionType;

    @Column(nullable = false)
    private String description;
}
