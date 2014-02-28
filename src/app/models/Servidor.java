/***********************************************************************************************
 * @(#)Servidor.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/18
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

import jedi.db.Models;
import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;
import jedi.db.models.QuerySet;

@Table(name="servidores")
public class Servidor extends Model {
	private static final long serialVersionUID = -3396514653550578357L;
	
	@CharField(max_length=200)
	private String nome;
	
	@CharField(max_length=50)
	private String matricula;
	
	@CharField(max_length=14)
	private String cpf;
	
	@ManyToManyField(model="Notebook",
	        references="notebooks",
	        on_delete=Models.CASCADE,
	        on_update=Models.CASCADE)
	private QuerySet<Notebook> notebooksEmprestados;
	
	public static Manager objects = new Manager(Servidor.class);
	
	public Servidor() {}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public String getMatricula() {
		return matricula;
	}
	
	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}
	
	public String getCpf() {
		return cpf;
	}
	
	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet getEmprestimoSet() {
        return Emprestimo.objects.getSet(Servidor.class, this.id);
    }
}