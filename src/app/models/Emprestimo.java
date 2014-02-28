/***********************************************************************************************
 * @(#)Emprestimo.java
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

import java.sql.Date;

import jedi.db.Models;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Emprestimo extends Model {
	private static final long serialVersionUID = -4411210868254749824L;
	
	@ForeignKeyField(model="Notebook", 
			references="notebooks",
			constraint_name="fk_emprestimos_notebooks", 
			on_delete=Models.CASCADE, 
			on_update=Models.CASCADE)
	private Notebook notebook;
	
	@ForeignKeyField(model="Servidor",
			references="servidores",
			constraint_name="fk_emprestimos_servidores",
			on_delete=Models.CASCADE,
			on_update=Models.CASCADE)
	private Servidor servidor;
	
	@DateField(auto_now_add=true, auto_now=true)
	private Date dataInicio;
	
	@DateField(auto_now_add=true, auto_now=true)
	private Date dataFim;
	
	public static Manager objects = new Manager(Emprestimo.class);

	public Notebook getNotebook() {
		return notebook;
	}

	public void setNotebook(Notebook notebook) {
		this.notebook = notebook;
	}

	public Servidor getServidor() {
		return servidor;
	}

	public void setServidor(Servidor servidor) {
		this.servidor = servidor;
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataFim() {
		return dataFim;
	}

	public void setDataFim(Date dataFim) {
		this.dataFim = dataFim;
	}
}