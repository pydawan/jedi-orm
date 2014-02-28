/***********************************************************************************************
 * @(#)ConnectionFactory.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/01/26
 * 
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * 
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the GNU Public License v2.0 which accompanies 
 * this distribution, and is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *    Thiago Alexandre Martins Monteiro - initial API and implementation
 ************************************************************************************************/

package jedi.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import jedi.db.engine.JediORMEngine;

/**
 * Database Connection Factory.
 * 
 * @author Thiago Alexandre Martins Monteiro
 * @version 1.0
 * 
 */
public class ConnectionFactory {

    private static String databasePropertiesPath;

    /**
     * @return String The path to the database properties.
     */
    public String getDatabasePropertiesPath() {
        return databasePropertiesPath;
    }

    /**
     * @param databasePropertiesPath
     */
    public void setDatabasePropertiesPath(String databasePropertiesPath) {
        ConnectionFactory.databasePropertiesPath = databasePropertiesPath;
    }

    /**
     * @return Connection Returns a connection to a database.
     */
    public static Connection connect() {
        return getConnection();
    }

    /**
     * @param args
     * @return
     */
    public static Connection connect(String... args) {
        return getConnection(args);
    }

    /**
     * @return Connection
     */
    public static Connection getConnection() {
        Connection connection = null;

        if (JediORMEngine.APP_DB_CONFIG_FILE != null && (new File(JediORMEngine.APP_DB_CONFIG_FILE)).exists()) {
            try {
                Properties databaseSettings = new Properties();
                FileInputStream fileInputStream = new FileInputStream(JediORMEngine.APP_DB_CONFIG_FILE);
                databaseSettings.load(fileInputStream);

                String databaseEngine = databaseSettings.getProperty("database.engine") != null
                        ? databaseSettings.getProperty("database.engine")
                        : databaseSettings.getProperty("database.engine");

                String databaseHost = databaseSettings.getProperty("database.host") != null
                        ? databaseSettings.getProperty("database.host")
                        : databaseSettings.getProperty("database.host");

                String databasePort = databaseSettings.getProperty("database.port") != null
                        ? databaseSettings.getProperty("database.port")
                        : databaseSettings.getProperty("database.port");

                String databaseUser = databaseSettings.getProperty("database.user") != null
                        ? databaseSettings.getProperty("database.user")
                        : databaseSettings.getProperty("database.user");

                String databasePassword = databaseSettings.getProperty("database.password") != null
                        ? databaseSettings.getProperty("database.password")
                        : databaseSettings.getProperty("database.password");

                String databaseName = databaseSettings.getProperty("database.name") != null
                        ? databaseSettings.getProperty("database.name")
                        : databaseSettings.getProperty("database.name");

                String databaseOptionsAutocommit = databaseSettings.getProperty("database.options.autocommit") != null
                        ? databaseSettings.getProperty("database.options.autocommit") 
                        : databaseSettings.getProperty("database.options.autocommit");

                String jdbcDriver = "";

                if (databaseEngine != null && !databaseEngine.equals("")) {
                    if (databaseHost == null || (databaseHost != null && databaseHost.equals(""))) {
                        if (!databaseEngine.equalsIgnoreCase("h2") && !databaseEngine.equalsIgnoreCase("sqlite")) {
                            databaseHost = "localhost";
                        }
                    }

                    if (databasePort == null || (databasePort != null && databasePort.equals(""))) {
                        if (databaseEngine.equalsIgnoreCase("mysql")) {
                            databasePort = "3306";
                        } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                            databasePort = "5432";
                        } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                            databasePort = "1521";
                        }
                    }

                    if (databaseUser == null || (databaseUser != null && databaseUser.equals(""))) {
                        if (databaseEngine.equalsIgnoreCase("mysql")) {
                            databaseUser = "root";
                        } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                            databaseUser = "postgres";
                        } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                            databaseUser = "hr";
                        } else if (databaseEngine.equalsIgnoreCase("h2")) {
                            databaseUser = "sa";
                        }
                    }

                    if (databaseName == null || (databaseName != null && databaseName.equals(""))) {
                        if (databaseEngine.equalsIgnoreCase("mysql")) {
                            databaseName = "mysql";
                        } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                            databaseName = "postgres";
                        } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                            databaseName = "xe";
                        } else if (databaseEngine.equalsIgnoreCase("h2")) {
                            databaseName = "test";
                        }
                    }

                    if (databaseEngine.equalsIgnoreCase("mysql")) {
                        jdbcDriver = "com.mysql.jdbc.Driver";
                        Class.forName(jdbcDriver);
                        connection = DriverManager.getConnection(
                            String.format(
                                "jdbc:mysql://%s:%s/%s?user=%s&password=%s", 
                                databaseHost, databasePort, databaseName, databaseUser, databasePassword
                            )
                        );
                    } else if (databaseEngine.equalsIgnoreCase("postgresql")) {
                        jdbcDriver = "org.postgresql.Driver";
                        Class.forName(jdbcDriver);
                        connection = DriverManager.getConnection(
                            String.format(
                                "jdbc:postgresql://%s:%s/%s", 
                                databaseHost, databasePort, databaseName
                            ), 
                            databaseUser, 
                            databasePassword
                        );
                    } else if (databaseEngine.equalsIgnoreCase("oracle")) {
                        jdbcDriver = "oracle.jdbc.driver.OracleDriver";
                        Class.forName(jdbcDriver);
                        connection = DriverManager.getConnection(
                            String.format(
                                "jdbc:oracle:thin:@%s:%s:%s", 
                                databaseHost, databasePort, databaseName
                            ), 
                            databaseUser, 
                            databasePassword
                        );
                    } else if (databaseEngine.equalsIgnoreCase("sqlite")) {
                        jdbcDriver = "org.sqlite.JDBC";
                        Class.forName(jdbcDriver);
                        connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", databaseName));
                    } else if (databaseEngine.equalsIgnoreCase("h2")) {
                        jdbcDriver = "org.h2.Driver";
                        Class.forName(jdbcDriver);
                        connection = DriverManager.getConnection(
                            String.format(
                                "jdbc:%s:~/%s", databaseEngine, databaseName
                            ), 
                            databaseUser, 
                            databasePassword
                        );
                    }
                }

                if (connection != null) {
                    if (databaseOptionsAutocommit != null && !databaseOptionsAutocommit.isEmpty()
                        && (databaseOptionsAutocommit.equalsIgnoreCase("true") 
                        || databaseOptionsAutocommit.equalsIgnoreCase("false"))) {
                        connection.setAutoCommit(Boolean.parseBoolean(databaseOptionsAutocommit));
                    } else {
                        connection.setAutoCommit(false);
                    }
                }
                fileInputStream.close();
            } catch (SQLException e) {
                System.out.println("Ocorreram uma ou mais falhas ao tentar obter uma conexão com o banco de dados.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("O driver de conexão com o banco de dados não foi encontrado.");
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                System.out.println("O arquivo com as configurações não foi encontrado na raiz do projeto.");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("Erro ao ler o arquivo de configurações.");
                e.printStackTrace();
            }
        }
        return connection;
    }

    /**
     * @param args
     * @return
     */
    public static Connection getConnection(String... args) {
        Connection connection = null;

        if (args != null && args.length > 0) {
            try {
                String databaseEngine = "";
                String databaseHost = "";
                String databasePort = "";
                String databaseUser = "";
                String databasePassword = "";
                String databaseName = "";
                String databaseOptionsAutocommit = "";

                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].toLowerCase();
                    args[i] = args[i].replace(" = ", "=");

                    // Engine
                    if (args[i].equals("engine=mysql")) {
                        Class.forName("com.mysql.jdbc.Driver");
                        databaseEngine = "mysql";
                    } else if (args[i].equals("engine=postgresql")) {
                        Class.forName("org.postgresql.Driver");
                        databaseEngine = "postgresql";
                    } else if (args[i].equals("engine=oracle")) {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                        databaseEngine = "oracle";
                    } else if (args[i].equals("engine=sqlite")) {
                        Class.forName("org.sqlite.JDBC");
                        databaseEngine = "sqlite";
                    } else if (args[i].equals("engine=h2")) {
                        databaseEngine = "h2";
                        Class.forName("org.h2.Driver");
                    }

                    // Host
                    if (args[i].startsWith("host=")) {
                        if (args[i].split("=").length > 1) {
                            databaseHost = args[i].split("=")[1];
                        }
                    }

                    if (databaseHost != null && databaseHost.isEmpty() && !databaseEngine.equals("h2") 
                        && !databaseEngine.equals("sqlite")) {
                        databaseHost = "localhost";
                    }

                    // Port
                    if (args[i].matches("port=\\d+")) {
                        databasePort = args[i].split("=")[1];
                    }

                    if (databasePort != null && databasePort.isEmpty()) {
                        if (databaseEngine.equals("mysql")) {
                            databasePort = "3306";
                        } else if (databaseEngine.equals("postgresql")) {
                            databasePort = "5432";
                        } else if (databaseEngine.equals("oracle")) {
                            databasePort = "1521";
                        }
                    }

                    // Database
                    if (args[i].startsWith("database=")) {
                        if (args[i].split("=").length > 1) {
                            databaseName = args[i].split("=")[1];
                        }
                    }

                    if (databaseName != null && databaseName.isEmpty()) {
                        if (databaseEngine.equals("mysql")) {
                            databaseName = "mysql";
                        } else if (databaseEngine.equals("postgresql")) {
                            databaseName = "postgres";
                        } else if (databaseEngine.equals("oracle")) {
                            databaseName = "xe";
                        } else if (databaseEngine.equals("h2")) {
                            databaseName = "test";
                        }
                    }

                    // User
                    if (args[i].startsWith("user=")) {
                        if (args[i].split("=").length > 1) {
                            databaseUser = args[i].split("=")[1];
                        }
                    }

                    if (databaseUser != null && databaseUser.isEmpty()) {
                        if (databaseEngine.equals("mysql")) {
                            databaseUser = "root";
                        } else if (databaseEngine.equals("postgresql")) {
                            databaseUser = "postgres";
                        } else if (databaseEngine.equals("oracle")) {
                            databaseUser = "hr";
                        } else if (databaseEngine.equals("h2")) {
                            databaseUser = "sa";
                        }
                    }

                    // Password
                    if (args[i].startsWith("password=")) {
                        if (args[i].split("=").length > 1) {
                            databasePassword = args[i].split("=")[1];
                        }
                    }

                    if (databasePassword != null && databasePassword.isEmpty()) {
                        if (databaseEngine.equals("mysql")) {
                            databasePassword = "mysql";
                        } else if (databaseEngine.equals("postgresql")) {
                            databasePassword = "postgres";
                        } else if (databaseEngine.equals("oracle")) {
                            databasePassword = "hr";
                        } else if (databaseEngine.equals("h2")) {
                            databasePassword = "1";
                        }
                    }

                    if (args[i].startsWith("autocommit=")) {
                        if (args[i].split("=").length > 1) {
                            databaseOptionsAutocommit = args[i].split("=")[1];
                        }
                    }
                    args[i] = args[i].replace("=", " = ");
                }

                if (databaseEngine.equals("mysql")) {
                    connection = DriverManager.getConnection(
                        String.format(
                            "jdbc:mysql://%s:%s/%s?user=%s&password=%s", 
                            databaseHost, databasePort, databaseName, databaseUser, databasePassword
                        )
                    );
                } else if (databaseEngine.equals("postgresql")) {
                    connection = DriverManager.getConnection(
                        String.format(
                            "jdbc:postgresql://%s:%s/%s", 
                            databaseHost, databasePort, databaseName
                        ), 
                        databaseUser, 
                        databasePassword
                    );
                } else if (databaseEngine.equals("oracle")) {
                    String sid = databaseName;
                    String url = "jdbc:oracle:thin:@" + databaseHost + ":" + databasePort + ":" + sid;
                    connection = DriverManager.getConnection(url, databaseUser, databasePassword);
                } else if (databaseEngine.equals("sqlite")) {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName);
                } else if (databaseEngine.equals("h2")) {
                    connection = DriverManager.getConnection(
                        String.format(
                            "jdbc:%s:~/%s", 
                            databaseEngine, databaseName
                        ), 
                        databaseUser, 
                        databasePassword
                    );
                }

                if (connection != null) {
                    if (!databaseOptionsAutocommit.isEmpty() && (databaseOptionsAutocommit.equalsIgnoreCase("true") 
                        || databaseOptionsAutocommit.equalsIgnoreCase("false"))) {
                        connection.setAutoCommit(Boolean.parseBoolean(databaseOptionsAutocommit));
                    } else {
                        connection.setAutoCommit(false);
                    }
                }
            } catch (SQLException e) {
                System.out.println("Ocorreram uma ou mais falhas ao tentar obter uma conexão com o banco de dados.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("O driver de conexão com o banco de dados não foi encontrado.");
                e.printStackTrace();
            }
        }
        return connection;
    }
}