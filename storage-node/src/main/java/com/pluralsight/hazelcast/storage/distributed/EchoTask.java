package com.pluralsight.hazelcast.storage.distributed;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class EchoTask implements Serializable, HazelcastInstanceAware, Callable<String> {


    String input = null;
    private transient HazelcastInstance hazelcastInstance;
    public EchoTask() {
    }
    public void setHazelcastInstance( HazelcastInstance hazelcastInstance ) {
        this.hazelcastInstance = hazelcastInstance;
    }
    public EchoTask(String input) {
        this.input = input;
    }
    public String call() {
        System.out.println(hazelcastInstance.getCluster().getLocalMember().toString() + ":" + input);
        return hazelcastInstance.getCluster().getLocalMember().toString() + ":" +input;

    }

}
