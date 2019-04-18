package org.cvilsmeier.todo.db;


public abstract class Entity {

	private String id;

	public Entity(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
