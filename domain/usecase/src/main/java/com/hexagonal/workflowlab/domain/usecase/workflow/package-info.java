/**
 * Use cases around the workflow aggregate (CRUD and publication).
 *
 * <p>Owns: orchestration only: load, ask the aggregate to act, save. May depend on: {@code domain.model}.
 * Invariant: Spring is limited to the {@code @Service} stereotype; no web, data or JPA types.
 */
package com.hexagonal.workflowlab.domain.usecase.workflow;
