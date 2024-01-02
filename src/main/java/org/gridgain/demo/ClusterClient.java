package org.gridgain.demo;

import org.apache.ignite.cache.CacheKeyConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.collision.priorityqueue.PriorityQueueCollisionSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.gridgain.control.agent.processor.deployment.ManagedDeploymentSpi;
import org.gridgain.demo.model.CarSales;
import org.gridgain.demo.model.CarSalesProfits;
import org.gridgain.demo.model.CarSalesProfitsKey;
import org.gridgain.grid.configuration.GridGainConfiguration;

import java.io.Closeable;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/*
  This is a simple class that wraps the GridGain configuration
  and provides the ability to start & stop the GridGain server.
  This class also populates the cache configuration for CAR_SALES
  and CAR_SALES_PROFITS caches. These two caches are created just
  after starting the GridGain client application.
 */
public class ClusterClient implements Closeable {

    public static final String SQL_SCHEMA = "PUBLIC";

    public static final String CAR_SALES_CACHE_NAME = "CAR_SALES";

    public static final String CAR_SALES_PROFITS_CACHE_NAME = "CAR_SALES_PROFITS";

    private Ignite ignite = null;

    private static IgniteCache<String, CarSales> carSalesCache;

    private static IgniteCache<CarSalesProfitsKey, CarSalesProfits> carSalesProfitsCache;

    public ClusterClient() {
        System.setProperty("IGNITE_EVENT_DRIVEN_SERVICE_PROCESSOR_ENABLED", "true");
    }

    public Ignite startClient() {
        IgniteConfiguration iConfig = new IgniteConfiguration();
        iConfig.setDeploymentSpi(new ManagedDeploymentSpi());
        PriorityQueueCollisionSpi colSpi = new PriorityQueueCollisionSpi();
        colSpi.setPriorityAttributeKey("myPriorityAttributeKey");
        colSpi.setParallelJobsNumber(5);
        iConfig.setCollisionSpi(colSpi);
        iConfig.setClientMode(true);
        iConfig.setDiscoverySpi(new TcpDiscoverySpi()
                        .setIpFinder(new TcpDiscoveryVmIpFinder()
                                .setAddresses(Collections.singleton("127.0.0.1:47500..47509"))));
        iConfig.setPeerClassLoadingEnabled(true);
        GridGainConfiguration gConfig = new GridGainConfiguration();
        gConfig.setDataCenterId(Byte.valueOf("1"));
        gConfig.setLicenseUrl("C:\\Users\\peter.whitney\\license\\gridgain-ultimate-license.xml");
        gConfig.setRollingUpdatesEnabled(true);
        iConfig.setPluginConfigurations(gConfig);

        // Start the client
        ignite = Ignition.start(iConfig);

        // Configure the caches
        carSalesCache = ignite.getOrCreateCache(new CarSalesConfiguration<String, CarSales>());
        carSalesProfitsCache = ignite.getOrCreateCache(new CarSalesProfitsConfiguration<CarSalesProfitsKey, CarSalesProfits>());

        ignite.log().debug("org.gridgain.demo.ClusterClient ignite client started");
        return ignite;
    }

    public void close() {
        ignite.log().debug("org.gridgain.demo.ClusterClient closing ignite client");
        ignite.close();
    }

    // Java configuration for the CarSales collection
    // This is the original data to be queries using a group by statement.
    private static class CarSalesConfiguration<K, V> extends CacheConfiguration<String, CarSales> {

        public CarSalesConfiguration() {
            // Set required cache configuration properties.
            setName(CAR_SALES_CACHE_NAME);
            setCacheMode(CacheMode.PARTITIONED);
            setSqlSchema(SQL_SCHEMA);
            setStatisticsEnabled(true);
            QueryEntity qe = new QueryEntity(String.class, CarSales.class)
                    .setTableName(CAR_SALES_CACHE_NAME)
                    .addQueryField("vin", String.class.getName(), "vin")
                    .addQueryField("saleDate", Date.class.getName(), "sale_date")
                    .addQueryField("purchaseCost", Double.class.getName(), "purchase_cost")
                    .addQueryField("salePrice", Double.class.getName(), "sale_price")
                    .setKeyFieldName("vin")
                    .setIndexes(Arrays.asList(new QueryIndex("vin")));
            setQueryEntities(Arrays.asList(qe));
        }
    }

    // Java configuration for the CarSalesProfits collection
    // This is the collection to hold temporary results.
    // This collection uses affinity by saleDate
    private static class CarSalesProfitsConfiguration<K, V> extends CacheConfiguration<CarSalesProfitsKey, CarSalesProfits> {

        public CarSalesProfitsConfiguration() {

            // Set required cache configuration properties.
            setName(CAR_SALES_PROFITS_CACHE_NAME);
            setCacheMode(CacheMode.PARTITIONED);
            setSqlSchema(SQL_SCHEMA);
            setStatisticsEnabled(true);
            CacheKeyConfiguration affinityKey = new CacheKeyConfiguration(CarSalesProfitsKey.class);
            affinityKey.setAffinityKeyFieldName("saleDate");
            setKeyConfiguration(affinityKey);
            QueryEntity qe = new QueryEntity(CarSalesProfitsKey.class, CarSalesProfits.class)
                    .setTableName(CAR_SALES_PROFITS_CACHE_NAME)
                    .addQueryField("id", UUID.class.getName(), null)
                    .addQueryField("saleDate", Date.class.getName(), "sale_date")
                    .addQueryField("totalExpense", Double.class.getName(), "total_expense")
                    .addQueryField("totalIncome", Double.class.getName(), "total_income")
                    .addQueryField("isFinal", Boolean.class.getName(), "is_final")
                    .setKeyFields(new HashSet<String>(Arrays.asList("id", "saleDate")))
                    .setKeyType(CarSalesProfitsKey.class.getName())
                    .setIndexes(Arrays.asList(new QueryIndex("id"), new QueryIndex("saleDate")));
            setQueryEntities(Arrays.asList(qe));
        }
    }

}
