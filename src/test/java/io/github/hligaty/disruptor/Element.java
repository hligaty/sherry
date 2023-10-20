package io.github.hligaty.disruptor;

public class Element {

    private Long value;

    public Long get() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Element{" +
               "value=" + value +
               '}';
    }

    public void clear() {
        value = null;
    }

}
