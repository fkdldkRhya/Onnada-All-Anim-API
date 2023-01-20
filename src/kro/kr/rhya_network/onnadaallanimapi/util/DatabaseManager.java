package kro.kr.rhya_network.onnadaallanimapi.util;

import org.ini4j.Ini;

import java.io.File;
import java.io.IOException;
import java.sql.*;

public class DatabaseManager {
    private final String db_driver = "com.mysql.cj.jdbc.Driver";
    private final String db_url;
    private final String db_id;
    private final String db_pw;

    private Connection connection = null;
    private PreparedStatement preparableStatement = null;
    private ResultSet resultSet = null;

    public DatabaseManager(String settingFile) throws IOException {
        final String sectionName = "database";

        Ini ini = new Ini(new File(settingFile));
        db_url = ini.get(sectionName, "url");
        db_id = ini.get(sectionName, "username");
        db_pw = ini.get(sectionName, "password");
    }

    public void connection() throws ClassNotFoundException, SQLException {
        Class.forName(db_driver);
        connection = DriverManager.getConnection(db_url, db_id, db_pw);
    }


    public Connection getConnection() {
        return connection;
    }

    public void closeConntection() throws SQLException {
        connection.close();
    }

    public void setPreparedStatement(String sql) throws SQLException {
        preparableStatement = connection.prepareStatement(sql);
    }

    public PreparedStatement getPreparedStatement() {
        return preparableStatement;
    }

    public void closePreparedStatement() throws SQLException {
        preparableStatement.close();
    }

    public int executeUpdate() throws SQLException {
        return preparableStatement.executeUpdate();
    }

    public void setResultSet() throws SQLException {
        resultSet = preparableStatement.executeQuery();
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public void closeResultSet() throws SQLException {
        resultSet.close();
    }

    public void allClose() throws SQLException {
        if (connection != null) connection.close();
        if (preparableStatement != null) connection.close();
        if (resultSet != null) resultSet.close();
    }
}

