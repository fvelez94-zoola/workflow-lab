/**
 * The experiment aggregate and the conversion from a workflow.
 *
 * <p>Owns: what an experiment is and how a workflow becomes one. May depend on: {@code workflow}.
 * Invariant: the dependency goes one way only; {@code workflow} knows nothing about experiments.
 */
package com.hexagonal.workflowlab.domain.model.experiment;
