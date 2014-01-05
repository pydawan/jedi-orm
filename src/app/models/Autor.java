package app.models;

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

@Table(name="autores")
public class Autor extends Model {
//	Atributos
	private static final long serialVersionUID = -8520333625577424268L;

	@CharField(max_length=30)
	private String nome;
	
	@CharField(max_length=30, required=false)
	private String sobrenome;
	
	@CharField(max_length=30, required=true, unique=true)
	private String email;
	
	public static Manager objects = new Manager(Autor.class);
	
//	Construtores
	public Autor() {}
	
	public Autor(int id) {
		this.id = id;
	}
	
	public Autor(String nome) {
		this.nome = nome;
	}
	
	public Autor(String nome, String sobrenome, String email) {
		this.nome = nome;
		this.sobrenome = sobrenome;
		this.email = email;
	}
	
	public Autor(int id, String nome, String sobrenome, String email) {
		this.id = id;
		this.nome = nome;
		this.sobrenome = sobrenome;
		this.email = email;
	}
	
//	Getters
	public String getNome() {
		return nome;
	}
	
	public String nome() {
		return this.nome;
	}
	
	public String getSobrenome() {
		return sobrenome;
	}
	
	public String sobrenome() {
		return sobrenome;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String email() {
		return email;
	}

//	Setters
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public void nome(String nome) {
		this.nome = nome;
	}

	public void setSobrenome(String sobrenome) {
		this.sobrenome = sobrenome;
	}
	
	public void sobrenome(String sobrenome) {
		this.sobrenome = sobrenome;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public void email(String email) {
		this.email = email;
	}

	@SuppressWarnings("rawtypes")
	public jedi.db.models.QuerySet livro_set() {
		return Livro.objects.get_set(Autor.class, this.id);
	}
}