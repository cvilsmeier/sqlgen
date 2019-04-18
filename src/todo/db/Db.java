package todo.db;

import java.util.List;

public interface Db {

	void saveUser(User user);
	
	List<User> findUsers();
	
	void saveTask(Task task);
	
	List<Task> findTasks();
	
}
