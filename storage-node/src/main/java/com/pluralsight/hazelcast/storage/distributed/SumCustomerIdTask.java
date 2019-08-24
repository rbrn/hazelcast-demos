package com.pluralsight.hazelcast.storage.distributed;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import com.pluralsight.hazelcast.shared.Customer;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class SumCustomerIdTask implements Callable<Integer>, Serializable, HazelcastInstanceAware {

    private transient HazelcastInstance hazelcastInstance;
    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public Integer call() throws Exception {
        IMap<Long, Customer> map = hazelcastInstance.getMap( "customers" );
        System.out.println(hazelcastInstance.getName());
        Integer result  = map.localKeySet().stream().map(key-> map.get(key).getId()).mapToInt(Long::intValue).reduce(0, (subtotal, element)-> subtotal + element);

        return result;
    }
}
