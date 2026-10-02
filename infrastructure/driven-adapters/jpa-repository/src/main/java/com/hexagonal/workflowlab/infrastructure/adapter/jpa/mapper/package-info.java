/**
 * Mapping between domain objects and JPA entities.
 *
 * <p>Owns: the translation. May depend on: the domain model and the entities. Invariant: exhaustive
 * {@code switch} over the sealed node type, so a new node type breaks the build here until handled.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.jpa.mapper;
