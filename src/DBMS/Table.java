package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

public class Table implements Serializable {
	public static final long serialVersionUID = 1L;

	public String tableName;
	public int pagesCount;
	public String[] columnNames;
	public int recordsCount;
	public ArrayList<String> trace;

	public ArrayList<String> indexedColumns;

	public Table(String name, String[] columns) {
		this.tableName = name;
		this.pagesCount = 0;
		this.columnNames = columns;
		this.recordsCount = 0;
		this.trace = new ArrayList<>();
		this.indexedColumns = new ArrayList<>();
	}

	public void addTrace(String operation) {
		trace.add(operation);
	}

	public String getFullTrace() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < trace.size(); i++) {
			sb.append(trace.get(i)).append("\n");
		}

		ArrayList<String> sorted = new ArrayList<>(indexedColumns);
		Collections.sort(sorted);
		sb.append("Pages Count: ").append(pagesCount)
				.append(", Records Count: ").append(recordsCount)
				.append(", Indexed Columns: ").append(sorted.toString());
		return sb.toString();
	}

	public String getLastTrace() {
		if (trace.isEmpty()) {
			return "";
		}
		return trace.get(trace.size() - 1);
	}

	public int getColumnIndex(String colName) {
		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i].equals(colName)) {
				return i;
			}
		}
		return -1;
	}
}
