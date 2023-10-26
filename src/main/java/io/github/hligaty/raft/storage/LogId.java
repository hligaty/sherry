package io.github.hligaty.raft.storage;

import javax.annotation.Nonnull;
import java.io.Serializable;

public record LogId(
        long index,
        long term
) implements Serializable, Comparable<LogId> {
    @Override
    public int compareTo(@Nonnull LogId o) {
        final int c = Long.compare(term, o.term);
        if (c == 0) {
            return Long.compare(index, o.index);
        } else {
            return c;
        }
    }
}
