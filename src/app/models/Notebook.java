/***********************************************************************************************
 * @(#)Notebook.java
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

import jedi.db.annotations.fields.CharField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;

public class Notebook extends Model {
	private static final long serialVersionUID = 2208401363196881965L;
	
	@CharField(max_length=50)
	private String numeroSerie;
	
	@CharField(max_length=50)
	private String numeroPatrimonio;
	
	public static Manager objects = new Manager(Notebook.class);
	
	public Notebook() {}

	public String getNumeroSerie() {
		return numeroSerie;
	}

	public void setNumeroSerie(String numeroSerie) {
		this.numeroSerie = numeroSerie;
	}

	public String getNumeroPatrimonio() {
		return numeroPatrimonio;
	}

	public void setNumeroPatrimonio(String numeroPatrimonio) {
		this.numeroPatrimonio = numeroPatrimonio;
	}    

    public jedi.db.models.query.QuerySet<Emprestimo> getEmprestimoSet() {
        return Emprestimo.objects.getSet(Notebook.class, this.id);
    }

    public jedi.db.models.query.QuerySet<Servidor> getServidorSet() {
        return Servidor.objects.getSet(Notebook.class, this.id);
    }
}