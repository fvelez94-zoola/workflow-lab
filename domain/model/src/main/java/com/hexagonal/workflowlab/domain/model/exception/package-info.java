/**
 * Business exceptions raised by the domain.
 *
 * <p>Owns: the vocabulary of things that can go wrong. May depend on: other domain packages only.
 * Invariant: none of these know about HTTP; the entry point decides how to translate them.
 */
package com.hexagonal.workflowlab.domain.model.exception;
