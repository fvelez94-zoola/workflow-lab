/**
 * Business analysis of a workflow: how long it takes and which chain of nodes decides that.
 *
 * <p>Owns: duration assumptions and graph math. May depend on: {@code workflow}. Invariant: results are
 * domain values ({@code Duration}, node ids); nothing here knows how they will be displayed. This is the
 * only package of the model that may touch Spring, and only the {@code @Component} stereotype on its three
 * stateless collaborators (enforced by ArchUnit).
 */
package com.hexagonal.workflowlab.domain.model.analysis;
