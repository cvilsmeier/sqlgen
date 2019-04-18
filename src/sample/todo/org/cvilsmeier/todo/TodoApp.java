package org.cvilsmeier.todo;

import java.time.Instant;
import java.util.List;

import org.cvilsmeier.todo.db.Db;
import org.cvilsmeier.todo.db.Task;
import org.cvilsmeier.todo.db.TaskStatus;
import org.cvilsmeier.todo.db.User;
import org.cvilsmeier.todo.db.sql.SqlDb;

public class TodoApp {

	public static void main(String[] args) {
		try {
			System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
			try (SqlDb db = new SqlDb()) {
				insertData(db);
				dumpData(db);
				updateData(db);
				dumpData(db);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void insertData(Db db) {
		System.out.println("--- insertData");
		// users
		User alice = new User(null, "alice", "aa");
		User bob = new User(null, "bob", "bb");
		User carol = new User(null, "carol", "cc");
		db.saveUser(alice);
		db.saveUser(bob);
		db.saveUser(carol);
		// tasks for alice
		{
			Instant now = Instant.parse("2019-01-01T10:00:00Z");
			Task task1 = new Task(null, now.plusSeconds(1), alice.getId(), "getup", TaskStatus.Done);
			Task task2 = new Task(null, now.plusSeconds(2), alice.getId(), "eat", TaskStatus.InProgress);
			Task task3 = new Task(null, now.plusSeconds(3), alice.getId(), "work", TaskStatus.Waiting);
			db.saveTask(task1);
			db.saveTask(task2);
			db.saveTask(task3);
		}
		// tasks for bob
		{
			Instant now = Instant.parse("2019-01-01T11:00:00Z");
			Task task1 = new Task(null, now.plusSeconds(1), bob.getId(), "code", TaskStatus.Done);
			Task task2 = new Task(null, now.plusSeconds(2), bob.getId(), "ship", TaskStatus.InProgress);
			db.saveTask(task1);
			db.saveTask(task2);
		}
	}
	
	private static void updateData(Db db) {
		System.out.println("--- updateData");
		// users
		List<User> users = db.findUsers();
		for( User user : users ) {
			String newLogin = user.getLogin().toUpperCase();
			user.setLogin(newLogin);
			db.saveUser(user);
		}
		// tasks
		List<Task> tasks = db.findTasks();
		for( Task task : tasks) {
			String newTitle = task.getTitle().toUpperCase();
			task.setTitle(newTitle);
			db.saveTask(task);
		}
	}

	private static void dumpData(Db db) {
		System.out.println("--- dumpData");
		List<User> users = db.findUsers();
		System.out.println("" + users.size() + " users");
		for( User u : users) {
			System.out.println("user " + u.getId() + ", login="+u.getLogin() + ", password="+u.getPassword());
		}
		List<Task> tasks = db.findTasks();
		System.out.println("" + tasks.size() + " tasks");
		for( Task u : tasks) {
			System.out.println("task " + u.getId() + ", created="+u.getCreated() + ", userId="+u.getUserId() + ", title="+u.getTitle() + ", status="+u.getStatus());
		}
	}

	
}
