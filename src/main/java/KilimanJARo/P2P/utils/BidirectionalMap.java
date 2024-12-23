package KilimanJARo.P2P.utils;

import java.util.HashMap;
import java.util.Map;

public class BidirectionalMap<K, V> {
	private Map<K, V> forward = new HashMap<>();
	private Map<V, K> backward = new HashMap<>();

	public void put(K key, V value) {
		forward.put(key, value);
		backward.put(value, key);
	}

	public V getByKey(K key) {
		return forward.get(key);
	}

	public K getByValue(V value) {
		return backward.get(value);
	}

	public void removeByKey(K key) {
		V value = forward.remove(key);
		backward.remove(value);
	}

	public void removeByValue(V value) {
		K key = backward.remove(value);
		forward.remove(key);
	}
}
