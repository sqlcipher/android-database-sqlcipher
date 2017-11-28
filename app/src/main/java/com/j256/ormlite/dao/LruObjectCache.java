package com.j256.ormlite.dao;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for ORMLite which stores a certain number of items for each Class. Inserting an object into the cache once it
 * is full will cause the least-recently-used object to be ejected. They can be injected into a dao with the
 * {@link Dao#setObjectCache(ObjectCache)}.
 * 
 * <p>
 * <b>NOTE:</b> If you set the capacity to be 100 then each <i>Class</i> will allow 100 items in the cache. If you have
 * 5 classes then the cache will hold 500 objects.
 * </p>
 * 
 * @author graywatson
 */
public class LruObjectCache implements ObjectCache {

	private final int capacity;
	private final ConcurrentHashMap<Class<?>, Map<Object, Object>> classMaps =
			new ConcurrentHashMap<Class<?>, Map<Object, Object>>();

	public LruObjectCache(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public synchronized <T> void registerClass(Class<T> clazz) {
		Map<Object, Object> objectMap = classMaps.get(clazz);
		if (objectMap == null) {
			objectMap = Collections.synchronizedMap(new LimitedLinkedHashMap<Object, Object>(capacity));
			classMaps.put(clazz, objectMap);
		}
	}

	@Override
	public <T, ID> T get(Class<T> clazz, ID id) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return null;
		}
		Object obj = objectMap.get(id);
		@SuppressWarnings("unchecked")
		T castObj = (T) obj;
		return castObj;
	}

	@Override
	public <T, ID> void put(Class<T> clazz, ID id, T data) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			objectMap.put(id, data);
		}
	}

	@Override
	public <T> void clear(Class<T> clazz) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			objectMap.clear();
		}
	}

	@Override
	public void clearAll() {
		for (Map<Object, Object> objectMap : classMaps.values()) {
			objectMap.clear();
		}
	}

	@Override
	public <T, ID> void remove(Class<T> clazz, ID id) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			objectMap.remove(id);
		}
	}

	@Override
	public <T, ID> T updateId(Class<T> clazz, ID oldId, ID newId) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return null;
		}
		Object obj = objectMap.remove(oldId);
		if (obj == null) {
			return null;
		}
		objectMap.put(newId, obj);
		@SuppressWarnings("unchecked")
		T castObj = (T) obj;
		return castObj;
	}

	@Override
	public <T> int size(Class<T> clazz) {
		Map<Object, Object> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return 0;
		} else {
			return objectMap.size();
		}
	}

	@Override
	public int sizeAll() {
		int size = 0;
		for (Map<Object, Object> objectMap : classMaps.values()) {
			size += objectMap.size();
		}
		return size;
	}

	private Map<Object, Object> getMapForClass(Class<?> clazz) {
		Map<Object, Object> objectMap = classMaps.get(clazz);
		if (objectMap == null) {
			return null;
		} else {
			return objectMap;
		}
	}

	/**
	 * Little extension of the LimitedLinkedHashMap
	 */
	private static class LimitedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {

		private static final long serialVersionUID = -4566528080395573236L;
		private final int capacity;

		public LimitedLinkedHashMap(int capacity) {
			super(capacity, 0.75F, true);
			this.capacity = capacity;
		}

		@Override
		protected boolean removeEldestEntry(Entry<K, V> eldest) {
			return size() > capacity;
		}
	}
}
