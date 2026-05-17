# DBMS Engine — Java

A disk-based Database Management System engine built in Java, implementing core database concepts including bitmap indexing, multi-condition queries, and data recovery.

## Features

- **Table Management** — Create tables with custom column schemas
- **Page-Based Storage** — Records stored in fixed-size disk pages via serialization
- **Bitmap Indexing** — Create bitmap indices on columns for fast filtered queries
- **Indexed Selection** — Smart query execution using bitmap AND operations across indexed columns
- **Data Validation & Recovery** — Detect and recover missing/corrupted pages from trace history
- **Operation Tracing** — Full audit trail of all operations per table

## Project Structure

| File | Description |
|------|-------------|
| `DBApp.java` | Core database engine — all public API methods |
| `Table.java` | Table metadata, column info, and trace log |
| `Page.java` | A single data page holding records |
| `BitmapIndex.java` | Bitmap index structure for a single column |
| `FileManager.java` | Handles all disk I/O (serialization/deserialization) |

## How It Works

### Storage
Records are stored in fixed-size pages on disk. Each page holds up to N records (configurable via `dataPageSize`).

### Bitmap Index
Each indexed column gets a `.db` index file mapping every distinct value to a bitstring — one bit per record. A `1` at position `i` means record `i` has that value.

### Query Execution (selectIndex)
- **No columns indexed** → full linear scan
- **Some columns indexed** → bitmap AND on indexed columns → filter candidates on non-indexed columns
- **All columns indexed** → bitmap AND only, no scan needed

### Recovery
The engine reconstructs expected records from the insertion trace, then rewrites any missing pages back to disk.

## Technologies

- Java (no external dependencies)
- Java Serialization for disk persistence
- JUnit 4 for testing
