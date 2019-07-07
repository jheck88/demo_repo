package com.tyrannos.data.collector.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class DBManager {

    public String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm").format(new Date());
    private final String createTable = "CREATE TABLE \"ShipData\" (id SERIAL NOT NULL PRIMARY KEY, data jsonb, date timestamp, shiptype VARCHAR(100), shipprice decimal, shipname VARCHAR(100));";
    private static final int LoginTimeout = 10;
    private static final Logger LOG = Logger.getLogger(DBManager.class);

    public DBManager() {
    }

    public Connection createConnection() throws IOException, ClassNotFoundException, SQLException {
        Properties prop = new Properties();
        String host;
        String username;
        String password;
        String driver;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream resourceStream = loader.getResourceAsStream("shipsdb.cfg")) {
            prop.load(resourceStream);
        }
        host = prop.getProperty("host").toString();
        username = prop.getProperty("username").toString();
        password = prop.getProperty("password").toString();
        driver = prop.getProperty("driver").toString();


        Class.forName(driver);
        DriverManager.setLoginTimeout(LoginTimeout);
        Connection connection = DriverManager.getConnection(host, username, password);

        return connection;
    }

    public String runSqlStatement() {
        String result = "";
        try {
            Statement statement = createConnection().createStatement();
            statement.execute("DROP TABLE ShipData");
            statement.close();
        } catch (IOException | ClassNotFoundException ex) {
            result = ex.getMessage();
            LOG.error(ex);
        } catch (SQLException ex) {
            result = ex.getMessage();
            LOG.error(ex);
        }
        try {
            Statement statement = createConnection().createStatement();
            System.out.println("SQL query: " + createTable);
            statement.execute(createTable);
        } catch (IOException | ClassNotFoundException ex) {
            result = ex.getMessage();
            LOG.error(ex);
        } catch (SQLException ex) {
            result = ex.getMessage();
            LOG.error(ex);
        }

        return result;
    }
}