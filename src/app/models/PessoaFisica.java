package app.models;

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.OneToOneField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class PessoaFisica extends Model {
	private static final long serialVersionUID = -2834119019885606438L;
	
	@OneToOneField(model = "Pessoa", 
			constraint_name = "fk_pessoas_fisicas_pessoas",
			references = "pessoas",
			on_delete = Models.CASCADE,
			on_update = Models.CASCADE)
	private Pessoa pessoa;
	
	@CharField(max_length=14, unique=true)
	private String cpf; 
	
	public static Manager objects = new Manager(PessoaFisica.class);
	
	public PessoaFisica() {}
	
	public Pessoa getPessoa() {
		return pessoa;
	}
	
	public void setPessoa(Pessoa pessoa) {
		this.pessoa = pessoa;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}
}