package DBMS;

import java.io.Serializable;
import java.util.ArrayList;

public class Page implements Serializable {
	private static final long serialVersionUID = 1L;

	public int pageNumber;
	public ArrayList<String[]> records;

	public Page(int pageNumber) {
		this.pageNumber = pageNumber;
		this.records = new ArrayList<>();
	}

	public void addRecord(String[] rec) {
		records.add(rec);
	}

	public int size() {
		return records.size();
	}

	public String[] getRecord(int index) {
		return records.get(index);
	}

}