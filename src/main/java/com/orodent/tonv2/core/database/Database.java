package com.orodent.tonv2.core.database;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final String DBNAME = "TonDatabase";
    private final String DB_USER = "APP";
    private final String DB_PSW = "pw";

    public Connection getConnection() {
        try {
            return getEmbeddedConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Errore connessione DB", e);
        }
    }

    public void start() {
        try {
            System.setProperty("derby.drda.startNetworkServer", "true");
            String ip = InetAddress.getLocalHost().getHostAddress();
            System.setProperty("derby.system.home", "C:\\");        //      I:\CliZr\Tommaso\
            System.setProperty("derby.drda.host", ip);
            Class<?> clazz = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            clazz.getConstructor().newInstance();
            waitForStart();
            System.out.println("Database avviato");
        } catch (Exception e) {
            System.out.println("Errore di avvio del database.");
        }
    }

    /**
     * Attende fino a 6 secondi che il server risponda al ping.
     *
     * @throws Exception se il server non risponde dopo il tempo atteso.
     */
    private void waitForStart() throws Exception {
        org.apache.derby.drda.NetworkServerControl server = new org.apache.derby.drda.NetworkServerControl();
        for (int i = 0; i < 6; i++) {
            try {
                Thread.sleep(1000);
                server.ping();
                return;
            } catch (Exception e) {
                if (i == 5) {
                    throw e;
                }
            }
        }
    }

    /**
     * Ottiene una connessione embedded.
     * @return la connessione embedded
     * @throws SQLException se si verifica un errore
     */
    private Connection getEmbeddedConnection() throws SQLException {
        String url = "jdbc:derby:" + DBNAME + ";create=true;user=" + DB_USER + ";password=" + DB_PSW;// I:\CliZr\Tommaso\
        return DriverManager.getConnection(url);
    }

    public void stop(){
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (Exception e) {
            System.out.println("Errore durante la chiusura del database.");
        }
    }
}
