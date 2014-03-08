/***********************************************************************************************
 * @(#)PessoaFisica.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/03/07
 * 
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * 
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the GNU Public License v2.0 which accompanies 
 * this distribution, and is available at http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *    Thiago Alexandre Martins Monteiro - initial API and implementation
 ************************************************************************************************/

package app.models;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.OneToOneField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;

public class PessoaFisica extends Model {
	private static final long serialVersionUID = -2834119019885606438L;
	
	@OneToOneField
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