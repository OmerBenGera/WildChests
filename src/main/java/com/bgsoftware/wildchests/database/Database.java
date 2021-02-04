package com.bgsoftware.wildchests.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Database {

    private static final int CONNECTION_TIMEOUT = 900;

    private static String dbFilePath = "";

    private static Connection connection = null;
    private static long lastConnectionCreation = 0;

    private Database(){}

    public static void start(File databaseFile){
        Database.dbFilePath = databaseFile.getAbsolutePath().replace("\\", "/");
        DatabaseQueue.start();
    }

    public static void stop(){
        try {
            DatabaseQueue.stop();
            if(connection != null)
                connection.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void startTransaction(){
        //executeUpdate("BEGIN TRANSACTION");
    }

    public static void commitTransaction(){
        executeUpdate("COMMIT");
    }

    public static void executeQuery(String statement, DatabaseConsumer<ResultSet> callback){
        initializeConnection();
        try(PreparedStatement preparedStatement = connection.prepareStatement(statement); ResultSet resultSet = preparedStatement.executeQuery()){
            callback.accept(resultSet);
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static void executeUpdate(String statement) {
        executeUpdate(statement, preparedStatement -> {});
    }

    public static void executeUpdate(String statement, DatabaseConsumer<PreparedStatement> statementConsumer){
        initializeConnection();
        try(PreparedStatement preparedStatement = connection.prepareStatement(statement)){
            statementConsumer.accept(preparedStatement);
            preparedStatement.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static Connection getConnection() {
        initializeConnection();
        return connection;
    }

    private static void initializeConnection(){
        try {
            long currentTime = System.currentTimeMillis() / 1000;
            if(connection == null || currentTime - lastConnectionCreation > CONNECTION_TIMEOUT) {
                if(connection != null)
                    connection.close();
                Class.forName("org.sqlite.JDBC");
                String sqlURL = "jdbc:sqlite:" + dbFilePath;
                connection = DriverManager.getConnection(sqlURL);
                connection.setAutoCommit(false);
                lastConnectionCreation = currentTime;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public interface DatabaseConsumer<T>{

        void accept(T t) throws SQLException;

    }


}
