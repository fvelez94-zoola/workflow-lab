/**
 * JPA entities: the persistence shape of the data.
 *
 * <p>Owns: table and column mapping. May depend on: Jakarta Persistence and Lombok only. Invariant:
 * entities never leave this adapter; the domain model is rebuilt from them by the mappers.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.jpa.entity;
