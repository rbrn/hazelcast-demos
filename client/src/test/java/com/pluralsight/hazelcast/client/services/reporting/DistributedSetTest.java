package com.pluralsight.hazelcast.client.services.reporting;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicReference;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.ringbuffer.OverflowPolicy;
import com.hazelcast.ringbuffer.Ringbuffer;
import com.pluralsight.hazelcast.client.HazelcastClientTestConfiguration;
import com.pluralsight.hazelcast.client.helper.StorageNodeFactory;
import com.pluralsight.hazelcast.shared.Customer;
import com.pluralsight.hazelcast.shared.Transaction;
import com.pluralsight.hazelcast.storage.CustomerDao;
import com.pluralsight.hazelcast.storage.StorageNodeApplication;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
public class DistributedSetTest {

    @Autowired
    ReportingService reportingService;

    @Autowired
    CustomerDao customerDao;


    @Autowired
    @Qualifier("ClientInstance")
    private HazelcastInstance hazelcastInstance;

    @Test
    public void testMapWithDestryReloadskeys(){
        IntStream.rangeClosed(1, 100).forEach(i -> customerDao.save(createCustomer(Long.valueOf(i))));


        Map<Long, Customer> customers = hazelcastInstance.getMap("customers");
        ((IMap<Long, Customer>) customers).destroy();
        System.out.println(customers.size());

        assertThat(customers.size(), is(100));

    }


    @Test
    public void testMapWithLoadAllReloadskeys(){
        IntStream.rangeClosed(1, 100).forEach(i -> customerDao.save(createCustomer(Long.valueOf(i))));


        Map<Long, Customer> customers = hazelcastInstance.getMap("customers");
        ((IMap<Long, Customer>) customers).loadAll(true);
        System.out.println(customers.size());

        assertThat(customers.size(), is(100));

    }

    private Customer createCustomer(Long valueOf) {
        Customer customer = new Customer(valueOf, "Name"+valueOf.intValue(), new Date(), "email");
        return  customer;

    }

    @Test
    public void testGetIncome() throws Exception {

        Set<Transaction> transactions = hazelcastInstance.getSet("SetDemo");

        transactions.add(createTransation(1L));
        transactions.add(createTransation(2L));

        transactions.stream().forEach(
                f -> System.out.println(f.toString())
        );
    }

    @Test
    public void testGetQueue() throws Exception {

        Queue<Transaction> transactions = hazelcastInstance.getQueue("QueueDemo");

        IntStream.range(1, 100).forEach(i -> transactions.offer(createTransation(Long.valueOf(i))));

        try {
            Transaction firstTransaction = ((IQueue<Transaction>) transactions).poll(1000, TimeUnit.MILLISECONDS);
            Transaction lastTransaction = ((IQueue<Transaction>) transactions).take();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetRingBuffer() throws Exception {

        Ringbuffer<Transaction> transactions = hazelcastInstance.getRingbuffer("RingBufferDemo");

        IntStream.rangeClosed(1, 300).forEach(i -> transactions.addAsync(createTransation(Long.valueOf(i)), OverflowPolicy.OVERWRITE));

        try {
            Transaction firstTransaction = transactions.readOne(transactions.tailSequence());

            assertTrue(firstTransaction.getCustomerId().equals(300L));
            transactions.addAsync(createTransation(Long.valueOf(100)), OverflowPolicy.OVERWRITE);
            firstTransaction = transactions.readOne(transactions.tailSequence());
            assertTrue(firstTransaction.getCustomerId().equals(100L));

            assertThat(transactions.size(), is(5L));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testAtomicReference() throws Exception {

        IAtomicReference<Transaction> transactions = hazelcastInstance.getAtomicReference("atomicReferenceDemo");

        IntStream.rangeClosed(1, 10).forEach(i -> transactions.setAsync(createTransation(Long.valueOf(i))));

        Transaction firstTransaction = transactions.get();

        assertTrue(firstTransaction.getCustomerId().equals(10L));

    }

    private Transaction createTransation(Long value) {
        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.TEN);
        transaction.setCustomerId(value);
        transaction.setTransactionDateTime(new Date());
        System.out.println("Created transaction " + transaction.toString());
        return transaction;
    }


    public Calendar getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        DateUtils.truncate(cal, Calendar.DATE);
        return cal;
    }


    @Bean(name = "ClientInstance")
    public HazelcastInstance clientInstance(StorageNodeFactory storageNodeFactory, ClientConfig config) throws Exception {
        //Ensure there is at least 1 running instance();
        storageNodeFactory.ensureClusterSize(1);
        return HazelcastClient.newHazelcastClient(config);
    }


}