package app.models;

import java.math.BigDecimal;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DecimalField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Produto extends Model {
//	Atributos
	private static final long serialVersionUID = -5355826250714874543L;
	
	@CharField(max_length=30)
	private String nome;
	
	@DecimalField(precision=4, scale=2, unique=true)
	private BigDecimal preco;
	
	public static Manager objects = new Manager(Produto.class);
	
//	Construtores
	public Produto() {}
	
	public Produto(int id) {
		this.id = id;
	}
	
	public Produto(String nome) {
		this.nome = nome;
	}
	
	public Produto(int id, String nome, BigDecimal preco) {
		this.id = id;
		this.nome = nome;
		this.preco = preco;
	}

//	Getters
	public String getNome() {
		return nome;
	}

	public String nome() {
		return nome;
	}
	
	public BigDecimal getPreco() {
		return preco;
	}

	public BigDecimal preco() {
		return preco;
	}
	
//	Setters
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public void nome(String nome) {
		this.nome = nome;
	}
	
	public void setPreco(BigDecimal preco) {
		this.preco = preco;
	}

	public void preco(BigDecimal preco) {
		this.preco = preco;
	}
}