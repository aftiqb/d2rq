package de.fuberlin.wiwiss.d2rq.map;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.vocab.D2RQ;


/**
 * Representation of a d2rq:Database from the mapping file.
 *
 * @author Chris Bizer chris@bizer.de
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Database.java,v 1.22 2009/08/06 10:34:21 fatorange Exp $
 */
public class Database extends MapObject {
	public static final int NO_LIMIT = -1;
	public static final int NO_FETCH_SIZE = -1;	
	
	/**
	 * Pre-registers a JDBC driver if its class can be found on the
	 * classpath. If the class is not found, nothing will happen.
	 * @param driverClassName Fully qualified class name of a JDBC driver
	 */
	public static void registerJDBCDriverIfPresent(String driverClassName) {
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			// not present, just ignore this driver
		}
	}

	/**
	 * Registers a JDBC driver class.
	 * @param driverClassName Fully qualified class name of a JDBC driver
	 * @throws D2RQException If the class could not be found
	 */
	public static void registerJDBCDriver(String driverClassName) {
		try {
			Class.forName(driverClassName);
		} catch (ClassNotFoundException ex) {
			throw new D2RQException("Database driver class not found: " + driverClassName,
					D2RQException.DATABASE_JDBCDRIVER_CLASS_NOT_FOUND);
		}
	}
	
	/**
	 * Tries to guess the class name of a suitable JDBC driver from a JDBC URL.
	 * This only works in the unlikely case that the driver has been registered
	 * earlier using Class.forName(classname).
	 * @param jdbcURL A JDBC URL
	 * @return The corresponding JDBC driver class name, or <tt>null</tt> if not known
	 */
	public static String guessJDBCDriverClass(String jdbcURL) {
		try {
			return DriverManager.getDriver(jdbcURL).getClass().getName();
		} catch (SQLException ex) {
			return null;
		}
	}
		
	private String odbcDSN;
	private String jdbcDSN;
	private String jdbcDriver;
	private String username;
	private String password;
	private Set textColumns = new HashSet();
	private Set numericColumns = new HashSet();
	private Set dateColumns = new HashSet();
	private Set timestampColumns = new HashSet();
    private int limit = NO_LIMIT;
    private int fetchSize = NO_FETCH_SIZE;
	private boolean allowDistinct = true;
	private ConnectedDB connection = null;
	private Properties connectionProperties = new Properties();
	
	public Database(Resource resource) {
		super(resource);
	}

	public void setODBCDSN(String odbcDSN) {
		assertNotYetDefined(this.odbcDSN, D2RQ.odbcDSN, 
				D2RQException.DATABASE_DUPLICATE_ODBCDSN);
		checkNotConnected();		
		this.odbcDSN = odbcDSN;
	}

	public void setJDBCDSN(String jdbcDSN) {
		assertNotYetDefined(this.jdbcDSN, D2RQ.jdbcDSN,
				D2RQException.DATABASE_DUPLICATE_JDBCDSN);
		checkNotConnected();		
		this.jdbcDSN = jdbcDSN;
	}
	
	public String getJDBCDSN() {
		return this.jdbcDSN;
	}

	public void setJDBCDriver(String jdbcDriver) {
		assertNotYetDefined(this.jdbcDriver, D2RQ.jdbcDriver,
				D2RQException.DATABASE_DUPLICATE_JDBCDRIVER);
		checkNotConnected();		
		this.jdbcDriver = jdbcDriver;
	}

	public void setUsername(String username) {
		assertNotYetDefined(this.username, D2RQ.username,
				D2RQException.DATABASE_DUPLICATE_USERNAME);
		checkNotConnected();		
		this.username = username;
	}

	public void setPassword(String password) {
		assertNotYetDefined(this.password, D2RQ.password,
				D2RQException.DATABASE_DUPLICATE_PASSWORD);
		checkNotConnected();		
		this.password = password;
	}

	public void addTextColumn(String column) {
		checkNotConnected();		
		this.textColumns.add(column);
	}
	
	public void addNumericColumn(String column) {
		checkNotConnected();		
		this.numericColumns.add(column);
	}
	
	public void addDateColumn(String column) {
		checkNotConnected();		
		this.dateColumns.add(column);
	}
	
	public void addTimestampColumn(String column) {
		checkNotConnected();		
		this.timestampColumns.add(column);
	}
	
	public void setAllowDistinct(boolean b) {
		checkNotConnected();		
		this.allowDistinct = b;
	}

	public void setResultSizeLimit(int limit) {
		checkNotConnected();		
		this.limit = limit;
	}
	
	public int getFetchSize() {
		return this.fetchSize;
	}
	
	public void setFetchSize(int fetchSize) {
		checkNotConnected();		
		this.fetchSize = fetchSize;
	}
		
	public void setConnectionProperty(String key, String value) {
		checkNotConnected();		
		this.connectionProperties.setProperty(key, value);
	}
	
	public ConnectedDB connectedDB() {
		if (this.connection != null) {
			return this.connection;
		}
		String url;
		String driver = null;
		if (this.odbcDSN != null) {
			driver = "sun.jdbc.odbc.JdbcOdbcDriver";
			url = "jdbc:odbc:" + this.odbcDSN;
		} else {
			driver = this.jdbcDriver;
			url = this.jdbcDSN;
		}
		if (driver != null) {
			registerJDBCDriver(driver);
		}
		this.connection = new ConnectedDB(url, this.username, this.password, this.allowDistinct,
				this.textColumns, this.numericColumns, this.dateColumns, this.timestampColumns,
				this.limit, this.fetchSize, this.connectionProperties);
		return this.connection;
	}

	public String toString() {
		return "d2rq:Database " + super.toString();
	}

	public void validate() throws D2RQException {
		if (this.jdbcDSN == null && this.odbcDSN == null) {
			throw new D2RQException("d2rq:Database must have either d2rq:jdbcDSN or d2rq:odbcDSN",
					D2RQException.DATABASE_MISSING_DSN);
		}
		if (this.jdbcDSN != null && this.odbcDSN != null) {
			throw new D2RQException("Can't combine d2rq:odbcDSN with d2rq:jdbcDSN",
					D2RQException.DATABASE_ODBC_WITH_JDBC);
		}
		if (this.jdbcDSN != null && this.jdbcDriver == null) {
			throw new D2RQException("Missing d2rq:jdbcDriver",
					D2RQException.DATABASE_MISSING_JDBCDRIVER);
		}
		if (this.odbcDSN != null && this.jdbcDriver != null) {
			throw new D2RQException("Can't use d2rq:jdbcDriver with jdbc:odbcDSN",
					D2RQException.DATABASE_ODBC_WITH_JDBCDRIVER);
		}
		// TODO
	}
	
	private void checkNotConnected() {
		if (this.connection != null) {
			throw new D2RQException("Cannot modify Database as it is already connected",
					D2RQException.DATABASE_ALREADY_CONNECTED);
		}
	}
}