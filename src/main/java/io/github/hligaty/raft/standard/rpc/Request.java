package io.github.hligaty.raft.standard.rpc;

import java.io.Serializable;

public class Request implements Serializable {
    
    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
