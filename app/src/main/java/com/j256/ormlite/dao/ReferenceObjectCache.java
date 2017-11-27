package com.j256.ormlite.dao;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for ORMLite which stores objects with a {@link WeakReference} or {@link SoftReference} to them. Java Garbage
 * Collection can then free these objects if no one has a "strong" reference to the object (weak) or if it runs out of
 * memory (soft).
 * 
 * @author graywatson
 */
public class ReferenceObjectCache implements ObjectCache {

	private final ConcurrentHashMap<Class<?>, Map<Object, Reference<Object>>> classMaps =
			new ConcurrentHashMap<Class<?>, Map<Object, Reference<Object>>>();
	private final boolean useWeak;

	/**
	 * @param useWeak
	 *            Set to true if you want the cache to use {@link WeakReference}. If false then the cache will use
	 *            {@link SoftReference}.
	 */
	public ReferenceObjectCache(boolean useWeak) {
		this.useWeak = useWeak;
	}

	/**
	 * Create and return an object cache using {@link WeakReference}.
	 */
	public static ReferenceObjectCache makeWeakCache() {
		return new ReferenceObjectCache(true);
	}

	/**
	 * Create and return an object cache using {@link SoftReference}.
	 */
	public static ReferenceObjectCache makeSoftCache() {
		return new ReferenceObjectCache(false);
	}

	@Override
	public synchronized <T> void registerClass(Class<T> clazz) {
		Map<Object, Reference<Object>> objectMap = classMaps.get(clazz);
		if (objectMap == null) {
			objectMap = new ConcurrentHashMap<Object, Reference<Object>>();
			classMaps.put(clazz, objectMap);
		}
	}

	@Override
	public <T, ID> T get(Class<T> clazz, ID id) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return null;
		}
		Reference<Object> ref = objectMap.get(id);
		if (ref == null) {
			return null;
		}
		Object obj = ref.get();
		if (obj == null) {
			objectMap.remove(id);
			return null;
		} else {
			@SuppressWarnings("unchecked")
			T castObj = (T) obj;
			return castObj;
		}
	}

	@Override
	public <T, ID> void put(Class<T> clazz, ID id, T data) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			if (useWeak) {
				objectMap.put(id, new WeakReference<Object>(data));
			} else {
				objectMap.put(id, new SoftReference<Object>(data));
			}
		}
	}

	@Override
	public <T> void clear(Class<T> clazz) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			objectMap.clear();
		}
	}

	@Override
	public void clearAll() {
		for (Map<Object, Reference<Object>> objectMap : classMaps.values()) {
			objectMap.clear();
		}
	}

	@Override
	public <T, ID> void remove(Class<T> clazz, ID id) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			objectMap.remove(id);
		}
	}

	@Override
	public <T, ID> T updateId(Class<T> clazz, ID oldId, ID newId) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return null;
		}
		Reference<Object> ref = objectMap.remove(oldId);
		if (ref == null) {
			return null;
		}
		objectMap.put(newId, ref);
		@SuppressWarnings("unchecked")
		T castObj = (T) ref.get();
		return castObj;
	}

	@Override
	public <T> int size(Class<T> clazz) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap == null) {
			return 0;
		} else {
			return objectMap.size();
		}
	}

	@Override
	public int sizeAll() {
		int size = 0;
		for (Map<Object, Reference<Object>> objectMap : classMaps.values()) {
			size += objectMap.size();
		}
		return size;
	}

	/**
	 * Run through the map and remove any references that have been null'd out by the GC.
	 */
	public <T> void cleanNullReferences(Class<T> clazz) {
		Map<Object, Reference<Object>> objectMap = getMapForClass(clazz);
		if (objectMap != null) {
			cleanMap(objectMap);
		}
	}

	/**
	 * Run through all maps and remove any references that have been null'd out by the GC.
	 */
	public <T> void cleanNullReferencesAll() {
		for (Map<Object, Reference<Object>> objectMap : classMaps.values()) {
			cleanMap(objectMap);
		}
	}

	private void cleanMap(Map<Object, Reference<Object>> objectMap) {
		Iterator<Entry<Object, Reference<Object>>> iterator = objectMap.entrySet().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getValue().get() == null) {
				iterator.remove();
			}
		}
	}

	private Map<Object, Reference<Object>> getMapForClass(Class<?> clazz) {
		Map<Object, Reference<Object>> objectMap = classMaps.get(clazz);
		if (objectMap == null) {
			return null;
		} else {
			return objectMap;
		}
	}
}
