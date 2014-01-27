/*******************************************************************************
 * Copyright (c) 2014 Thiago Alexandre Martins Monteiro.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Thiago Alexandre Martins Monteiro - initial API and implementation
 ******************************************************************************/
package jedi.db.models;

//Especificacao do Model Manager para associacoes 1-N e N-N
//https://docs.djangoproject.com/en/dev/ref/models/relations/#django.db.models.fields.related.RelatedManager.add
public class RelatedManager extends Manager {
	public void add(Model ... models) {
		
	}

//	public void create(String ... args) {
//		
//	}
	
	public void remove(Model ... models) {
		
	}
	
	public void clear() {
		
	}
}
