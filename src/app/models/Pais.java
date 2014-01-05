package app.models;

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

@Table(name="paises", engine="InnoDB", charset="utf8", comment="Tabela de paises.")
public class Pais extends Model {
//  Atributos
	private static final long serialVersionUID = 8197600275231031642L;
	
	@CharField(max_length=50, unique=true, comment="Nome do país.")
	private String nome;
	
	@CharField(max_length=2, unique=true, comment="Sigla do país.")
	private String sigla;
	
	@CharField(max_length=50, required=false, unique=true, comment="Capital do país.")
	private String capital;
	
	@CharField(max_length=50, required=false, comment="Continente em que o país está localizado.")
	private String continente;
	
	@CharField(max_length=1, required=false, default_value="A", comment="Situação do País (A - Ativo / I - Inativo)")
	private String situacao;
	
	public static Manager objects = new Manager(Pais.class);
	
//	Construtores

//	Obs: Criacao de construtores de acordo com a obrigatoriedade dos campos na insercao.
	
//	Default  
	public Pais() {}
	
	public Pais(int id) {
		this.id = id;
	}
	
	public Pais(String nome) {
		this.nome = nome;
	}
	
	public Pais(int id, String nome, String sigla) {
		this.id = id;
		this.nome = nome;
		this.sigla = sigla;
	}
	
	public Pais(String nome, String sigla) {
		this.nome = nome;
		this.sigla = sigla;
	}
	
	public Pais(String nome, String sigla, String capital, String continente) {
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.continente = continente;		
	}
	
	public Pais(String nome, String sigla, String capital, String continente, String situacao) {
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.continente = continente;
		this.situacao = situacao;
	}
	
	public Pais(int id, String nome, String sigla, String capital, String continente, String situacao) {
		this.id = id;
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.continente = continente;
		this.situacao = situacao;
	}
	
//	Getters
	public String getNome() {
		return this.nome;
	}
	
	public String nome() {
		return this.nome;
	}
	
	public String getCapital() {
		return capital;
	}
	
	public String capital() {
		return capital;
	}
	
	public String getContinente() {
		return continente;
	}
	
	public String continente() {
		return continente;
	}
	
	public String getSigla() {
		return sigla;
	}
	
	public String sigla() {
		return sigla;
	}
	
	public String getSituacao() {
		return this.situacao;
	}
	
	public String situacao() {
		return this.situacao;
	}
	
//	Setters
	
	public Pais id(int id) {
		return (Pais) super.id(id);
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
//	Setter com suporte a chain methods - fluent interface.
	public Pais nome(String nome) {
		this.nome = nome;
		return this;
	}
	
	public void setSigla(String sigla) {
		this.sigla = sigla;
	}
	
	public Pais sigla(String sigla) {
		this.sigla = sigla;
		return this;
	}
	
	public void setCapital(String capital) {
		this.capital = capital;
	}
	
	public Pais capital(String capital) {
		this.capital = capital;
		return this;
	}
	
	public void setContinente(String continente) {
		this.continente = continente;
	}
	
	public Pais continente(String continente) {
		this.continente = continente;
		return this;
	}
	
	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}
	
	public Pais situacao(String situacao) {
		this.situacao = situacao;
		return this;
	}

	@SuppressWarnings("rawtypes")
	public jedi.db.models.QuerySet uf_set() {
		return Uf.objects.get_set(Pais.class, this.id);
	}
}