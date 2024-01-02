package org.gridgain.demo;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteDataStreamer;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteRunnable;
import org.gridgain.demo.model.CarSalesProfits;
import org.gridgain.demo.model.CarSalesProfitsKey;

import java.sql.Date;
import java.util.*;

/*
  This class issues a local only query that gathers the grouping results that were spread
  around the cluster by the prior MapEmulation process. Leveraging the fatc that the
  CAR_SALES_PROFITS collection uses affinity by sale_date all groups that previously existed
  on all hosts from the CAR_SALES collection are now co-located by sale_date omto specific
  hosts. These records will be receive a final summation by this class via the code below!
 */
public class OptimalQueryReduceEmulation implements IgniteRunnable {

    private Ignite ignite_;

    private long startTime_;

    private Date savedDate_ = null;
    private double savedExpense_ = 0.0;
    private double savedIncome_ = 0.0;

    private List<CarSalesProfits> savedResults_;

    public OptimalQueryReduceEmulation(Ignite ignite, long startTime) {
        ignite_ = ignite;
        startTime_ = startTime;
        savedResults_ = new ArrayList<CarSalesProfits>();
    }

    public void run() {
        IgniteCache carSalesProfitsCache = ignite_.getOrCreateCache(new CacheConfiguration("CAR_SALES_PROFITS"));
        // Here's our core query:
        FieldsQueryCursor<List<?>> results = carSalesProfitsCache.query(
                new SqlFieldsQuery("SELECT sale_date, SUM(total_expense) AS total_expense, SUM(total_income) AS total_income FROM CAR_SALES_PROFITS GROUP BY sale_date ORDER BY sale_date;")
                        .setSchema("PUBLIC")
                        .setLocal(true));

        // Now we iterate through the query results for each local node and sum up the local results.
        // These are saved, then the local results are deleted and the summed results are saved back
        // to the same collection.
        // It should be noted that you would save time by simply doing whatever you need done with your
        // local results as your final step.
        // I only execute the delete and re-save operation as a demonstration which could later be queried
        // and verified.
        try (IgniteDataStreamer<CarSalesProfitsKey, CarSalesProfits> streamer = ignite_.dataStreamer("CAR_SALES_PROFITS")) {
            Iterator<List<?>> iter = results.iterator();
            boolean isFirstRow = true;
            while (iter.hasNext()) {
                List<?> row = iter.next();
                Date currentRowDate = ((Date) row.get(0));
                Double currentRowTotalExpense = ((Double) row.get(1));
                Double currentRowTotalIncome = ((Double) row.get(2));
                accumulateOrSendAndSaveResultsFor(isFirstRow, currentRowDate, currentRowTotalExpense, currentRowTotalIncome);
                isFirstRow = false;
            }
            saveResultsData();
            long endTime = System.currentTimeMillis();
            ignite_.log().info("Processing duration = " + (endTime - startTime_) + " ms.");
            deletePriorResults(carSalesProfitsCache);
            sendResultsData(streamer);
        }
    }

    // There may be more than 1 row that makes up the results for each group, so save the prior results
    // and determine if you now have a new date which would indicate a new grouping row and as such the
    // prior results can be saved.
    private void accumulateOrSendAndSaveResultsFor(boolean isFirstRow, Date aDate, Double expense, Double income) {
        if(isFirstRow) {
            savedDate_ = aDate;
            savedExpense_ = expense.doubleValue();
            savedIncome_ = income.doubleValue();
        } else {
            if(aDate.equals(savedDate_)) {
                savedExpense_ += expense.doubleValue();
                savedIncome_ += income.doubleValue();
            } else {
                saveResultsData();
                savedDate_ = aDate;
                savedExpense_ = expense.doubleValue();
                savedIncome_ = income.doubleValue();
            }
        }
    }

    // Simple add to a list for later transmission
    private void saveResultsData() {
        savedResults_.add(new CarSalesProfits(java.util.UUID.randomUUID(), savedDate_, savedExpense_, savedIncome_, true));
    }

    // Clear out the prior results before saving the data stored in the list.
    private void deletePriorResults(IgniteCache carSalesProfitsCache) {
        carSalesProfitsCache.query(
                new SqlFieldsQuery("DELETE FROM CAR_SALES_PROFITS;")
                    .setSchema("PUBLIC")
                    .setLocal(true))
                    .getAll();
    }

    // Stream results back to the same collection.
    // Note that this streaming operation will be local only too as the data
    // that this node processed will be saved on this same node too.
    // As a result there is no network transmission for this operation either!
    private void sendResultsData(IgniteDataStreamer<CarSalesProfitsKey, CarSalesProfits> streamer) {
        for(CarSalesProfits profits : savedResults_) {
            streamer.addData(new CarSalesProfitsKey(profits.getId(), profits.getSaleDate()), profits);
        }
    }

}
