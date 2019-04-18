package org.cvilsmeier.todo.db.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SqlParams {

	public static SqlParams empty() {
		return new SqlParams();
	}
	
	public static SqlParams of(String v) {
		return new SqlParams().and(v);
	}
	
	public static SqlParams of(int v) {
		return new SqlParams().and(v);
	}
	
	public static SqlParams of(boolean v) {
		return new SqlParams().and(v);
	}
	
	public static SqlParams of(Instant v) {
		return new SqlParams().and(v);
	}
	
	static class Param {
		final int type;
		final Object obj;
		public Param(int type, Object obj) {
			super();
			this.type = type;
			this.obj = obj;
		}
	}
	
	private final List<Param> params = new ArrayList<>();
	
	public SqlParams and(String v) {
		params.add(new Param(Types.VARCHAR, v));
		return this;
	}
	
	public SqlParams and(int v) {
		params.add(new Param(Types.INTEGER, v));
		return this;
	}
	
	public SqlParams and(boolean v) {
		params.add(new Param(Types.BOOLEAN, v));
		return this;
	}
	
	public SqlParams and(Boolean v) {
		params.add(new Param(Types.BOOLEAN, v));
		return this;
	}
	
	public SqlParams and(Instant v) {
		Timestamp ts = v == null ? null : Timestamp.from(v);
		params.add(new Param(Types.TIMESTAMP, ts));
		return this;
	}
	
	void writeTo(PreparedStatement pst, SqlIndex si) throws SQLException {
		for( Param param  : params) {
			pst.setObject(si.next(), param.obj, param.type);
		}
	}
	
}
