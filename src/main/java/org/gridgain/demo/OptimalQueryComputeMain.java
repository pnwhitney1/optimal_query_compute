package org.gridgain.demo;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.lang.IgniteFuture;

/*
  This is the main class for the OptimalQueryCompute application.
  If started with no arguments this class will display a help screen.
  The class can also be started with a -help argument to render the same output.
  Lastly you can start this application with a -exec argument to have it
  pre-populate the CAR_SALES cache and subsequently execute the Map & Reduce
  emulation classes that use local queries to accomplish the same results as
  a query executed via GridGain.
 */
public class OptimalQueryComputeMain {

    private static final String EXEC = new String("-exec");
    private static final String HELP = new String("-help");

    private ClusterClient clusterClient;

    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Invalid invocation!");
            displayHelp();
        } else if(!isValidArgument(args[0])) {
            System.out.println("Invalid invocation!");
            displayHelp();
        } else if (HELP.equals(args[0])) {
            displayHelp();
        } else {
            try (ClusterClient clusterClient = new ClusterClient()) {
                Ignite ignite = clusterClient.startClient();
                IgniteCompute compute = ignite.compute();
                if (EXEC.equals(args[0])) {

                    // Seed the CAR_SALES cache with data
                    OptimalQueryComputeCache cacheJob = new OptimalQueryComputeCache(ignite);
                    compute.run(cacheJob);

                    // Collect the start time before initiating the map operation.
                    long start = System.currentTimeMillis();

                    // Initiate the map operation and wait for its completion.
                    OptimalQueryMapEmulation mapEmulation = new OptimalQueryMapEmulation(ignite);
                    IgniteFuture<Void> result1 = compute.broadcastAsync(mapEmulation);
                    try {
                        while(!result1.isDone()) {
                            Thread.sleep(50);
                        }
                    } catch (InterruptedException ie) {

                    }

                    // Execute the reduce operation.
                    OptimalQueryReduceEmulation reduceEmulation = new OptimalQueryReduceEmulation(ignite, start);
                    compute.broadcast(reduceEmulation);

                    // Record the total processing time
                    System.out.println("Total duration = " + (System.currentTimeMillis() - start) + " ms");
                }
                clusterClient.close();
            }
        }
    }

    private static boolean isValidArgument(String argument) {
        boolean result = false;
        if(EXEC.equals(argument) || HELP.equals(argument)) {
            result = true;
        }
        return result;
    }

    private static void displayHelp() {
        System.out.println("*******************************************************************************");
        System.out.println("Valid arguments are:");
        System.out.println("  -exec which executes the optimal query processing tasks.");
        System.out.println("*******************************************************************************");
        System.out.println("Example invocations follow");
        System.out.println("  java -cp optimal-query-processing.jar org.gridgain.demo.OptimalQueryComputeMain -exec");
        System.out.println("*******************************************************************************");
    }

}
