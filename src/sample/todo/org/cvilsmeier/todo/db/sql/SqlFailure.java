package org.cvilsmeier.todo.db.sql;
public class SqlFailure extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SqlFailure() {
		super();
	}

	public SqlFailure(String message, Throwable cause) {
		super(message, cause);
	}

	public SqlFailure(String message) {
		super(message);
	}

	public SqlFailure(Throwable cause) {
		super(cause);
	}


}
