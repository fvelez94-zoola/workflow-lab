/**
 * Driven adapter that writes protocol documents.
 *
 * <p>Owns: how a format-free {@code ProtocolSheet} becomes Markdown or CSV, plus the formatting helpers
 * ({@code MarkdownTable}, {@code CsvEscaper}). May depend on: the domain model and Spring. Invariant:
 * these helpers are package-private on purpose; nothing outside this adapter can reach them, and the
 * domain cannot depend on this package at all.
 */
package com.hexagonal.workflowlab.infrastructure.adapter.protocoldocument;
