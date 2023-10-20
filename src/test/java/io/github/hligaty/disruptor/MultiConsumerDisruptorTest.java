package io.github.hligaty.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import io.github.hligaty.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

public class MultiConsumerDisruptorTest extends BaseTest {

    @Test
    public void multiConsumer() {
        // numberOfProducers 个生产者, numberOfConsumers 个消费者

        int numberOfProducers = 8;
        int numberOfConsumers = 1;

        Disruptor<Element> disruptor = new Disruptor<>(
                Element::new,
                1024,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );

        for (int i = 0; i < numberOfConsumers; i++) {
            disruptor.handleEventsWith(new MultiConsumerBatchEventHandler<>(i, numberOfConsumers) {
                @Override
                public void execute(List<Element> elements) {
                    System.out.println(Thread.currentThread().getName() + ":" + elements);
                    // 如果 ringBufferSize 很大, 并且 Element 对象也很大, 那么每次用完之后释放 Element 对象中的数据引用是一个好的习惯
                    for (Element element : elements) {
                        element.clear();
                    }
                }
            });
        }

        RingBuffer<Element> ringBuffer = disruptor.start();
        
        for (int i = 0; i < numberOfProducers; i++) {
            new Thread(() -> {
                Random random = new Random();
                while (true) {
                    long next = ringBuffer.next();
                    Element element = ringBuffer.get(next);
                    element.setValue(next);
                    ringBuffer.publish(next);
                    sleep(random.nextLong(0, 2));
                }
            }).start();
        }
    }

}
