/**
 * Spring Data repositories over the JPA entities.
 *
 * <p>Owns: the technical CRUD plumbing. May depend on: entities and Spring Data. Invariant: only the
 * adapters in the parent package use them; the domain talks to its own ports instead.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.jpa.repository;
