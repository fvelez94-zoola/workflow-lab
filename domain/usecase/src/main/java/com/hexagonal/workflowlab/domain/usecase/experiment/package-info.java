/**
 * Use case that converts a published workflow into an experiment and submits it.
 *
 * <p>Owns: the sequence save -> submit -> mark as submitted. May depend on: {@code domain.model}.
 * Invariant: the conversion rules themselves live in the model, not here.
 */
package com.hexagonal.workflowlab.domain.usecase.experiment;
