package com.cqu.drs.model;

/**
 * Shared lifecycle status for both a {@link DisasterReport} and a {@link ResponseTask}:
 * reported/assessed -&gt; assigned -&gt; in progress -&gt; resolved.
 */
public enum Status {
    REPORTED,
    ASSESSED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED
}
