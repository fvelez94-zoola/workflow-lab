/**
 * The workflow aggregate: nodes, dependencies, graph algorithms and publication rules.
 *
 * <p>Owns: what a valid workflow is. May depend on: {@code exception} and the JDK. Invariant: no
 * framework annotation or import in here, ever (enforced by ArchUnit in {@code app-service}).
 */
package com.hexagonal.workflowlab.domain.model.workflow;
