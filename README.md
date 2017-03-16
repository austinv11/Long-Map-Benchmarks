# Long-Map-Benchmarks
Benchmarking the best way to store long, Object value pairs in a map in a threadsafe way.

### Running the benchmarks
In order to run this set of benchmarks:
1. Clone the repository
2. Run `gradlew jmh`
3. Look at the results in the `build/reports/jmh/results.csv` file

**Note**: This test is expected to run for ~8 hours.

# Analysis
## Collections Tested
* `ConcurrentHashMap<Long, Object>` (Built-in JDK collections)
* `HashMap<Long, Object>` (Built-in JDK collections)
* `TLongObjectHashMap<Object>` ([Trove 3.0.3](http://trove.starlight-systems.com/))
* `SynchronizedTLongObjectHashMap<Object>` ([Trove 3.03](http://trove.starlight-systems.com/))
* `LongObjectHashMap<Object>` ([Eclipse Collections 8.0.0](https://www.eclipse.org/collections/))
* `SynchronizedLongObjectHashMap<Object>` ([Eclipse Collections 8.0.0](https://www.eclipse.org/collections/))
* `HashLongObjMap<Object>` ([Koloboke 1.0.0](https://koloboke.com/))
* `HashLongObjectMap<Object>` ([HPPCRT 0.7.4](https://github.com/vsonnier/hppcrt))
* `Long2ObjectArrayMap<Object>` ([Fastutil 7.1.0](http://fastutil.di.unimi.it/))
* `Long2ObjectOpenHashMap<Object>` ([Fastutil 7.1.0](http://fastutil.di.unimi.it/))
* `Long2ObjectHashMap<Object>` ([Agrona 0.9.3](https://github.com/real-logic/Agrona))

## Methodology
Each map undergoes 4 seperate operations tested. `Copy`, `Get`, `Remove`, and `Update`. And for each operation, the throughput, average time, sampled time and single-shot time is measured.

**Note**: For non-concurrent implementations, a manual `synchronized` block is used after the declaration of the map using the map as the lock.

For each test type, the map being tested is populated with 1 thousand key value pairs. Each pair's key is incremented by 1, starting at 0 and each value is a random object which can either be: `Integer`, `Long`, `Boolean`, `Float`, `Double`, `byte[]` (of variable size between 0-1000 random bytes), `String` (of variable length from 0-250 random chars), and `ArbitraryPOJO` which holds 2 objects in seperate fields which are randomly generated objects ranging from any of the aforementioned types.

### For the `Copy` test:
The map tested is copied using a provided method when available, otherwise it is manually constructed.

### For the `Get` test:
The map tested attempts to do (map.size/2) successful data retrievals and (map.size/2) unsuccessful data retrievals.

### For the `Remove` test:
The map tested is manually iterated through and has one key,value pair cleared per iteration.

### For the `Update` test:
The map tesed is manually iterated through and each key has a new value assigned to it.

## Potential Issues With These Benchmarks
As with any statistics, nothing should be taken definitively. So here are potential issues with these particular benchmarks:
* Agrona's "Get" test failed, this is likely due to it not failing silently with an incorrect key 
* This test does *not* test concurrent access ability. It is designed with the intention of the maps being used in a multithreaded environment with few actually simultaneous method invocations.
* There is not enough data to do a true statistical analysis so while general trends can be seen, the concrete numbers are not nescessarily accurate or precise. 
* This was done in Java 8, using JMH version 1.12 (which is slightly outdated). We were forced to use 1.12 because of incompatibility issues with the gradle plugin at versions greater than 1.12.

## The Data
### Copy
#### Throughput results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-16.png "Benchmark")
**What this suggests**: Fastutil's Long2ObjectArrayMap is the worst performing with the fewest amount of ops/ms and ConcurrentHashMap doesn't do much better. However, Koloboke's HashLongObjMap and Fastutil's Long2ObjectOpenHashMap are significantly more performant than other maps. Additionally, Eclipse's two map implementations perform about equally and are close runner-ups to the most performant in this test.
#### Average Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-15.png "Benchmark")
**What this suggests**: Most map implementations have similar copy times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap are considerably slower than the rest.
#### Sample Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-14.png "Benchmark")
**What this suggests**: Most map implementations have similar copy times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap are considerably slower than the rest.
#### Single-Shot Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-13.png "Benchmark")
**What this suggests**: Most map implementations have similar copy times, however ConcurrentHashMap is considerably slower. This means that most maps perform similarly on "cold" runs.
### Get
#### Throughput results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-12.png "Benchmark")
**What this suggests**: Fastutil's Long2ObjectArrayMap is the worst performing with the fewest amount of ops/ms and ConcurrentHashMap doesn't do much better. However, HashMap, Koloboke's HashLongObjMap and Fastutil's Long2ObjectOpenHashMap are significantly more performant than other maps for retrieving data. Additionally, Trove's two map implementations perform about equally and are close runner-ups to the most performant in this test.
#### Average Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-11.png "Benchmark")
**What this suggests**: Most map implementations have similar data retrieval times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap are considerably slower than the rest.
#### Sample Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-10.png "Benchmark")
**What this suggests**: Most map implementations have similar data retrieval times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap are considerably slower than the rest.
#### Single-Shot Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-9.png "Benchmark")
**What this suggests**: Most map implementations have similar data retrieval times, however ConcurrentHashMap and (and especially) Fastutil's Long2ObjectArrayMap are considerably slower. This means that these maps perform poorly on "cold" runs.
### Remove
#### Throughput results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-8.png "Benchmark")
**What this suggests**: Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMap are the worst performing maps with the fewest amount of ops/ms and ConcurrentHashMap doesn't do much better. However, HashMap, Koloboke's HashLongObjMap and Eclipse's LongObjectHashMap are significantly more performant than other maps for removing data.
#### Average Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-7.png "Benchmark")
**What this suggests**: Most map implementations have similar data removal times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower than the rest.
#### Sample Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-6.png "Benchmark")
**What this suggests**: Most map implementations have similar data removal times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower than the rest.
#### Single-Shot Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-5.png "Benchmark")
**What this suggests**: Most map implementations have similar data removal times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower. This means that these maps perform poorly on "cold" runs.
### Update
#### Throughput results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-4.png "Benchmark")
**What this suggests**: Fastutil's Long2ObjectArrayMap is the worst performing with the fewest amount of ops/ms and ConcurrentHashMap doesn't do much better. However, Koloboke's HashLongObjMap, Agrona's Long2ObjectHashMap and Fastutil's Long2ObjectOpenHashMap are significantly more performant than other maps for updating data.
#### Average Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-3.png "Benchmark")
**What this suggests**: Most map implementations have similar data update times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower than the rest.
#### Sample Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-2.png "Benchmark")
**What this suggests**: Most map implementations have similar data update times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower than the rest.
#### Single-Shot Time results
![bench](https://raw.githubusercontent.com/austinv11/Long-Map-Benchmarks/master/graphs/graphs-1.png "Benchmark")
**What this suggests**: Most map implementations have similar data update times, however ConcurrentHashMap (and especially) Fastutil's Long2ObjectArrayMap and Agrona's Long2ObjectHashMaps are considerably slower than the rest.

## Potential Conclusions
* While not always the *most* performant, Koloboke's `HashLongObjMap` *consistently* one of the fastest collections measured. This makes Koloboke's collection the most favorable for most general cases.
* Fastutil's `Long2ObjectOpenHashMap` is a close runner-up for general use-cases, but it slows down slightly on remove operations.
* Fastutil's `Long2ObjectArrayMap` and Agrona's `Long2ObjectHashMaps` a consistently on the bottom of performance tests.
* For this specific use case, manually synchronized HashMaps are preferred to ConcurrentHashMaps.
