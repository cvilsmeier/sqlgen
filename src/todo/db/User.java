package todo.db;

import java.util.List;

// @sql-class User
public class User extends Entity {

	// @sql-fields
	private String login;
	private String password;
	// @sql-fields
	private List<Task> tasks = null; // the tasks assigned to this user

	public User(String id, String login, String password) {
		super(id);
		this.login = login;
		this.password = password;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

}
