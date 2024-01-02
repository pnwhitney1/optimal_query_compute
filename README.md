# Optimal Query Operations

## Overview
This program shares a compute job implementation that provides  
optimizations over a standard GridGain SQL query processing that  
has a GROUP BY clause which returns many grouping results.  

The standard GridGain SQL query processing with a large GROUP BY clause may  
be slowed because each host that has grouping results will send those  
results to a reducer operation executing on 1 single host. As a result  
that host becomes the limiting factor because that host has only 1  
network interface card and that interface card has to receive results  
from each of the many hosts that have results. If this is a large cluster  
then there are many hosts sending many grouping results and the process  
slows.  

This compute job provides improved performance by implementing an interium  
cache that uses affinity according to the same element or elements that are  
used in the SQL grouping clause. The Mapping implementation gets distributed  
to all nodes. The Mapping implementation executes against only the local data.  
The local data results are then distributed leveraging affinity by the grouping  
clause to all hosts, not just 1.  

The reduce compute job then gets distributed and it too executes locally to  
gather up the map results and produce a final answer.  

The combination of 2 compute jobs executing on local data and distributing  
result to all hosts and not just one provides a performance boost by  
eliminating the single reducer bottleneck that exists with the standard SQL  
processing solution.  

As noted in the supporting articles you would not use this implementation  
approach for all queries. This extra work would only be warranted for those  
times that you need the most optimal SQL query result times AND you have query  
or cluster conditions that will benefit from this implementation.  

Regardless of if you actually use this implementation or not, you are now much  
further along in understanding how the standard solution works and how  
improvements can be realized should they be needed  

## Build
This application is a standard maven project built using  
maven version 3.8.7  
Java version 11.0.20  

To build this project simply chech out the source code, navigate to the check out  
directory and enter "mvn clean package"  

## Running the Application

### GridGain version
This application was executed against GridGain Ultimate Edition version 8.8.26.  
However it could easily be modified to execute against a wide variety of other  
versions by modifying the gridgain version found in the pom.xml file.  
Modifications to the server configuration can be found in the ClusterClient.java  
source file.  

### Arguments
The program supports either no arguments or  
the defined arguments described as follows:  

No arguments:  
  The program displays a help screen that details how to execute all options.  

-help:  
  The program displays a help screen that details how to execute all options.  

-exec:  
  The program launches all tasks to connect to a configured cluster at 127.0.0.1  
  The program will create CAR_SALES and CAR_SALES_PROFITS collections.  
  The program will populate the CAR_SALES collection with a set of example data  
  The program will then exceute the Map & Reduce compute job implementations.
  
  
### Example Invocations
Example invocations are listed below:  
  java -cp optimal-quey-compute.jar org.gridgain.demo.OptimalQueryComputeMain  
  java -cp optimal-quey-compute.jar org.gridgain.demo.OptimalQueryComputeMain -help  
  java -cp optimal-quey-compute.jar org.gridgain.demo.OptimalQueryComputeMain -exec
