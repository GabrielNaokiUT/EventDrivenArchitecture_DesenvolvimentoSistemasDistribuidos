package eda.core;

import java.time.Instant;

public record Envelope(String id, Instant occurredAt, Event event) {
}
