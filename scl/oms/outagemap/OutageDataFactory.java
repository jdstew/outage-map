package scl.oms.outagemap;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Provides database access.
 *
 * @author jstewart
 */
public class OutageDataFactory {

    private static OracleConnection sourceDBConn;

    private OutageDataFactory() {
    }

    private static OracleConnection getDatabaseConnection() throws IOException, SQLException {
        Logger log = Log.getLogger();
        log.log(Level.FINEST, "Creating source Oracle data source connection, using: {0} (environment={1})",
                new Object[]{Config.INSTANCE.getSourceDbConn(), Config.INSTANCE.getEnvironmentLabel()});
        OracleDataSource sourceODS = new OracleDataSource();
        sourceODS.setURL("jdbc:oracle:thin:" + Config.INSTANCE.getSourceDbConn());
        log.log(Level.FINEST, "Source OracleDataSource set to: {0} (environment={1})",
                new Object[]{sourceODS.getURL(), Config.INSTANCE.getEnvironmentLabel()});
        try {
            return (OracleConnection) sourceODS.getConnection();
        } catch (java.sql.SQLException e) {
            log.severe(e.toString());
            throw e;
        } catch (Exception e) {
            log.severe(e.toString());
            throw e;
        }
    }

    /**
     *
     * @return @throws IOException
     * @throws SQLException
     */
    public static OracleResultSet getCustomersOut() throws IOException, SQLException {
        OracleResultSet customersOut;
        sourceDBConn = OutageDataFactory.getDatabaseConnection();
        String sqlString = Config.INSTANCE.getSourceDbSQL();
        Logger log = Log.getLogger();
        log.log(Level.FINEST, "SQL string is: {0} (evironment ={1})",
                new Object[]{sqlString, Config.INSTANCE.getEnvironmentLabel()});
        Statement sqlStatement = sourceDBConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        customersOut = (OracleResultSet) sqlStatement.executeQuery(sqlString);

        return customersOut;
    }

    /**
     * Closes the database connection. Note that the database connection can not
     * be closed while the OracleResultSet from .getCustomersOut () is or will
     * be read.
     *
     * @throws IOException
     */
    public static void closeDatabaseConnection() throws IOException {
        try {
            sourceDBConn.close();
        } catch (SQLException ex) {
            Logger log = Log.getLogger();
            log.getLogger(OutageDataFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
