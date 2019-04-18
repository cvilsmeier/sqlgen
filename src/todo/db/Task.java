package todo.db;

import java.time.Instant;

//@sql-class Task
public class Task extends Entity {

	// @sql-fields
	private Instant created;
	private String userId; // -> users.id, the user responsible for this task
	private String title;
	private TaskStatus status;
	// @sql-fields
	private User user = null;

	public Task(String id, Instant created, String userId, String title, TaskStatus status) {
		super(id);
		this.created = created;
		this.userId = userId;
		this.title = title;
		this.status = status;
	}

	public Instant getCreated() {
		return created;
	}

	public void setCreated(Instant created) {
		this.created = created;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
