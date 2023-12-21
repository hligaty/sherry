package io.github.hligaty.timeWheel;

import java.util.ArrayList;

public class PrintInsertTimeList<E> extends ArrayList<E> {

    private final long startTime = System.currentTimeMillis();

    @Override
    public synchronized boolean add(E e) {
        System.out.println(STR."value [\{e}] insert time: [\{System.currentTimeMillis() - startTime}]");
        return super.add(e);
    }

}
