package jedi.db.engine;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import jedi.db.ConnectionFactory;
import jedi.db.Models;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.DecimalField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.IntegerField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Manager;

/**
 * Classe do Mecanismo de Mapeamento Objeto-Relacional.
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JediORMEngine {
	
	// Diretório raiz da aplicação.
	public static String app_root_dir = System.getProperty("user.dir");
	
	// Diretório dos códigos-fonte da aplicação.
	public static String app_src_dir = String.format("%s/web/WEB-INF/src", app_root_dir);
	
	public static String app_db_config = String.format("%s/web/WEB-INF/config/database.properties", JediORMEngine.app_root_dir);
	
	// Diretório de arquivos de model deste framework.
	public static final String jedi_db_models = String.format("jedi%sdb%smodels", File.separator, File.separator, File.separator);
	
	// Modelos da aplicação lidos e que serão mapeados em tabelas por este framework.
	public static List<String> readed_app_models = new ArrayList<String>();
	
	// Tabelas geradas.
	public static List<String> generated_tables = new ArrayList<String>();
	
	/**
	 * Método responsável por converter modelos da aplicação em tabelas no banco de dados.
	 * 
	 * @author Thiago Alexandre Martins Monteiro
	 * @param path
	 */
	public static void syncdb(String path) {
		
		// Referenciando o diretório da aplicação.
		File app_dir = new File(path);

		// Verificando se app_dir existe.
		if (app_dir != null && app_dir.exists() ) {
			
			// Verificando se app_dir é um diretório e não um arquivo (tanto arquivos como diretórios são referenciados por File).
			if (app_dir.isDirectory() ) {
				
				// Obtendo o conteúdo (arquivos e sub-diretórios) de app_dir (diretório da aplicação).
				File[] app_dir_contents = app_dir.listFiles();
				
				// Procurando pelo sub-diretório app/models no conteúdo de app_dir.
				for (File app_dir_content : app_dir_contents) {
					
					// Obtém todos os diretórios models que não sejam do framework.					
					if (!app_dir_content.getAbsolutePath().contains(JediORMEngine.jedi_db_models) && app_dir_content.getAbsolutePath().endsWith("models") ) {
						
						// Obter a lista de arquivos de app/models.
						File[] app_models_files = app_dir_content.listFiles();
						
						//Strings para criação de instruções SQL.
						
						// SQL geral.
						String sql = "";
						
						// SQL para criação de índices (indexes).
						String sql_index = "";
						
						// SQL para criação de chaves estrangeiras (foreign keys).
						String sql_foreign_key = "";
						
						// SQL para criação de relações de relacionamento (tabela de associações).
						String sql_many_to_many_association = "";
						
						Map<String, String> sql_oracle_auto_increment_triggers = new HashMap<String, String>();
						
						String sql_oracle_sequences = "";
						
						// Triggers para DATETIME no MySQL.
						List<String> mysql_datetime_triggers = new ArrayList<String>();
						
						int mysql_version_number = 0;
						
						// Obtendo uma conexão com o banco de dados por meio de uma fábrica de conexões.
						Connection conn = ConnectionFactory.getConnection();
						
						Statement stmt = null;
						
						String database_engine = "";
						
						try {
							
							// Desabilitando o auto-commit.
							conn.setAutoCommit(false);
							
							stmt = conn.createStatement();
							
							Properties database_settings = new Properties();
							
							// BUG: Se o arquivo nao for encontrado tem que encerrar aqui ou lancar excecao.
							FileInputStream file_input_stream = new FileInputStream(app_db_config);
							
							database_settings.load(file_input_stream);
							
							database_engine = database_settings.getProperty("database.engine");
							
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						// Realizando o mapeamento ORM (criando a estrutura de banco de dados) para cada classe de modelo do projeto da aplicação.
						for (File app_model_file : app_models_files) {
							
							try {
								
								String model_class_name = app_model_file.getAbsolutePath();
								
								// Despreza arquivos que não terminem com .java
								if (!model_class_name.endsWith("java") ) {
									continue;
								}
								
								model_class_name = model_class_name.replace(String.format("%s%ssrc%s", JediORMEngine.app_root_dir, File.separator, File.separator) , "");
								
								// model_class_name = model_class_name.replace("/", ".").replace(".java", "");
								model_class_name = model_class_name.replace(File.separator, ".").replace(".java", "");
								
								JediORMEngine.readed_app_models.add(model_class_name);
								
								// Referenciando o objeto que armazena a definição da classe de modelo.
								// Class model_class = Class.forName(String.format("app.models.%s", app_model_file.getName().replace(".java", "") ) );
								Class model_class = Class.forName(model_class_name);
								
								// Ignora classes que não sejam sub-classes de jedi.db.models.Model.
								if (!model_class.getSuperclass().getName().equals("jedi.db.models.Model") ) {
									continue;
								}
								
								String table_name = "";
								
								// Obtendo a anotação da classe de modelo.
								Table table_annotation = (Table) model_class.getAnnotation(Table.class);
								
								// Verificando se a anotação existe e se um nome foi informado para a tabela do modelo.
								if (table_annotation != null && !( (Table) table_annotation).name().equals("") ) {
									
									table_name = ( (Table) table_annotation).name();
									
								} else {
									
									table_name = String.format("%ss", model_class.getSimpleName() );
									
								}
								
								table_name = table_name.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2");
								
								table_name = table_name.toLowerCase();
								
								generated_tables.add(table_name);
								
								if (database_engine.trim().equalsIgnoreCase("mysql") || database_engine.trim().equalsIgnoreCase("h2") ) {
									
									sql += String.format("CREATE TABLE IF NOT EXISTS %s (\n", table_name);
									
									sql += String.format("    %s INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n", model_class.getSuperclass().getDeclaredField("id").getName() );
									
								} else {
									
									sql += String.format("CREATE TABLE %s (\n", table_name);
									
								}
								
								if (database_engine.trim().equalsIgnoreCase("postgresql") ) {
									
									sql += String.format("    %s SERIAL NOT NULL PRIMARY KEY,\n", model_class.getSuperclass().getDeclaredField("id").getName() );
									
								} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
									
									sql += String.format("    %s NUMBER(10,0) NOT NULL PRIMARY KEY,\n", model_class.getSuperclass().getDeclaredField("id").getName() );
									
									sql_oracle_sequences += String.format("CREATE SEQUENCE seq_%s MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;\n\n", table_name);
									
									// CREATE SEQUENCE "PRODUTO_SEQ" MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE; 
									
									// trg_auto_increment_table_name é um nome extenso.
									sql_oracle_auto_increment_triggers.put(table_name, String.format("tgr_autoincr_%s", table_name) );
									
								}
								
								String postgresql_or_oracle_columns_comments = "";
								
								for (Field field : model_class.getDeclaredFields() ) {
									
									if (field.getName().equals("serialVersionUID") ) {
										continue;
									}
									
									for (Annotation field_annotation : field.getAnnotations() ) {
										
										if (field_annotation instanceof CharField) {
											
											CharField char_field_annotation = (CharField) field_annotation;
											
											if (database_engine.equalsIgnoreCase("mysql") ) {
												
												sql += String.format(
														
														"    %s VARCHAR(%d)%s%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														char_field_annotation.max_length(), 
														
														char_field_annotation.required() ? " NOT NULL" : "",
														
														(char_field_annotation.default_value() != null && !char_field_annotation.default_value().equals("\\0") ) ? " DEFAULT '" + char_field_annotation.default_value() + "'" : "",
														
														char_field_annotation.unique() ? " UNIQUE" : "",
														
														(char_field_annotation.comment() != null && !char_field_annotation.comment().equals("") ) ? " COMMENT '" + char_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.equalsIgnoreCase("postgresql") ) {
												
												sql += String.format(
														
														"    %s VARCHAR(%d)%s%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														char_field_annotation.max_length(), 
														
														char_field_annotation.required() ? " NOT NULL" : "",
																
														(char_field_annotation.default_value() != null && !char_field_annotation.default_value().equals("\\0") ) ? " DEFAULT '" + char_field_annotation.default_value() + "'" : "",
														
														char_field_annotation.unique() ? " UNIQUE" : ""

												);
												
												if (char_field_annotation != null && char_field_annotation.comment() != null && !char_field_annotation.comment().trim().isEmpty() ) {
												
													postgresql_or_oracle_columns_comments += String.format(
															
															"COMMENT ON COLUMN %s.%s IS '%s';\n\n",
															
															table_name,
															
															field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
															
															char_field_annotation.comment() 
													);													
												}
												
											} else if (database_engine.equalsIgnoreCase("oracle") ) {
												
												sql += String.format(														
														
														"    %s VARCHAR2(%d)%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														char_field_annotation.max_length(),
														
														(char_field_annotation.default_value() != null && !char_field_annotation.default_value().equals("\\0") ) ? " DEFAULT '" + char_field_annotation.default_value() + "'" : "",
														
														char_field_annotation.required() ? " NOT NULL" : "",
														
														char_field_annotation.unique() ? " UNIQUE" : ""

												);
												
												if (char_field_annotation != null && char_field_annotation.comment() != null && !char_field_annotation.comment().trim().isEmpty() ) {
												
													postgresql_or_oracle_columns_comments += String.format(
															
															"COMMENT ON COLUMN %s.%s IS '%s';\n\n", 
															
															table_name, 
															
															field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
															
															char_field_annotation.comment() 
													);													
												}
												
											} else if (database_engine.trim().equalsIgnoreCase("h2") ) {
												
												sql += String.format(
														
														"    %s VARCHAR(%d)%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														char_field_annotation.max_length(), 
														
														char_field_annotation.required() ? " NOT NULL" : "",
														
														(char_field_annotation.default_value() != null && !char_field_annotation.default_value().equals("\\0") ) ? " DEFAULT '" + char_field_annotation.default_value() + "'" : "",
														
														char_field_annotation.unique() ? " UNIQUE" : ""
												);
											} else {
												
											}
											
										}
										
										if (field_annotation instanceof IntegerField) {
											
											IntegerField integer_field_annotation = (IntegerField) field_annotation;
											
											if (database_engine.trim().equalsIgnoreCase("mysql") ) {
											
												sql += String.format(
														
														"    %s INT(%d)%s%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														integer_field_annotation.size(), 
														
														integer_field_annotation.required() ? " NOT NULL" : "",
														
														!integer_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", integer_field_annotation.default_value() ) : "",
														
														integer_field_annotation.unique() ? " UNIQUE" : "",
														
														(integer_field_annotation.comment() != null && !integer_field_annotation.comment().equals("") && database_engine.trim().equalsIgnoreCase("mysql") ) ? " COMMENT '" + integer_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("postgresql") ) {
												
												sql += String.format(
													
														"    %s INT%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														integer_field_annotation.required() ? " NOT NULL" : "",
																
														!integer_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", integer_field_annotation.default_value() ) : "",
														
														integer_field_annotation.unique() ? " UNIQUE" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
												
												sql += String.format(
														
														"    %s NUMBER(%d,0) %s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														integer_field_annotation.size(),
														
														!integer_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", integer_field_annotation.default_value() ) : "",
														
														integer_field_annotation.required() ? " NOT NULL" : "",
														
														integer_field_annotation.unique() ? " UNIQUE" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("h2") ) {
												
												sql += String.format(
														
														"    %s INT(%d)%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														integer_field_annotation.size(), 
														
														integer_field_annotation.required() ? " NOT NULL" : "",
														
														!integer_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", integer_field_annotation.default_value() ) : "",
														
														integer_field_annotation.unique() ? " UNIQUE" : ""
												);

											} else {
												
											}
										}
										
										if (field_annotation instanceof DecimalField) {
											
											DecimalField decimal_field_annotation = (DecimalField) field_annotation;
											
											if (database_engine.trim().equalsIgnoreCase("mysql") ) {
												
												sql += String.format(
														
														"    %s DECIMAL(%d,%d)%s%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
																												
														decimal_field_annotation.precision(),
														
														decimal_field_annotation.scale(),
														
														decimal_field_annotation.required() ? " NOT NULL" : "",
																
														!decimal_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", decimal_field_annotation.default_value() ) : "",
														
														decimal_field_annotation.unique() ? " UNIQUE" : "",
														
														(decimal_field_annotation.comment() != null && !decimal_field_annotation.comment().equals("") && database_engine.trim().equalsIgnoreCase("mysql") ) ? " COMMENT '" + decimal_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("postgresql") ) {
												
												sql += String.format(
														
														"    %s DECIMAL(%d,%d)%s%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
																												
														decimal_field_annotation.precision(),
														
														decimal_field_annotation.scale(),
														
														decimal_field_annotation.required() ? " NOT NULL" : "",
																
														!decimal_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", decimal_field_annotation.default_value() ) : "",
														
														decimal_field_annotation.unique() ? " UNIQUE" : "",
														
														(decimal_field_annotation.comment() != null && !decimal_field_annotation.comment().equals("") && database_engine.trim().equalsIgnoreCase("mysql") ) ? " COMMENT '" + decimal_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
												
												sql += String.format(
														
														"    %s NUMERIC(%d,%d)%s%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
																												
														decimal_field_annotation.precision(),
														
														decimal_field_annotation.scale(),
														
														!decimal_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", decimal_field_annotation.default_value() ) : "",
														
														decimal_field_annotation.required() ? " NOT NULL" : "",
														
														decimal_field_annotation.unique() ? " UNIQUE" : "",
														
														(decimal_field_annotation.comment() != null && !decimal_field_annotation.comment().equals("") && database_engine.trim().equalsIgnoreCase("mysql") ) ? " COMMENT '" + decimal_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("h2") ) {
												
												sql += String.format(
														
														"    %s DECIMAL(%d,%d)%s%s%s,\n", 
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
																												
														decimal_field_annotation.precision(),
														
														decimal_field_annotation.scale(),
														
														decimal_field_annotation.required() ? " NOT NULL" : "",
																
														!decimal_field_annotation.default_value().trim().isEmpty() ? String.format(" DEFAULT %s", decimal_field_annotation.default_value() ) : "",
														
														decimal_field_annotation.unique() ? " UNIQUE" : ""												
												);

											} else {
												
											}
										}
										
										if (field_annotation instanceof DateField) {
											
											DateField date_field_annotation = (DateField) field_annotation;
											
											String format = "    %s DATE%s%s%s%s%s,\n";
											
											String default_date = date_field_annotation.default_value().isEmpty() ? "" : String.format(" DEFAULT '%s'", date_field_annotation.default_value().trim().toUpperCase() );
											
											default_date = default_date.replace("'NULL'", "NULL");
											
											if (default_date.contains("NULL") ) {
												default_date = default_date.replace("DEFAULT", "");
											}
											
											if (date_field_annotation.auto_now_add() || date_field_annotation.auto_now() ) {
												
												if (database_engine.equals("mysql") ) {
													
													format = "    %s DATETIME%s%s%s%s%s,\n";
													
													Manager manager = new Manager(model_class);
													
													String[] mysql_version = manager.raw("SELECT VERSION()").get(0).get(0).get("VERSION()").toString().split("\\.");
													
													mysql_version_number = Integer.parseInt(String.format("%s%s", mysql_version[0], mysql_version[1]) );
													
													if (mysql_version_number >= 56 && date_field_annotation.auto_now_add() ) {
														default_date = String.format(" DEFAULT CURRENT_TIMESTAMP", date_field_annotation.default_value() );
													}
													
												} else {													
													format = "    %s TIMESTAMP%s%s%s%s%s,\n";
												}
												
												//sb.append("DELIMITER $\n\n");
												
												if (mysql_version_number < 56) {
												
													StringBuilder sb = null;
													
													if (date_field_annotation.auto_now_add() ) {
														
														sb = new StringBuilder();
														
														sb.append(
															String.format(
																"\nCREATE TRIGGER tgr_%s_%s_insert BEFORE INSERT ON %s\n", 
																table_name,
																field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
																table_name
															) 
														);
														
														sb.append("FOR EACH ROW\n");
														sb.append("BEGIN\n");
														sb.append("    SET NEW.data_nascimento = NOW();\n");
														//sb.append("END;$\n\n");
														sb.append("END;\n\n");
														
														mysql_datetime_triggers.add(sb.toString() );
													}
													
													if (date_field_annotation.auto_now() ) {
														
														sb = new StringBuilder();
														
														sb.append(
															String.format(
																"CREATE TRIGGER tgr_%s_%s_update BEFORE UPDATE ON %s\n", 
																table_name,
																field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
																table_name
															) 
														);
														
														sb.append("FOR EACH ROW\n");
														sb.append("BEGIN\n");
														sb.append("    SET NEW.data_nascimento = NOW();\n");
														//sb.append("END;$\n\n");
														sb.append("END;\n\n");
														
														mysql_datetime_triggers.add(sb.toString() );
													}
													
													sb = null;
													
													//sb.append("DELIMITER ;\n");	
												}
											}
											
											if (database_engine.equalsIgnoreCase("mysql") ) {
												
												sql += String.format(
														
														format,
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														date_field_annotation.required() ? " NOT NULL" : "",
																
														default_date,
														
														date_field_annotation.auto_now() && (mysql_version_number >= 56 || !database_engine.equals("mysql") ) ? String.format(" ON UPDATE CURRENT_TIMESTAMP") : "",
														
														date_field_annotation.unique() ? " UNIQUE" : "", 
														
														(date_field_annotation.comment() != null && !date_field_annotation.comment().equals("") ) ? " COMMENT '" + date_field_annotation.comment() + "'" : ""
												);
												
											} else if (database_engine.equalsIgnoreCase("postgresql") ) {
												
												sql += String.format(
														
														"    %s DATE%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														date_field_annotation.required() ? " NOT NULL" : "",
														
														date_field_annotation.unique() ? " UNIQUE" : "", 
														
														(date_field_annotation.default_value() != null && !date_field_annotation.default_value().equals("\0") ) ? " DEFAULT '" + date_field_annotation.default_value() + "'" : ""

												);
												
												if (date_field_annotation != null && date_field_annotation.comment() != null && !date_field_annotation.comment().trim().isEmpty() ) {
												
													postgresql_or_oracle_columns_comments += String.format(
															"COMMENT ON COLUMN %s.%s IS '%s';\n\n", 
															table_name, 
															field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
															date_field_annotation.comment() 
													);													
												}
												
											} else if (database_engine.equalsIgnoreCase("oracle") ) {
												
												sql += String.format(														
														
														"    %s DATE%s%s%s,\n",
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
														
														(date_field_annotation.default_value() != null && !date_field_annotation.default_value().equals("\0") ) ? " DEFAULT '" + date_field_annotation.default_value() + "'" : "",
														
														date_field_annotation.required() ? " NOT NULL" : "",
														
														date_field_annotation.unique() ? " UNIQUE" : ""

												);
												
												if (date_field_annotation != null && date_field_annotation.comment() != null && !date_field_annotation.comment().trim().isEmpty() ) {
												
													postgresql_or_oracle_columns_comments += String.format(
															
															"COMMENT ON COLUMN %s.%s IS '%s';\n\n", 
															
															table_name, 
															
															field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
															
															date_field_annotation.comment() 
													);
												}
												
											} else if (database_engine.equalsIgnoreCase("h2") ) {
												
												format = "    %s TIMESTAMP%s%s%s,\n";
												
												if (date_field_annotation.auto_now() ) {
													default_date = " AS CURRENT_TIMESTAMP()";
												} else {
													if (date_field_annotation.auto_now_add() ) {
														default_date = " DEFAULT CURRENT_TIMESTAMP";
													}
												}
																								
												sql += String.format(
														
														format,
														
														field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														date_field_annotation.required() ? " NOT NULL" : "",
																
														default_date,
														
														date_field_annotation.unique() ? " UNIQUE" : ""
												);
												
											} else {
												
											}
											
										}

										
										if (field_annotation instanceof ForeignKeyField) {
											
											ForeignKeyField foreign_key_field_annotation = (ForeignKeyField) field_annotation;
											
											String column_name = "";
											
											String referenced_column = "";
											
											if (foreign_key_field_annotation.column_name().equals("") ) {
												
												column_name = field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
												
												column_name = String.format("%s_id", column_name);
												
											} else {
												
												column_name = foreign_key_field_annotation.column_name().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
												
											}
											
											if (foreign_key_field_annotation.referenced_column().equals("") ) {
												
												referenced_column = "id";
												
											} else {
												
												referenced_column = foreign_key_field_annotation.referenced_column().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
												
											}
											
											sql += String.format(
													
													"    %s INT NOT NULL%s,\n", 
													
													column_name,
													
													(foreign_key_field_annotation.comment() != null && !foreign_key_field_annotation.comment().equals("") && database_engine.trim().equalsIgnoreCase("mysql") ) ? " COMMENT '" + foreign_key_field_annotation.comment() + "'" : ""
											);
											
											String on_delete_string = "";
											
											if (foreign_key_field_annotation.on_delete().equals(Models.PROTECT) ) {
												
												on_delete_string = " ON DELETE RESTRICT";
												
											} else if (foreign_key_field_annotation.on_delete().equals(Models.SET_NULL) ) {
												
												on_delete_string = " ON DELETE SET NULL";
												
											} else if (foreign_key_field_annotation.on_delete().equals(Models.CASCADE) ) {
												
												on_delete_string = " ON DELETE CASCADE";
												
											} else if (foreign_key_field_annotation.on_delete().equals(Models.SET_DEFAULT) ) {
												
												on_delete_string = " ON DELETE SET DEFAULT";
												
											}
											
											String on_update_string = " ON UPDATE";
											
											if (foreign_key_field_annotation.on_update().equals(Models.PROTECT) ) {
												
												on_update_string = " ON UPDATE RESTRICT";
												
											} else if (foreign_key_field_annotation.on_update().equals(Models.SET_NULL) ) {
												
												on_update_string = " ON UPDATE SET NULL";
												
											} else if (foreign_key_field_annotation.on_update().equals(Models.CASCADE) ) {
												
												on_update_string = " ON UPDATE CASCADE";
												
												if (database_engine != null && database_engine.equalsIgnoreCase("oracle") ) {
													
													on_update_string = "";
												}
												
											} else if (foreign_key_field_annotation.on_update().equals(Models.SET_DEFAULT) ) {
												
												on_update_string = " ON UPDATE SET DEFAULT";
												
											}
											
											if (database_engine.trim().equalsIgnoreCase("mysql") ) {
												
												sql_foreign_key += String.format(
														
														"ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n", 
														
														table_name, 
														
														foreign_key_field_annotation.constraint_name(), 
														
														column_name, 
														
														foreign_key_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														referenced_column,
														
														on_delete_string,
														
														on_update_string
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("postgresql") ) {
												
												sql_foreign_key += String.format(
														
														// "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s%s;\n\n", 
														
														"ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n",
														
														table_name, 
														
														foreign_key_field_annotation.constraint_name(), 
														
														column_name, 
														
														foreign_key_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														referenced_column,
														
														on_delete_string,
														
														on_update_string
														
														// " DEFERRABLE INITIALLY DEFERRED"
												);
												
											} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
												
												sql_foreign_key += String.format(
														
														// "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s%s;\n\n",
														
														"ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)%s%s;\n\n",
														
														table_name, 
														
														foreign_key_field_annotation.constraint_name(), 
														
														column_name, 
														
														foreign_key_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
														
														referenced_column,
														
														on_delete_string,
														
														on_update_string
														
														// " DEFERRABLE INITIALLY DEFERRED"
												);
												
											}
											
											sql_index += String.format(
												
												"CREATE INDEX idx_%s_%s ON %s (%s);\n\n",
												
												table_name,
													
												column_name, 
													
												table_name, 
													
												column_name													
											);
											
											java.io.RandomAccessFile out = null;
																																	
											try {
												
												boolean generate_code = true;
												
												String class_path = app_model_file.getAbsolutePath();
																								
												class_path = class_path.replace(app_model_file.getName(), String.format("%s.java", foreign_key_field_annotation.model() ) );
												
												// Criando arquivo de acesso aleatório.
												out = new java.io.RandomAccessFile(class_path, "rw");
												
												// Posicionando o ponteiro de registro no início do arquivo.
												out.seek(0);
												
												String current_line = null;
												
												while ( (current_line = out.readLine() ) != null)   {
													
													if (current_line.contains(String.format("%s_set", model_class.getSimpleName().toLowerCase() ) ) ) {
														generate_code = false;
													}
												}
												
												// Posicionando o ponteiro de registro no fim do arquivo.
												if (out.length() > 0) {
													out.seek(out.length() - 1);
												} else {
													out.seek(out.length() );
												}
											    
											    StringBuilder method_str = new StringBuilder();
											    
											    method_str.append("\n");
											    
											    method_str.append("\t@SuppressWarnings(\"rawtypes\")\n");
											    
											    method_str.append(String.format("\tpublic jedi.db.models.QuerySet %s_set() {\n", model_class.getSimpleName().toLowerCase() ) );
											    
											    method_str.append(String.format("\t\treturn %s.objects.get_set(%s.class, this.id);\n", model_class.getSimpleName(), foreign_key_field_annotation.model() ) );
											    
											    method_str.append("\t}\n");
											    
											    method_str.append("}");
											    
											    if (generate_code) {
											    	out.writeBytes(method_str.toString() );
											    }
											    	
											    
											} catch (java.io.IOException e) {
											    
												System.err.println(e);
											    
											} finally{
											    
												if (out != null) {
											    	out.close();
											    }
											} 
										}
										
										if (field_annotation instanceof ManyToManyField) {
											
											ManyToManyField many_to_many_field_annotation = (ManyToManyField) field_annotation;
											
											String fmt = "";
											
											if (database_engine.trim().equalsIgnoreCase("mysql") ) {
											
												fmt = "CREATE TABLE IF NOT EXISTS %s_%s (\n";
										
												fmt += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
											
												fmt += ");\n\n";
												
											} else if (database_engine.trim().equalsIgnoreCase("postgresql") ) {
												
												fmt = "CREATE TABLE %s_%s (\n";
												
												fmt += "    id SERIAL NOT NULL PRIMARY KEY,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
											
												fmt += ");\n\n";												
												
											} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
													
													fmt = "CREATE TABLE %s_%s (\n";
													
													fmt += "    id NUMBER(10, 0) NOT NULL PRIMARY KEY,\n";
												
													fmt += "    %s_id INT NOT NULL,\n";
												
													fmt += "    %s_id INT NOT NULL,\n";
												
													fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
												
													fmt += ");\n\n";
												
											} else if (database_engine.trim().equalsIgnoreCase("h2") ) {
												
												fmt = "CREATE TABLE IF NOT EXISTS %s_%s (\n";
												
												fmt += "    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    %s_id INT NOT NULL,\n";
											
												fmt += "    CONSTRAINT unq_%s_%s UNIQUE (%s_id, %s_id)\n";
											
												fmt += ");\n\n";
												
											} else {
												
											}
											
											sql_many_to_many_association += String.format(
													
													fmt,
													
													table_name,
													
													many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
													
													model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
													
													many_to_many_field_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
													
													table_name,
													
													many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
													
													model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
													
													many_to_many_field_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()
											);
											
											//	Sequência
											sql_oracle_sequences += String.format(
													
													"CREATE SEQUENCE seq_%s_%s MINVALUE 1 MAXVALUE 999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER NOCYCLE;\n\n",
													
													table_name, 
													
													many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()
											);
											
											// Trigger de auto-incremento
											sql_oracle_auto_increment_triggers.put(													
													
													String.format("%s_%s", table_name, many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ),								
													
													String.format("tgr_autoincr_%s_%s", table_name, many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() )
											);
											
											sql_foreign_key += String.format(
												
												"ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id);\n\n", 
												
												table_name,
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
												
												table_name,
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
												
												table_name, 
												
												model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),												
												
												table_name
											);
											
											sql_index += String.format(
													
												"CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n\n", 
												
												table_name, 
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												table_name,
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() 
											);
											
											sql_foreign_key += String.format(
													
												"ALTER TABLE %s_%s ADD CONSTRAINT fk_%s_%s_%s FOREIGN KEY (%s_id) REFERENCES %s (id);\n\n", 
												
												table_name,
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
												
												table_name,
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												many_to_many_field_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),												
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase()
											);
											
											sql_index += String.format(
												
												"CREATE INDEX idx_%s_%s_%s_id ON %s_%s (%s_id);\n\n", 
												
												table_name, 
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												many_to_many_field_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												table_name, 
												
												many_to_many_field_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
												
												many_to_many_field_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() 
											);
											
											java.io.RandomAccessFile out = null;
											
											try {
												
												boolean generate_code = true;
												
												String class_path = app_model_file.getAbsolutePath();
																								
												class_path = class_path.replace(app_model_file.getName(), String.format("%s.java", many_to_many_field_annotation.model() ) );
												
												//	Criando arquivo de acesso aleatório.
												out = new java.io.RandomAccessFile(class_path, "rw");
												
												//	Posicionando o ponteiro de registro no início do arquivo.
												out.seek(0);
												
												String current_line = null;
												
												while ( (current_line = out.readLine() ) != null)   {
													
													if (current_line.contains(String.format("%s_set", model_class.getSimpleName().toLowerCase() ) ) ) {
														generate_code = false;
													}
												}
												
												//	Posicionando o ponteiro de registro no fim do arquivo.
												if (out.length() > 0) {
													out.seek(out.length() - 1);
												} else {
													out.seek(out.length() );
												}
												
											    
											    StringBuilder method_str = new StringBuilder();
											    
											    method_str.append("\n");
											    
											    method_str.append("\t@SuppressWarnings(\"rawtypes\")\n");
											    
											    method_str.append(String.format("\tpublic jedi.db.models.QuerySet %s_set() {\n", model_class.getSimpleName().toLowerCase() ) );
											    
											    method_str.append(String.format("\t\treturn %s.objects.get_set(%s.class, this.id);\n", model_class.getSimpleName(), many_to_many_field_annotation.model() ) );
											    
											    method_str.append("\t}\n");
											    
											    method_str.append("}");
											    
											    if (generate_code) {
											    	out.writeBytes(method_str.toString() );
											    }
											    
											} catch (java.io.IOException e) {
												
											    System.err.println(e);
											    
											} finally{
												
											    if (out != null) {
											    	out.close();
											    }
											}
										}
									}
								}
								
								sql = sql.substring(0, sql.lastIndexOf(",") ) + "\n";
								
								if (table_annotation != null) {
									
									if (database_engine.trim().equalsIgnoreCase("mysql") ) {
									
										sql += String.format(
												
												") %s %s %s;\n\n",
												
												table_annotation.engine().trim().equals("") ? "" : "ENGINE=" + table_annotation.engine(),
														
												table_annotation.charset().trim().equals("") ? "" : "DEFAULT CHARSET=" + table_annotation.charset(),
														
												table_annotation.comment().trim().equals("") ? "" : "COMMENT '" + table_annotation.comment() + "'"
										);
										
									} else if (database_engine.trim().equalsIgnoreCase("postgresql") || database_engine.trim().equalsIgnoreCase("oracle") ) {
										
										sql += String.format(
												
												");\n\n%s",
														
												table_annotation.comment().trim().equals("") ? "" : String.format("COMMENT ON TABLE %s IS '%s';\n\n", table_name, table_annotation.comment() )
										);
										
										sql += postgresql_or_oracle_columns_comments;
										
									} else {
										
										sql += ");\n\n";
									}
									
								} else {
									
									sql += ");\n\n";
								}
								
							} catch (Exception e) {								
								e.printStackTrace();
							}
						}
						
						try {
							
							String sql_transaction = "";							
							
							if (database_engine.trim().equalsIgnoreCase("mysql") || database_engine.trim().equalsIgnoreCase("postgresql") ) {
								sql_transaction = "BEGIN;\n\n";
							}
							
							sql_transaction += sql;
							
							sql_transaction += sql_many_to_many_association;
							
							sql_transaction += sql_foreign_key;
							
							sql_transaction += sql_index;
							
							if (database_engine.trim().equalsIgnoreCase("oracle") ) {
								
								sql_transaction += sql_oracle_sequences;
								
							}
							
							sql_transaction += "COMMIT";
							
							if (database_engine.trim().equalsIgnoreCase("oracle") ) {
								
								sql_transaction = sql_transaction.toUpperCase();
							}
							
							//	Mostrando o SQL completo da criação da estrutura de banco de dados da aplicação.
							
							//	System.out.println(sql_transaction);
							
							Scanner scanner = new Scanner(sql_transaction);
							
							scanner.useDelimiter(";\n");
							
							//	Comando atual (current statement).
							String current_statement = "";
							
							System.out.println("");
							
							while (scanner.hasNext() ) {
								
								current_statement = scanner.next();
								
								//	Mostrando o cada sendo executada do SQL da transação.								
								System.out.println(current_statement + ";\n");
								
								stmt.execute(current_statement);
								
							}
							
							scanner.close();
							
							if (database_engine.trim().equalsIgnoreCase("oracle") ) {
								
								String oracle_triggers = "";
								
								for (Map.Entry<String, String> sql_oracle_auto_increment_trigger : sql_oracle_auto_increment_triggers.entrySet() ) {
									
								    String table = sql_oracle_auto_increment_trigger.getKey();
								    
								    String trigger = sql_oracle_auto_increment_trigger.getValue();
								    
								    String oracle_trigger = "" +
								    
								    		"CREATE OR REPLACE TRIGGER " + trigger + "\n" +
								    		"BEFORE INSERT ON " + table + " FOR EACH ROW\n" +
								    		"DECLARE\n" +
								    		"    MAX_ID NUMBER;\n" +
								    		"    CUR_SEQ NUMBER;\n" +
								    		"BEGIN\n" +
								    		"    IF :NEW.ID IS NULL THEN\n" +
								    		"        -- No ID passed, get one from the sequence\n" +
								    		"        SELECT seq_" + table + ".NEXTVAL INTO :NEW.ID FROM DUAL;\n" +
								    		"    ELSE\n" +
								    		"        -- ID was set via insert, so update the sequence\n" +
								    		"        SELECT GREATEST(NVL(MAX(ID), 0), :NEW.ID) INTO MAX_ID FROM " + table + ";\n" +
								    		"        SELECT seq_" + table + ".NEXTVAL INTO CUR_SEQ FROM DUAL;\n" +
								    		"        WHILE CUR_SEQ < MAX_ID\n" +
								    		"        LOOP\n" +
								    		"            SELECT seq_" + table + ".NEXTVAL INTO CUR_SEQ FROM DUAL;\n" +
								    		"        END LOOP;\n" +
								    		"    END IF;\n" +
								    		"END;\n";
								    		//	+ "/";								    
								    
								    oracle_triggers += oracle_trigger.toUpperCase() + "\n\n";
								    
								    oracle_triggers += String.format("ALTER TRIGGER %s ENABLE\n\n", trigger.toUpperCase() );
								    
								    stmt.executeUpdate(oracle_trigger);

								}
								
								System.out.println(oracle_triggers);
								
								if (!conn.getAutoCommit() ) {
									
									conn.commit();
									
								}
							} else if (database_engine.trim().equalsIgnoreCase("mysql") && mysql_version_number < 56) {
								
								if (mysql_datetime_triggers.size() > 0) {
								
									for (String trigger : mysql_datetime_triggers) {
										stmt.executeUpdate(trigger);
									}
										
									System.out.println(mysql_datetime_triggers.toString().replace("[", "").replace("]", "").replace(", ", "") );
										
									if (!conn.getAutoCommit() ) {
										conn.commit();
										
										System.out.println("COMMIT;");
									}
								}
							}
							
						} catch (SQLException e) {

							e.printStackTrace();
							
						} finally {
							
							try {
								
								stmt.close();
								
								conn.close();
								
							} catch (SQLException e) {
								
								e.printStackTrace();	
							}
						}
						
					} else {
						
						syncdb(app_dir_content.getAbsolutePath() );
					}
				}
			}
		}
	}
}
