package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class BitmapIndex implements Serializable {
	public static final long serialVersionUID = 1L;

	public LinkedHashMap<String, StringBuilder> index;
	public int totalRecords;

	public BitmapIndex() {
		index = new LinkedHashMap<>();
		totalRecords = 0;
	}

	public void build(String tableName, String collumnName) {
		index.clear();
		totalRecords = 0;
		Table table = FileManager.loadTable(tableName);
		int collumnIndex = table.getColumnIndex(collumnName);

		ArrayList<String> valuesInOrder = new ArrayList<>();
		for (int i = 0; i < table.pagesCount; i++) {
			Page page = FileManager.loadTablePage(tableName, i);
			if (page == null) {
				continue;
			}
			for (int j = 0; j < page.records.size(); j++) {
				valuesInOrder.add(page.records.get(j)[collumnIndex]);
			}
		}

		totalRecords = valuesInOrder.size();

		for (int i = 0; i < valuesInOrder.size(); i++) {
			String val = valuesInOrder.get(i);
			if (!index.containsKey(val)) {
				index.put(val, new StringBuilder());
			}
		}

		ArrayList<String> keys = new ArrayList<>(index.keySet());
		for (int i = 0; i < keys.size(); i++) {
			String val = keys.get(i);
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < totalRecords; j++) {
				sb.append(valuesInOrder.get(j).equals(val) ? '1' : '0');
			}
			index.put(val, sb);
		}
	}

	public void insertRecord(String value) {

		ArrayList<StringBuilder> vals = new ArrayList<>(index.values());
		for (int i = 0; i < vals.size(); i++) {
			vals.get(i).append('0');
		}

		if (!index.containsKey(value)) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < totalRecords; i++) {
				sb.append('0');
			}
			sb.append('0');
			index.put(value, sb);
		}

		StringBuilder sb = index.get(value);
		sb.setCharAt(sb.length() - 1, '1');

		totalRecords++;
	}

	public String getBits(String value) {
		if (index.containsKey(value)) {
			return index.get(value).toString();
		}

		StringBuilder zeros = new StringBuilder();
		for (int i = 0; i < totalRecords; i++) {
			zeros.append('0');
		}
		return zeros.toString();
	}

	public static String andBits(String a, String b) {
		int len = Math.min(a.length(), b.length());
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < len; i++) {
			res.append((a.charAt(i) == '1' && b.charAt(i) == '1') ? '1' : '0');
		}
		return res.toString();
	}

	public ArrayList<Integer> getMatchingRecordIndices(String value) {
		ArrayList<Integer> result = new ArrayList<>();
		String bits = getBits(value);
		for (int i = 0; i < bits.length(); i++) {
			if (bits.charAt(i) == '1') {
				result.add(i);
			}
		}
		return result;
	}

	public int getTotalRecords() {
		return totalRecords;
	}
}
