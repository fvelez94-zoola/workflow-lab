/**
 * Driven adapters that implement the persistence ports with Spring Data JPA.
 *
 * <p>Owns: how workflows and experiments are stored. May depend on: the domain model, JPA, Spring Data.
 * Invariant: must not know about the REST layer, the use cases, or the other driven adapters.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.jpa;
