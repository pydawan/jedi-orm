package app.models;

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Editora extends Model {
//	Atributos
	private static final long serialVersionUID = -4632483608458698726L;

	@CharField(max_length=30)
	private String nome;
	
	@CharField(max_length=50, required=false)
	private String endereco;
	
	@ForeignKeyField(model="Uf", constraint_name="fk_editoras_ufs", references="ufs", on_delete=Models.CASCADE)
	private Uf uf;
	
	public static Manager objects = new Manager(Editora.class);

//	Construtores
	public Editora() {}
	
	public Editora(int id) {
		this.id = id;
	}
	
	public Editora(String nome) {
		this.nome = nome;
	}
	
	public Editora(String nome, Model uf) {
		this.nome = nome;		
		this.uf = (Uf) uf;
	}
	
	public Editora(String nome, String endereco, Model uf) {
		this.nome = nome;
		this.endereco = endereco;
		this.uf = (Uf) uf;
	}
	
	public Editora(int id, String nome, String endereco, Model uf) {
		this.id = id;
		this.nome = nome;
		this.endereco = endereco;
		this.uf = (Uf) uf;
	}
	
//	Getters
	public String getNome() {
		return nome;
	}

	public String nome() {
		return nome;
	}
	
	public String getEndereco() {
		return endereco;
	}
	
	public String endereco() {
		return endereco;
	}
	
	public Uf getUf() {
		return uf;
	}

	public Uf uf() {
		return uf;
	}
	
//	Setters
	public void setNome(String nome) {
		this.nome = nome;
	}
		
	public void nome(String nome) {
		this.nome = nome;
	}
	
	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}
	
	public void endereco(String endereco) {
		this.endereco = endereco;
	}
	
	public void setUf(Model uf) {
		this.uf = (Uf) uf;
	}
	
	public void uf(Model uf) {
		this.uf = (Uf) uf;
	}

	@SuppressWarnings("rawtypes")
	public jedi.db.models.QuerySet livro_set() {
		return Livro.objects.get_set(Editora.class, this.id);
	}
}