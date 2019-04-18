package todo.db.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import todo.db.Entity;

public class SqlList<E extends Entity> {

	private final ArrayList<E> list = new ArrayList<>();
	private final HashMap<String, E> map = new HashMap<>();

	public void add(E e) {
		if (e != null && e.getId() != null) {
			if (!map.containsKey(e.getId())) {
				list.add(e);
				map.put(e.getId(), e);
			}
		}
	}

	public E find(String id) {
		return map.get(id);
	}

	public E need(String id) {
		E e = map.get(id);
		if (e == null) {
			throw new NullPointerException("entity id '" + id + "' not found in sql list");
		}
		return e;
	}

	public List<E> list() {
		return list;
	}

	public E firstOrNull() {
		return list.isEmpty() ? null : list.get(0);
	}
}