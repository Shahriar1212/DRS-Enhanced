package com.cqu.drs.model;

/** Lifecycle of a {@link RestorationJob}, derived from its progress percentage. */
public enum JobStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED;

    /** Maps a 0-100 progress value to a status. */
    public static JobStatus fromProgress(int progress) {
        if (progress <= 0) {
            return PLANNED;
        }
        return progress >= 100 ? COMPLETED : IN_PROGRESS;
    }
}
