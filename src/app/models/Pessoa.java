package app.models;

import java.util.Date;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.IntegerField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Pessoa extends Model {
//	Atributos
	private static final long serialVersionUID = 887615433470716542L;

	@CharField(max_length=50, unique=true, comment="Esse campo armazena o nome da pessoa.")
	private String nome;
	
	@IntegerField
	private int idade;
	
	@CharField(max_length=14, required=false)
	private String cpf;
	
	@CharField(max_length=10, required=false)
	private String rg;
	
	@CharField(max_length=1)
	private String tipoPessoa;
	
//	Se auto_now for true criar campo no banco como sendo do tipo timestamp without timezone (with timezone não é uma boa estratégia).
	@DateField(auto_now=true, auto_now_add=true, comment="Data do aniversário")
	private Date dataNascimento;
	
//	Entity Manager dessa classe de objetos.
	public static Manager objects = new Manager(Pessoa.class);
	
//	Construtores
	public Pessoa() {}
	
	public Pessoa(int id, String nome, int idade, String tipoPessoa) {
		this.id = id;
		this.nome = nome;
		this.idade = idade;		
		this.tipoPessoa = tipoPessoa;		
	}
	
	public Pessoa(String nome, int idade, String tipoPessoa) {		
		this.nome = nome;
		this.idade = idade;		
		this.tipoPessoa = tipoPessoa;		
	}	
		
	public Pessoa(int id, String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
		this.id = id;
		this.nome = nome;
		this.idade = idade;
		this.cpf = cpf;
		this.rg = rg;
		this.tipoPessoa = tipoPessoa;
		this.dataNascimento = dataNascimento;
	}
	
	public Pessoa(String nome, int idade, String cpf, String rg, String tipoPessoa, Date dataNascimento) {
		this.nome = nome;
		this.idade = idade;
		this.cpf = cpf;
		this.rg = rg;
		this.tipoPessoa = tipoPessoa;
		this.dataNascimento = dataNascimento;
	}

	
//	Getters
	public String getNome() {
		return nome;
	}
	
	public String nome() {
		return nome;
	}
	
	public int getIdade() {
		return idade;
	}
	
	public int idade() {
		return idade;
	}
	
	public String getCpf() {
		return cpf;
	}
	
	public String cpf() {
		return cpf;
	}
	
	public String getRg() {
		return rg;
	}
	
	public String rg() {
		return rg;
	}
	
	public String getTipoPessoa() {
		return tipoPessoa;
	}
	
	public String tipo_pessoa() {
		return tipoPessoa;
	}
	
	public Date getDataNascimento() {
		return dataNascimento;
	}
	
	public Date data_nascimento() {
		return dataNascimento;
	}

//	Setters
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public void nome(String nome) {
		this.nome = nome;
	}

	public void setIdade(int idade) {
		this.idade = idade;
	}
	
	public void idade(int idade) {
		this.idade = idade;
	}
	
	public void setCpf(String cpf) {
		this.cpf = cpf;
	}
	
	public void cpf(String cpf) {
		this.cpf = cpf;
	}

	public void setRg(String rg) {
		this.rg = rg;
	}
	
	public void rg(String rg) {
		this.rg = rg;
	}

	public void setTipoPessoa(String tipoPessoa) {
		this.tipoPessoa = tipoPessoa;
	}
	
	public void tipo_pessoa(String tipo_pessoa) {
		this.tipoPessoa = tipo_pessoa;
	}
	
	public void setDataNascimento(Date dataNascimento) {
		this.dataNascimento = dataNascimento;
	}
	
	public void data_nascimento(Date data_nascimento) {
		this.dataNascimento = data_nascimento;
	}
}