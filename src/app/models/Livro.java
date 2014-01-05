package app.models;

import java.util.List;

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;
import jedi.db.models.QuerySet;

@SuppressWarnings({"rawtypes", "unchecked"})
@Table(name="livros", engine="InnoDB", charset="utf8", comment="Tabela que armazena os livros do sistema de biblioteca.")
public class Livro extends Model {
//	Atributos
	private static final long serialVersionUID = 9076408430303339094L;
	
	@CharField(max_length=30, required=true, unique=true, comment="Esse campo armazena o nome do livro.")
	private String titulo;
	
	@ManyToManyField(model="Autor", references="autores")
	private QuerySet<Autor> autores;
	
	@ForeignKeyField(model="Editora", constraint_name="fk_livros_editoras", references="editoras")
	private Editora editora;
	
	@CharField(max_length=15, required=true, comment="Data de publicação do livro.")
	private String dataPublicacao;
	
	// Registrando o model manager para a classe de objetos livro.
	public static Manager objects = new Manager(Livro.class);
	
//	public static Manager objects = new Manager<Livro>();
	
//	Construtores
	public Livro() {}
	
	public Livro(int id) {
		this.id = id;
	}
	
	public Livro(String titulo) {
		this.titulo = titulo;
	}
	
	public Livro(String titulo, List<Autor> autores, Model editora, String dataPublicacao) {
		this.titulo = titulo;
		this.autores = new QuerySet<Autor>();
		this.autores.add(autores);
		this.editora = (Editora) editora;
		this.dataPublicacao = dataPublicacao;
	}
	
	public Livro(int id, String titulo, List<Autor> autores, Model editora, String dataPublicacao) {
		this.id = id;
		this.titulo = titulo;
		this.autores = new QuerySet<Autor>();
		this.autores.add(autores);
		this.editora = (Editora) editora;
		this.dataPublicacao = dataPublicacao;
	}
	
//	Getters
	public String getTitulo() {
		return titulo;
	}

	public String titulo() {
		return titulo;
	}
	
	public QuerySet getAutores() {
		return autores;
	}
	
	public QuerySet autores() {
		return autores;
	}
	
	public Editora getEditora() {
		return editora;
	}

	public Editora editora() {
		return editora;
	}
	
	public String getDataPublicacao() {
		return dataPublicacao;
	}

	public String dataPublicacao() {
		return dataPublicacao;
	}

//	Setters
	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}
	
	public void titulo(String titulo) {
		this.titulo = titulo;
	}

	public void setAutores(List<Autor> autores) {
		this.autores = new QuerySet<Autor>();
		this.autores.add(autores);
	}
	
	public void autores(List<Autor> autores) {
		this.autores = new QuerySet<Autor>();
		this.autores.add(autores);
	}
	
	// Alternativa mais flexível.
	public void setAutores(QuerySet autores) {
		this.autores = autores;
	}
	
	public void autores(QuerySet autores) {
		this.autores = autores;
	}

	public void setEditora(Model editora) {
		this.editora = (Editora) editora;
	}
	
	public void editora(Model editora) {
		this.editora = (Editora) editora;
	}
	
	public void setDataPublicacao(String dataPublicacao) {
		this.dataPublicacao = dataPublicacao;
	}
	
	public void dataPublicacao(String dataPublicacao) {
		this.dataPublicacao = dataPublicacao;
	}
}