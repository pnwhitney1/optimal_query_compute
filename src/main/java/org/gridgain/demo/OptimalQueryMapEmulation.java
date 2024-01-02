package org.gridgain.demo;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteRunnable;
import org.gridgain.demo.model.CarSalesProfitsKey;
import org.gridgain.demo.model.CarSalesProfits;

import java.sql.Date;
import java.util.Iterator;
import java.util.List;

/*
  This class issues a local only query that provides the summation for each sale_date for
  the local data. Local summations are distributed to all hosts based on the sale_date
  because the CAR_SALES_PROFITS cache uses affinity by sale_date for its affinity key.
  As a result all hosts will receive data from each host's local query results. This
  forces a fan out of data from each host for subsequent processing which will also be
  local queries in the reduce phase.
 */
public class OptimalQueryMapEmulation implements IgniteRunnable {

    private Ignite ignite_;

    public OptimalQueryMapEmulation(Ignite ignite) {
        ignite_ = ignite;
    }

    public void run() {
        IgniteCache utilityCache = ignite_.getOrCreateCache(new CacheConfiguration("utilityCache"));

        // Note that this query executes only against the data stored locally on each node.
        FieldsQueryCursor<List<?>> results = utilityCache.query(
                new SqlFieldsQuery("SELECT sale_date, SUM(purchase_cost) AS total_expense, SUM(sale_price) AS total_income FROM CAR_SALES GROUP BY sale_date;")
                        .setSchema("PUBLIC")
                        .setLocal(true));

        // Iterate through the local data and stream 1 single instance for each result out to the CAR_SALES_PROFITS collection
        // This collection has affinity by saleDate. As a result certain dates will all be stored on certain hosts.
        // We will leverage this saleDate locality on the next reduce operation to calculate the grouping results for each host.
        try (IgniteDataStreamer<CarSalesProfitsKey, CarSalesProfits> streamer = ignite_.dataStreamer("CAR_SALES_PROFITS")) {
            Iterator<List<?>> iter = results.iterator();
            while (iter.hasNext()) {
                List<?> row = iter.next();
                Date currentDate = ((Date) row.get(0));
                Double currentTotalExpense = ((Double) row.get(1));
                Double currentTotalIncome = ((Double) row.get(2));
                saveResultsFor(streamer, currentDate, currentTotalExpense, currentTotalIncome);
            }
        }
    }

    // The Streamer will send the CarSaleProfits results to the proper host based on the saleDate
    // As such this will involve each host sending some data to each of the other hosts in a cluster.
    // But the core benefit is that the total result set does not go to just 1 single host and potentially
    // overload that host with data! All host share a portion of the total result set that is steamed out by
    // each of the mapper hosts.
    private void saveResultsFor(IgniteDataStreamer<CarSalesProfitsKey, CarSalesProfits> streamer, Date aDate, double expense, double income) {
        CarSalesProfitsKey key = new CarSalesProfitsKey(java.util.UUID.randomUUID(), aDate);
        CarSalesProfits profits = new CarSalesProfits(key.getId(), key.getSaleDate(), expense, income, false);
        streamer.addData(key, profits);
    }

}
