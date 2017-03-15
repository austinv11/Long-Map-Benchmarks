package com.austinv11.bench;

import org.openjdk.jmh.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MapTests {
	
	@State(Scope.Benchmark)
	public static class Context {
		
		private static final int ARRAY_SIZE = 64_000;

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
			switch (random.nextInt(9)) {
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
					int length = random.nextInt(ARRAY_SIZE);
					StringBuilder buffer = new StringBuilder(length);
					for (int i = 0; i < length; i++)
						buffer.append((char)random.nextInt(Character.MAX_VALUE));
					return buffer.toString();
				case 7: //Arbitrary object
					return new ArbitraryPOJO();
				case 8: //Null
					return null;
			}
			return null;
		}

		public class ArbitraryPOJO {

			public Object obj1, obj2, obj3;

			public ArbitraryPOJO() {
				obj1 = randObject();
				obj2 = randObject();
				obj3 = randObject();
			}

			@Override
			public int hashCode() {
				int result = obj1 != null ? obj1.hashCode() : 0;
				result = 31*result+(obj2 != null ? obj2.hashCode() : 0);
				result = 31*result+(obj3 != null ? obj3.hashCode() : 0);
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
				if (obj2 != null ? !obj2.equals(that.obj2) : that.obj2 != null)
					return false;
				return obj3 != null ? obj3.equals(that.obj3) : that.obj3 == null;
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
}
