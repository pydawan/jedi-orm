package jedi.db.models;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jedi.db.ConnectionFactory;
import jedi.db.annotations.Table;
import jedi.types.Block;
import jedi.types.Function;


/**
 * Classe que representa uma lista de objetos retornados do banco de dados ou 
 * armazenados na memória. QuerySet possui uma API praticamente igual para listas 
 * de objetos persistentens ou transientes.
 *  
 * @author Thiago Alexandre Martins Monteiro
 * @param <T>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class QuerySet<T extends Model> extends ArrayList<T> {
	
    // Atributos	
	private static final long serialVersionUID = 1071905522184893192L;	
	private Class<T> entity = null;	
	private int offset = 0;	
	private transient boolean is_persited;	
	
	// Construtores	
	public QuerySet() {
		
	}	
	
	public QuerySet(Class<T> entity) {		
		this.entity = entity;
	}	
	
	public QuerySet(Collection<T> collection) {		
		super(collection);
	}
	
	
	// Getters	
	public Class<T> getEntity() {		
		return entity;
	}
	
	public Class<T> entity() {		
		return entity;
	}
	
	public boolean isPersited() {		
		return this.is_persited;
	}
	
	public boolean is_persited() {		
		return this.is_persited;
	}
	
	public boolean is_empty() {		
		return this.isEmpty();
	}
	
	
	// Setters
	public void setEntity(Class entity) {
		this.entity = entity;
	}
	
	public void entity(Class entity) {
		this.entity = entity;
	}
	
	
	public QuerySet<T> is_persisted(boolean is_persisted) {		
		this.is_persited = is_persisted;		
		return this;
	}
	
	public QuerySet orderBy(String field) {
		return this.order_by(field);
	}
	
	// orderBy
	public QuerySet order_by(String field) {
		
		QuerySet orderedList = null;
		
		if (field != null && !field.equals("") && !this.isEmpty() ) {
			
			Comparator comparator = null;
			
			try {
				
				// As variáveis abaixo tem modificador final para serem acessadas nas classes internas.
				final String fld = field.replace("-", "");
				
				final String fld2 = field;
				
				Field f = null;
				
				if (field.equals("id") || field.equals("-id") ) {
					
					f = this.entity.getSuperclass().getDeclaredField("id");
					
				} else {
					
					f = this.entity.getDeclaredField(fld);
				}
				
				f.setAccessible(true);
				
				if (f != null) {
					
					if (field.equals("id") ) {
						comparator = new Comparator<Model>() {
	
							public int compare(Model m1, Model m2) {
								if (m1.getId() < m2.getId() ) {
									return -1;
								}
								
								if (m1.getId() > m2.getId() ) {
									return 1;
								}
								
								return 0;
							}					
						};
					} else if (field.equals("-id") ) {
						comparator = new Comparator<Model>() {
	
							public int compare(Model m1, Model m2) {
								if (m1.getId() < m2.getId() ) {
									return 1;
								}
								
								if (m1.getId() > m2.getId() ) {
									return -1;
								}
								
								return 0;
							}					
						};				
					}
		
					// String.class.isInstance() serve para comparar o valor do campo.
					// f.getType().getName().equals("String") serve para comparar o tipo do campo.
					
					if (f.getType().getName().equals("java.lang.String") ) {
						comparator = new Comparator<Model>() {

							public int compare(Model m1, Model m2) {
								int result = 0;
								
								try {
									Field f1 = m1.getClass().getDeclaredField(fld);
									f1.setAccessible(true);
									
									Field f2 = m2.getClass().getDeclaredField(fld);
									f2.setAccessible(true);
									
									if (fld2.startsWith("-") ) {
										result = ( (String) f2.get(m2) ).compareTo( (String) f1.get(m1) );
									} else {
										result = ( (String) f1.get(m1) ).compareTo( (String) f2.get(m2) );
									} 
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								return result;
							}
						};
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
						
			Collections.sort(this, comparator);
			
			orderedList = this;
		}
		
		return orderedList;
	}
	
	public QuerySet limit(int ... params) {
		
		QuerySet objs = null;
				
		if (!this.isEmpty() && params != null) {
			
			objs = new QuerySet();
			
			objs.entity = this.entity;
			
			int start = 0;
			int end = 0;
		
			// Se for um argumento:
			// params[0] - limit

			if (params.length == 1) {
				
				if (this.offset > 0) {
					
					start = this.offset;
					
					// Reconfigurando a memória de deslocamento.
					this.offset = 0;
					
				} else {
					
					start = 0;
				}
				
				end = start + params[0];
			}
			
			// Se forem dois argumentos:
			// params[0] - offset
			// params[1] - limit

			if (params.length == 2) {				
				
				start = params[0];
				
				end = params[0] + params[1];
			
			}
			
			if (end > this.size() ) {
				
				end = this.size();
			}
			
			for (int i = start; i < end; i++) {
				
				objs.add(this.get(i) );
			}
		}
		
		return objs;
	}
	
	public QuerySet offset(int offset) {
		
		QuerySet records = null;
		
		this.offset = offset;
		
		// Verificando se a lista é vazia.
		if (!this.isEmpty() ) {
			
			records = new QuerySet();
			
			records.entity = this.entity;
			
			for (int i = offset; i < this.size(); i++) {
				
				records.add(this.get(i) );
			}
		}
		
		return records;
	}
	

	public QuerySet save() {

		Connection connection = ConnectionFactory.getConnection();
		
		if (connection != null) {
		
			if (!this.isEmpty() ) {
				
				boolean auto_close_connection;
			
				for (Object o : this) {
					
					Model model = (Model) o;
					
					model.connection(connection);
					
					auto_close_connection = model.auto_close_connection();
					
					model.auto_close_connection(false);
					
					model.save();
					
					model.auto_close_connection(auto_close_connection);
				}
				
				// Informando que a lista foi persistida.
				this.is_persisted(true);
			}
			
			try {
				
				connection.close();
				
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}
		
		return this;
	}
	
	
	public QuerySet delete() {
		
		Connection connection = ConnectionFactory.getConnection();
		
		if (connection != null) {
		
			if (!this.isEmpty() ) {
				
				Model model;
				
				for (Object o : this) {
					
					model = (Model) o;
					
					// Desabilitando o fechamento automático da conexão após cada operação no banco de dados.
					model.auto_close_connection(false);
					
					model.connection(connection);
					
					model.delete();
				}
				
				// Informando que a lista não se encontra persistida no banco de dados.
				this.is_persisted(false);
				
				this.removeAll(this);
			}
			
			try {
				
				connection.close();
				
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}
		
		return this;
	}
	
	
	// Tem que tratar update para poder atualizar atributos que armazenam referências.
	// Por exemplo: ufs.update("pais", paises.filter("nome", "Brasil") );
	
	public QuerySet update(String ... args) {
	
		Connection connection = ConnectionFactory.getConnection();
		
		if (connection != null) {
		
			if (!this.isEmpty() ) {
				
				boolean auto_close_connection;
				
				for (Object o : this) {
					
					Model model = (Model) o;
					
					auto_close_connection = model.auto_close_connection();
					
					model.auto_close_connection(false);
					
					model.connection(connection);
					
					model.update(args);
					
					model.auto_close_connection(auto_close_connection);
				}
			}
			
			try {
				
				connection.close();
				
			} catch (SQLException e) {

				e.printStackTrace();
			}
		}
		
		return this;
	}
	
	
	public int count() {
		return this.size();
	}
	
	public QuerySet all() {
		
		QuerySet query_set = new QuerySet();
		
		for (int i = 0; i < this.size(); i++) {
			
			query_set.add(this.get(i) );
		}
		
		return query_set;
	}
	
	private QuerySet<T> in(String query) {
		
		QuerySet<T> query_set = null;
		
		try {
			
			if (query != null && !query.trim().isEmpty() ) {
				
				query = query.replace("__in", "");
				
				// query = query.replace("', ", "',");
				
				query = query.replaceAll("',\\s+", "',");
				
				query = query.replaceAll("[\\[\\]]", "");
				
				String[] query_components = query.split("=");
				
				if (query_components != null && query_components.length > 0) {
					
					query_set = new QuerySet<T>();
					
					query_set.entity(this.entity);
					
					String field_name = query_components[0].trim();
					
					String[] field_values = query_components[1].split(",");
					
					Field field = null;
					
					if (field_name.equalsIgnoreCase("id") ) {
						
						field = this.entity.getSuperclass().getDeclaredField(field_name);
						
					} else {
					
						field = this.entity.getDeclaredField(field_name);
					}
					
					field.setAccessible(true);
					
					for (T model : this) {
						
						for (String field_value : field_values) {
							
							if (field.get(model) != null && field.get(model).toString().equals(field_value.replaceAll("'(.*)'", "$1") ) ) {
								
								query_set.add(model);
							}
						}
					}
				}
			}			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
				
		return query_set;
	}
	
	

	
	
	private QuerySet<T> range(String query) {
		
		QuerySet<T> query_set = null;
		
		try {
			
			if (query != null && !query.trim().isEmpty() ) {
				
				query = query.replace("__range", "");
				
				query = query.replace(", ", ",");
				
				query = query.replaceAll("['\\[\\]]", "");
				
				String[] query_components = query.split("=");
				
				if (query_components != null && query_components.length > 0) {
					
					query_set = new QuerySet<T>();
					
					query_set.entity(this.entity);
					
					String field_name = query_components[0];
					
					String[] field_values = query_components[1].split(",");
					
					Field field = null;
					
					if (field_name.trim().equalsIgnoreCase("id") ) {
						
						field = this.entity.getSuperclass().getDeclaredField(field_name);
						
					} else {
					
						field = this.entity.getDeclaredField(field_name);
					}
					
					field.setAccessible(true);
					
					for (T model : this) {
						
						for (int field_value = Integer.parseInt(field_values[0]); field_value <= Integer.parseInt(field_values[1]); field_value++) {
							
							if (field.get(model).equals(field_value) ) {
								
								query_set.add(model);
							}
						}
					}
				}
			}			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
				
		return query_set;
	}

	
	private QuerySet<T> filter_numeric_field(String query) {
	
		QuerySet<T> query_set = null;
		
		if (query != null && !query.trim().isEmpty() ) {
			
			query_set = new QuerySet();
			
			query = query.trim().toLowerCase();
			
			// (<|<=|==|!=|>=|>)
			String[] query_components = query.split("\\s+");
			
			String field_name = query_components[0].trim();
			
			String operator = query_components[1].trim();
			
			String field_value = query_components[2].trim();
			
			// System.out.printf("%s%s%s", field_name, operator, field_value);
			
			Field field = null;
			
			try {
				
				if (field_name.equals("id") ) {
					
					field = this.entity.getSuperclass().getDeclaredField(field_name);
					
				} else {
				
					field = this.entity.getDeclaredField(field_name);
				}
				
				field.setAccessible(true);
				
				for (T model : this) {
					
					if (operator.equals("<") ) {
						
						if (field.getDouble(model) < Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else if (operator.equals("<=") ) {
						
						if (field.getDouble(model) <= Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else if (operator.equals("=") ) {
						
						if (field.getDouble(model) == Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else if (operator.equals("!=") ) { 
						
						if (field.getDouble(model) != Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else if (operator.equals(">") ) {
						
						if (field.getDouble(model) > Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else if (operator.equals(">=") ) {
						
						if (field.getDouble(model) >= Double.parseDouble(field_value) ) {
							
							query_set.add(model);
						}
						
					} else {
						
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}			
		}
		
		return query_set;
	}
	
	
	private QuerySet<T> exact(String query) {
		
		QuerySet<T> query_set = null;
		
		if (query != null && !query.trim().isEmpty() ) {
			
			query_set = new QuerySet<T>();
			
			// Já são feitos o trim e o lowercase no método filter.
			// query = query.trim().toLowerCase();
						
			query = query.replace("__exact", "");
			
			String[] query_components = query.split("=");
			
			String field_name = query_components[0];
			
			String field_value = query_components[1];
			
			if (field_value.equalsIgnoreCase("null") ) {
				
				query_set.add(this.is_null(String.format("%s__isnull=true", field_name) ) );
				
			} else {
			
				query_set.add(this.in(String.format("%s__in=[%s]", field_name, field_value) ) );
			}
		}
		
		return query_set;
	}
	
	
	private QuerySet<T> is_null(String query) {
		
		QuerySet<T> query_set = null;
		
		if (query != null && !query.trim().isEmpty() ) {
			
			query_set = new QuerySet<T>();
			
			query = query.trim().toLowerCase();
			
			query = query.replace("__isnull", "");
			
			String[] query_components = query.split("=");
			
			String field_name = query_components[0];
			
			boolean is_null = Boolean.parseBoolean(query_components[1]); 
			
			Field field = null;
			
			try {
				if (field_name.equalsIgnoreCase("id") ) {
					
					field = this.entity.getSuperclass().getDeclaredField(field_name);
					
				} else {
				
					field = this.entity.getDeclaredField(field_name);
				}
				
				field.setAccessible(true);
				
				for (T model : this) {
					
					if (is_null) {
						
						if (field.get(model) == null) {
						
							query_set.add(model);
						}
						
					} else {
						
						if (field.get(model) != null) {
							
							query_set.add(model);
						}
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	
	public QuerySet<T> starts_with(String query) {
		
		QuerySet<T> query_set = null;
		
		if (query != null && !query.isEmpty() ) {
			
			query_set = new QuerySet<T>();
						
			query = query.replace("__startswith", "");
			
			String[] query_components = query.split("=");
			
			String field_name = query_components[0];
			
			String field_value = query_components[1];
			
			Field field = null;
			
			try {
				
				if (field_name.equals("id") ) {
					
					field = this.entity.getSuperclass().getDeclaredField(field_name);
					
				} else {
				
					field = this.entity.getDeclaredField(field_name);
				}
				
				field.setAccessible(true);
				
				String pattern = String.format("^%s.*$", field_value.replace("'", "") );
				
				for (T model : this) {
					
					if (field.get(model) != null && field.get(model).toString().matches(pattern) ) {
						
						query_set.add(model);
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	
	public QuerySet<T> ends_with(String query) {
		
		QuerySet<T> query_set = null;
		
		if (query != null && !query.isEmpty() ) {
			
			query_set = new QuerySet<T>();
						
			query = query.replace("__endswith", "");
			
			String[] query_components = query.split("=");
			
			String field_name = query_components[0];
			
			String field_value = query_components[1];
			
			Field field = null;
			
			try {
				
				if (field_name.equals("id") ) {
					
					field = this.entity.getSuperclass().getDeclaredField(field_name);
					
				} else {
				
					field = this.entity.getDeclaredField(field_name);
				}
				
				field.setAccessible(true);
				
				String pattern = String.format("^.*%s$", field_value.replace("'", "") );
				
				for (T model : this) {
					
					if (field.get(model) != null && field.get(model).toString().matches(pattern) ) {
						
						query_set.add(model);
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	
	public QuerySet<T> contains(String query) {
		
		QuerySet<T> query_set = null;
		
		if (query != null && !query.isEmpty() ) {
			
			query_set = new QuerySet<T>();
						
			query = query.replace("__contains", "");
			
			String[] query_components = query.split("=");
			
			String field_name = query_components[0];
			
			String field_value = query_components[1];
			
			Field field = null;
			
			try {
				
				if (field_name.equals("id") ) {
					
					field = this.entity.getSuperclass().getDeclaredField(field_name);
					
				} else {
				
					field = this.entity.getDeclaredField(field_name);
				}
				
				field.setAccessible(true);
				
				String pattern = String.format("^.*%s.*$", field_value.replace("'", "") );
				
				for (T model : this) {
					
					if (field.get(model) != null && field.get(model).toString().matches(pattern) ) {
						
						query_set.add(model);
					}
				}
				
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		return query_set;
	}
	
	
	public QuerySet<T> filter(String ... queries) {
		
		QuerySet<T> query_set = null;
		
		if (queries != null && !queries.toString().trim().isEmpty() ) {
			
			query_set = new QuerySet<T>();
			
			for (String query : queries) {
				
				query = query.trim();
				
				// Retirando o excesso de espaços ao redor do operador =
				query = query.replaceAll("\\s*=\\s*", "=");
				
				// System.out.println(query);
				
				// Tratamento para consulta field__in=['valor1', 'valor2']
				
				if (query.matches("^\\w+__in=\\[(\\d+|\\d+((,|, )\\d+)+)\\]$") ) {
					
					query = query.replaceAll("(\\d+)", "'$1'");
					
					query_set.add(this.in(query) );
					
				} else if (query.matches("^\\w+__in=\\[('[^']+'|'[^']+'((,|, )'[^']+')+)\\]$") ) {
					
					// System.out.println("CORRESPONDE A REGEX DO field__in=[]");
					
					query_set.add(this.in(query) );
					
				} else if (query.matches("^\\w+__range=\\[\\d+(,|, )\\d+\\]$") ) {
					
					// System.out.println("CORRESPONDE A REGEX field__range[]");
					
					query_set.add(this.range(query) );
					
				} else if (query.matches("^\\s*\\w+\\s+(<|<=|==|!=|>=|>)\\s+\\d+\\s*$") ) {
					
					// Regex para string id <= 10 and id > 50 or id == 100 ...
					
					// "^\\s*\\w+\\s+(<|<=|==|!=|>=|>)\\s+\\d+(\\s+(and|or|AND|OR)\\s+\\w+\\s+(<|<=|==|!=|>=|>)\\s+\\d+)*\\s*$"					
					
					// System.out.println("CORRESPONDE A REGEX PARA NÚMEROS");
					
					// id > 10 and id < 50
					
					// id >= 12 or id <= 50 and id > 100
					// id >= 12 or,id <= 50 and,id > 100
					
					// Retirando os espaços em brancos das extremidades da string de consulta.
					
					query_set.add(this.filter_numeric_field(query) );
					
				}  else if (query.matches("^(\\w+)__isnull\\s*=\\s*(true|false)\\s*$") ) {
					
					// query.matches("^(\\w+)__isnull\\s*=\\s*([tT][rR][uU][eE])\\s*$"
					
					query_set.add(this.is_null(query) );
					
				} else if (query.matches("^(\\w+)__exact\\s*=\\s*('[^']+'|\\d+|null)\\s*$") ) {
					
					query_set.add(this.exact(query) );
					
				} else if (query.matches("^(\\w+)__(lt|lte|gt|gte)\\s*=\\s*(\\d+)$") ) {
					
					// "^(\\w+)__(lt|lte|gt|gte|exact)\\s*=\\s*(\\d+)$"
					
					// query = query.replaceAll("__exact=", " = ");
					
					query = query.replace("__lt=", " < ");
					
					query = query.replace("__lte=", " <= ");
					
					query = query.replace("__gt=", " > ");
					
					query = query.replace("__gte=", " >= ");
					
					query_set = this.filter_numeric_field(query);
					
				} else if (query.matches("^(\\w+)__startswith\\s*=\\s*('[^']+'|\\d+)$") ) { 
					
					query_set = this.starts_with(query);
					
				} else if (query.matches("^(\\w+)__endswith\\s*=\\s*('[^']+'|\\d+)$") ) {
					
					query_set = this.ends_with(query);

				} else if (query.matches("^(\\w+)__contains\\s*=\\s*('[^']+'|\\d+)$") ) {
					
					query_set = this.contains(query);
					
				} else {
					
				}
			}
			
			query_set.entity(this.entity);
		}
		
		return query_set;
	}
	
	
	// Funciona como o filter negado.
	public QuerySet<T> exclude(String ... queries) {
		
		QuerySet<T> query_set = this.all();
			
		query_set.entity(this.entity);
		
		query_set = query_set.remove(query_set.filter(queries) );
		
		return query_set;
	}
	
	
	// O código desse método entrou em conflito com o método save no Manager.java
	// uma vez que ao inserir elementos na QuerySet eles tem seu id definido e o método save só insere
	// models com o id igual a 0.
	// Esse conflito foi solucionado através do atributo is_persisted.
	
	public boolean add(T model) {
		
		if (model != null && model.id == 0) {
			
			model.id(this.size() + 1);
		}
		
		return super.add(model);
	}
	
	
	public QuerySet<T> add(QuerySet<T> query_set) {
					
		if (query_set != null && !query_set.isEmpty() ) {
			
			query_set.entity(this.entity() );
			
			this.addAll(query_set);
		}
		
		return this;
	}
	
	
	public QuerySet<T> add(QuerySet<T> ... query_sets) {
		
		if (query_sets != null && query_sets.length > 0) {
			
			for (QuerySet<T> query_set : query_sets) {
				
				query_set.entity(this.entity() );
				
				this.add(query_set);
			}
		}
		
		return this;
	}
	
	
	public QuerySet<T> add(T ... models) {
		
		// Verificando se o array de modelos passada existe e não está vazia.
		if (models != null && models.length > 0) {
			
			// Percorrendo cada modelo do array.
			for (T model : models) {
				
				this.add(model);
			}
		}
		
		return this;
	}
	
	public QuerySet<T> add(List<T> models) {
		
		if (models != null && models.size() > 0) {
			
			for (T model : models) {
				this.add(model);
			}
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(QuerySet<T> query_set) {
		
		if (query_set != null && !query_set.isEmpty() ) {
			
			this.removeAll(query_set);
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(QuerySet<T> ... query_sets) {
		
		if (query_sets != null && query_sets.length > 0) {
			
			for (QuerySet<T> query_set : query_sets) {
				
				this.removeAll(query_set);
			}
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(String ... queries) {
	
		QuerySet<T> query_set = this.filter(queries);
		
		if (query_set != null && !query_set.isEmpty() ) {
			
			this.removeAll(query_set);
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(String query) {
		
		QuerySet<T> query_set = this.filter(query);
		
		if (query_set != null && !query_set.isEmpty() ) {
			
			this.removeAll(query_set);
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(T model) {
		
		if (!this.isEmpty() && model != null) {
			
			// Fazendo cast para Object para evitar StackOverFlowError.
			// Esse erro ocorre porque é feita uma chamada recursiva a esse método e ao fazer o cast o Java
			// chamada o método desejado.
			
			this.remove( (Object) model);
		}
		
		return this;
	}
	
	
	public QuerySet<T> remove(T ... models) {
		
		if (models != null && models.length > 0) {
			
			for (T model : models) {
				
				this.remove(model);
			}
		}
		
		return this;
	}
	
	
	public QuerySet<T> distinct() {
		
		QuerySet<T> query_set = null;
		
		if (!this.isEmpty() ) {
			
			// Eliminando elementos repetidos da coleção através de HashSet.
			
			query_set = new QuerySet(new HashSet<T>(this) );
			
		}
 		
		return query_set;
	}
	
	
	public T earliest() {
		
		T model = null;
		
		if (!this.isEmpty() ) {
			
			model = this.get(0);
		}
		
		return model;
	}
	
	
	public T latest() {
		
		T model = null;
		
		if (!this.isEmpty() ) {
			
			model = this.get(this.size() - 1);
		}
		
		return model;
	}
	
	
	public QuerySet<T> get(String field, Object value) {
		
		QuerySet<T> query_set = null;
		
		if (!this.isEmpty() ) {
			
			if (value instanceof String) {
				
				query_set = this.filter(String.format("%s__in=['%s']", field, value) );
				
			} else {
				
				query_set = this.filter(String.format("%s__in=[%s]", field, value) );
			}
		}
		
		return query_set;
	}
	
	
	// Tendo int como o tipo primitivo de value não ocorre mais o StackOverFlowError
	// ocasionado pela chamada recursiva gerada pelo polimorfismo.
	// Integeger -> Object.
	
	public T get(String id, int value) {
		
		T model = null;
		
		QuerySet<T> query_set = null;
		
		if (!this.isEmpty() ) {
			
			query_set = this.get("id", new Integer(value) );
			
			model = query_set != null && !query_set.isEmpty() ? query_set.get(0) : null;
		}
		
		return model;
	}
	
	
	public boolean exists() {
		
		if (!this.isEmpty() ) {
			
			return true;
		}
		
		return false;
	}
	
	public QuerySet reverse() {
		
		if (!this.isEmpty() ) {
			
			Collections.reverse(this);
			
			return this;
		}
		
		return null;
	}
	
	public String to_json() {
		
		String json_string = "[";
		
		if (!this.isEmpty() ) {
			json_string += "\n";
		}
		
		for (Model model : this) {
			json_string += String.format("%s,\n", model.to_json(1) );
		}
		
		if (!this.isEmpty() ) {
		
			json_string = json_string.substring(0, json_string.length() - 2);
			
			json_string += "\n";
		}
		
		json_string += "]";
		
		return json_string;
	}
	
	public String to_xml() {
		
		String xml_string = "";
		
		String xml_element_open_tag = "";
		
		String xml_element_close_tag = "";
		
		Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
		
		if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
			
			xml_element_open_tag += String.format("<%s>", table_annotation.name().trim().toLowerCase() );
			
			xml_element_close_tag += String.format("<%s>", table_annotation.name().trim().toLowerCase() );
			
		} else {
			
			xml_element_open_tag += String.format("<%ss>", this.entity.getSimpleName().toLowerCase() );
			
			xml_element_close_tag += String.format("</%ss>", this.entity.getSimpleName().toLowerCase() );
			
		}
		
		if (!this.isEmpty() ) {
			
			for (Model model : this) {
				xml_string += String.format("%s\n", model.to_xml(1) );
			}
			
			// xml_string = String.format(""<?xml version=\"1.0\"?>\n"%s\n%s%s", xml_element_open_tag, xml_string, xml_element_close_tag);

			xml_string = String.format("%s\n%s%s", xml_element_open_tag, xml_string, xml_element_close_tag);
			
		} else {
			
			xml_element_open_tag = xml_element_open_tag.replace(">", " />");
			
			xml_string = String.format("%s", xml_element_open_tag);
			
		}
		
		return xml_string;
	}
	
	public String to_extense_xml() {
		
		String xml_string = "";
		
		String xml_element_open_tag = "";
		
		String xml_element_close_tag = "";
		
		Table table_annotation = (Table) this.entity.getAnnotation(Table.class);
		
		if (table_annotation != null && !table_annotation.name().trim().isEmpty() ) {
			
			xml_element_open_tag += String.format("<%s>", table_annotation.name().trim().toLowerCase() );
			
			xml_element_close_tag += String.format("<%s>", table_annotation.name().trim().toLowerCase() );
			
		} else {
			
			xml_element_open_tag += String.format("<%ss>", this.entity.getSimpleName().toLowerCase() );
			
			xml_element_close_tag += String.format("</%ss>", this.entity.getSimpleName().toLowerCase() );
			
		}
		
		if (!this.isEmpty() ) {
			
			for (Model model : this) {
				xml_string += String.format("%s\n", model.to_extense_xml(1) );
			}
			
			xml_string = String.format("%s\n%s%s", xml_element_open_tag, xml_string, xml_element_close_tag);
			
		} else {
			
			xml_element_open_tag = xml_element_open_tag.replace(">", " />");
			
			xml_string = String.format("%s", xml_element_open_tag);
			
		}
		
		return xml_string;
	}
	
	public QuerySet append(T object) {
		
		if (object != null) {
			this.add(object);
		}
		
		return this;
	}
	
	
	public <E extends Model> QuerySet<E> as(Class<E> c) {
		return (QuerySet<E>) this;
	}
	
	
	public void each(Block block) {
		
		int index = 0;
		
		if (block != null) {
			for (T object : this) {
				block.index = index++;
				block.value = object;
				block.run();
			}
		}
	}
	
	public void each(Function function) {
		
		int index = 0;
		
		if (function != null) {
			for (T object : this) {
				function.index = index++;
				function.value = object;
				function.run();
			}
		}
	}
	
	public QuerySet<T> set(String field, Object value) {
		
		for (T model : this) {
			model.set(field, value);
		}
		
		return this;
	}
	
	public List<List<String> > get(String field_names) {
		
		List<List<String> > fields_values = null;
		
		if (field_names != null && !field_names.trim().isEmpty() ) {
			
			fields_values = new ArrayList<List<String> >();
			
			String[] fields = null;
			
			// Apenas um atributo.
			if (field_names.matches("^(\\w+)$") ) {
				
				fields = new String[]{field_names};
				
			} else if (field_names.matches("^[\\w+,\\s+\\w+]+$") ) {

				// Mais de um atributo.
				
				// Criando array de fields utilizando vírgula seguida ou não de espaço como separador.
				fields = field_names.split(",\\s+");
				
			} else {
				
			}
			
			for (T model : this) {
				
				List<String> field_value = new ArrayList<String>();	
				
				for (String field : fields) {
					
					if (model.get(field) != null) {
					
						field_value.add( (model.get(field) ).toString() );
						
					} else {
						
						field_value.add( (String) model.get(field) );
					}
				}
				
				fields_values.add(field_value);
			}
		}
			
		return fields_values;
	}
	
	
	/**
	 * Método que retorna o primeiro objeto correspondente a consulta ou null.
	 *  
	 * @return Model
	 */	
	public T first() {
		
		T obj = null;
		
		// Verificando se a lista não é vazia.		
		if (!this.isEmpty() ) {
			
			// Ordenando a lista em ordem crescente pela chave primária.			
			this.order_by("id");
			
			// Referenciando o primeiro item da lista.			
			obj = this.get(0);
		}
		
		return obj;
	}
	
	
	/**
	 * Método que retorna o último objeto correspondente a consulta ou null.
	 * 
	 * @return Model
	 */	
	public T last() {
		
		T obj = null;
		
		if (!this.is_empty() ) {
			
			// Ordenando a lista em ordem decrescente pela chave primária.
			
			this.order_by("-id");
			
			obj = this.get(0);
		}
		
		return obj;
	}
}