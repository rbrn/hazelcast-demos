package com.pluralsight.hazelcast.client.services.reporting;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.pluralsight.hazelcast.client.HazelcastClientTestConfiguration;
import com.pluralsight.hazelcast.client.helper.StorageNodeFactory;
import com.pluralsight.hazelcast.shared.Customer;
import com.pluralsight.hazelcast.storage.CustomerDao;
import com.pluralsight.hazelcast.storage.StorageNodeApplication;
import com.pluralsight.hazelcast.storage.distributed.EchoTask;
import com.pluralsight.hazelcast.storage.distributed.SumCustomerIdTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.pluralsight.hazelcast.client.services.reporting.DistributedDataStructuresTest.createCustomer;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by Grant Little (grant@grantlittle.me)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(
        classes = {
                HazelcastClientTestConfiguration.class,
                StorageNodeApplication.class
        }
)
public class DistributedComputingTest {


    @Autowired
    CustomerDao customerDao;

    @Autowired
    @Qualifier("ClientInstance")
    private HazelcastInstance hazelcastInstance;
    private CountDownLatch lock = new CountDownLatch(1);


    /**
     * Test Distrinbuted map. The map is duplicated on each node. Data is transfered from one node to another
     * without partitioning
     */
    @Test
    public void testDistributedCallables() throws InterruptedException, ExecutionException {
        IntStream.rangeClosed(1, 3).forEach(i -> customerDao.save(createCustomer(Long.valueOf(i))));


        Map<Long, Customer> customerIMap = hazelcastInstance.getMap("customers");
        ((IMap<Long, Customer>) customerIMap).destroy();
        System.out.println(customerIMap.size());

        assertThat(customerIMap.size(), is(3));


        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");

        Map<Member, Future<Integer>> stringFuture = iExecutorService.submitToAllMembers(new SumCustomerIdTask());

        lock.await(2000, TimeUnit.MILLISECONDS);

        System.out.println(stringFuture.size());
        Integer result = 0;
        for(Future<Integer> integerFuture : stringFuture.values()){
            System.out.println(integerFuture.get());
            result += integerFuture.get();
        }

        assertThat(result, is(6));
    }


    @Test
    public void testDistributedRunnables() throws InterruptedException {
        IExecutorService iExecutorService = hazelcastInstance.getExecutorService("default");

        Map<Member, Future<String>> future = iExecutorService.submitToAllMembers(new EchoTask("Hello from"));

        lock.await(2000, TimeUnit.MILLISECONDS);

    }



    @Bean(name = "ClientInstance")
    public HazelcastInstance clientInstance(StorageNodeFactory storageNodeFactory, ClientConfig config) throws Exception {
        //Ensure there is at least 1 running instance();
        storageNodeFactory.ensureClusterSize(1);
        return HazelcastClient.newHazelcastClient(config);
    }


}