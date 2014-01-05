package jedi.db.models;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jedi.db.ConnectionFactory;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;

/**
 * Model é a classe de objetos responsável por encapsular dados e operações
 * de um determinado negócio.
 * 
 * Na arquitetura MVC é a camada Model.
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 */

@SuppressWarnings({"rawtypes", "unused", "unchecked", "deprecation"})
public class Model implements Comparable<Model>, Serializable {
	
	//	Atributos	
	private static final long serialVersionUID = 3655866130678459258L;
	
	/* Número identificador do modelo. */
	protected int id;
	
	/* Indica se o modelo já foi persistido. */
	protected transient boolean is_persisted;
	
	/* Conexão com o banco de dados através do qual o modelo será persistido. */
	private transient Connection connection;
	
	/* Nome da tabela para qual o modelo será mapeado. */
	private static String _table_name;
	
	/* Indica se a conexão com o banco de dados deverá ser fechada automaticamente. */
	private boolean auto_close_connection = true;
	
	//	Construtores	
	public Model() {
		
	}	
	
	public Model(Connection connection) {
		this.connection = connection;
	}	
	
	//	Destrutor
	protected void finalize() {
		
		// Mostrar uma mensagem caso o modo DEBUG esteja habilitado.		
		try {
			
			super.finalize();
			
			// Verificando se a conexão existe e se ela não está fechada.
			// OBS: Dependendo da situação um SGBD pode finalizar uma conexão existente.			
			if (this.connection != null && !this.connection.isValid(10) ) {
			
				this.connection.close();
			}
			
		} catch (Throwable e) {
			
			e.printStackTrace();
		}
	}	
	
	//	Getters	
	public Connection getConnection() {		
		return connection;
	}	
	
	public Connection connection() {		
		return connection;
	}	
	
	public String table_name() {		
		return _table_name;
	}	
	
	public int getId() {		
		return id;
	}	
	
	public int id() {		
		return id;
	}	
	
	public boolean getAutoCloseConnection() {		
		return auto_close_connection;
	}	
	
	public boolean auto_close_connection() {		
		return auto_close_connection;
	}	
	
	public boolean isPersisted() {		
		return is_persisted;
	}	
	
	public boolean is_persisted() {		
		return is_persisted;
	}	
	
	public Object get(String field) {
		
		Object object = null;
		
		if (field != null && !field.trim().isEmpty() ) {
			
			try {
				
				Field f = null;
				
				if (field.trim().equalsIgnoreCase("id") ) {
					
					f = this.getClass().getSuperclass().getDeclaredField(field);
					
				} else {
				
					f = this.getClass().getDeclaredField(field);
				}
				
				f.setAccessible(true);
				
				object = f.get(this);
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return object;
	}
	
	
	//	Setters	
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	
	public Model connection(Connection connection) {	
		this.connection = connection;
		return this;
	}
	
	
	public void setTableName(String table_name) {
		_table_name = table_name.toLowerCase();
	}
	
	
	public Model table_name(String table_name) {
		_table_name = table_name.toLowerCase();
		
		return this;
	}
	
		
	public void setId(int id) {
		this.id = id;
	}
	
	
	public Model id(int id) {
		this.id = id;
		
		return this;
	}
	
	
	public void setAutoCloseConnection(boolean autoCloseConnection) {
		this.auto_close_connection = autoCloseConnection;
	}
	
	
	public Model auto_close_connection(boolean auto_close_connection) {
		this.auto_close_connection = auto_close_connection;
		
		return this;
	}
	
	
	public void isPersited(boolean isPersisted) {
		this.is_persisted = isPersisted;
	}
	
	
	public Model is_persisted(boolean was_persisted) {
		this.is_persisted = was_persisted;
		
		return this;
	}
	
	
	public Model set(String field, Object value) {
		
		if (field != null && !field.trim().isEmpty() ) {
			
			try {
				
				Field f = this.getClass().getDeclaredField(field);
				
				f.setAccessible(true);
				
				f.set(this, value);
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return this;
	}
	
	
	/**
	 * Método que insere o modelo invocador na tabela apropriada no banco de dados.
	 *  
	 * @author - Thiago Alexandre Martins Monteiro  
	 * @param - nenhum 
	 * @return - nenhum 
	 * @throws - java.lang.Exception
	 */
	
	public void insert() {

		//	Verificando se há conexão com o banco de dados.		
		if (this.connection == null) {		
			this.connection = ConnectionFactory.getConnection();	
		}
			
		try {
			
			String sql = "INSERT INTO";
			
			String fields = "";
			
			String values = "";
			
			String many_to_many_sql_formatter = "INSERT INTO %s_%s (%s_id, %s_id) VALUES (%d,";
			
			List<String> many_to_many_sqls =  new ArrayList<String>();
			
			String table_name = String.format("%ss", this.getClass().getSimpleName().toLowerCase() );
			
			Table table_annotation = (Table) this.getClass().getAnnotation(Table.class);
			
			ManyToManyField many_to_many_annotation = null;
							
			Manager associated_model_manager = null;
			
			if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
				
				table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
			} else if (_table_name != null && !_table_name.trim().equals("") ) {
				
				table_name = _table_name.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
			}
			
			for (Field field : this.getClass().getDeclaredFields() ) {
				
				field.setAccessible(true);
				
				if (field.getName().equals("serialVersionUID") )
					continue;
				
				if (field.getName().equals("objects") )
					continue;
				
				ForeignKeyField foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
				
				if (field.getType().getSuperclass() != null && field.getType().getSuperclass().getSimpleName().equals("Model") ) {	 
					
					if (foreign_key_annotation != null && !foreign_key_annotation.references().trim().isEmpty() ) {
						
						fields += String.format("%s_id, ", field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );						
					}
					
				} else if (field.getType().getName().equals("java.util.List") || field.getType().getName().equals("jedi.db.models.QuerySet") ) {
					
					//	Não cria o field para esse.
					
				} else {
					
					fields += String.format("%s, ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
					
				}
				
				// Tratando values.				
				if (field.getType().toString().endsWith("String") ) {
					
					if (field.get(this) != null) {
						
						//	Substituindo ' por \' para evitar erro de sintaxe no SQL.						
						values += String.format("'%s', ", ( (String) field.get(this) ).replaceAll("'", "\\\\'") );
						
					} else {
						
						CharField char_field_annotation = field.getAnnotation(CharField.class);
						
						if (char_field_annotation != null) {
							
							if (char_field_annotation.default_value().trim().equals("\\0") ) {
							
								fields = fields.replace(String.format("%s, ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ), "");
								
							} else {									
								values += String.format("'%s', ", char_field_annotation.default_value().replaceAll("'", "\\\\'") );
							}
							
						} else {
							values += String.format("'', ", field.get(this) );
						}
					}
					
				} else if (field.getType().toString().endsWith("Date") || field.getType().toString().endsWith("PyDate") ) {
					
					Date date = (Date) field.get(this);
										
					if (date != null) {
						
						values += String.format(
								
							"'%d-%02d-%02d %02d:%02d:%02d', ", 
							
							date.getYear() + 1900, 
							
							date.getMonth() + 1, 
							
							date.getDate(),
							
							date.getHours(), 
							
							date.getMinutes(), 
							
							date.getSeconds() 
						);
						
					} else {
						
						DateField date_field_annotation = field.getAnnotation(DateField.class);
						
						if (date_field_annotation != null) {
							
							if (date_field_annotation.default_value().trim().equals("") ) {
							
								if (date_field_annotation.auto_now() ) {
									
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
									
									values += String.format("'%s', ", sdf.format(new Date() ) );
									
								} else {									
									fields = fields.replace(String.format("%s, ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() ), "");
								}
								
							} else {									
								values += String.format(date_field_annotation.default_value().trim().equalsIgnoreCase("NULL") ? "%s, " : "'%s', ", date_field_annotation.default_value().trim() );
							}
							
						} else {
							
							values += String.format("'', ", field.get(this) );
						}							
					}
					
				} else {
					
					if (foreign_key_annotation != null) {
						
						// 1 - Obtendo a referencia para o campo do modelo atual.
						// 2 - Recuperando o valor do id.
						// OBS: Tratar o retorno de toString() do modelo e muito perigoso uma vez que
						// o usuario pode ter a possibilidade de mudar a representacao do objeto e o codigo
						// que faz o tratamento do retorno e altamente dependente do formato do retorno.
						
						Object id = ( (Model) field.get(this) ).id;
						
						if (Integer.parseInt(id.toString() ) == 0) {
							
							( (Model) field.get(this) ).save();
							
							id = ( (Model) field.get(this ) ).id;
						}
						
						// String id = "";
						// Lancar excessao de campo nao existente informando qual o campo.
						// Se o atributo pais nao for configurado em Uf por exemplo, ocorrera NullPointerException.
						// id = field.get(this).toString();
						// id = id.substring(id.indexOf("id"), id.indexOf(",") );
						// id = id.replace(" ", "");
						// id = id.substring(id.indexOf("=") + 1, id.length() );
						// id = id.trim();
						
						values += String.format("%s, ", id);
						
					} else if (field.getType().getName().equals("java.util.List") || field.getType().getName().equals("jedi.db.models.QuerySet") ) {
						
						many_to_many_annotation = field.getAnnotation(ManyToManyField.class);
						
						associated_model_manager = new Manager(
							
							Class.forName(
								
								String.format(
									
									"app.models.%s", 
									
									many_to_many_annotation.model()
								) 
							) 
						);
						
						if (field.getType().getName().equals("java.util.List") ) {
							
							for (Object obj : (List) field.get(this) ) {
								
								( (Model) obj).insert();
								
								many_to_many_sqls.add(
										
									String.format(
										
										many_to_many_sql_formatter,
										
										table_name,
										
										many_to_many_annotation.references().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										many_to_many_annotation.model().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										this.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										( (Model) obj).id()
									)
								);									
							}								
						}
						
						if (field.getType().getName().equals("jedi.db.models.QuerySet") ) {
							
							for (Object obj : (QuerySet) field.get(this) ) {
									
								( (Model) obj).insert();
								
								many_to_many_sqls.add(
									
									String.format(
										
										many_to_many_sql_formatter,
										
										table_name,
										
										many_to_many_annotation.references().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										many_to_many_annotation.model().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										this.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase(),
										
										( (Model) obj).id()
									)
								);	
							}	
						}	
						
					} else {
						
						values += String.format("%s, ", field.get(this) );
					}
				}
			}
			
			fields = fields.substring(0, fields.lastIndexOf(',') );
			
			values = values.substring(0, values.lastIndexOf(',') );
			
			sql = String.format("%s %s (%s) VALUES (%s)", sql, table_name, fields, values);
			
			// Se o modo debug ou log estiver habilitado mostrar o SQL gerado na saída padrão.
			// System.out.println(sql);
						
			// this.connection.prepareStatement(sql).execute();
			
			this.connection.createStatement().executeUpdate(sql);
			
			if (!this.connection.getAutoCommit() ) {
				this.connection.commit();
			}
			
			// Verificar se o usuario optou por habilitar o recurso de auto-incremento.
			Manager manager = new Manager(this.getClass() );
			
			// Obtendo o id do último registro inserido e atribuindo ao id do objeto atual.
			this.id = manager.last_inserted_id();
			
			if (many_to_many_annotation != null) {
				
				for (String associated_model_sql : many_to_many_sqls) {
			
					associated_model_sql = String.format("%s %d) ", associated_model_sql, this.id() );
				
					// System.out.println(associated_model_sql);
					
					associated_model_manager.raw(associated_model_sql);
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
			
		} finally {
			
			if (this.auto_close_connection) {
			
				try {
					this.connection.close();
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Método que atualiza o modelo invocador na tabela apropriada no banco de dados.
	 *  
	 * @author - Thiago Alexandre Martins Monteiro  
	 * @param - nenhum 
	 * @return - nenhum 
	 * @throws - java.lang.Exception
	 */
	public void update(String ... args) {
		
		// Verificando se há conexão com o banco de dados.		
		if (this.connection == null) {
			this.connection = ConnectionFactory.getConnection();	
		}
			
		try {
			
			String sql = "UPDATE";
			
			// String table_name = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1).toLowerCase() + "s";
			
			String table_name = String.format("%ss", this.getClass().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
			
			Table table_annotation = (Table) this.getClass().getAnnotation(Table.class);
			
			if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
				
				table_name = table_annotation.name().trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
				
			} else if (_table_name != null && !_table_name.trim().equals("") ) {
				
				table_name = _table_name.trim().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase();
			}
			
			sql = String.format("%s %s SET", sql, table_name);
			
			String fields_and_values = "";
			
			if (args.length == 0) {
				
				for (Field field : this.getClass().getDeclaredFields() ) {
					
					field.setAccessible(true);
					
					if (field.getName().equals("serialVersionUID") )
						continue;
					
					if (field.getName().equals("objects") )
						continue;
					
					ForeignKeyField foreign_key_annotation = null;
					
					if (field.getType().getSuperclass() != null && field.getType().getSuperclass().getSimpleName().equals("Model") ) {	
						
						foreign_key_annotation = field.getAnnotation(ForeignKeyField.class);
						
						if (foreign_key_annotation != null && !foreign_key_annotation.references().isEmpty() ) {
							
							fields_and_values += String.format("%s_id = ", field.getType().getSimpleName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
						}
						
					} else {
						
						// fields_and_values += String.format("%s = ", field.toString().substring(field.toString().lastIndexOf('.') + 1).toLowerCase() );
						
						fields_and_values += String.format("%s = ", field.getName().replaceAll("([a-z0-9]+)([A-Z])", "$1_$2").toLowerCase() );
					}
											
					if (field.getType().toString().endsWith("String") ) {
						
						if (field.get(this) != null) {
							
							fields_and_values += String.format("'%s', ", ( (String) field.get(this) ).replaceAll("'", "\\\\'") );
							
						} else {
							fields_and_values += "'', ";
						}
						
					} else if (field.getType().toString().endsWith("Date") || field.getType().toString().endsWith("PyDate") ) {
						
						Date date = (Date) field.get(this);
						
						fields_and_values += String.format(
							
							"'%d-%02d-%02d %02d:%02d:%02d', ", 
							date.getYear() + 1900, 
							date.getMonth() + 1, 
							date.getDate(),
							date.getHours(), 
							date.getMinutes(), 
							date.getSeconds() 
						);
						
					} else {
						
						//	Verificando se o campo foi anotado como chave estrangeira (fk - foreign key).						
						if (foreign_key_annotation != null) {
							
							Object id = ( (Model) field.get(this) ).id;
							
							fields_and_values += String.format("%s, ", id);
							
						} else {
							fields_and_values += String.format("%s, ", field.get(this) );
						}
					}
				}
				
				fields_and_values = fields_and_values.substring(0, fields_and_values.lastIndexOf(',') );
				
			} else {
				
				if (args.length > 0) {
					
					Field field = null;					
					
					String fieldName = "";
					String fieldValue = "";
					
					for (int i = 0; i < args.length; i++) {
						
						fieldName = args[i].split("=")[0];
						
						if (fieldName.endsWith("_id") ) {
							fieldName = fieldName.replace("_id", "");
						}
						
						fieldValue = args[i].split("=")[1];
						//fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
						
						if (fieldValue.startsWith("'") && fieldValue.endsWith("'") ) {
							fieldValue = fieldValue.substring(1, fieldValue.length() - 1);
						}
						
						field = this.getClass().getDeclaredField(fieldName);
						
						field.setAccessible(true);
						
						if (field.getType() == String.class) {
							
							field.set(this, fieldValue);
							
						} else if (field.getType() == Integer.class) {
							
							field.set(this, Integer.parseInt(fieldValue) );
							
						} else if (field.getType() == Float.class) {
							
							field.set(this, Float.parseFloat(fieldValue) );
							
						} else if (field.getType() == Double.class) {
							
							field.set(this, Double.parseDouble(fieldValue) );
							
						} else if (field.getType() == Date.class) {
							
							field.set(this, Date.parse(fieldValue) );
							
						} else if (field.getType() == Boolean.class) {
							
							field.set(this, Boolean.parseBoolean(fieldValue) );
							
						} else if (field.getAnnotation(ForeignKeyField.class) != null) {
						
							if (field.get(this) != null) {
							
								( (Model) field.get(this) ).setId(Integer.parseInt(fieldValue) );
							}
							
						} else {
							
						}
						
						fields_and_values += args[i] + ", ";
					}
					
					fields_and_values = fields_and_values.substring(0, fields_and_values.lastIndexOf(",") );
				}
				
			}
			
			sql = String.format("%s %s WHERE id = %s", sql, fields_and_values, this.getClass().getSuperclass().getDeclaredField("id").get(this) );
			
			// System.out.println(sql);
			
			this.connection.prepareStatement(sql).execute();
			
			if (!this.connection.getAutoCommit() ) {
				this.connection.commit();
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
			
		} finally {
			
			if (auto_close_connection) {
			
				try {
					this.connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public void save() {

		// Depois se necessario tratar desativacao do recurso de auto-incremento.		
		try {
			
			// Inserção.
			if (!this.is_persisted) {				
				this.insert();
				//this.is_persisted(true);
			} else {				
				// Atualizando.				
				this.update();
			}
			
			this.is_persisted(true);
			
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
	
	public <T extends Model> T save(Class<T> model_class) {		
		this.save();		
		return this.as(model_class);
	}
	
	
	public void delete() {
		
		// Verificando se há conexão com o banco de dados.		
		if (this.connection == null) {		
			this.connection = ConnectionFactory.getConnection();	
		}
			
		try {
			
			String sql = "DELETE FROM";
			
			String table_name = String.format("%ss", this.getClass().getSimpleName().toLowerCase() );
			
			Table table_annotation = (Table) this.getClass().getAnnotation(Table.class);
			
			if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
				
				table_name = table_annotation.name().trim().toLowerCase();
				
			} else if (_table_name != null && !_table_name.trim().equals("") ) {
				
				table_name = _table_name;
			}
							
			sql = String.format("%s %s WHERE", sql, table_name);
			
			sql = String.format("%s id = %s", sql, this.getClass().getSuperclass().getDeclaredField("id").get(this) );
			
			this.connection.prepareStatement(sql).execute();
			
			if (!this.connection.getAutoCommit() ) {
				this.connection.commit();
			}
			
			// Informando que o modelo não se encontra persistido no banco de dados.			
			this.is_persisted(false);
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
			try {
				
				if (!this.connection.getAutoCommit() ) {
					this.connection.rollback();
				}
				
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
		} finally {
			
			if (this.auto_close_connection) {
			
				try {
					
					this.connection.close();
					
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	public int compareTo(Model model) {
		
		if (this.id < model.id) {
			return -1;
		}
		
		if (this.id > model.id) {
			return 1;
		}
		
		return 0;
	}
	
	public String toString() {
		
		String s = "";

		try {
			
			// s = String.format("<%s: id = %s, ", this.getClass().getSimpleName(), this.getClass().getSuperclass().getDeclaredField("id").get(this) );
			
			s = String.format("{%s: id = %s, ", this.getClass().getSimpleName(), this.getClass().getSuperclass().getDeclaredField("id").get(this) );
			
			Field[] fields = this.getClass().getDeclaredFields();
			
			for (Field f : fields) {
				
				f.setAccessible(true);
				
				if (f.getName().equals("serialVersionUID") )
					continue;
				
				if (f.getName().equalsIgnoreCase("objects") )
					continue;
				
				s += String.format("%s = %s, ", f.getName(), f.get(this) );
			}
			
			s = s.substring(0, s.lastIndexOf(",") );
			
			// s += ">";
			
			s += "}";
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return s;
	}
	
	@Override
	public boolean equals(Object o) {
		boolean r = false;
		
		if (this.id == ( (Model)o ).id) {
			r = true;
		}
		
		return r;
	}
	
	public String to_json(int i) {
		
		// i - nivel de identacao		
		String json_string = "";		
		String identation_to_class = "";		
		String identation_to_fields = "    ";		
		String identation_to_list_items = "        ";
		
		for (int j = 0; j < i; j++) {		
			identation_to_class += "    ";			
			identation_to_fields += "    ";			
			identation_to_list_items += "    ";		
		}
		
		try {
			
			json_string = String.format(
					
				"%s%s {\n%sid: %s,",
				
				identation_to_class,
				
				this.getClass().getSimpleName(),
				
				identation_to_fields,
				
				this.getClass().getSuperclass().getDeclaredField("id").get(this)
				
			);
			
			Field[] fields = this.getClass().getDeclaredFields();
			
			for (Field f : fields) {
				
				f.setAccessible(true);
				
				if (f.getName().equals("serialVersionUID") )
					continue;
				
				if (f.getName().equalsIgnoreCase("objects") )
					continue;
				
				if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model") ) {
					
					if (f.get(this) != null) {
					
						json_string += String.format("\n%s,", ( (Model) f.get(this) ).to_json(i + 1) );
					}
					
				} else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet") ) {
					
					String str_items = "";
					
					for (Object item : (List) f.get(this) ) {
						str_items += String.format("\n%s,", ( (Model) item ).to_json( (i + 2) ) );
					}
					
					if (str_items.lastIndexOf(",") >= 0) {
						str_items = str_items.substring(0, str_items.lastIndexOf(",") );
					}
					
					json_string += String.format("\n%s%s: [%s\n%s],", identation_to_fields, f.getName(), str_items, identation_to_fields);
					
				} else {
					json_string += String.format("\n%s%s: %s,", identation_to_fields, f.getName(), f.get(this) );
				}
			}
			
			if (json_string.lastIndexOf(",") >= 0) {
				json_string = json_string.substring(0, json_string.lastIndexOf(",") );
			}
			
			json_string += String.format("\n%s}", identation_to_class);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return json_string;
	}
	
	public String to_json() {
		return to_json(0);
	}
	
	public String to_xml(int i) {
		
		// i - nivel de identacao
		
		String xml_element = this.getClass().getSimpleName().toLowerCase();
		
		StringBuilder xml_string = new StringBuilder();
		StringBuilder xml_element_attributes = new StringBuilder();
		StringBuilder xml_child_elements = new StringBuilder();
		
		xml_element_attributes.append("");
		
		String xml_element_string = "";		
		String identation_to_element = "";		
		// String identation_to_attributes = "    ";	
		String identation_to_child_elements = "    ";
		
		for (int j = 0; j < i; j++) {		
			identation_to_element += "    ";	
			// identation_to_attributes += "    ";			
			identation_to_child_elements += "    ";		
		}
		
		try {
						
			xml_element_attributes.append(String.format("id=\"%d\"", this.getClass().getSuperclass().getDeclaredField("id").getInt(this) ) );

			Field[] fields = this.getClass().getDeclaredFields();
			
			for (Field f : fields) {
				
				f.setAccessible(true);
				
				if (f.getName().equals("serialVersionUID") )
					continue;
				
				if (f.getName().equalsIgnoreCase("objects") )
					continue;
				
				if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model") ) {
					
					xml_child_elements.append(String.format("\n%s\n", ( (Model) f.get(this) ).to_xml(i + 1) ) );
					
				} else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet") ) {
					
					String xml_child_open_tag = "";
					
					String xml_child_close_tag = "";
					
					Table table_annotation = null;
					
					if ( !( (List) f.get(this) ).isEmpty() ) {
						
						table_annotation = ( (List) f.get(this) ).get(0).getClass().getAnnotation(Table.class);
						
						if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
							
							xml_child_open_tag = String.format("\n%s<%s>", identation_to_child_elements, table_annotation.name().trim().toLowerCase() );
							
							xml_child_close_tag = String.format("\n%s</%s>", identation_to_child_elements, table_annotation.name().trim().toLowerCase() );
							
						} else {
							
							xml_child_open_tag = String.format("\n%s<%ss>", identation_to_child_elements, ( (List) f.get(this) ).get(0).getClass().getSimpleName().toLowerCase() );
							
							xml_child_close_tag = String.format("\n%s</%ss>", identation_to_child_elements, ( (List) f.get(this) ).get(0).getClass().getSimpleName().toLowerCase() );
							
						}
						
						xml_child_elements.append(xml_child_open_tag);
						
						for (Object item : (List) f.get(this) ) {						
							xml_child_elements.append(String.format("\n%s", ( (Model) item ).to_xml(i + 2) ) );							
						}
						
						xml_child_elements.append(xml_child_close_tag);
					}
					
				} else {
					xml_element_attributes.append(String.format(" %s=\"%s\"", f.getName(), f.get(this) ) );
				}
			}
						
			if (xml_child_elements.toString().isEmpty() ) {
				
				xml_string.append(String.format("%s<%s %s />", identation_to_element, xml_element, xml_element_attributes.toString() ) );
				
			} else {
				
				xml_string.append(
					
					String.format(
						
						"%s<%s %s>%s%s</%s>", 
						
						identation_to_element, 
						
						xml_element, 
						
						xml_element_attributes.toString(), 
						
						xml_child_elements, 
						
						identation_to_element, 
						
						xml_element
					) 
				);				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return xml_string.toString();
	}
	
	public String to_xml() {		
		StringBuilder xml_string = new StringBuilder();		
		xml_string.append(to_xml(0) );
		
		return xml_string.toString();
	}
	
	
	public String to_extense_xml(int i) {
		
		// A IMPLEMENTAR: Fazer o tratamento de caracteres especiais como < ou > no conteudo dos atributos
		// ao produzir o xml de retorno.
		
		// i - nivel de identacao
		
		String xml_element = this.getClass().getSimpleName().toLowerCase();
		
		
		StringBuilder xml_string = new StringBuilder();		
		StringBuilder xml_element_attributes = new StringBuilder();		
		StringBuilder xml_child_elements = new StringBuilder();		
		
		xml_element_attributes.append("");
		
		String xml_element_string = "";		
		String identation_to_element = "";		
		String identation_to_attributes = "    ";		
		String identation_to_child_elements = "    ";
		
		for (int j = 0; j < i; j++) {		
			identation_to_element += "    ";			
			identation_to_attributes += "    ";			
			identation_to_child_elements += "    ";		
		}
		
		try {
			
			xml_element_attributes.append(
				
				String.format(
					
					"\n%s<id>%d</id>\n", 
					
					identation_to_attributes, 
					
					this.getClass().getSuperclass().getDeclaredField("id").getInt(this) 
				) 
			);

			Field[] fields = this.getClass().getDeclaredFields();
			
			for (Field f : fields) {
				
				f.setAccessible(true);
				
				if (f.getName().equals("serialVersionUID") )
					continue;
				
				if (f.getName().equalsIgnoreCase("objects") )
					continue;
				
				if (f.getType().getSuperclass() != null && f.getType().getSuperclass().getName().equals("jedi.db.models.Model") ) {
										
					xml_child_elements.append(String.format("%s\n", ( (Model) f.get(this) ).to_xml(i + 1) ) );
					
				} else if (f.getType().getName().equals("java.util.List") || f.getType().getName().equals("jedi.db.models.QuerySet") ) {
					
					String xml_child_open_tag = "";
					
					String xml_child_close_tag = "";
					
					Table table_annotation = null;
					
					if ( !( (List) f.get(this) ).isEmpty() ) {
						
						table_annotation = ( (List) f.get(this) ).get(0).getClass().getAnnotation(Table.class);
						
						if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
														
							xml_child_open_tag = String.format("%s<%s>", identation_to_child_elements, table_annotation.name().trim().toLowerCase() );
							
							xml_child_close_tag = String.format("\n%s</%s>\n", identation_to_child_elements, table_annotation.name().trim().toLowerCase() );
							
						} else {
														
							xml_child_open_tag = String.format("%s<%ss>", identation_to_child_elements, ( (List) f.get(this) ).get(0).getClass().getSimpleName().toLowerCase() );
							
							xml_child_close_tag = String.format("\n%s</%ss>\n", identation_to_child_elements, ( (List) f.get(this) ).get(0).getClass().getSimpleName().toLowerCase() );
							
						}
						
						xml_child_elements.append(xml_child_open_tag);
						
						for (Object item : (List) f.get(this) ) {
						
							xml_child_elements.append(String.format("\n%s", ( (Model) item ).to_xml(i + 2) ) );
							
						}
						
						xml_child_elements.append(xml_child_close_tag);
					}
					
				} else {
					
					xml_element_attributes.append(String.format("%s<%s>%s</%s>\n", identation_to_attributes, f.getName(), f.get(this), f.getName() ) );
				}
			}
						
			if (xml_child_elements.toString().isEmpty() ) {
								
				xml_string.append(
					
					String.format(
					
						"%s<%s>%s%s</%s>", 
						
						identation_to_element, 
						
						xml_element, 
						
						xml_element_attributes.toString(),
						
						identation_to_element,
						
						xml_element
					) 
				);
				
			} else {
				
				xml_string.append(
					
					String.format(
					
						"%s<%s>%s%s%s</%s>", 
						
						identation_to_element, 
						
						xml_element, 
						
						xml_element_attributes.toString(), 
						
						xml_child_elements,
						
						identation_to_element, 
						
						xml_element
					) 
				);	
			}
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return xml_string.toString();
	}
	
	public String to_extense_xml() {
		
		StringBuilder xml_string = new StringBuilder();		
		xml_string.append(to_extense_xml(0) );
		
		return xml_string.toString();
	}
	
	public <T extends Model> T as(Class<T> c) {
		return (T) this;
	}
}