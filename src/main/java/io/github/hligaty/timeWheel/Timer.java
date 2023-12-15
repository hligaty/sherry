package io.github.hligaty.timeWheel;

import java.time.Duration;
import java.util.Set;

public interface Timer {

    void newTimeout(Runnable task, Duration delay);
    
    Set<Runnable> stop();

}
