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
 * Fábrica de conexões com bancos de dados.
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
public class ConnectionFactory {
	
	private static String database_properties_path_;
	
	
	public String database_properties_path() {
		return database_properties_path_;
	}
	
	
	public void database_properties_path(String database_properties_path) {
		database_properties_path_ = database_properties_path;
	}
	
	
	public static Connection connect() {
		return getConnection();
	}
	
	public static Connection connect(String ... args) {
		return getConnection(args);
	}
	
	public static Connection getConnection() {
		
		Connection connection = null;
				
		if ( JediORMEngine.app_db_config != null && (new File(JediORMEngine.app_db_config) ).exists() ) {

			try {
				
				Properties database_settings = new Properties();
				
				FileInputStream file_input_stream = new FileInputStream(JediORMEngine.app_db_config);
				
				database_settings.load(file_input_stream);
				
				String database_engine = database_settings.getProperty("database.engine") != null ? database_settings.getProperty("database.engine") : database_settings.getProperty("database.engine");
				
				String database_host = database_settings.getProperty("database.host") != null ? database_settings.getProperty("database.host") : database_settings.getProperty("database.host");
				
				String database_port = database_settings.getProperty("database.port") != null ? database_settings.getProperty("database.port") : database_settings.getProperty("database.port");
				
				String database_user = database_settings.getProperty("database.user") != null ? database_settings.getProperty("database.user") : database_settings.getProperty("database.user");
				
				String database_password = database_settings.getProperty("database.password") != null ? database_settings.getProperty("database.password") : database_settings.getProperty("database.password");
				
				String database_name = database_settings.getProperty("database.name") != null ? database_settings.getProperty("database.name") : database_settings.getProperty("database.name");
				
				String database_options_autocommit = database_settings.getProperty("database.options.autocommit") != null ? database_settings.getProperty("database.options.autocommit") : database_settings.getProperty("database.options.autocommit");
				
				
				String jdbc_driver = "";
				
				if (database_engine != null && !database_engine.equals("") ) {					
					
					if (database_host == null || (database_host != null && database_host.equals("") ) ) {
						
						if (!database_engine.equalsIgnoreCase("h2") && !database_engine.equalsIgnoreCase("sqlite") ) {
							
							database_host = "localhost";
						}
					}
					
					if (database_port == null || (database_port != null && database_port.equals("") ) ) {
						
						if (database_engine.equalsIgnoreCase("mysql") ) {
							
							database_port = "3306";
							
						} else if (database_engine.equalsIgnoreCase("postgresql") ) {
							
							database_port = "5432";
							
						} else if (database_engine.equalsIgnoreCase("oracle") ) {
							
							database_port = "1521";
						}
					}
					
					if (database_user == null || (database_user != null && database_user.equals("") ) ) {
						
						if (database_engine.equalsIgnoreCase("mysql") ) {
							
							database_user = "root";
							
						} else if (database_engine.equalsIgnoreCase("postgresql") ) {
							
							database_user = "postgres";
							
						} else if (database_engine.equalsIgnoreCase("oracle") ) {
							
							database_user = "hr";
							
						} else if (database_engine.equalsIgnoreCase("h2") ) {
							
							database_user = "sa";
						}
					}
					
					if (database_name == null || (database_name != null && database_name.equals("") ) ) {
						
						if (database_engine.equalsIgnoreCase("mysql") ) {
							
							database_name = "mysql";
							
						} else if (database_engine.equalsIgnoreCase("postgresql") ) {
							
							database_name = "postgres";
							
						} else if (database_engine.equalsIgnoreCase("oracle") ) {
							
							database_name = "xe";
							
						} else if (database_engine.equalsIgnoreCase("h2") ) {
							
							database_name = "test";
						}
					}
					
					if (database_engine.equalsIgnoreCase("mysql") ) {
						
						jdbc_driver = "com.mysql.jdbc.Driver";
						
						Class.forName(jdbc_driver);
						
						connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", database_host, database_port, database_name, database_user, database_password) );
						
					} else if (database_engine.equalsIgnoreCase("postgresql") ) {		
						
						jdbc_driver = "org.postgresql.Driver";
						
						Class.forName(jdbc_driver);
						
						connection = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", database_host, database_port, database_name), database_user, database_password);
						
					} else if (database_engine.equalsIgnoreCase("oracle") ) {
						
						jdbc_driver = "oracle.jdbc.driver.OracleDriver";
						
						Class.forName(jdbc_driver);
						
						connection = DriverManager.getConnection(String.format("jdbc:oracle:thin:@%s:%s:%s", database_host, database_port, database_name), database_user, database_password);
						
						//	connection = DriverManager.getConnection(String.format("jdbc:oracle:thin:@%s:%s/%s", database_host, database_port, database_name), database_user, database_password);
						
					} else if (database_engine.equalsIgnoreCase("sqlite") ) {
						
						jdbc_driver = "org.sqlite.JDBC";
						
						Class.forName(jdbc_driver);
						
						connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", database_name) );
						
					} else if (database_engine.equalsIgnoreCase("h2") ) {
						
						jdbc_driver = "org.h2.Driver";
						
						Class.forName(jdbc_driver);
						
						connection =  DriverManager.getConnection(String.format("jdbc:%s:~/%s", database_engine, database_name), database_user, database_password);
					}
					
				}
				
				if (connection != null) {
					
					if (database_options_autocommit != null && !database_options_autocommit.isEmpty() && (database_options_autocommit.equalsIgnoreCase("true") || database_options_autocommit.equalsIgnoreCase("false") ) ) {
						
						connection.setAutoCommit(Boolean.parseBoolean(database_options_autocommit) );
						
					} else {
						
						connection.setAutoCommit(false);
					}
				}
				
				file_input_stream.close();
				
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
	
	public static Connection getConnection(String ... args) {
		
		Connection connection = null;
		
		if (args != null && args.length > 0) {
			
			try {
				
				String database_engine = "";
				
				String database_host = "";
				
				String database_port = "";
				
				String database_user = "";
				
				String database_password = "";
				
				String database_name = "";
				
				String database_options_autocommit = "";
				
				for (int i = 0; i < args.length; i++) {
					
					//	Convertendo o texto para letras minúsculas.
					args[i] = args[i].toLowerCase();
					
					//	Retirando os espaços em branco que possam estar envolvendo o sinal de igualdade.
					args[i] = args[i].replace(" = ", "=");
					
					//	Carregando o driver adequado e memorizando o engine escolhido pelo usuário.
					if (args[i].equals("engine=mysql") ) {
						
						Class.forName("com.mysql.jdbc.Driver");
						
						database_engine = "mysql";
						
					} else if (args[i].equals("engine=postgresql") ) {
						
						Class.forName("org.postgresql.Driver");
						
						database_engine = "postgresql";
						
					} else if (args[i].equals("engine=oracle") ) {
						
						Class.forName("oracle.jdbc.driver.OracleDriver");
						
						database_engine = "oracle";
						
					} else if (args[i].equals("engine=sqlite") ) {
						
						Class.forName("org.sqlite.JDBC");
						
						database_engine = "sqlite";
						
					} else if (args[i].equals("engine=h2") ) {
						
						database_engine = "h2";
						
						Class.forName("org.h2.Driver");
						
					}
					
					//	Configurando o host ou máquina hospedeira ou servidor de banco de dados.
					if (args[i].startsWith("host=") ) {
					
						if (args[i].split("=").length > 1) {
						
							database_host = args[i].split("=")[1];
						}
					}
					
					if (database_host != null && database_host.isEmpty() && !database_engine.equals("h2") && !database_engine.equals("sqlite") ) {
						
						database_host = "localhost";
					}
					
					//	Configurando a porta de conexão.
					if (args[i].matches("port=\\d+") ) {
						
						database_port = args[i].split("=")[1]; 
					}
					
					if (database_port != null && database_port.isEmpty() ) {
						
						if (database_engine.equals("mysql") ) {
						
							database_port = "3306";
							
						} else if (database_engine.equals("postgresql") ) {
							
							database_port = "5432";
							
						} else if (database_engine.equals("oracle") ) {
							
							database_port = "1521";
						}
					}
					
					// Definindo o banco de dados.
					if (args[i].startsWith("database=") ) {
						
						if (args[i].split("=").length > 1) {
							
							database_name = args[i].split("=")[1];
						}						
					}
					
					if (database_name != null && database_name.isEmpty() ) {
						
						if (database_engine.equals("mysql") ) {
						
							database_name = "mysql";
							
						} else if (database_engine.equals("postgresql") ) {
							
							database_name = "postgres";
							
						} else if (database_engine.equals("oracle") ) {
							
							database_name = "xe";
							
						} else if (database_engine.equals("h2") ) {
							
							database_name = "test";
						}
					}
					
					// Definindo o usuário.
					if (args[i].startsWith("user=") ) {
						
						if (args[i].split("=").length > 1) {
							
							database_user = args[i].split("=")[1];
						}
					}
					
					if (database_user != null && database_user.isEmpty() ) {
						
						if (database_engine.equals("mysql") ) {
							
							database_user = "root";
							
						} else if (database_engine.equals("postgresql") ) {
							
							database_user = "postgres";
							
						} else if (database_engine.equals("oracle") ) {
							
							database_user = "hr";
							
						} else if (database_engine.equals("h2") ) {
							
							database_user = "sa";
						}
					}
					
					// Definindo a senha.
					if (args[i].startsWith("password=") ) {
						
						if (args[i].split("=").length > 1) {
							
							database_password = args[i].split("=")[1];
						}
					}
					
					if (database_password != null && database_password.isEmpty() ) {
						
						if (database_engine.equals("mysql") ) {
							
							database_password = "mysql";
							
						} else if (database_engine.equals("postgresql") ) {
							
							database_password = "postgres";
							
						} else if (database_engine.equals("oracle") ) {
							
							database_password = "hr";
							
						} else if (database_engine.equals("h2") ) {
							
							database_password = "1";
						}
					}
					
					if (args[i].startsWith("autocommit=") ) {
						
						if (args[i].split("=").length > 1) {
							
							database_options_autocommit = args[i].split("=")[1];
						}
					}
					
					args[i] = args[i].replace("=", " = ");
				}
				
				if (database_engine.equals("mysql") ) {
					
					connection = DriverManager.getConnection(String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s", database_host, database_port, database_name, database_user, database_password) );
					
				} else if (database_engine.equals("postgresql") ) {
					
					connection = DriverManager.getConnection(String.format("jdbc:postgresql://%s:%s/%s", database_host, database_port, database_name), database_user, database_password);
					
				} else if (database_engine.equals("oracle") ) {
					
					String sid = database_name;  
			        
			        String url = "jdbc:oracle:thin:@" + database_host + ":" + database_port + ":" + sid;
			        
			        connection = DriverManager.getConnection(url, database_user, database_password);
			        
				} else if (database_engine.equals("sqlite") ) {
						
					Class.forName("org.sqlite.JDBC");
						
					connection = DriverManager.getConnection("jdbc:sqlite:" + database_name);
					
				} else if (database_engine.equals("h2") ) {
					
					connection =  DriverManager.getConnection(String.format("jdbc:%s:~/%s", database_engine, database_name), database_user, database_password);
				}
				
				if (connection != null) {
					
					if (!database_options_autocommit.isEmpty() && (database_options_autocommit.equalsIgnoreCase("true") || database_options_autocommit.equalsIgnoreCase("false") ) ) {
						
						connection.setAutoCommit(Boolean.parseBoolean(database_options_autocommit) );
						
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