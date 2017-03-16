package com.austinv11.bench;

import com.koloboke.collect.map.hash.HashLongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import gnu.trove.impl.sync.TSynchronizedLongObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import it.unimi.dsi.fastutil.longs.AbstractLong2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.agrona.collections.Long2ObjectHashMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class MapTests {
	
	@State(Scope.Benchmark)
	public static class Context {
		
		private static final int ARRAY_SIZE = 1_000;

		public long[] testKeys;
		public Object[] testValues;
		public Object[] testValues2;
		public Random random;

		@Setup(Level.Trial)
		public void init() {
			random = new Random();
			testKeys = new long[ARRAY_SIZE];
			testValues = new Object[ARRAY_SIZE];
			testValues2 = new Object[ARRAY_SIZE];
			populateData();
		}

		@TearDown(Level.Trial)
		public void clean() {
			testKeys = null;
			testValues = null;
			random = null;
		}

		private void populateData() {
			for (int i = 0; i < testKeys.length; i++) {
				testKeys[i] = i;
				testValues[i] = randObject();
				testValues2[i] = randObject();
			}
		}

		private Object randObject() {
			switch (random.nextInt(8)) {
				case 0: //Int
					return Integer.MIN_VALUE + random.nextInt(Integer.MAX_VALUE);
				case 1: //Long
					return random.nextLong();
				case 2: //Boolean
					return random.nextBoolean();
				case 3: //Float
					return random.nextFloat();
				case 4: //Double
					return random.nextDouble();
				case 5: 
					byte[] bytes = new byte[random.nextInt(ARRAY_SIZE)];
					random.nextBytes(bytes);
					return bytes;
				case 6: //String
					int length = random.nextInt(ARRAY_SIZE / 4);
					StringBuilder buffer = new StringBuilder(length);
					for (int i = 0; i < length; i++)
						buffer.append((char)random.nextInt(Character.MAX_VALUE));
					return buffer.toString();
				case 7: //Arbitrary object
					return new ArbitraryPOJO();
			}
			return null;
		}

		public class ArbitraryPOJO {

			public Object obj1, obj2;

			public ArbitraryPOJO() {
				obj1 = randObject();
				obj2 = randObject();
			}

			@Override
			public int hashCode() {
				int result = obj1 != null ? obj1.hashCode() : 0;
				result = 31*result+(obj2 != null ? obj2.hashCode() : 0);
				return result;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o)
					return true;
				if (o == null || getClass() != o.getClass())
					return false;

				ArbitraryPOJO that = (ArbitraryPOJO) o;

				if (obj1 != null ? !obj1.equals(that.obj1) : that.obj1 != null)
					return false;
				return obj2 != null ? obj2.equals(that.obj2) : that.obj2 == null;
			}

			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}

			@Override
			public String toString() {
				return super.toString();
			}

			@Override
			protected void finalize() throws Throwable {
				super.finalize();
			}
		}
	}
	
	private void mapGet(Context context, Map map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void mapPutUpdate(Context context, Map map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void mapPutRemove(Context context, Map map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}

	//JDK Maps
	
	@Benchmark
	public void synchronizedHashMapGet(Context context) {
		HashMap<Long, Object> map = new HashMap<>();

		synchronized (map) {
			mapGet(context, map);
		}
	}

	@Benchmark
	public void synchronizedHashMapPutUpdate(Context context) { 
		HashMap<Long, Object> map = new HashMap<>();

		synchronized (map) {
			mapPutUpdate(context, map);
		}
	}

	@Benchmark
	public void synchronizedHashMapPutRemove(Context context) {
		HashMap<Long, Object> map = new HashMap<>();

		synchronized (map) {
			mapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public HashMap synchronizedHashMapCopy(Context context) { //Populates a map, then copies it
		HashMap<Long, Object> map = new HashMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			HashMap<Long, Object> copy = new HashMap<>(map);
			return copy;
		}
	}
	
	@Benchmark
	public void concurrentHashMapGet(Context context) {
		ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap<>();
		
		mapGet(context, map);
	}
	
	@Benchmark
	public void concurrentHashMapPutUpdate(Context context) {
		ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap<>();
		
		mapPutUpdate(context, map);
	}
	
	@Benchmark
	public void concurrentHashMapPutRemove(Context context) {
		ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap<>();
		
		mapPutRemove(context, map);
	}
	
	@Benchmark
	public ConcurrentHashMap concurrentHashMapCopy(Context context) { //Populates a map, then copies it
		ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap<>();
		
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Copy!
		ConcurrentHashMap<Long, Object> copy = new ConcurrentHashMap<>(map);
		return copy;
	}
	
	//Fastutil
	private void fastutilMapGet(Context context, AbstractLong2ObjectMap map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void fastutilMapPutUpdate(Context context, AbstractLong2ObjectMap map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void fastutilMapPutRemove(Context context, AbstractLong2ObjectMap map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectArrayMapGet(Context context) {
		Long2ObjectArrayMap<Object> map = new Long2ObjectArrayMap<>();
		
		synchronized (map) {
			fastutilMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectArrayMapPutUpdate(Context context) {
		Long2ObjectArrayMap<Object> map = new Long2ObjectArrayMap<>();
		
		synchronized (map) {
			fastutilMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectArrayMapPutRemove(Context context) {
		Long2ObjectArrayMap<Object> map = new Long2ObjectArrayMap<>();
		
		synchronized (map) {
			fastutilMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public Long2ObjectArrayMap synchronizedLong2ObjectArrayMapCopy(Context context) {
		Long2ObjectArrayMap<Object> map = new Long2ObjectArrayMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			Long2ObjectArrayMap<Object> copy = map.clone();
			return copy;
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectOpenHashMapGet(Context context) {
		Long2ObjectOpenHashMap<Object> map = new Long2ObjectOpenHashMap<>();
		
		synchronized (map) {
			fastutilMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectOpenHashMapPutUpdate(Context context) {
		Long2ObjectOpenHashMap<Object> map = new Long2ObjectOpenHashMap<>();
		
		synchronized (map) {
			fastutilMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectOpenHashMapPutRemove(Context context) {
		Long2ObjectOpenHashMap<Object> map = new Long2ObjectOpenHashMap<>();
		
		synchronized (map) {
			fastutilMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public Long2ObjectOpenHashMap synchronizedLong2ObjectOpenHashMapCopy(Context context) {
		Long2ObjectOpenHashMap<Object> map = new Long2ObjectOpenHashMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			Long2ObjectOpenHashMap<Object> copy = map.clone();
			return copy;
		}
	}
	
	//Eclipse Collections
	private void eclipseMapGet(Context context, MutableLongObjectMap map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void eclipseMapPutUpdate(Context context, MutableLongObjectMap map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void eclipseMapPutRemove(Context context, MutableLongObjectMap map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	private ImmutableLongObjectMap eclipseMapCopy(Context context, MutableLongObjectMap map) { //Populates a map, then copies it to an immutable copy
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		return map.toImmutable();
	}
	
	@Benchmark
	public void manualSynchronizedLongObjectHashMapGet(Context context) {
		LongObjectHashMap<Object> map = new LongObjectHashMap<>();
		
		synchronized (map) {
			eclipseMapGet(context, map);
		}
	}
	
	@Benchmark
	public void manualSynchronizedLongObjectHashMapPutUpdate(Context context) {
		LongObjectHashMap<Object> map = new LongObjectHashMap<>();
		
		synchronized (map) {
			eclipseMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void manualSynchronizedLongObjectHashMapPutRemove(Context context) {
		LongObjectHashMap<Object> map = new LongObjectHashMap<>();
		
		synchronized (map) {
			eclipseMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public ImmutableLongObjectMap manualSynchronizedLongObjectHashMapCopy(Context context) {
		LongObjectHashMap<Object> map = new LongObjectHashMap<>();
		
		synchronized (map) {
			return eclipseMapCopy(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLongObjectHashMapGet(Context context) {
		MutableLongObjectMap<Object> map = new LongObjectHashMap<>().asSynchronized();
		
		eclipseMapGet(context, map);
	}
	
	@Benchmark
	public void synchronizedLongObjectHashMapPutUpdate(Context context) {
		MutableLongObjectMap<Object> map = new LongObjectHashMap<>().asSynchronized();
		
		eclipseMapPutUpdate(context, map);
	}
	
	@Benchmark
	public void synchronizedLongObjectHashMapPutRemove(Context context) {
		MutableLongObjectMap<Object> map = new LongObjectHashMap<>().asSynchronized();
		
		eclipseMapPutRemove(context, map);
	}
	
	@Benchmark
	public ImmutableLongObjectMap synchronizedLongObjectHashMapCopy(Context context) {
		MutableLongObjectMap<Object> map = new LongObjectHashMap<>().asSynchronized();
		return eclipseMapCopy(context, map);
	}
	
	//Koloboke
	private void kolobokeMapGet(Context context, HashLongObjMap map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void kolobokeMapPutUpdate(Context context, HashLongObjMap map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void kolobokeMapPutRemove(Context context, HashLongObjMap map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjMapGet(Context context) {
		HashLongObjMap<Object> map = HashLongObjMaps.newMutableMap();
		
		synchronized (map) {
			kolobokeMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjMapUpdate(Context context) {
		HashLongObjMap<Object> map = HashLongObjMaps.newMutableMap();
		
		synchronized (map) {
			kolobokeMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjMapPutRemove(Context context) {
		HashLongObjMap<Object> map = HashLongObjMaps.newMutableMap();
		
		synchronized (map) {
			kolobokeMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public HashLongObjMap synchronizedHashLongObjMapMapCopy(Context context) {
		HashLongObjMap<Object> map = HashLongObjMaps.newMutableMap();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			HashLongObjMap<Object> copy = HashLongObjMaps.newMutableMap(map);
			return copy;
		}
	}
	
	//Trove
	private void troveMapGet(Context context, TLongObjectMap map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void troveMapPutUpdate(Context context, TLongObjectMap map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void troveMapPutRemove(Context context, TLongObjectMap map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	@Benchmark
	public void manualSnchronizedTLongObjectHashMapGet(Context context) {
		TLongObjectHashMap<Object> map = new TLongObjectHashMap<>();
		
		synchronized (map) {
			troveMapGet(context, map);
		}
	}
	
	@Benchmark
	public void manualSnchronizedTLongObjectHashMapUpdate(Context context) {
		TLongObjectHashMap<Object> map = new TLongObjectHashMap<>();
		
		synchronized (map) {
			troveMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void manualSnchronizedTLongObjectHashMapPutRemove(Context context) {
		TLongObjectHashMap<Object> map = new TLongObjectHashMap<>();
		
		synchronized (map) {
			troveMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public TLongObjectHashMap manualSnchronizedTLongObjectHashMapMapCopy(Context context) {
		TLongObjectHashMap<Object> map = new TLongObjectHashMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			TLongObjectHashMap<Object> copy = new TLongObjectHashMap<>(map);
			return copy;
		}
	}
	
	@Benchmark
	public void synchronizedTLongObjectHashMapGet(Context context) {
		TSynchronizedLongObjectMap<Object> map = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
		
		synchronized (map) {
			troveMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedTLongObjectHashMapUpdate(Context context) {
		TSynchronizedLongObjectMap<Object> map = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
		
		synchronized (map) {
			troveMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedTLongObjectHashMapPutRemove(Context context) {
		TSynchronizedLongObjectMap<Object> map = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
		
		synchronized (map) {
			troveMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public TSynchronizedLongObjectMap synchronizedTLongObjectHashMapMapCopy(Context context) {
		TSynchronizedLongObjectMap<Object> map = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>());
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			TSynchronizedLongObjectMap<Object> copy = new TSynchronizedLongObjectMap<>(new TLongObjectHashMap<>(map));
			return copy;
		}
	}
	
	//hppcrt
	private void hppcrtMapGet(Context context, com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void hppcrtMapPutUpdate(Context context, com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void hppcrtMapPutRemove(Context context, com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjectMapGet(Context context) {
		com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map = new com.carrotsearch.hppcrt.maps.LongObjectHashMap<>();
		
		synchronized (map) {
			hppcrtMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjectMapUpdate(Context context) {
		com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map = new com.carrotsearch.hppcrt.maps.LongObjectHashMap<>();
		
		synchronized (map) {
			hppcrtMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedHashLongObjectMapPutRemove(Context context) {
		com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map = new com.carrotsearch.hppcrt.maps.LongObjectHashMap<>();
		
		synchronized (map) {
			hppcrtMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> synchronizedHashLongObjectMapMapCopy(Context context) {
		com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> map = new com.carrotsearch.hppcrt.maps.LongObjectHashMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			com.carrotsearch.hppcrt.maps.LongObjectHashMap<Object> copy = new com.carrotsearch.hppcrt.maps.LongObjectHashMap<>(map);
			return copy;
		}
	}
	
	//Agrona
	private void agronaMapGet(Context context, Map map) { //Populates a map, makes 50% successful and 50% unsuccessful get() calls
		//Populate the map
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		for (int i = 0; i < context.testValues.length/2; i++) {
			map.get(context.testKeys[context.random.nextInt(context.testKeys.length)]); //Successful call
			map.get(Integer.MIN_VALUE+context.random.nextInt(Integer.MAX_VALUE)-1); //Unsuccessful call
		}
	}
	
	private void agronaMapPutUpdate(Context context, Map map) { //Populates a map, then repopulates it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Update every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.put(context.testKeys[i], context.testValues2[i]);
		}
	}
	
	private void agronaMapPutRemove(Context context, Map map) { //Populates a map, then manually clears the contents of it
		//Populate the map the first time
		for (int i = 0; i < context.testValues.length; i++)
			map.put(context.testKeys[i], context.testValues[i]);
		
		//Remove every map pair
		for (int i = 0; i < context.testValues.length; i++) {
			map.remove(context.testKeys[i]);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectHashMapGet(Context context) {
		Long2ObjectHashMap<Object> map = new Long2ObjectHashMap<>();
		
		synchronized (map) {
			agronaMapGet(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectHashMapPutUpdate(Context context) {
		Long2ObjectHashMap<Object> map = new Long2ObjectHashMap<>();
		
		synchronized (map) {
			agronaMapPutUpdate(context, map);
		}
	}
	
	@Benchmark
	public void synchronizedLong2ObjectHashMapPutRemove(Context context) {
		Long2ObjectHashMap<Object> map = new Long2ObjectHashMap<>();
		
		synchronized (map) {
			agronaMapPutRemove(context, map);
		}
	}
	
	@Benchmark
	public Long2ObjectHashMap synchronizedLong2ObjectHashMapCopy(Context context) { //Populates a map, then copies it
		Long2ObjectHashMap<Object> map = new Long2ObjectHashMap<>();
		
		synchronized (map) {
			//Populate the map the first time
			for (int i = 0; i < context.testValues.length; i++)
				map.put(context.testKeys[i], context.testValues[i]);
			
			//Copy!
			Long2ObjectHashMap<Object> copy = new Long2ObjectHashMap<>();
			copy.putAll(map);
			return copy;
		}
	}
}
