package jedi.db.models;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jedi.db.ConnectionFactory;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.engine.JediORMEngine;

/**
 * Classe de objetos que realizam consultas no banco de dados.
 * Um objeto de modelo utiliza um Manager para realizar consultas no banco de dados.
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Manager {
	
	private Connection connection;	
	
	private String tableName;	
	
	public Class entity;
	
	
	public Manager() {
		this.connection = ConnectionFactory.getConnection();
	}
	
	public Manager(Connection connection) {
		this.connection = connection;
	}
	
	public Manager(Class entity) {
		this();
		this.entity = entity;
	}
	
	protected void finalize() {
		try {
			
			super.finalize();
			
			if (this.connection != null && this.connection.isValid(10) ) {
				this.connection.close();
			}
			
		} catch (Throwable e) {
			
			e.printStackTrace();
		}
	}
	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public <T extends Model> QuerySet<T> all() {
		return this.all(this.entity);
	}
	
	/**
	 *  Método que retorna todos os registros da tabela.
	 *  
	 * @author Thiago Alexandre Martins Monteiro
	 * @return
	 */	
	public <T extends Model> QuerySet<T> all(Class<T> model_class) {
	
		QuerySet<T> query_set = null;
	    
		// Verificando se existe uma conexão com o banco de dados.
		if (this.connection != null) {
			
			try {
				
				String sql = "SELECT * FROM";
				
				// Verificando se a classe de modelo da aplicação foi anotada como Table.
				Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
				
				if (table_annotation != null && !table_annotation.name().trim().equals("") ) {
					
					table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
				}
				
				sql = String.format("%s %s", sql, table_name);
				
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				if (result_set == null) {					
					return null;					
				}
				
				query_set = new QuerySet<T>();
				
				query_set.setEntity(this.entity);
				
				while (result_set.next() ) {
					
					Object obj = entity.newInstance();
					
					Field id = entity.getSuperclass().getDeclaredField("id");
				
					// Tratando o tipo de dado BigDecimal que é retornado pelo Oracle.
					// Nos MySQL e no PostgreSQL o valor retornado é do tipo Integer.
					
					if (this.connection.toString().startsWith("oracle") ) {
					
						id.set(obj, ( (java.math.BigDecimal) result_set.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1) ) ).intValue() );
						
					} else {
					
						id.set(obj, result_set.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1) ) );
						
					}
					
					for (Field field : entity.getDeclaredFields() ) {
						
						field.setAccessible(true);
						
						if (field.toString().substring(field.toString().lastIndexOf('.') + 1).equals("serialVersionUID") )
							continue;
						
						if (field.getName().equalsIgnoreCase("objects") )
							continue;
						
						
						// Abaixo é verificado se o atributo é anotado como ForeignKeyField ou ManyToManyField.
						ForeignKeyField foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
						
						ManyToManyField many_to_many_annotation = field.getAnnotation(ManyToManyField.class);
						
						Manager manager = null;
						
						if (many_to_many_annotation != null && !many_to_many_annotation.references().isEmpty() ) {
							
							Class associated_model_class = Class.forName(String.format("app.models.%s", many_to_many_annotation.model() ) );
							
							manager = new Manager(associated_model_class);
							
							QuerySet query_set_associated_models = manager.raw(
									
								String.format(
									
									"SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
									
									many_to_many_annotation.references().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									many_to_many_annotation.model().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									table_name,
									
									many_to_many_annotation.references().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									obj.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									( (Model) obj).getId()
								),
								
								associated_model_class
							);
							
							field.set(obj, query_set_associated_models);
							
						} else if (foreign_key_annotation != null && !foreign_key_annotation.references().isEmpty() ) {
							
							// Caso seja recupera a classe do atributo.
							Class associated_model_class = Class.forName(field.getType().getName() );
							
							// Instanciando um model manager.
							manager = new Manager(associated_model_class);
							
							// Chamando o método esse método (get) recursivamente.
							Model associated_model = manager.get(
								"id", 
								result_set.getObject(
									String.format(
										"%s_id", field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() 
									) 
								) 
							);
							
							//	Referenciando o modelo associado por foreign key.
							field.set(obj, associated_model);
							
						} else {
							
							// Configurando campos que não são instancias de Model.
							if ( (field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer") ) && this.connection.toString().startsWith("oracle") ) {
								
								if (result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) == null) {
									
									field.set(obj, 0);
									
								} else {
									
									field.set(obj, ( (java.math.BigDecimal) result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ).intValue() );									
								}
									
							} else {
							
								field.set(obj, result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) );
							}
						}
						
						manager = null;
					}
					
					T model = (T) obj;
					
					if (model != null) {
					
						model.is_persisted(true);
					}
					
					query_set.add(model);
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return (QuerySet<T>) query_set;
	}	
	
	
	public <T extends Model> QuerySet<T> filter(String ...fields) {
		return (QuerySet<T>) this.filter(this.entity, fields);
	}
	
	
	/**
	 * @author Thiago Alexandre Martins Monteiro
	 * @param fields
	 * @return
	 */	
	public <T extends Model> QuerySet<T> filter(Class<T> model_class, String ... fields) {
		
		QuerySet<T> query_set = null;
		
		if (this.connection != null && fields != null && !fields.equals("") ) {
			
			try {
				
				String table_name = String.format("%ss", entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
				
				Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				if (table_annotation != null) {
					
					if (!table_annotation.name().trim().isEmpty() ) {
						
						table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
					
					}
					
				} else if (tableName != null && !tableName.isEmpty() ) {
					
					table_name = tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
				}
				
				String sql = String.format("SELECT * FROM %s WHERE", table_name);
				
				String where = "";
				
				// Percorrendo os pares campo=valor informados.
				for (int i = 0; i < fields.length; i++) {
					
					// Alterando o nome do campo para refletir o padrão de nome para colunas de tabelas.
					if (fields[i].contains("=") ) {
					
						fields[i] = String.format(
								
							"%s%s",
								
							fields[i].substring(0, fields[i].lastIndexOf("=") ).replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
								
							fields[i].substring(fields[i].lastIndexOf("=") )
						);
					}
					
					//	Acrescentando espaço em branco entre o nome do campo e o valor.
					fields[i] = fields[i].replace("=", " = ");
					
					// Substituindo % por \%
					fields[i] = fields[i].replace("%", "\\%");
					
					// Acrescentando espaço em branco entre valores separados por vírgula.
					fields[i] = fields[i].replace(",", ", ");
					
					// Verificando se o par atual possui um dos textos: __startswith, __contains ou __endswith.
					if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__endswith") > -1) {
						
						//	Criando a instrução LIKE do SQL.
						
						if (fields[i].indexOf("__startswith") > -1) {
							
							fields[i] = fields[i].replace("__startswith = ", " LIKE ");
							
							// Substituindo 'valor' por 'valor%'.
							fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'") );
							
							fields[i] = fields[i] + "%\'";
							
						} else if (fields[i].indexOf("__contains") > -1) {
							
							fields[i] = fields[i].replace("__contains = ", " LIKE ");
							
							// Substituindo 'valor' por '%valor%'.
							fields[i] = fields[i].replaceFirst("\'", "\'%");
							
							fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'") );
							
							fields[i] = fields[i] + "%\'";
							
						} else if (fields[i].indexOf("__endswith") > -1) {
							
							fields[i] = fields[i].replace("__endswith = ", " LIKE ");
							
							// Substituindo 'valor' por '%valor'.
							fields[i] = fields[i].replaceFirst("\'", "\'%");
							
						}
					}
					
					if (fields[i].indexOf("__in") > -1) {
						
						// Criando a instrução IN do SQL.
						fields[i] = fields[i].replace("__in = ", " IN ");
						
						// Substituindo os caracteres [] por ().
						fields[i] = fields[i].replace("[", "(");
						
						fields[i] = fields[i].replace("]", ")");
					}
					
					if (fields[i].indexOf("__range") > -1) {
						
						// Criando a instrução BETWEEN do SQL.
						fields[i] = fields[i].replace("__range = ", " BETWEEN ");
						
						// Substituindo o caracter [ ou ] por string vazia.
						fields[i] = fields[i].replace("[", "");
						
						fields[i] = fields[i].replace("]", "");
						
						// Substituindo o caracter , por AND.
						fields[i] = fields[i].replace(", ", " AND ");
					}
					
					if (fields[i].indexOf("__lt") > -1) {
						
						fields[i] = fields[i].replace("__lt = ", " < ");
					}
					
					if (fields[i].indexOf("__lte") > -1) {
						
						fields[i] = fields[i].replace("__lte = ", " <= ");
					}
					
					if (fields[i].indexOf("__gt") > -1) {
						
						fields[i] = fields[i].replace("__gt = ", " > ");
					}
					
					if (fields[i].indexOf("__gte") > -1) {
						
						fields[i] = fields[i].replace("__gte = ", " >= ");
					}
					
					if (fields[i].indexOf("__exact") > -1) {
						
						fields[i] = fields[i].replace("__exact = ", " = ");
					}
					
					if (fields[i].indexOf("__isnull") > -1) {
						
						String bool = fields[i].substring(fields[i].indexOf("=") + 1, fields[i].length() ).trim();
						
						if (bool.equalsIgnoreCase("true") ) {
							
							fields[i] = fields[i].replace("__isnull = ", " IS NULL ");
						}
						
						if (bool.equalsIgnoreCase("false") ) {
							
							fields[i] = fields[i].replace("__isnull = ", " IS NOT NULL ");
						}
						
						fields[i] = fields[i].replace(bool, "");
					}
					
					where += fields[i] + " AND ";
					
					where = where.replace(" AND OR AND", " OR");
					
					where = where.replace(" AND AND AND", " AND");
				}
				
				where = where.substring(0, where.lastIndexOf("AND") );
				
				sql = String.format("%s %s", sql, where);
				
				// System.out.println(sql);
				
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				query_set = new QuerySet<T>();
				
				query_set.setEntity(this.entity);
				
				while (result_set.next() ) {
					
					Object obj = entity.newInstance();
					
					if (result_set.getObject("id") != null) {
						
						Field id = entity.getSuperclass().getDeclaredField("id");
						
						if (this.connection.toString().startsWith("oracle") ) {
							
							id.set(obj, ( (java.math.BigDecimal) result_set.getObject(id.getName() ) ).intValue() );
							
						} else {
						
							id.set(obj, result_set.getObject(id.getName() ) );
							
						}
					}
					
					// Percorrendo os atributos declarados no modelo.
					for (Field field : entity.getDeclaredFields() ) {
						
						field.setAccessible(true);
						
						if (field.getName().equals("serialVersionUID") )
							continue;
						
						if (field.getName().equalsIgnoreCase("objects") )
							continue;
						
						// Abaixo é verificado se o atributo é anotado como ForeignKeyField ou ManyToManyField.
						
						ForeignKeyField foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
						
						ManyToManyField many_to_many_annotation = field.getAnnotation(ManyToManyField.class);
						
						Manager manager = null;
						
						if (many_to_many_annotation != null && !many_to_many_annotation.references().isEmpty() ) {
							
							Class associated_model_class = Class.forName(String.format("app.models.%s", many_to_many_annotation.model() ) );
							
							manager = new Manager(associated_model_class);
							
							List<List<HashMap<String, Object> > > record_set = null;
							
							record_set = manager.raw(
								
								String.format(
									
									"SELECT %s_id FROM %s_%s WHERE %s_id = %d", 
									
									many_to_many_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									table_name, 
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(), 
									
									this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									( (Model) obj).id()
								) 
							);
							
							String args = record_set.toString();
							
							args = args.replace("[", "");
							
							args = args.replace("{", "");
							
							args = args.replace("]", "");
							
							args = args.replace("}", "");
							
							args = args.replace("=", "");
							
							args = args.replace(", ", ",");
							
							args = args.replace(String.format("%s_id", many_to_many_annotation.model().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) , "");
							
							args = String.format("id__in=[%s]", args);
							
							QuerySet query_set_associated_models = manager.filter(args);
							
							field.set(obj, query_set_associated_models);
							
						} else if (foreign_key_annotation != null && !foreign_key_annotation.references().isEmpty() ) {
						
						    // Caso seja recupera a classe do atributo.
							Class associated_model_class = Class.forName(field.getType().getName() );
						
							// Instanciando um model manager.
							manager = new Manager(associated_model_class);
						
							// Chamando o método esse método (get) recursivamente.
							Model associated_model = manager.get("id", result_set.getObject(String.format("%s_id", field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ) );
						
							// Referenciando o modelo associado por foreign key.
							field.set(obj, associated_model);
							
						} else {
							
							// Configurando campos que não são instancias de Model.
							if ( (field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer") ) && this.connection.toString().startsWith("oracle") ) {
								
								if (result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) == null) {
									
									field.set(obj, 0);
									
								} else {
									
									field.set(obj, ( (java.math.BigDecimal) result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ).intValue() );
									
								}
									
							} else {
							
								field.set(obj, result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) );
							}
						}
						
						manager = null;
					}
					
					T model = (T) obj;
					
					if (model != null) {
					
						model.is_persisted(true);
					}
					
					query_set.add(model);
				}
				
				if (query_set != null) {
					query_set.is_persisted(true);
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return (QuerySet<T>) query_set;
	}
	
	
	public <T extends Model> T create(String ... list) {
		return (T) this.create(entity, list);
	}
	
	
	/**
	 * @author Thiago Alexandre Martins Monteiro
	 * @param list
	 * @return
	 */	
	public <T extends Model> T create(Class<T> modelClass, String ... list) {
		
		Object obj = null;
		
		// Verificando se existe uma conexão com o banco de dados e se foram passados argumentos para o método.
		if (this.connection != null && list != null && !list.equals("") ) {
			
			try {
				
				// Definindo o nome da tabela associada ao modelo.
				String table_name = this.entity.getName().replace(this.entity.getPackage().getName() + ".", "") + "s";
				
				table_name = table_name.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
				Table tb_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				if (tb_annotation != null) {
					
					if (!tb_annotation.name().trim().equals("") ) {
						
						table_name = tb_annotation.name().toLowerCase();
					}
					
				} else if (tableName != null && !tableName.equals("") ) {
					
					table_name = tableName;
				}
				
				String sql = String.format("INSERT INTO %s", table_name);
				
				String fields = "";
				
				String field = "";
				
				String values = "";
				
				String value = "";
				
				// Instanciando um objeto do modelo gerenciado por esse manager.
				obj = this.entity.newInstance();
				
				for (int i = 0; i < list.length; i++) {
					
					field = list[i].split("=")[0];
					
					value = list[i].split("=")[1];
					
					Field f = null;
					
					if (field.endsWith("_id") ) {
						
						f = this.entity.getDeclaredField(field.replace("_id", "") );
						
					} else {
						
						f = this.entity.getDeclaredField(field);
					}
					
					// Alterando o nome do campo para refletir o padrão de nome para colunas de tabelas.
					field = String.format("%s", field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
					
					// Tratar inserção para ForeignKeyField e ManyToManyField.
					ForeignKeyField foreign_key_annotation = f.getAnnotation(ForeignKeyField.class);
					
					// Permitindo o acesso aos atributos privados ou protegidos.
					f.setAccessible(true);
					
					if (f.getName().equals("serialVersionUID") )
						continue;
					
					if (f.getName().equalsIgnoreCase("objects") )
						continue;
					
					// Convertendo para os tipos apropriados
					if (value.matches("\\d+") ) {
						
						if (foreign_key_annotation != null) {
							
							Manager manager = new Manager(f.getType() );
							
							f.set(obj, manager.get("id", value) );
							
						} else {
							f.set(obj, Integer.parseInt(value) ); // Integer
						}
						
					} else if (value.matches("\\d+f") ) { // Float
						
						f.set(obj, Float.parseFloat(value) );
						
					} else if (value.matches("\\d+.d+") ) { // Double
						
						f.set(obj, Double.parseDouble(value) );
						
					} else { // String
						f.set(obj, list[i].split("=")[1]);
					}
					
					fields += field + ", ";
					
					values += value + ", ";
				}
				
				fields = fields.substring(0, fields.lastIndexOf(",") );
				
				values = values.substring(0, values.lastIndexOf(",") );
				
				sql = String.format("%s (%s) VALUES (%s)", sql, fields, values);
				
				// Mostrando a instrução SQL gerada.
				// System.out.println(sql);
				
				// Executando a instrução SQL.
				this.connection.prepareStatement(sql).execute();
				
				Field f = this.entity.getSuperclass().getDeclaredField("id");
				
				// Obtendo a chave primária do último registro inserido e atribuindo para o objeto.
				// Dessa forma a referência retornada será de alguma utilidade.
				f.set(obj, this.last_inserted_id() );
				
				T model = (T) obj;
				
				if (model != null) {
				
					model.is_persisted(true);
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return (T) obj;
	}
	
	
	/**
	 * Método que retorna o número identificador (id) do último registro inserido.
	 * 
	 * @author Thiago Alexandre Martins Monteiro
	 * @return int
	 */	
	public int last_inserted_id() {
		
		int id = 0;
		
		if (this.connection != null) {
			
			try {
				
				String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ); 
				
				Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				if (table_annotation != null) {
					
					if (!table_annotation.name().trim().equals("") ) {
						
						table_name = table_annotation.name().toLowerCase();
					}
					
				} else if (tableName != null && !tableName.trim().equals("") ) {
					
					table_name = tableName;
				}
				
				String sql = "";
				
				Properties database_settings = new Properties();
				
				FileInputStream file_input_stream = new FileInputStream(JediORMEngine.app_db_config);
				
				database_settings.load(file_input_stream);
				
				String database_engine = database_settings.getProperty("database.engine");
				
				if (database_engine != null) {
					
					if (database_engine.trim().equalsIgnoreCase("mysql") || database_engine.trim().equalsIgnoreCase("postgresql") || database_engine.trim().equalsIgnoreCase("h2") ) {
						
						sql = String.format("SELECT id FROM %s ORDER BY id DESC LIMIT 1", table_name);
						
					} else if (database_engine.trim().equalsIgnoreCase("oracle") ) {
						
						sql = String.format("SELECT MAX(id) AS id FROM %s", table_name);
						
					} else {
						
					}
					
				} else {
					
					return id;
				}
				
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				while (result_set.next() ) {
					
					id = result_set.getInt("id");
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return id;
	}
	
	
	/**
	 * @author Thiago Alexandre Martins Monteiro
	 * @param PythonString conditions
	 * @return int
	 */	
	public int count(String ... conditions) {
		
		int rows = 0;
		
		// Verificando se existe conexão com o banco de dados.
		if (this.connection != null) {
			
			try {
				
				String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ); 
				
				Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				if (table_annotation != null) {
					
					if (!table_annotation.name().trim().equals("") ) {
						
						table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
						
					} else if (tableName != null && !tableName.trim().equals("") ) {
						
						table_name = tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
					}
				}
				
				String sql = String.format("SELECT COUNT(id) AS \"rows\" FROM %s", table_name);
				
				if (conditions != null && conditions.length > 0) {
					
					String where = "WHERE";
					
					for (int i = 0; i < conditions.length; i++) {
						
						if (!conditions[i].equals("") ) {
							
							// Alterando o nome do campo para refletir o padrão de nome para colunas de tabelas.
							if (conditions[i].contains("=") ) {
							
								conditions[i] = String.format(
										
									"%s%s",
										
									conditions[i].substring(0, conditions[i].lastIndexOf("=") ).replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
									conditions[i].substring(conditions[i].lastIndexOf("=") )
								);
							}
							
							// Acrescentando espaço em branco entre o nome do campo e o valor.
							conditions[i] = conditions[i].replace("=", " = ");
							
							// Substituindo % por \%
							conditions[i] = conditions[i].replace("%", "\\%");
							
							// Acrescentando espaço em branco entre valores separados por vírgula.
							conditions[i] = conditions[i].replace(",", ", ");
							
							// Verificando se o par atual possui um dos textos: __startswith, __contains ou __endswith.
							if (conditions[i].indexOf("__startswith") > -1 || conditions[i].indexOf("__contains") > -1 || conditions[i].indexOf("__endswith") > -1) {
								
								//Criando a instrução LIKE do SQL.
								
								if (conditions[i].indexOf("__startswith") > -1) {
									
									conditions[i] = conditions[i].replace("__startswith = ", " LIKE ");
									
									// Substituindo 'valor' por 'valor%'.
									conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'") );
							
									conditions[i] = conditions[i] + "%\'";
									
								} else if (conditions[i].indexOf("__contains") > -1) {
									
									conditions[i] = conditions[i].replace("__contains = ", " LIKE ");
									
									//Substituindo 'valor' por '%valor%'.
									conditions[i] = conditions[i].replaceFirst("\'", "\'%");
									
									conditions[i] = conditions[i].substring(0, conditions[i].lastIndexOf("\'") );
									
									conditions[i] = conditions[i] + "%\'";
									
								} else if (conditions[i].indexOf("__endswith") > -1) {
									
									conditions[i] = conditions[i].replace("__endswith = ", " LIKE ");
									
									// Substituindo 'valor' por '%valor'.
									conditions[i] = conditions[i].replaceFirst("\'", "\'%");
									
								}
							}
							
							if (conditions[i].indexOf("__in") > -1) {
								
								// Criando a instrução IN do SQL.
								conditions[i] = conditions[i].replace("__in = ", " IN ");
								
								// Substituindo os caracteres [] por ().
								conditions[i] = conditions[i].replace("[", "(");
								
								conditions[i] = conditions[i].replace("]", ")");
								
							} else
							
							if (conditions[i].indexOf("__range") > -1) {
								
								 // Criando a instrução BETWEEN do SQL.
								conditions[i] = conditions[i].replace("__range = ", " BETWEEN ");
								
								// Substituindo o caracter [ ou ] por string vazia.
								conditions[i] = conditions[i].replace("[", "");
								
								conditions[i] = conditions[i].replace("]", "");
								
								// Substituindo o caracter , por AND.
								conditions[i] = conditions[i].replace(", ", " AND ");
							}
							
							if (conditions[i].indexOf("__lt") > -1) {
								
								conditions[i] = conditions[i].replace("__lt = ", " < ");
							}
							
							if (conditions[i].indexOf("__lte") > -1) {
								conditions[i] = conditions[i].replace("__lte = ", " <= ");
							}
							
							if (conditions[i].indexOf("__gt") > -1) {
								
								conditions[i] = conditions[i].replace("__gt = ", " > ");
							}
							
							if (conditions[i].indexOf("__gte") > -1) {
								
								conditions[i] = conditions[i].replace("__gte = ", " >= ");
							}
							
							if (conditions[i].indexOf("__exact") > -1) {
								
								conditions[i] = conditions[i].replace("__exact = ", " = ");
							}
							
							where += " " + conditions[i] + " AND";
							
							where = where.replace(" AND OR AND", " OR");
							
							where = where.replace(" AND AND AND", " AND");
						}
					}
					
					if (where.indexOf(" AND") > -1) {
						
						where = where.substring(0, where.lastIndexOf("AND") );
						
						sql = String.format("%s %s", sql, where);
					}
				}
				
				// Mostrando a instrução SQL gerada.
				// System.out.println(sql);
				
				// Executando a instrução SQL.
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				while (result_set.next() ) {
					
					rows = result_set.getInt("rows");
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return rows;
	}
	
	public int count() {
		return count("");
	}
	
	
	public <T extends Model> QuerySet<T> exclude(String ... fields) {
		
		QuerySet<T> query_set = null;
		
		if (this.connection != null && fields != null && !fields.equals("") ) {
			
			try {
				
				String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
				
				Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
				
				if (table_annotation != null) {
					
					if (!table_annotation.name().trim().equals("") ) {
						
						table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
						
					} else if (tableName != null && !tableName.trim().equals("") ) {
						
						table_name = tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
					}
				}
				
				String sql = String.format("SELECT * FROM %s WHERE", table_name);
				
				String where = "";
				
				// Percorrendo os pares campo=valor informados.
				for (int i = 0; i < fields.length; i++) {
					
					// Alterando o nome do campo para refletir o padrão de nome para colunas de tabelas.
					if (fields[i].contains("=") ) {
					
						fields[i] = String.format(
								
							"%s%s",
								
							fields[i].substring(0, fields[i].lastIndexOf("=") ).replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
								
							fields[i].substring(fields[i].lastIndexOf("=") )
						);
					}
					
					// Acrescentando espaço em branco entre o nome do campo e o valor.
					fields[i] = fields[i].replace("=", " = ");
					
					// Substituindo % por \%
					fields[i] = fields[i].replace("%", "\\%");
					
					// Acrescentando espaço em branco entre valores separados por vírgula.
					fields[i] = fields[i].replace(",", ", ");
					
					// Verificando se o par atual possui um dos textos: __startswith, __contains ou __endswith.
					if (fields[i].indexOf("__startswith") > -1 || fields[i].indexOf("__contains") > -1 || fields[i].indexOf("__endswith") > -1) {
						
						// Criando a instrução LIKE do SQL.
						
						if (fields[i].indexOf("__startswith") > -1) {
							
							fields[i] = fields[i].replace("__startswith = ", " LIKE ");
							
							// Substituindo 'valor' por 'valor%'.
							fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'") );
							
							fields[i] = fields[i] + "%\'";
							
						} else if (fields[i].indexOf("__contains") > -1) {
							
							fields[i] = fields[i].replace("__contains = ", " LIKE ");
							
							// Substituindo 'valor' por '%valor%'.
							fields[i] = fields[i].replaceFirst("\'", "\'%");
							
							fields[i] = fields[i].substring(0, fields[i].lastIndexOf("\'") );
							
							fields[i] = fields[i] + "%\'";
							
						} else if (fields[i].indexOf("__endswith") > -1) {
							
							fields[i] = fields[i].replace("__endswith = ", " LIKE ");
							
							// Substituindo 'valor' por '%valor'.
							fields[i] = fields[i].replaceFirst("\'", "\'%");
						}
					}
					
					if (fields[i].indexOf("__in") > -1) {
						
						// Criando a instrução IN do SQL.
						fields[i] = fields[i].replace("__in = ", " IN ");
						
						// Substituindo os caracteres [] por ().
						fields[i] = fields[i].replace("[", "(");
						
						fields[i] = fields[i].replace("]", ")");
					}
					
					if (fields[i].indexOf("__range") > -1) {
						
						// Criando a instrução BETWEEN do SQL.
						fields[i] = fields[i].replace("__range = ", " BETWEEN ");
						
						// Substituindo o caracter [ ou ] por string vazia.
						fields[i] = fields[i].replace("[", "");
						
						fields[i] = fields[i].replace("]", "");
						
						// Substituindo o caracter , por AND.
						fields[i] = fields[i].replace(", ", " AND ");
					}
					
					if (fields[i].indexOf("__lt") > -1) {
						
						fields[i] = fields[i].replace("__lt = ", " < ");
					}
					
					if (fields[i].indexOf("__lte") > -1) {
						
						fields[i] = fields[i].replace("__lte = ", " <= ");
					}
					
					if (fields[i].indexOf("__gt") > -1) {
						
						fields[i] = fields[i].replace("__gt = ", " > ");
					}
					
					if (fields[i].indexOf("__gte") > -1) {
						
						fields[i] = fields[i].replace("__gte = ", " >= ");
					}
					
					if (fields[i].indexOf("__exact") > -1) {
						
						fields[i] = fields[i].replace("__exact = ", " = ");
					}
					
					where += fields[i] + " AND ";
				}
				
				where = where.substring(0, where.lastIndexOf("AND") );
				
				sql = String.format("%s NOT (%s)", sql, where);
				
				// Mostrando a instrução SQL gerada na saída padrão.
				// System.out.println(sql);
				
				// Executando a instrução SQL.
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				query_set = new QuerySet();
				
				query_set.setEntity(this.entity);
				
				while (result_set.next() ) {
					
					Object obj = entity.newInstance();
					
					if (result_set.getObject("id") != null) {
						
						Field id = entity.getSuperclass().getDeclaredField("id");
						
						if (this.connection.toString().startsWith("oracle") ) {
							
							id.set(obj, ( (java.math.BigDecimal) result_set.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1) ) ).intValue() );
							
						} else {
						
							id.set(obj, result_set.getObject(id.toString().substring(id.toString().lastIndexOf('.') + 1) ) );
							
						}
					}
					
					for (Field field : entity.getDeclaredFields() ) {
						
						field.setAccessible(true);
						
						if (field.getName().equals("serialVersionUID") ) 
							continue;
						
						if (field.getName().equals("objects") ) 
							continue;
						
						// Abaixo é verificado se o atributo é anotado como ForeignKeyField ou ManyToManyField.
						ForeignKeyField foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
						
						ManyToManyField many_to_many_annotation = field.getAnnotation(ManyToManyField.class);
						
						Manager manager = null;
						
						if (many_to_many_annotation != null && !many_to_many_annotation.references().isEmpty() ) {
							
							Class associated_model_class = Class.forName(String.format("app.models.%s", many_to_many_annotation.model() ) );
							
							manager = new Manager(associated_model_class);
							
							QuerySet query_set_associated_models = manager.raw(
									
								String.format(
									
									"SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									many_to_many_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									table_name,
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									obj.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									( (Model) obj).getId()
								),
								
								associated_model_class
							);
							
							field.set(obj, query_set_associated_models);
							
						} else if (foreign_key_annotation != null && !foreign_key_annotation.references().isEmpty() ) {
							
							// Caso seja recupera a classe do atributo.
							Class associated_model_class = Class.forName(field.getType().getName() );
							
							// Instanciando um model manager.
							manager = new Manager(associated_model_class);
							
							// Chamando o método esse método (get) recursivamente.
							Model associated_model = manager.get(String.format("id"), result_set.getObject(String.format("%s_id", field.getType().getSimpleName().toLowerCase() ) ) );
							
							// Referenciando o modelo associado por foreign key.
							field.set(obj, associated_model);
							
						} else {
							
							if ( (field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer") ) && this.connection.toString().startsWith("oracle") ) {
								
								if (result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) == null) {
									
									field.set(obj, 0);
									
								} else {
									
									field.set(obj, ( (java.math.BigDecimal) result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ).intValue() );
									
								}
									
							} else {
							
								field.set(obj, result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) );
							}
						}
					}
					
					T model = (T) obj;
					
					if (model != null) {
					
						model.is_persisted(true);
					}
					
					query_set.add(model);
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	
	public List<List<HashMap<String, Object> > > raw(String sql) {
		
		// Cria uma lista de lista de mapa, no Python seria uma lista de lista de dicionario.
		// A primeira lista representa o conjunto de registros.
		// A segunda lista representa um registro.
		// O mapa representa um campo do registro.
		
		List<List<HashMap<String, Object> > > record_set = null;
		
		if (this.connection != null && sql != null && !sql.trim().equals("") ) {
			
			try {
				
				ResultSet result_set = null;
				
				// DQL - Data Query Language (SELECT).
				if (sql.startsWith("select") || sql.startsWith("SELECT") ) {

					// Retornando um result set navegável.
					result_set = this.connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY).executeQuery();
					
					ResultSetMetaData table_metadata = null;
					
					if (result_set != null) {
						
						table_metadata = result_set.getMetaData();
					
						if (table_metadata != null) {
							
							// Deslocando o cursor até o último registro.
							// result_set.last();
								
							record_set = new ArrayList<List<HashMap<String, Object> > >();
							
							// O método getFetchSize() retorna o número de registros que o SGBD recupera por vez no banco de dados. Por padrão é 10. 
							// for (int i = 1; i < result_set.getFetchSize(); i++) {
							
							// Obtendo a quantidade de registros retornados pela consulta.
							// int rows = result_set.getRow();
							
							// Reposicionando o ponteiro de registros
							// result_set.beforeFirst();
							
							while (result_set.next() ) {
								
								List<HashMap<String, Object> > table_row = new ArrayList<HashMap<String, Object> >();
								
								HashMap<String, Object> table_column = new HashMap<String, Object>();
								
								for (int i = 1; i <= table_metadata.getColumnCount(); i++) {
									
									table_column.put(table_metadata.getColumnLabel(i), result_set.getObject(table_metadata.getColumnLabel(i) ) );
								}
								
								table_row.add(table_column);
								
								record_set.add(table_row);
								
							}
							
							result_set.close();
						}
					}
					
				} else {
					
					// DML - Data Manipulation Language (INSERT, UPDATE and DELETE).
					this.connection.prepareStatement(sql).executeUpdate();
					
					if (!this.connection.getAutoCommit() ) {						
						this.connection.commit();						
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
				
				try {
					
					if (!this.connection.getAutoCommit() ) {						
						this.connection.rollback();
					}					
					
				} catch (SQLException e1) {
					
					e1.printStackTrace();
				}
			}
		}
		
		return record_set;
	}
	

	public <T extends Model> QuerySet<T> raw(String sql, Class<T> model_class) {
		
		QuerySet<T> query_set = null;
			
		// Verificando existe conexão com o banco de dados.
		if (this.connection != null && sql != null && !sql.trim().equals("") ) {
			
			try {
				
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				query_set = new QuerySet();
				
				while (result_set.next() ) {
					
					// Criando uma instância da classe de modelo (model_class) informado.
					T obj = model_class.newInstance();
					
					if (result_set.getObject("id") != null) {
						
						Field id = model_class.getSuperclass().getDeclaredField("id");
						
						if (this.connection.toString().startsWith("oracle") ) {
							
							id.set(obj, ( (java.math.BigDecimal) result_set.getObject(id.getName() ) ).intValue() );
							
						} else {
						
							id.set(obj, result_set.getObject(id.getName() ) );
							
						}

					}
					
					if (obj != null) {					
						obj.is_persisted(true);
					}
	
					// Percorrendo os atributos da classe de modelo.
					for (Field field : model_class.getDeclaredFields() ) {
						
						// Configurando como acessíveis todos os atributos (evita problemas com private e etc).
						field.setAccessible(true);
						
						// Ignorando o atributo serialVersionUID durante a iteração.
						if (field.getName().equals("serialVersionUID") )
							continue;
						
						// Ignorando o atributo objects durante a iteração.
						if (field.getName().equalsIgnoreCase("objects") )
							continue;
						
						// Verificando se o atributo é anotado com a anotação ForeignKeyField.
						ForeignKeyField foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
						
						ManyToManyField many_to_many_annotation = field.getAnnotation(ManyToManyField.class);
						
						Table table_annotation = model_class.getAnnotation(Table.class);
						
						String table_name = String.format("%ss", model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
						
						if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
							
							table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
						}
						
						Manager manager = null;
						
						// O atributo está anotado como ForeignKeyField?
						if (foreign_key_annotation != null && !foreign_key_annotation.references().isEmpty() ) {
							
							// Caso seja recupera a classe do atributo.
							Class associated_model_class = Class.forName(field.getType().getName() );
							
							// Instanciando um model manager.
							manager = new Manager(associated_model_class);
							
							// Chamando o método esse método (get) recursivamente.
							Model associated_model = manager.get(String.format("id"), result_set.getObject(String.format("%s_id", field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ) );
							
							// Referenciando o modelo associado por foreign key.
							field.set(obj, associated_model);
							
						} else if (many_to_many_annotation != null && !many_to_many_annotation.references().isEmpty() ) {
							
							Class associated_model_class = Class.forName(String.format("app.models.%s", many_to_many_annotation.model() ) );
							
							manager = new Manager(associated_model_class);
							
							List<List<HashMap<String, Object> > > associated_models_record_set = null;
							
							// É um recordset.
							associated_models_record_set = manager.raw(
									
								String.format(
									"SELECT %s_id FROM %s_%s WHERE %s_id = %d",
									
									many_to_many_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									table_name,
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									obj.id()
								) 
							);
							
							if (associated_models_record_set != null) {
							
								String args = associated_models_record_set.toString().toLowerCase();
								
								args = args.replace("[", "");
								
								args = args.replace("{", "");
								
								args = args.replace("]", "");
								
								args = args.replace("}", "");
								
								args = args.replace("=", "");
								
								args = args.replace(", ", ",");
								
								args = args.replace(String.format("%s_id", many_to_many_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) , "");
								
								args = String.format("id__in=[%s]", args);
								
								QuerySet qs = manager.filter(args);
								
								field.set(obj, qs);
								
							} else {
								
								field.set(obj, null);
							}
							
						} else {
							
							// Configurando campos que não são instancias de Model.
							if ( (field.getType().getSimpleName().equals("int") || field.getType().getSimpleName().equals("Integer") ) && this.connection.toString().startsWith("oracle") ) {
								
								if (result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) == null) {
									
									field.set(obj, 0);
									
								} else {
									
									field.set(obj, ( (java.math.BigDecimal) result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ).intValue() );
									
								}
									
							} else {
							
								field.set(obj, result_set.getObject(field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) );
							}
						}
						
						manager = null;
					}
					
					T model = (T) obj;
					
					if (model != null) {
						
						model.is_persisted(true);
					}
					
					query_set.add(model);
				}
				
				result_set.close();
				
				query_set.setEntity(model_class);
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	// DEVE SER ALTERADO POIS PODE RETORNAR MAIS DE UM OBJETO.
	public <T extends Model> T get(String field, Object value, Class<T> model_class) {
		
		// Model obj = null;
		
		T obj = null;
		
		// 1 - Verificando a existência da conexão.
		// 2 - Verificando se o campo da tabela foi informado.
		// 3 - Verificando se o campo não é uma string vazia.
		
		// Verificando se existe conexão com o banco de dados e se um campo foi informado.
		if (this.connection != null && field != null && !field.trim().isEmpty() ) {
			
			try {
				
				field = field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
				String sql = "SELECT * FROM";
				
				String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ); 
				
				Table table_annotation = (Table) entity.getAnnotation(Table.class);
				
				if (table_annotation != null) {
					
					if (!table_annotation.name().trim().equals("") ) {
						
						table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
					}
					
				} else if (tableName != null && !tableName.trim().equals("") ) {
					
					table_name = tableName.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				}
				
				if (value != null) {
					
					sql = String.format("%s %s WHERE %s = '%s'", sql, table_name, field, value.toString() );
					
				} else {
					
					sql = String.format("%s %s WHERE %s IS NULL", sql, table_name, field);
				}
				
				// Caso o valor passado seja de um tipo de dados numérico retira a apóstofre.
				if (Integer.class.isInstance(value) || Float.class.isInstance(value) || Double.class.isInstance(value) ) {
					
					sql = sql.replaceAll("\'", "");
				}
				
				ResultSet result_set = this.connection.prepareStatement(sql).executeQuery();
				
				while (result_set.next() ) {
					
					// obj = (Model) entity.newInstance();
					
					obj = (T) entity.newInstance();
					
					if (result_set.getObject("id") != null) {
						
						Field id = entity.getSuperclass().getDeclaredField("id");
						
						// Tratando o tipo de dado BigDecimal que é retornado pelo Oracle.
						// Nos MySQL e no PostgreSQL o valor retornado é do tipo Integer.
						
						if (this.connection.toString().startsWith("oracle") ) {
						
							id.set(obj, ( (java.math.BigDecimal) result_set.getObject(id.getName() ) ).intValue() );
							
						} else {						
							id.set(obj, result_set.getObject(id.getName() ) );							
						}
						
					}

					for (Field f : entity.getDeclaredFields() ) {
						
						f.setAccessible(true);
						
						if (f.getName().equals("serialVersionUID") )
							continue;
						
						if (f.getName().equalsIgnoreCase("objects") )
							continue;
						
						// Verificando se o atributo é anotado com a anotação ForeignKeyField.
						ForeignKeyField foreign_key_annotation = f.getAnnotation(ForeignKeyField.class);
						
						ManyToManyField many_to_many_annotation = f.getAnnotation(ManyToManyField.class);
						
						Manager manager = null;
						
						if (many_to_many_annotation != null && !many_to_many_annotation.references().trim().isEmpty() ) {
							
							Class associated_model_class = Class.forName(String.format("app.models.%s", many_to_many_annotation.model() ) );
							
							manager = new Manager(associated_model_class);
							
							QuerySet associated_models_query_set = manager.raw(
									
								String.format(
										
									"SELECT * FROM %s WHERE id IN (SELECT %s_id FROM %s_%s WHERE %s_id = %d)",
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									many_to_many_annotation.model().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									table_name,
									
									many_to_many_annotation.references().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									obj.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
									
									obj.getId()
								),
								
								associated_model_class
							);
							
							// Configurando o campo (atributo) com a referência para o queryset criado anteriormente.
							f.set(obj, associated_models_query_set);
							
						} else if (foreign_key_annotation != null && !foreign_key_annotation.references().trim().isEmpty() ) {
							
							// Caso seja recupera a classe do atributo.
							Class associated_model_class = Class.forName(f.getType().getName() );
							
							// Instanciando um model manager.
							manager = new Manager(associated_model_class);
							
							// Chamando o método esse método (get) recursivamente.
							Model associated_model = manager.get(String.format("id"), result_set.getObject(String.format("%s_id", f.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ) );
							
							// Atributo (campo) referenciando o modelo anotado como ForeignKeyField.
							f.set(obj, associated_model);
							
						} else {
							
							// Configurando campos que não são instancias de Model.
							if ( (f.getType().getSimpleName().equals("int") || f.getType().getSimpleName().equals("Integer") ) && this.connection.toString().startsWith("oracle") ) {
								
								if (result_set.getObject(f.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) == null) {
									
									f.set(obj, 0);
									
								} else {									
									f.set(obj, ( (java.math.BigDecimal) result_set.getObject(f.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) ).intValue() );									
								}
									
							} else {							
								f.set(obj, result_set.getObject(f.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ) );
							}
						}
						
						manager = null;
					}
				}
				
				if (obj != null) {
				
					obj.is_persisted(true);
				}
				
				result_set.close();
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return (T) obj;
	}
	
	public <T extends Model> T get(String field, Object value) {
		return (T) this.get(field, value, this.entity);
	}
	
	public <T extends Model> T latest(String field, Class<T> model_class) {
		
		// Model model = null;
		
		T model = null;
		
		if (this.connection != null && field != null && !field.trim().isEmpty() ) {
			
			// Renomeando o atributo para ficar no mesmo padrão do nome da coluna na tabela associada ao modelo.
			field = field.replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
			
			Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
			
			String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
			
			if (table_annotation != null) {
				
				table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
			}
			
			String sql = String.format("SELECT * FROM %s ORDER BY %s DESC LIMIT 1", table_name, field);
			
			if (this.connection.toString().startsWith("oracle") ) {
				
				sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s DESC", table_name, field);
			}
			
			QuerySet query_set = this.raw(sql, entity);
			
			if (query_set != null) {
				
				// model = (Model) query_set.get(0);
				
				model = (T) query_set.get(0);
				
				if (model != null) {
				
					model.is_persisted(true);
				}
			}
		}
		
		return model;
	}
	
	public <T extends Model> T latest(String field) {
		return (T) latest(field, entity);
	}
	
	public <T extends Model> T latest() {
		return (T) latest("id", entity);
	}
	
	public <T extends Model> T earliest(String field, Class<T> model_class) {
		
		// Model model = null;
		
		T model = null;
		
		if (this.connection != null && field != null && !field.trim().isEmpty() ) {
			
			Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
			
			String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
			
			if (table_annotation != null) {
				
				table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
			}
			
			String sql = String.format("SELECT * FROM %s ORDER BY %s ASC LIMIT 1", table_name, field);
			
			if (this.connection.toString().startsWith("oracle") ) {
				
				sql = String.format("SELECT * FROM %s WHERE ROWNUM < 2 ORDER BY %s ASC", table_name, field);
			}
			
			QuerySet query_set = this.raw(sql, entity);
			
			if (query_set != null) {
				
				// model = (Model) query_set.get(0);
				
				model = (T) query_set.get(0);
				
				if (model != null) {
				
					model.is_persisted(true);
				}
			}
		}
		
		// return (T) model;
		
		return model;
	}
	
	public <T extends Model> T earliest(String field) {
		return (T) earliest(field, entity);
	}
	
	public <T extends Model> T earliest() {
		return (T) earliest("id", entity);
	}
	
	public <T extends Model> QuerySet<T> get_set(Class<T> associated_model_class, int obj_id) {
		
		QuerySet query_set = null;
		
		if (associated_model_class != null && associated_model_class.getSuperclass().getName().equals("jedi.db.models.Model") ) {
			
			String sql = "";
			
			Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
			
			Table table_annotation_associated_model = (Table) associated_model_class.getAnnotation(Table.class);
			
			String table_name = String.format("%ss", this.entity.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
			
			String table_name_associated_model = String.format("%ss", associated_model_class.getSimpleName().toLowerCase() );
			
			if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
				
				table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
			}
			
			if (table_annotation_associated_model != null && !table_annotation_associated_model.name().trim().isEmpty() ) {
				
				table_name_associated_model = table_annotation_associated_model.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
			}
			
			// SELECT livros.* FROM livros, livros_autores WHERE livros.id = livros_autores.livro_id AND livros_autores.autor_id = 1;
			
			ForeignKeyField foreign_key_annotation = null;
			
			
			for (Field field : this.entity.getDeclaredFields() ) {
				
				foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
				
				if (foreign_key_annotation != null && foreign_key_annotation.model().equals(associated_model_class.getSimpleName() ) ) {
					
					query_set = this.filter(String.format("%s_id=%d", associated_model_class.getSimpleName().toLowerCase(), obj_id) );
				}
			}
			
			if (query_set == null ) {
				
				sql = String.format(
						
					"SELECT %s.* FROM %s, %s_%s WHERE %s.id = %s_%s.%s_id AND %s_%s.%s_id = %d",
						
					table_name,
						
					table_name,
						
					table_name,
						
					table_name_associated_model,
						
					table_name,
						
					table_name,
						
					table_name_associated_model,
						
					this.entity.getSimpleName().toLowerCase().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
						
					table_name,
						
					table_name_associated_model,
					
					associated_model_class.getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
						
					obj_id
				);
				
				query_set = this.raw(sql, this.entity);
				
				// query_set.setEntity(this.entity);
			}
		}
		
		return query_set;
	}
	
	
	/**
	 * Método que insere no banco de dados a lista de objetos fornecida de uma maneira eficiente.
	 *  
	 * @param models
	 */

/*
	public void bulk_create(List<Model> objects) {
		
//		Esse deve ser executado como um SQL batch.
		
//		Caso haja conexão com o banco de dados e exista uma lista não vazia então
		
		if (connection != null && objects != null && !objects.isEmpty() ) {
			
			try {
				
				for (Model object : objects) {
					
					object.save();
				}
				
			} catch(Exception e) {
				
				e.printStackTrace();
			}
		}
	}
*/
	
/*	
	public <T extends Model> QuerySet<T> get_or_create() {
		
		QuerySet<T> query_set = null;
		
		return query_set;
	}
*/
	
}