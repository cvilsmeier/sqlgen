package todo.db.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hsqldb.jdbc.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import todo.db.Db;
import todo.db.Task;
import todo.db.TaskStatus;
import todo.db.User;

public class SqlDb implements Db, AutoCloseable {

	public static final String DB_URL = "jdbc:hsqldb:mem:mymemdb";
	public static final String DB_USER = "SA";
	public static final String DB_PASSWORD = "";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final JDBCPool pool;

	public SqlDb() throws Exception {
		super();
		Class.forName("org.hsqldb.jdbc.JDBCDriver");
		logger.info("connect " + DB_URL);
		this.pool = new JDBCPool();
		this.pool.setUrl(DB_URL);
		this.pool.setUser(DB_USER);
		this.pool.setPassword(DB_PASSWORD);
		this.pool.setLoginTimeout(5);
		tx("create schema", con -> {
			execSql(con, "CREATE TABLE users (" + //
			" id VARCHAR(100) PRIMARY KEY NOT NULL" + //
			", login VARCHAR(100) NOT NULL UNIQUE" + //
			", password VARCHAR(100) NOT NULL" + //
			")");
			execSql(con, "CREATE TABLE tasks (" + //
			" id VARCHAR(100) PRIMARY KEY NOT NULL" + //
			", created TIMESTAMP NOT NULL" + //
			", title VARCHAR(100) NOT NULL" + //
			", userId VARCHAR(100) NOT NULL REFERENCES users(id)" + //
			", status VARCHAR(100) NOT NULL" + //
			")");
		});
	}

	private void execSql(Connection con, String sql) throws SQLException {
		try (Statement st = con.createStatement()) {
			st.execute(sql);
		}
	}

	@Override
	public void saveUser(User user) {
		tx("saveUser", con -> {
			if (user.getId() == null) {
				user.setId(uuid());
				_insertUser(con, user);
			} else {
				_updateUser(con, user);
			}
		});
	}

	@Override
	public List<User> findUsers() {
		SqlList<User> users = new SqlList<>();
		SqlList<Task> tasks = new SqlList<>();
		tx("findUsers", con -> {
			String sql = "SELECT " + _userCols("users") + ", " + _taskCols("tasks") + " FROM users " + //
			" LEFT JOIN tasks ON tasks.userId=users.id " + //
			" ORDER BY users.login, tasks.created";
			query(con, sql, rst -> {
				SqlIndex si = new SqlIndex();
				users.add(_readUser(rst, si));
				tasks.add(_readTask(rst, si));
			});
			for (User user : users.list()) {
				user.setTasks(new ArrayList<>());
			}
			for (Task task : tasks.list()) {
				User user = users.need(task.getUserId());
				user.getTasks().add(task);
			}
		});
		return users.list();
	}

	@Override
	public void saveTask(Task task) {
		tx("saveTask", con -> {
			if (task.getId() == null) {
				task.setId(uuid());
				_insertTask(con, task);
			} else {
				_updateTask(con, task);
			}
		});
	}

	@Override
	public List<Task> findTasks() {
		SqlList<Task> tasks = new SqlList<>();
		SqlList<User> users = new SqlList<>();
		tx("findTasks", con -> {
			String sql = "SELECT " + _taskCols("tasks") + ", " + _userCols("users");
			sql += " FROM tasks ";
			sql += " LEFT JOIN users ON users.id = tasks.userId ";
			sql += " ORDER BY tasks.created";
			query(con, sql, rst -> {
				SqlIndex si = new SqlIndex();
				tasks.add(_readTask(rst, si));
				users.add(_readUser(rst, si));
			});
			for (Task task : tasks.list()) {
				User user = users.need(task.getUserId());
				task.setUser(user);
			}
		});
		return tasks.list();
	}

	@Override
	public void close() throws Exception {
		pool.close(0);
	}

	// @sql-begin


    // class Task

    private String _taskCols(String alias) {
        if (alias == null || alias.isEmpty()) {
            return "id,created,userId,title,status";
        }
        return alias + ".id" + "," + alias + ".created" + "," + alias + ".userId" + "," + alias + ".title" + "," + alias + ".status";
    }

    private Task _readTask(ResultSet rst, SqlIndex si) throws SQLException {
        String id = rst.getString(si.next());
        Instant created = toInstant(rst.getTimestamp(si.next()));
        String userId = rst.getString(si.next());
        String title = rst.getString(si.next());
        TaskStatus status = toTaskStatus(rst.getString(si.next()));
        return new Task(id,created,userId,title,status);
    }

    private void _writeTask(PreparedStatement pst, Task task, SqlIndex si) throws SQLException {
        pst.setTimestamp(si.next(), toTimestamp(task.getCreated()));
        pst.setString(si.next(), task.getUserId());
        pst.setString(si.next(), task.getTitle());
        pst.setString(si.next(), toVarchar(task.getStatus()));
    }

    private void _insertTask(Connection con, Task task) throws SQLException {
        String sql = "INSERT INTO tasks(id,created,userId,title,status) VALUES(?,?,?,?,?)";
        update(con, sql, pst -> {
            SqlIndex si = new SqlIndex();
            pst.setString(si.next(), task.getId());
            _writeTask(pst, task, si);
        });
    }

    private void _updateTask(Connection con, Task task) throws SQLException {
        String sql = "UPDATE tasks SET created=?,userId=?,title=?,status=? WHERE id=?";
        update(con, sql, pst -> {
            SqlIndex si = new SqlIndex();
            _writeTask(pst, task, si);
            pst.setString(si.next(), task.getId());
        });
    }

    // class User

    private String _userCols(String alias) {
        if (alias == null || alias.isEmpty()) {
            return "id,login,password";
        }
        return alias + ".id" + "," + alias + ".login" + "," + alias + ".password";
    }

    private User _readUser(ResultSet rst, SqlIndex si) throws SQLException {
        String id = rst.getString(si.next());
        String login = rst.getString(si.next());
        String password = rst.getString(si.next());
        return new User(id,login,password);
    }

    private void _writeUser(PreparedStatement pst, User user, SqlIndex si) throws SQLException {
        pst.setString(si.next(), user.getLogin());
        pst.setString(si.next(), user.getPassword());
    }

    private void _insertUser(Connection con, User user) throws SQLException {
        String sql = "INSERT INTO users(id,login,password) VALUES(?,?,?)";
        update(con, sql, pst -> {
            SqlIndex si = new SqlIndex();
            pst.setString(si.next(), user.getId());
            _writeUser(pst, user, si);
        });
    }

    private void _updateUser(Connection con, User user) throws SQLException {
        String sql = "UPDATE users SET login=?,password=? WHERE id=?";
        update(con, sql, pst -> {
            SqlIndex si = new SqlIndex();
            _writeUser(pst, user, si);
            pst.setString(si.next(), user.getId());
        });
    }


	// @sql-end

	@FunctionalInterface
	static interface Worker {
		void work(Connection con) throws SQLException;
	}

	private void tx(String name, Worker worker) {
		try (Connection con = pool.getConnection()) {
			try {
				long t1 = System.nanoTime();
				con.setAutoCommit(false);
				worker.work(con);
				if (logger.isTraceEnabled()) {
					logger.trace("commit");
				}
				con.commit();
				long t2 = System.nanoTime();
				long millis = (t2 - t1) / 1_000_000;
				if (millis >= 2000) {
					logger.info("tx '" + name + "' took long: " + millis + " ms");
				}
			} catch (SQLException wex) {
				logger.info("tx error: " + wex, wex);
				try {
					logger.info("rollback");
					con.rollback();
				} catch (SQLException rex) {
					logger.info("cannot rollback a tx: " + rex, rex);
				}
				throw new SqlFailure(wex);
			}
		} catch (SQLException cex) {
			logger.info("cannot get db connection: " + cex, cex);
			throw new SqlFailure(cex);
		}
	}

	@FunctionalInterface
	private static interface Reader {
		void read(ResultSet rst) throws SQLException;
	}

	private void query(Connection con, String sql, Reader r) throws SQLException {
		if (logger.isTraceEnabled()) {
			logger.trace(sql);
		}
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			try (ResultSet rst = pst.executeQuery()) {
				while (rst.next()) {
					r.read(rst);
				}
			}
		}
	}

	@FunctionalInterface
	private static interface Writer {
		void write(PreparedStatement pst) throws SQLException;
	}

	private void update(Connection con, String sql, Writer w) throws SQLException {
		if (logger.isTraceEnabled()) {
			logger.trace(sql);
		}
		try (PreparedStatement pst = con.prepareStatement(sql)) {
			w.write(pst);
			pst.executeUpdate();
		}
	}

	private String toVarchar(TaskStatus status) {
		if (status == null) {
			return null;
		}
		return status.name();
	}

	private TaskStatus toTaskStatus(String name) {
		if (name == null) {
			return null;
		}
		return TaskStatus.valueOf(name);
	}

	private Timestamp toTimestamp(Instant instant) {
		if (instant == null) {
			return null;
		}
		return Timestamp.from(instant);
	}

	private Instant toInstant(Timestamp timestamp) {
		if (timestamp == null) {
			return null;
		}
		return timestamp.toInstant();
	}

	private String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
