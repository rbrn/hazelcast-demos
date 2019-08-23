package com.pluralsight.hazelcast.storage;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.pluralsight.hazelcast.shared.MapNames;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static java.util.Arrays.asList;

@Configuration
@SpringBootApplication
@ComponentScan
@EnableJpaRepositories
public class StorageNodeApplication implements MapNames {

    @Bean(destroyMethod = "shutdown")
    public HazelcastInstance createStorageNode(
            @Qualifier("StorageNodeConfig")
            Config config
    ) throws Exception {
        return Hazelcast.newHazelcastInstance(config);
    }



    @Bean(name="StorageNodeConfig")
    public Config config(CustomersMapStore customersMapStore) throws Exception {
        Config config = new Config();

        //Create a new map configuration for the customers map
        MapConfig customerMapConfig = new MapConfig();

        //Create a map store config for the customer information
        MapStoreConfig customerMapStoreConfig = new MapStoreConfig();
        customerMapStoreConfig.setImplementation(customersMapStore);
        customerMapStoreConfig.setWriteDelaySeconds(3);

        //Update the customers map configuration to use the
        //customers map store config we just created
        customerMapConfig.setMapStoreConfig(customerMapStoreConfig);
        customerMapConfig.setName("customers");

        MapIndexConfig dobFieldIndex = new MapIndexConfig("dob", true);
        customerMapConfig.addMapIndexConfig(dobFieldIndex);

        //Add the customers map config to our storage node config
        config.addMapConfig(customerMapConfig);

        config.addSetConfig(createSetConfig());

        config.addQueueConfig(createQueueConfig());

        config.addRingBufferConfig(getRingBufferConfig());

        config.addAtomicReferenceConfig(createAtomicReferenceCOnfig());

        return config;
    }

    private AtomicReferenceConfig createAtomicReferenceCOnfig() {
        AtomicReferenceConfig atomicReferenceConfig = new AtomicReferenceConfig("atomicReferenceDemo");
        return atomicReferenceConfig;
    }

    private QueueConfig createQueueConfig() {
        QueueConfig queueConfig = new QueueConfig();

        queueConfig.setMaxSize(5);
        queueConfig.setName("QueueDemo");
        queueConfig.setStatisticsEnabled(true);


        queueConfig.setItemListenerConfigs(asList(new ItemListenerConfig(new ItemListener() {
            @Override
            public void itemAdded(ItemEvent itemEvent) {
                System.out.println(itemEvent.getItem());
            }

            @Override
            public void itemRemoved(ItemEvent itemEvent) {

            }
        }, true)));
        return queueConfig;
    }

    private SetConfig createSetConfig() {
        SetConfig setConfig =new SetConfig();
        setConfig.setName("SetDemo");
        return setConfig;
    }

    private RingbufferConfig getRingBufferConfig(){
        RingbufferConfig rbConfig = new RingbufferConfig("RingBufferDemo").setCapacity(5);
        return rbConfig;
    }

    public static void main(String[] args) {
        SpringApplication.run(StorageNodeApplication.class, args);
    }


}
