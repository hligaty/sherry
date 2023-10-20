package io.github.hligaty.disruptor;

import com.lmax.disruptor.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiConsumerBatchEventHandler<T> implements EventHandler<T> {

    final Queue<T> queue = new LinkedBlockingQueue<>();

    long sequence = -1;

    final long ordinal;

    final long numberOfConsumers;

    MultiConsumerBatchEventHandler(final long ordinal, final long numberOfConsumers) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
    }

    @Override
    public void onEvent(T event, long sequence, boolean endOfBatch) {
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }
        queue.add(event);
        if (endOfBatch) {
            List<T> list = new ArrayList<>();
            for (long l = this.sequence; l < sequence; l = l + numberOfConsumers) {
                list.add(queue.poll());
            }
            this.sequence = sequence;
            execute(list);
        }
    }

    public void execute(List<T> elements) {

    }
}
