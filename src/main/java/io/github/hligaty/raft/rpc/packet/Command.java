package io.github.hligaty.raft.rpc.packet;

import java.io.Serializable;

public class Command implements Serializable {

    private static final Command EMPTY = Command.write(null);

    private final Option option;
    
    private final Serializable data;

    private Command(Option option, Serializable data) {
        this.option = option;
        this.data = data;
    }
    
    public static Command read(Serializable data) {
        return new Command(Option.READ, data);
    }

    public static Command write(Serializable data) {
        return new Command(Option.WRITE, data);
    }
    
    public static Command noop() {
        return EMPTY;
    }

    public Serializable data() {
        return data;
    }

    public enum Option {
        READ,
        WRITE,
    }
    
    public boolean readOnly() {
        return option == Option.READ;
    }

    @Override
    public String toString() {
        return "Command{" +
               "option=" + option +
               ", data=" + data +
               '}';
    }
}
