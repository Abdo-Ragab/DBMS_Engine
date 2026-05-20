package DBMS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

public class DBApp {
	public static int dataPageSize = 2;

	public static void createTable(String tableName, String[] columnsNames) {
		Table table = new Table(tableName, columnsNames);
		String trace = "Table created name:" + tableName + ", columnsNames:"
				+ Arrays.toString(columnsNames);
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);
	}

	public static void insert(String tableName, String[] record) {
		long start = System.currentTimeMillis();

		Table table = FileManager.loadTable(tableName);
		Page page;
		int pageIndex;

		if (table.pagesCount == 0) {
			page = new Page(0);
			pageIndex = 0;
			table.pagesCount++;
		} else {
			page = FileManager.loadTablePage(tableName, table.pagesCount - 1);
			if (page.size() >= dataPageSize) {
				pageIndex = table.pagesCount;
				page = new Page(pageIndex);
				table.pagesCount++;
			} else {
				pageIndex = table.pagesCount - 1;
			}
		}

		page.addRecord(record);
		table.recordsCount++;
		FileManager.storeTablePage(tableName, pageIndex, page);

		updateIndicesOnInsert(tableName, table, record);

		long elapsed = System.currentTimeMillis() - start;

		String trace = "Inserted:" + Arrays.toString(record)
				+ ", at page number:" + pageIndex + ", execution time (mil):"
				+ elapsed;
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);
	}

	public static void updateIndicesOnInsert(String tableName, Table table,
			String[] record) {
		for (int i = 0; i < table.indexedColumns.size(); i++) {
			String colName = table.indexedColumns.get(i);
			BitmapIndex idx = FileManager.loadTableIndex(tableName, colName);
			if (idx == null) {
				continue;
			}
			int colIdx = table.getColumnIndex(colName);
			idx.insertRecord(record[colIdx]);
			FileManager.storeTableIndex(tableName, colName, idx);
		}
	}

	public static ArrayList<String[]> select(String tableName) {
		long start = System.currentTimeMillis();

		Table table = FileManager.loadTable(tableName);
		int pagesCount = table.pagesCount;
		ArrayList<String[]> res = new ArrayList<>();

		for (int i = 0; i < pagesCount; i++) {
			Page page = FileManager.loadTablePage(tableName, i);
			res.addAll(page.records);
		}
		long elapsed = System.currentTimeMillis() - start;
		String traceEntry = "Select all pages:" + pagesCount + ", records:"
				+ res.size() + ", execution time (mil):" + elapsed;
		table.addTrace(traceEntry);
		FileManager.storeTable(tableName, table);

		return res;
	}

	public static ArrayList<String[]> select(String tableName, int pageNumber,
			int recordNumber) {
		long start = System.currentTimeMillis();

		ArrayList<String[]> res = new ArrayList<>();
		Page page = FileManager.loadTablePage(tableName, pageNumber);

		if (page != null && recordNumber >= 0 && recordNumber < page.size()) {
			res.add(page.getRecord(recordNumber));
		}

		long elapsed = System.currentTimeMillis() - start;

		Table table = FileManager.loadTable(tableName);
		String trace = "Select pointer page:" + pageNumber + ", record:"
				+ recordNumber + ", total output count:" + res.size()
				+ ", execution time (mil):" + elapsed;
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);

		return res;
	}

	public static ArrayList<String[]> select(String tableName, String[] cols,
			String[] vals) {
		long start = System.currentTimeMillis();
		Table table = FileManager.loadTable(tableName);
		int pagesCount = table.pagesCount;
		ArrayList<String[]> res = new ArrayList<>();

		int[] columnIndices = new int[cols.length];
		for (int i = 0; i < cols.length; i++) {
			columnIndices[i] = table.getColumnIndex(cols[i]);
		}

		ArrayList<int[]> matchInfo = new ArrayList<>();

		for (int i = 0; i < pagesCount; i++) {
			Page page = FileManager.loadTablePage(tableName, i);
			ArrayList<String[]> records = page.records;

			int matchesInThisPage = 0;

			for (int j = 0; j < records.size(); j++) {
				String[] record = records.get(j);
				boolean match = true;

				for (int k = 0; k < columnIndices.length; k++) {
					int idx = columnIndices[k];
					if (idx < 0 || idx >= record.length
							|| !record[idx].equals(vals[k])) {
						match = false;
						break;
					}
				}
				if (match) {
					res.add(record);
					matchesInThisPage++;
				}
			}

			if (matchesInThisPage > 0) {
				matchInfo.add(new int[] { i, matchesInThisPage });
			}
		}

		long elapsed = System.currentTimeMillis() - start;

		StringBuilder recordsPerPage = new StringBuilder("[");
		for (int k = 0; k < matchInfo.size(); k++) {
			if (k > 0) {
				recordsPerPage.append(", ");
			}
			recordsPerPage.append(Arrays.toString(matchInfo.get(k)));
		}
		recordsPerPage.append("]");

		StringBuilder colVals = new StringBuilder("[");
		for (int j = 0; j < cols.length; j++) {
			if (j > 0) {
				colVals.append(", ");
			}
			colVals.append(cols[j]);
		}
		colVals.append("]->[");
		for (int j = 0; j < vals.length; j++) {
			if (j > 0) {
				colVals.append(", ");
			}
			colVals.append(vals[j]);
		}
		colVals.append("]");

		String traceEntry = "Select condition:" + colVals
				+ ", Records per page:" + recordsPerPage + ", records:"
				+ res.size() + ", execution time (mil):" + elapsed;
		table.addTrace(traceEntry);
		FileManager.storeTable(tableName, table);
		return res;
	}

	public static String getFullTrace(String tableName) {
		Table table = FileManager.loadTable(tableName);
		return table.getFullTrace();
	}

	public static String getLastTrace(String tableName) {
		Table table = FileManager.loadTable(tableName);
		return table.getLastTrace();
	}

	public static void createBitMapIndex(String tableName, String colName) {
		long start = System.currentTimeMillis();

		Table table = FileManager.loadTable(tableName);
		BitmapIndex idx = new BitmapIndex();
		idx.build(tableName, colName);
		FileManager.storeTableIndex(tableName, colName, idx);
		if (!table.indexedColumns.contains(colName)) {
			table.indexedColumns.add(colName);
		}

		long elapsed = System.currentTimeMillis() - start;
		String trace = "Index created for column: " + colName
				+ ", execution time (mil):" + elapsed;
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);
	}

	public static String getValueBits(String tableName, String colName,
			String value) {
		BitmapIndex idx = FileManager.loadTableIndex(tableName, colName);
		if (idx == null) {
			return "";
		}
		return idx.getBits(value);
	}

	public static ArrayList<String[]> selectIndex(String tableName,
			String[] cols, String[] vals) {
		long start = System.currentTimeMillis();

		Table table = FileManager.loadTable(tableName);

		ArrayList<String> indexedCondCols = new ArrayList<>();
		ArrayList<String> indexedCondVals = new ArrayList<>();
		ArrayList<String> nonIndexedCols = new ArrayList<>();
		ArrayList<String> nonIndexedVals = new ArrayList<>();

		for (int i = 0; i < cols.length; i++) {
			if (table.indexedColumns.contains(cols[i])) {
				indexedCondCols.add(cols[i]);
				indexedCondVals.add(vals[i]);
			} else {
				nonIndexedCols.add(cols[i]);
				nonIndexedVals.add(vals[i]);
			}
		}

		ArrayList<String[]> result = new ArrayList<>();
		if (indexedCondCols.isEmpty()) {

			result = linearSelect(tableName, table, cols, vals);

			long elapsed = System.currentTimeMillis() - start;
			String[] sortedCols = cols.clone();
			Arrays.sort(sortedCols);
			String trace = "Select index condition: " + Arrays.toString(cols)
					+ "->" + Arrays.toString(vals) + ", Non Indexed: "
					+ Arrays.toString(sortedCols) + ", Final count: "
					+ result.size() + ", execution time (mil):" + elapsed;
			table.addTrace(trace);
			FileManager.storeTable(tableName, table);
		} else if (nonIndexedCols.isEmpty()) {

			String combinedBits = getValueBits(tableName,
					indexedCondCols.get(0), indexedCondVals.get(0));
			for (int i = 1; i < indexedCondCols.size(); i++) {
				String bits = getValueBits(tableName, indexedCondCols.get(i),
						indexedCondVals.get(i));
				combinedBits = BitmapIndex.andBits(combinedBits, bits);
			}

			result = fetchRecordsByBits(tableName, table, combinedBits);

			long elapsed = System.currentTimeMillis() - start;
			String[] sortedCols = cols.clone();
			Arrays.sort(sortedCols);
			String trace = "Select index condition: " + Arrays.toString(cols)
					+ "->" + Arrays.toString(vals) + ", Indexed columns: "
					+ Arrays.toString(sortedCols)
					+ ", Indexed selection count: " + result.size()
					+ ", Final count: " + result.size()
					+ ", execution time (mil):" + elapsed;
			table.addTrace(trace);
			FileManager.storeTable(tableName, table);
		} else {

			String combinedBits = getValueBits(tableName,
					indexedCondCols.get(0), indexedCondVals.get(0));
			for (int i = 1; i < indexedCondCols.size(); i++) {
				String bits = getValueBits(tableName, indexedCondCols.get(i),
						indexedCondVals.get(i));
				combinedBits = BitmapIndex.andBits(combinedBits, bits);
			}

			ArrayList<String[]> candidates = fetchRecordsByBits(tableName,
					table, combinedBits);
			int indexedCount = candidates.size();

			int[] nonIdxColIndices = new int[nonIndexedCols.size()];
			for (int i = 0; i < nonIndexedCols.size(); i++) {
				nonIdxColIndices[i] = table.getColumnIndex(nonIndexedCols
						.get(i));
			}

			for (int i = 0; i < candidates.size(); i++) {
				String[] record = candidates.get(i);
				boolean match = true;
				for (int j = 0; j < nonIdxColIndices.length; j++) {
					int ci = nonIdxColIndices[j];
					if (ci < 0 || ci >= record.length
							|| !record[ci].equals(nonIndexedVals.get(j))) {
						match = false;
						break;
					}
				}
				if (match) {
					result.add(record);
				}
			}

			long elapsed = System.currentTimeMillis() - start;
			String[] sortedNonIndexed = nonIndexedCols.toArray(new String[0]);
			Arrays.sort(sortedNonIndexed);
			String[] sortedIndexed = indexedCondCols.toArray(new String[0]);
			Arrays.sort(sortedIndexed);
			String trace = "Select index condition: " + Arrays.toString(cols)
					+ "->" + Arrays.toString(vals) + ", Indexed columns: "
					+ Arrays.toString(sortedIndexed)
					+ ", Indexed selection count: " + indexedCount
					+ ", Non Indexed: " + Arrays.toString(sortedNonIndexed)
					+ ", Final count: " + result.size()
					+ ", execution time (mil):" + elapsed;
			table.addTrace(trace);
			FileManager.storeTable(tableName, table);
		}

		return result;
	}

	public static ArrayList<String[]> validateRecords(String tableName) {
		Table table = FileManager.loadTable(tableName);
		int pagesCount = table.pagesCount;

		ArrayList<String[]> allExpected = reconstructExpectedRecords(table);

		ArrayList<String[]> missing = new ArrayList<>();

		for (int pageNum = 0; pageNum < pagesCount; pageNum++) {
			Page page = FileManager.loadTablePage(tableName, pageNum);
			if (page == null) {

				int startRecord = pageNum * dataPageSize;
				int endRecord = Math.min(startRecord + dataPageSize,
						allExpected.size());
				for (int r = startRecord; r < endRecord; r++) {
					missing.add(allExpected.get(r));
				}
			}
		}

		String trace = "Validating records: " + missing.size()
				+ " records missing.";
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);

		return missing;
	}

	public static void recoverRecords(String tableName,
			ArrayList<String[]> missing) {
		if (missing == null || missing.isEmpty()) {
			Table t = FileManager.loadTable(tableName);
			String trace = "Recovering 0 records in pages: []";
			t.addTrace(trace);
			FileManager.storeTable(tableName, t);
			return;
		}

		Table table = FileManager.loadTable(tableName);
		int pagesCount = table.pagesCount;

		ArrayList<String[]> allExpected = reconstructExpectedRecords(table);

		ArrayList<Integer> missingPageNums = new ArrayList<>();
		for (int pageNum = 0; pageNum < pagesCount; pageNum++) {
			Page page = FileManager.loadTablePage(tableName, pageNum);
			if (page == null) {
				missingPageNums.add(pageNum);
			}
		}

		for (int i = 0; i < missingPageNums.size(); i++) {
			int pageNum = missingPageNums.get(i);
			Page newPage = new Page(pageNum);
			int startRecord = pageNum * dataPageSize;
			int endRecord = Math.min(startRecord + dataPageSize,
					allExpected.size());
			for (int r = startRecord; r < endRecord; r++) {
				newPage.addRecord(allExpected.get(r));
			}
			FileManager.storeTablePage(tableName, pageNum, newPage);
		}

		String trace = "Recovering " + missing.size() + " records in pages: "
				+ missingPageNums + ".";
		table.addTrace(trace);
		FileManager.storeTable(tableName, table);
	}

	public static ArrayList<String[]> reconstructExpectedRecords(Table table) {
		ArrayList<String[]> records = new ArrayList<>();
		for (int i = 0; i < table.trace.size(); i++) {
			String entry = table.trace.get(i);
			if (entry.startsWith("Inserted:")) {

				int start = entry.indexOf('[');
				int end = entry.indexOf(']');
				if (start == -1 || end == -1) {
					continue;
				}
				String inner = entry.substring(start + 1, end);
				String[] parts = inner.split(", ");
				records.add(parts);
			}
		}
		return records;
	}

	public static ArrayList<String[]> fetchRecordsByBits(String tableName,
			Table table, String bits) {
		ArrayList<String[]> result = new ArrayList<>();
		int pagesCount = table.pagesCount;

		int globalPos = 0;
		for (int pageNum = 0; pageNum < pagesCount; pageNum++) {
			Page page = FileManager.loadTablePage(tableName, pageNum);
			if (page == null) {

				globalPos += dataPageSize;
				continue;
			}
			for (int j = 0; j < page.records.size(); j++) {
				String[] record = page.records.get(j);
				if (globalPos < bits.length() && bits.charAt(globalPos) == '1') {
					result.add(record);
				}
				globalPos++;
			}
		}
		return result;
	}

	public static ArrayList<String[]> linearSelect(String tableName,
			Table table, String[] cols, String[] vals) {
		ArrayList<String[]> result = new ArrayList<>();
		int[] colIndices = new int[cols.length];
		for (int i = 0; i < cols.length; i++) {
			colIndices[i] = table.getColumnIndex(cols[i]);
		}

		for (int pageNum = 0; pageNum < table.pagesCount; pageNum++) {
			Page page = FileManager.loadTablePage(tableName, pageNum);
			if (page == null) {
				continue;
			}
			for (int j = 0; j < page.records.size(); j++) {
				String[] record = page.records.get(j);
				boolean match = true;
				for (int k = 0; k < colIndices.length; k++) {
					int ci = colIndices[k];
					if (ci < 0 || ci >= record.length
							|| !record[ci].equals(vals[k])) {
						match = false;
						break;
					}
				}
				if (match) {
					result.add(record);
				}
			}
		}
		return result;
	}

	public static void main(String[] args) throws IOException {
		FileManager.reset();
		String[] cols = { "id", "name", "major", "semester", "gpa" };
		createTable("student", cols);

		String[] r1 = { "1", "stud1", "CS", "5", "0.9" };
		insert("student", r1);

		String[] r2 = { "2", "stud2", "BI", "7", "1.2" };
		insert("student", r2);

		String[] r3 = { "3", "stud3", "CS", "2", "2.4" };
		insert("student", r3);

		String[] r4 = { "4", "stud4", "CS", "9", "1.2" };
		insert("student", r4);

		String[] r5 = { "5", "stud5", "BI", "4", "3.5" };
		insert("student", r5);

		// ////// This is the code used to delete pages from the table
		System.out.println("File Manager trace before deleting pages: "
				+ FileManager.trace());
		String path = FileManager.class.getResource("FileManager.class")
				.toString();
		File directory = new File(path.substring(6, path.length() - 17)
				+ File.separator + "Tables//student" + File.separator);
		File[] contents = directory.listFiles();
		int[] pageDel = { 0, 2 };

		for (int i = 0; i < pageDel.length; i++) {
			contents[pageDel[i]].delete();
		}
		// //////End of deleting pages code

		System.out.println("File Manager trace after deleting pages: "
				+ FileManager.trace());

		ArrayList<String[]> tr = validateRecords("student");
		System.out.println("Missing records count: " + tr.size());
		recoverRecords("student", tr);

		System.out.println("------");
		System.out.println("Recovering the missing records.");
		tr = validateRecords("student");
		System.out.println("Missing record count: " + tr.size());
		System.out
				.println("File Manager trace after recovering missing records: "
						+ FileManager.trace());
		System.out.println("--");
		System.out.println("Full trace of the table: ");
		System.out.println(getFullTrace("student"));
	}
}
