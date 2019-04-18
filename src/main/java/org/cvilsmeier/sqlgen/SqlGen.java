package org.cvilsmeier.sqlgen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class SqlGen {

	public static final boolean VERBOSE = true;
	public static final String SOURCE_DIR = "src/sample/todo";
	public static final String DEST_FILE = ""; // empty means print code to stdout

	public static final String SQL_CLASS_MARKER = "@" + "sql-class";
	public static final String SQL_TABLE_MARKER = "@" + "sql-table";
	public static final String SQL_NO_WRITE_MARKER = "@" + "sql-no-write";
	public static final String SQL_NO_UPDATE_MARKER = "@" + "sql-no-update";
	public static final String SQL_NO_INSERT_MARKER = "@" + "sql-no-insert";
	public static final String SQL_FIELDS_MARKER = "@" + "sql-fields";
	public static final String SQL_BEGIN_MARKER = "@" + "sql-begin";
	public static final String SQL_END_MARKER = "@" + "sql-end";

	public static void main(String[] args) {
		try {
			File sourceDir = new File(SOURCE_DIR);
			if (!sourceDir.isDirectory()) {
				System.out.println("ERROR: SOURCE_DIR is not a dir: " + sourceDir);
				System.exit(1);
			}
			Optional<File> destFile = Optional.empty();
			if (!DEST_FILE.isEmpty()) {
				File f = new File(DEST_FILE);
				if (!f.isFile()) {
					System.out.println("ERROR: DEST_FILE is not not a file: " + f.getAbsolutePath());
					System.exit(1);
				}
				destFile = Optional.of(f);
			}
			SqlGen s = new SqlGen();
			s.execute(sourceDir, destFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class Field {
		final String javaType;
		final String name;

		Field(String javaType, String name) {
			super();
			this.javaType = javaType;
			this.name = name;
		}
	}

	static class Klass {
		final String name;
		String table;
		boolean write = true;
		boolean insert = true;
		boolean update = true;
		final List<Field> fields = new ArrayList<>();

		Klass(String name, String table) {
			super();
			this.name = name;
			this.table = table;
		}
	}

	public void execute(File sourceFolder, Optional<File> destFile) throws IOException {
		List<Klass> klasses = scanKlasses(sourceFolder);
		String code = generateKlasses(klasses);
		if (destFile.isPresent()) {
			mergeInto(destFile.get(), code);
		} else {
			log("write stdout");
			System.out.println("");
			System.out.println("    // " + SQL_BEGIN_MARKER);
			System.out.println("");
			System.out.println(code);
			System.out.println("    // " + SQL_END_MARKER);
		}
	}

	private List<Klass> scanKlasses(File dir) throws IOException {
		List<Klass> klasses = new ArrayList<>();
		scanDir(dir, klasses);
		return klasses;
	}

	private void scanDir(File dir, List<Klass> klasses) throws IOException {
		for (File fileOrDir : dir.listFiles()) {
			if (fileOrDir.isDirectory()) {
				scanDir(fileOrDir, klasses);
			} else if (fileOrDir.getName().endsWith(".java")) {
				scanFile(fileOrDir, klasses);
			}
		}
	}

	private void scanFile(File file, List<Klass> klasses) throws IOException {
		log("scan " + file);
		Klass klass = null;
		boolean inFieldsBlock = false;
		try (LineNumberReader r = new LineNumberReader(new InputStreamReader(new FileInputStream(file)))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (klass == null) {
					int i = line.indexOf(SQL_CLASS_MARKER);
					if (i >= 0) {
						String name = line.substring(i + SQL_CLASS_MARKER.length() + 1).trim();
						String table = uncap(name) + "s";
						log("  class " + name);
						klass = new Klass(name, table);
						klasses.add(klass);
					}
				}
				if (klass != null) {
					if (line.contains(SQL_TABLE_MARKER)) {
						int i = line.indexOf(SQL_TABLE_MARKER);
						String table = line.substring(i + SQL_TABLE_MARKER.length() + 1).trim();
						log("  table " + table);
						klass.table = table;
					} else if (line.contains(SQL_NO_WRITE_MARKER)) {
						log("  no write");
						klass.write = false;
					} else if (line.contains(SQL_NO_UPDATE_MARKER)) {
						log("  no update");
						klass.update = false;
					} else if (line.contains(SQL_NO_INSERT_MARKER)) {
						log("  no insert");
						klass.insert = false;
					} else if (line.contains(SQL_FIELDS_MARKER)) {
						inFieldsBlock = !inFieldsBlock;
					} else {
						if (inFieldsBlock) {
							line = removePrefix(line, "private");
							line = removePrefix(line, "public");
							line = removePrefix(line, "protected");
							line = removePrefix(line, "final");
							int i = line.indexOf(';');
							if (i > 0) {
								line = line.substring(0, i).trim();
								String[] toks = line.split(" ");
								if (toks.length == 2) {
									log("    " + toks[0] + " " + toks[1]);
									Field field = new Field(toks[0], toks[1]);
									klass.fields.add(field);
								}
							}
						}
					}
				}
			}
		}
	}

	private String removePrefix(String text, String prefix) {
		if (text.startsWith(prefix)) {
			return text.substring(prefix.length()).trim();
		}
		return text;
	}

	private String generateKlasses(List<Klass> klasses) {
		StringBuilder code = new StringBuilder(10 * 1024);
		for (Klass klass : klasses) {
			generateKlass(klass, code);
		}
		return code.toString();
	}

	private void generateKlass(Klass klass, StringBuilder code) {
		code.append("    // class " + klass.name).append("\n");
		code.append("").append("\n");
		String var = uncap(klass.name);
		// _cols
		{
			String nakedCols = "id,createdAt";
			String aliasCols = "alias + \".id,\" + alias + \".createdAt\"";
			for (Field field : klass.fields) {
				nakedCols += "," + field.name;
				aliasCols += " + \",\" + alias + \"." + field.name + "\"";
			}
			code.append("    private String _" + var + "Cols(String alias) {").append("\n");
			code.append("        if (alias == null || alias.isEmpty()) {").append("\n");
			code.append("            return \"" + nakedCols + "\";").append("\n");
			code.append("        }").append("\n");
			code.append("        return " + aliasCols + ";").append("\n");
			code.append("    }").append("\n");
			code.append("\n");
		}
		// _read
		{
			code.append("    private " + klass.name + " _read" + klass.name + "(ResultSet rst, SqlIndex si) throws SQLException {").append("\n");
			code.append("        String id = rst.getString(si.next());").append("\n");
			code.append("        Instant createdAt = toInstant(rst.getTimestamp(si.next()));").append("\n");
			String args = "";
			for (Field field : klass.fields) {
				switch (field.javaType) {
				case "int":
					code.append("        int " + field.name + " = rst.getInt(si.next());").append("\n");
					break;
				case "boolean":
					code.append("        boolean " + field.name + " = rst.getBoolean(si.next());").append("\n");
					break;
				case "String":
					code.append("        String " + field.name + " = rst.getString(si.next());").append("\n");
					break;
				case "Instant":
					code.append("        Instant " + field.name + " = toInstant(rst.getTimestamp(si.next()));").append("\n");
					break;
				default:
					code.append("        " + field.javaType + " " + field.name + " = to" + field.javaType + "(rst.getString(si.next()));").append("\n");
					break;
				}
				args += "," + field.name;
			}
			code.append("        return new " + klass.name + "(id,createdAt" + args + ");").append("\n");
			code.append("    }").append("\n");
			code.append("\n");
		}
		// _write
		if (klass.write) {
			code.append("    private void _write" + klass.name + "(PreparedStatement pst, " + klass.name + " " + var + ", SqlIndex si) throws SQLException {").append("\n");
			code.append("        pst.setTimestamp(si.next(), toTimestamp(" + var + ".getCreatedAt()));").append("\n");
			for (Field field : klass.fields) {
				String getter = "get" + cap(field.name);
				String iser = "is" + cap(field.name);
				switch (field.javaType) {
				case "int":
					code.append("        pst.setInt(si.next(), " + var + "." + getter + "());").append("\n");
					break;
				case "boolean":
					code.append("        pst.setBoolean(si.next(), " + var + "." + iser + "());").append("\n");
					break;
				case "String":
					code.append("        pst.setString(si.next(), " + var + "." + getter + "());").append("\n");
					break;
				case "Instant":
					code.append("        pst.setTimestamp(si.next(), toTimestamp(" + var + "." + getter + "()));").append("\n");
					break;
				default:
					code.append("        pst.setString(si.next(), toVarchar(" + var + "." + getter + "()));").append("\n");
					break;
				}
			}
			code.append("    }").append("\n");
			code.append("\n");
		}
		// _insert
		if (klass.insert) {
			String cols = "id,createdAt";
			String placers = "?,?";
			for (Field field : klass.fields) {
				cols += "," + field.name;
				placers += ",?";
			}
			code.append("    private void _insert" + klass.name + "(Connection con, " + klass.name + " " + var + ") throws SQLException {").append("\n");
			code.append("        String sql = \"INSERT INTO " + klass.table + "(" + cols + ") VALUES(" + placers + ")\";").append("\n");
			code.append("        update(con, sql, pst -> {").append("\n");
			code.append("            SqlIndex si = new SqlIndex();").append("\n");
			code.append("            pst.setString(si.next(), " + var + ".getId());").append("\n");
			code.append("            _write" + klass.name + "(pst, " + var + ", si);").append("\n");
			code.append("        });").append("\n");
			code.append("    }").append("\n");
			code.append("\n");
		}
		// _update
		if (klass.update) {
			StringJoiner cols = new StringJoiner(",");
			cols.add("createdAt=?");
			for (Field field : klass.fields) {
				cols.add(field.name + "=?");
			}
			code.append("    private void _update" + klass.name + "(Connection con, " + klass.name + " " + var + ") throws SQLException {").append("\n");
			code.append("        String sql = \"UPDATE " + klass.table + " SET " + cols + " WHERE id=?\";").append("\n");
			code.append("        update(con, sql, pst -> {").append("\n");
			code.append("            SqlIndex si = new SqlIndex();").append("\n");
			code.append("            _write" + klass.name + "(pst, " + var + ", si);").append("\n");
			code.append("            pst.setString(si.next(), " + var + ".getId());").append("\n");
			code.append("        });").append("\n");
			code.append("    }").append("\n");
			code.append("\n");
		}
	}

	private String cap(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private String uncap(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	private static void mergeInto(File file, String code) throws IOException {
		StringBuilder textBuilder = new StringBuilder(10 * 1024);
		try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
			boolean pre = true;
			boolean post = false;
			String line;
			while ((line = r.readLine()) != null) {
				if (pre) {
					textBuilder.append(line).append(System.lineSeparator());
					if (line.contains(SQL_BEGIN_MARKER)) {
						textBuilder.append(System.lineSeparator());
						textBuilder.append(System.lineSeparator());
						textBuilder.append(code).append(System.lineSeparator());
						pre = false;
						post = false;
					}
				} else if (post) {
					textBuilder.append(line).append(System.lineSeparator());
				} else {
					if (line.contains(SQL_END_MARKER)) {
						textBuilder.append(line).append(System.lineSeparator());
						pre = false;
						post = true;
					}
				}
			}
		}
		String text = textBuilder.toString();
		if( ! text.isEmpty() ) {
			log("merged code into " + file);
			try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
				w.write(text);
			}
		} else {
			log("destFile did not contain "+SQL_BEGIN_MARKER+" and/or "+SQL_END_MARKER);
		}
	}

	private static void log(String s) {
		if (VERBOSE) {
			System.out.println(s);
		}
	}
}
