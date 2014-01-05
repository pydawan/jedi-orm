package app.models;

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Uf extends Model {
//  Atributos
	private static final long serialVersionUID = -6945954961476788586L;
	
	@CharField(max_length=50, unique=true, comment="Nome da Unidade Federativa.")
	private String nome;
	
	@CharField(max_length=2, unique=true, comment="Sigla da Unidade Federativa.")
	private String sigla;
	
	@CharField(max_length=50, unique=true, required=false, comment="Capital da Unidade Federativa.")
	private String capital;
	
	@CharField(max_length=30, unique=false, required=false, comment="Região em que está localizada a Unidade Federativa.")
	private String regiao;
	
	@ForeignKeyField(model="Pais", constraint_name="fk_ufs_paises", references="paises", on_delete=Models.CASCADE, on_update=Models.CASCADE, comment="País onde está localizada a Unidade Federativa.")
	private Pais pais;
	
	@CharField(max_length=1, default_value="A", comment="Situação da Unidade Federativa (A - Ativa / I - Inativa)")
	private String situacao;
	
	public static Manager objects = new Manager(Uf.class);
	
//	Construtores
	public Uf() {}
	
	public Uf(int id) {
		this.id = id;
	}
	
	public Uf(String nome) {
		this.nome = nome;
	}
	
	public Uf(String nome, String sigla) {
		this.nome = nome;
		this.sigla = sigla;
	}
	
	public Uf(String nome, String sigla, Pais pais) {
		this.nome = nome;
		this.sigla = sigla;
		this.pais = pais;
	}
	
	public Uf(String nome, String sigla, String situacao, Pais pais) {
		this.nome = nome;
		this.sigla = sigla;
		this.situacao = situacao;
		this.pais = pais;
	}
	
	public Uf(String nome, String sigla, String capital, String regiao, Model pais) {
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.regiao = regiao;
		this.pais = (Pais) pais;
	}
	
	public Uf(int id, String nome, String sigla, String capital, String regiao, Model pais) {
		this.id = id;
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.regiao = regiao;
		this.pais = (Pais) pais;
	}
	
	public Uf(String nome, String sigla, String capital, String regiao) {
		this.nome = nome;
		this.sigla = sigla;
		this.capital = capital;
		this.regiao = regiao;
	}
	
//	Getters
	public String getNome() {
		return nome;
	}
	
	public String nome() {
		return nome;
	}
	
	public String getSigla() {
		return sigla;
	}
	
	public String sigla() {
		return sigla;
	}
	
	public String getCapital() {
		return capital;
	}
	
	public String capital() {
		return capital;
	}
	
	public String getRegiao() {
		return regiao;
	}
	
	public String regiao() {
		return regiao;
	}
	
	public Pais getPais() {
		return pais;
	}
	
	public Pais pais() {
		return pais;
	}
	
	public String getSituacao() {
		return situacao;
	}
	
	public String situacao() {
		return situacao;
	}
	
//	Setters
	
	public Uf id(int id) {
		return (Uf) super.id(id);
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public Uf nome(String nome) {
		this.nome = nome;
		return this;
	}

	public void setSigla(String sigla) {
		this.sigla = sigla;
	}

	public Uf sigla(String sigla) {
		this.sigla = sigla;
		return this;
	}
	
	public void setCapital(String capital) {
		this.capital = capital;
	}
	
	public Uf capital(String capital) {
		this.capital = capital;
		return this;
	}
	
	public void setRegiao(String regiao) {
		this.regiao = regiao;
	}
	
	public Uf regiao(String regiao) {
		this.regiao = regiao;
		return this;
	}
	
	public void setPais(Model pais) {
		this.pais = (Pais) pais;
	}
	
	public Uf pais(Model pais) {		
		this.pais = (Pais) pais;
		return this;
	}
	
	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}
	
	public Uf situacao(String situacao) {
		this.situacao = situacao;
		return this;
	}

	
	@SuppressWarnings("rawtypes")
	public jedi.db.models.QuerySet editora_set() {
		return Editora.objects.get_set(Uf.class, this.id);
	}
}