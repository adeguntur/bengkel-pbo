
package lib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lib.DataConnector.Value.ValueType;

public class DataConnector implements AutoCloseable{
	
	private static final String DRIVER 	 = "jdbc:mysql";
	private static final String HOST 	 = "localhost";
	
	private final Connection con;
	
	// ============ Prepared Statement ===========
	private final Map<String, List<RowEntry>> batchInsertMap = new HashMap<String, List<RowEntry>>();
	private final Map<String, List<RowEntry>> batchUpdateMap = new HashMap<String, List<RowEntry>>();
	private final Map<String, List<Integer>>  batchDeleteMap = new HashMap<String, List<Integer>>();
	
	private Savepoint rollbackPoint;
	
	public DataConnector(String connectionUrl) throws SQLException {
		con = DriverManager.getConnection(connectionUrl);
		con.setAutoCommit(false);
	}
	
	public DataConnector(String host, String schema, String username) throws SQLException {
		this(DRIVER + "://" + host + ":" + "3306"
				+ "/" + schema 
				+ "?user=" + username
			);
	}
	
	public DataConnector(String host, String schema, String username, String password) throws SQLException {
		this(DRIVER + "://" + host + ":" + "3306"
				+ "/" + schema 
				+ "?user=" + username 
				+ "&password=" + password
			);
	}
	
	public DataConnector(String host, String port, String schema, String username, String password) throws SQLException {
		this(DRIVER + "://" + host + ":" + port
				+ "/" + schema 
				+ "?user=" + username
				+ "&password=" + password
			);
	}
	
	public DataConnector(String host, String port, String schema, String username, String password, boolean ssl) throws SQLException {
		this(DRIVER + "://" + host + ":" + port
				+ "/" + schema 
				+ "?user=" + username
				+ "&password=" + password
				+ "&useSSL=" + (ssl ? "true" : "false")
			);
	}
	
	/**
	 * Begin transaction to database <br/>
	 * 
	 * @throws SQLException 
	 */
	public void begin() throws SQLException {
		if(rollbackPoint != null) {
			throw new SQLException("Transaction already open");
		}
		
		rollbackPoint = con.setSavepoint();
	}
	
	/**
	 * Commit newly made transaction to database <br/>
	 *
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		if(rollbackPoint == null) {
			throw new SQLException("Transaction is closed");
		}
		
		// Update
		for(Entry<String, List<RowEntry>> kv : batchUpdateMap.entrySet()) {
			CachedQuery queries = cacheQuery.get(kv.getKey());
			try (PreparedStatement stmt = con.prepareStatement(queries.getUpdateQuery())) {
				setQueryParameter(stmt, kv.getValue(), queries.getColumnSequence(), true);
				stmt.executeBatch();
			}
		}
		
		// Insert
		for(Entry<String, List<RowEntry>> kv : batchInsertMap.entrySet()) {
			CachedQuery queries = cacheQuery.get(kv.getKey());
			try (PreparedStatement stmt = con.prepareStatement(queries.getInsertQuery())) {
				setQueryParameter(stmt, kv.getValue(), queries.getColumnSequence(), false);				
				stmt.executeBatch();
			}
		}
		
		// Delete
		for(Entry<String, List<Integer>> kv : batchDeleteMap.entrySet()) {
			CachedQuery queries = cacheQuery.get(kv.getKey());
			try (PreparedStatement stmt = con.prepareStatement(queries.getDeleteQuery())) {
				for(Integer value : kv.getValue()) {
					stmt.setInt(1, value);
					stmt.addBatch();
				}
				
				stmt.executeBatch();
			}
		}
		
		con.commit();
		con.releaseSavepoint(rollbackPoint);
		
		batchDeleteMap.clear();
		batchInsertMap.clear();
		batchUpdateMap.clear();
		
		rollbackPoint = null;
	}
	
	/**
	 * Revert data changed when error occurs
	 * 
	 * @throws SQLException
	 */
	public void rollback() throws SQLException {
		if(rollbackPoint == null) {
			throw new SQLException("Transaction is closed");
		}
		
		con.rollback(rollbackPoint);
		con.releaseSavepoint(rollbackPoint);
		
		batchDeleteMap.clear();
		batchInsertMap.clear();
		batchUpdateMap.clear();
		
		rollbackPoint = null;
	}
	
	/**
	 * Close connection and release resources used.
	 * 
	 * @throws SQLException 
	 */
	@Override
	public void close() throws SQLException {
		con.close();
	}
	
	// ----- SELECT -----
	
	/**
	 * List all rows from table
	 * @param tableName
	 * @return
	 * @throws SQLException
	 */
	public List<RowEntry> list(String tName) throws SQLException {
            List<RowEntry> rows = new LinkedList<>();
            try (PreparedStatement stmt = con.prepareStatement("SELECT * FROM " + tName)) {
                try (ResultSet rs = stmt.executeQuery()){
                    ResultSetMetaData meta = rs.getMetaData();

                    String tableName = rs.getMetaData().getTableName(1);

                    int columnCount = meta.getColumnCount();

                    while(rs.next()) {
                        RowEntry row = new RowEntry(tableName);

                        loopColumn : for(int i = 1; i <= columnCount; i++) {
                                String columnName = meta.getColumnName(i);

                                if(columnName.equals("id")) {
                                        row.persist(rs.getInt(i));
                                        continue loopColumn;
                                }

                                switch(meta.getColumnType(i)) {
                                case java.sql.Types.DATE :
                                case java.sql.Types.TIMESTAMP :
                                        row.set(columnName, Value.fromDate(rs.getDate(i)));
                                        break;

                                case java.sql.Types.DOUBLE :
                                case java.sql.Types.FLOAT : 
                                        row.set(columnName, Value.fromDate(rs.getDate(i)));
                                        break;

                                case java.sql.Types.BIGINT :
                                case java.sql.Types.SMALLINT :
                                case java.sql.Types.TINYINT :
                                case java.sql.Types.INTEGER :
                                        row.set(columnName, Value.fromInteger(rs.getInt(i)));
                                        break;

                                case java.sql.Types.BIT:
                                case java.sql.Types.BOOLEAN :
                                        row.set(columnName, Value.fromBoolean(rs.getBoolean(i)));
                                        break;

                                case java.sql.Types.VARCHAR:
                                case java.sql.Types.NVARCHAR:
                                        row.set(columnName, Value.fromString(rs.getString(i)));
                                        break;

                                default :
                                        throw new SQLException ("Unsupported data type");
                                }
                        }

                        rows.add(row);
                    }
                }
            }
            
            return rows;
		//return list(tableName, null, (Object[])null);
	}
	
	/**
	 * List rows from table by specific condition
	 * 
	 * @param tableName
	 * @param whereClause
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public List<RowEntry> list(String tableName, String whereClause, List<Object> parameters) throws SQLException {
		return list(tableName, whereClause, (Object[])parameters.toArray());
	}
	
	/**
	 * List rows from table by specific condition
	 * 
	 * @param tableName
	 * @param whereClause
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public List<RowEntry> list(String tableName, String whereClause, Object ... parameters) throws SQLException {
		String statement = "SELECT * FROM " + tableName + (whereClause == null ? "" : " WHERE " + whereClause);
		
		return list(statement, parameters);
	}
	
	/**
	 * List rows from table by specific condition
	 * 
	 * @param query
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public List<RowEntry> list(String query, Object ... parameters) throws SQLException{
		try (PreparedStatement stmt = con.prepareStatement(query)) {
			if(parameters == null) {
				stmt.setNull(1, java.sql.Types.NULL);
			}
			else {
				int index = 0;
				param: for(Object parameter : parameters) {
					if(parameter == null) {
						stmt.setNull(++index, java.sql.Types.NULL);
					}
					
					if(parameter instanceof Boolean) {
						stmt.setBoolean(++index, (boolean) parameter);
						continue param;
					}
					
					if(parameter instanceof Date) {
						stmt.setDate(++index, new java.sql.Date(((Date) parameter).getTime()));
						continue param;
					}
					
					if(parameter instanceof Integer) {
						stmt.setInt(++index, (int) parameter);
						continue param;
					}
					
					if(parameter instanceof Double) {
						stmt.setDouble(++index, (double) parameter);
						continue param;
					}
					
					if(parameter instanceof String) {
						stmt.setString(++index, (String) parameter);
						continue param;
					}
					
					if (parameter instanceof Value) {
						switch(((Value)parameter).type) {
						case BOOLEAN:
							stmt.setBoolean(++index, ((Value)parameter).asBoolean());
							continue param;
							
						case DATE:
							try {
								stmt.setDate(++index, new java.sql.Date(((Value)parameter).asDate().getTime()));
							}catch(ParseException e) {
								throw new SQLException ("Unknown data type");
							}
							continue param;
							
						case DOUBLE:
							stmt.setDouble(++index, ((Value)parameter).asDouble());
							continue param;
							
						case INT:
							stmt.setInt(++index, ((Value)parameter).asInteger());
							continue param;
							
						case STRING:
							stmt.setString(++index, ((Value)parameter).asString());
							continue param;
							
						default:
							throw new SQLException("Unknown data type"); 
						}
					}
					
					throw new SQLException ("Unknown data type");
				}
			}
			
			List<RowEntry> rows = new LinkedList<>();
			try (ResultSet rs = stmt.executeQuery()){
				ResultSetMetaData meta = rs.getMetaData();
				
				String tableName = rs.getMetaData().getTableName(1);
				
				int columnCount = meta.getColumnCount();
				
				while(rs.next()) {
					RowEntry row = new RowEntry(tableName);
					
					loopColumn : for(int i = 1; i <= columnCount; i++) {
						String columnName = meta.getColumnName(i);
						
						if(columnName.equals("id")) {
							row.persist(rs.getInt(i));
							continue loopColumn;
						}
						
						switch(meta.getColumnType(i)) {
						case java.sql.Types.DATE :
						case java.sql.Types.TIMESTAMP :
							row.set(columnName, Value.fromDate(rs.getDate(i)));
							break;
							
						case java.sql.Types.DOUBLE :
						case java.sql.Types.FLOAT : 
							row.set(columnName, Value.fromDate(rs.getDate(i)));
							break;
							
						case java.sql.Types.BIGINT :
						case java.sql.Types.SMALLINT :
						case java.sql.Types.TINYINT :
						case java.sql.Types.INTEGER :
							row.set(columnName, Value.fromInteger(rs.getInt(i)));
							break;
							
						case java.sql.Types.BIT:
						case java.sql.Types.BOOLEAN :
							row.set(columnName, Value.fromBoolean(rs.getBoolean(i)));
							break;
							
						case java.sql.Types.VARCHAR:
						case java.sql.Types.NVARCHAR:
							row.set(columnName, Value.fromString(rs.getString(i)));
							break;

						default :
							throw new SQLException ("Unsupported data type");
						}
					}
					
					rows.add(row);
				}
			}
			
			return rows;
		}
	}
	
	/**
	 * Find a row from table for a specific condition
	 * 
	 * @param tableName
	 * @param whereClause
	 * @param parameters
	 * @return
	 * @throws SQLException
	 */
	public RowEntry find(String tableName, String whereClause, Object ... parameters) throws SQLException {
		 List<RowEntry> rows = list(tableName, whereClause, parameters);
		 
		 if(rows.size() < 1) {
			 return null;
		 }
		 
		 if(rows.size() > 1) {
			 throw new SQLException ("Multiple return value");
		 }
		 
		 return rows.get(0);
	}
	
	public RowEntry find(String tableName, String whereClause, List<Object> parameters) throws SQLException {
		return find(tableName, whereClause, (Object[])parameters.toArray());
	}
	
	// ----- INSERT UPDATE -----
	
	/**
	 * Execute modification such as INSERT or UPDATE
	 * 
	 * @param entry Database entry
	 * @throws SQLException 
	 */
	public void persist(RowEntry entry) throws SQLException {
		cacheQuery.computeIfAbsent(entry.getTableName(), k -> new CachedQuery(entry));			
		(entry.isPersisted() ? batchUpdateMap : batchInsertMap).computeIfAbsent(entry.getTableName(), k -> new LinkedList<RowEntry>()).add(entry);

	}
	
	/**
	 * Execute modification such as INSERT or UPDATE for collection data
	 * 
	 * @param entries Collection of data
	 * @throws SQLException 
	 */
	public void persist(Collection<RowEntry> entries) throws SQLException {
		for(RowEntry entry : entries) { persist(entry); }
	}

	// ----- DELETE -----
	
	/**
	 * Delete entry from database
	 * 
	 * @param entry Database entry
	 * @throws SQLException 
	 */
	public void delete(RowEntry entry) throws SQLException {
		cacheQuery.computeIfAbsent(entry.getTableName(), k -> new CachedQuery(entry));
		batchDeleteMap.computeIfAbsent(entry.getTableName(), k -> new LinkedList<Integer>()).add(entry.getId());
	}
	
	/**
	 * Delete entry from database for collection data
	 * 
	 * @param entries Collection of data
	 * @throws SQLException 
	 */
	public void delete(Collection<RowEntry> entries) throws SQLException {
		for(RowEntry entry : entries) { delete(entry); }
	}
	
	void setQueryParameter(PreparedStatement stmt, List<RowEntry> entries, List<String> columnSequence, boolean includeId) throws SQLException {
		for(RowEntry entry : entries) {
			
			int index = 1;
			for(String columnName : columnSequence){
				Value parameter = entry.get(columnName);
				
				switch(parameter.type) {
				case BOOLEAN:
					stmt.setBoolean(index++, parameter.asBoolean());
					break;
					
				case DATE:
					try {
						stmt.setDate(index++, new java.sql.Date(parameter.asDate().getTime()));
					}catch(ParseException e) {
						throw new SQLException ("Unknown data type");
					}
					break;
					
				case DOUBLE:
					stmt.setDouble(index++, parameter.asDouble());
					break;
					
				case INT:
					stmt.setInt(index++, parameter.asInteger());
					break;
					
				case STRING:
					stmt.setString(index++, parameter.asString());
					break;
					
				default:
					throw new SQLException("Unknown data type"); 
				}
			}
			
			if(includeId) {
				stmt.setInt(index, entry.getId());
			}
			
			stmt.addBatch();
		}
	}

	// ======================== ============================ //
	
	private static final Map<String, CachedQuery> cacheQuery = new HashMap<String, CachedQuery>();

	static class CachedQuery {
		
		private final String updateQ;
		private final String insertQ;
		private final String deleteQ;
		
		private final List<String> columnSequence = new LinkedList<String>();
		
		CachedQuery(RowEntry ref) {
			ref.columnContainer.forEach((k,v) -> columnSequence.add(k));
			
			StringBuilder sbInsert = new StringBuilder();
			StringBuilder sbIValue = new StringBuilder(); 
			StringBuilder sbUpdate = new StringBuilder();
			
			
			boolean first = true;
			for(String column : columnSequence) {
				if(!first) {
					sbInsert.append(", ");
					sbIValue.append(", ");
					sbUpdate.append(", ");
				}
				first = false;
				
				sbInsert.append(column);
				sbIValue.append("?");
				sbUpdate.append(column + " = ?");
			}
			
			deleteQ = "DELETE FROM " + ref.getTableName() + " WHERE id = ?";
			
			insertQ = "INSERT INTO " + ref.getTableName() + "("
					+ sbInsert.toString()
					+ ") VALUES ("
					+ sbIValue.toString()
					+ ")";
			
			updateQ = "UPDATE " + ref.getTableName()
					+ " SET "
					+ sbUpdate.toString()
					+ " WHERE id = ?";
		}
		
		String getUpdateQuery() {
			return updateQ;
		}
		
		String getInsertQuery() {
			return insertQ;
		}
		
		String getDeleteQuery() {
			return deleteQ;
		}

		List<String> getColumnSequence() {
			return columnSequence;
		}
	}
	
	public static class RowEntry {
		private int id;
		private boolean persisted;
		
		private final Map<String, Value> columnContainer = new HashMap<String, Value>();
		private final Map<String, ValueType> columnsMap = new HashMap<String, ValueType>();
		
		private final String tableName;
		
		RowEntry(String tableName, int id){
			this.tableName = tableName;
			this.persisted = true;
			this.id = id;
		}
		
		public RowEntry(String tableName) {
			this.tableName = tableName;
			this.persisted = false;
			this.id = 0;
		}
		
		public String getTableName() {
			return tableName;
		}

		public int getId() {
			return this.id;
		}
		
		public void set(String columnName, Value value) {
			columnContainer.put(columnName, value);
			columnsMap.put(columnName, value.type);
		}
		
		public void set(String columnName, String value) {
			set(columnName, Value.fromString(value));
		}
		
		public void set(String columnName, boolean value) {
			set(columnName, Value.fromBoolean(value));
		}
		
		public void set(String columnName, int value) {
			set(columnName, Value.fromInteger(value));
		}
		
		public void set(String columnName, double value) {
			set(columnName, Value.fromDouble(value));
		}
		
		public void set(String columnName, Date value) {
			set(columnName, Value.fromDate(value));
		}
		
		public Value get(String columnName) {
			return columnContainer.get(columnName);
		}

		public boolean isPersisted() {
			return persisted;
		}
		
		public Map<String, Value.ValueType> getColumns(){
			return columnsMap;
		}
		
		void persist(int id) {
			this.id = id;
			persisted = true;
		}
	}
	
	/**
	 * Represent a value of data. <br/>
	 * This class is immutable
	 *
	 */
	public static final class Value {
		public static enum ValueType {
			STRING,
			INT,
			DOUBLE,
			DATE,
			BOOLEAN,
		}
		
		private final String actual;
		private final ValueType type;
		
		private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		private Value(String actual, ValueType type) {
			this.actual = actual;
			this.type = type;
		}
		
		public String asString() {
			return actual;
		}
		
		public static Value fromString(String actual) {
			return new Value(actual, ValueType.STRING);
		}
		
		public Date asDate() throws ParseException {
			return sdf.parse(actual);
		}
		
		public static Value fromDate(Date actual) {
			return new Value(sdf.format(actual), ValueType.DATE);
		}
		
		public int asInteger() {
			return Integer.parseInt(actual);
		}
		
		public static Value fromInteger(int actual) {
			return new Value(String.valueOf(actual), ValueType.INT);
		}
		
		public double asDouble() {
			return Double.parseDouble(actual);
		}
		
		public static Value fromDouble(double actual) {
			return new Value(String.format("%.0f", actual), ValueType.DOUBLE);
		}
		
		public boolean asBoolean() {
			return actual.equals("1");
		}
		
		public static Value fromBoolean(boolean bool) {
			return new Value(bool ? "1" : "0", ValueType.BOOLEAN);
		}
		
		@Override
		public String toString() {
			return asString();
		}
	}
}