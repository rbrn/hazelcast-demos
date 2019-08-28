package com.pluralsight.hazelcast.client.services.reporting;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.stream.IntStream;

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
public class DistributedDataDataStoreWithMetadataTest {

    @Autowired
    ReportingService reportingService;

    @Autowired
    CustomerDao customerDao;


    @Autowired
    @Qualifier("ClientInstance")
    private HazelcastInstance hazelcastInstance;

    @Test
    public void testMapStoreWithExpirationDateKeys(){
        IntStream.rangeClosed(1, 100).forEach(i -> customerDao.save(createCustomer(Long.valueOf(i))));


        Map<Long, Customer> customers = hazelcastInstance.getMap("customers");
        ((IMap<Long, Customer>) customers).destroy();
        System.out.println(customers.size());

        assertThat(customers.size(), is(100));

    }


    public static Customer createCustomer(Long valueOf) {
        Customer customer = new Customer(valueOf, "Name"+valueOf.intValue(), new Date(), "email");
        return  customer;

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