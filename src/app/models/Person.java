/***********************************************************************************************
 * @(#)Person.java
 * 
 * Version: 1.0
 * 
 * Date: 2014/02/20
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

import jedi.db.annotations.Table;
import jedi.db.annotations.fields.CharField;
import jedi.db.models.Manager;
import jedi.db.models.Model;
import jedi.db.models.QuerySet;

@Table(name="people")
public class Person extends Model {
	private static final long serialVersionUID = -4040286187559266582L;
	
	@CharField(max_length=128)
	private String name;
	
	public static Manager objects = new Manager(Person.class);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @SuppressWarnings("rawtypes")
    public jedi.db.models.QuerySet getGroupSet() {
    	QuerySet<Membership> memberships = Membership.objects.getSet(Person.class, this.id);    	
    	QuerySet<Group> groups = new QuerySet<Group>();
    	Group group = null;
    	for (Membership membership : memberships) {
    		group = Group.objects.<Group>get("id", membership.getGroup().getId());
    		// ManyToManyField.
    		group.getMembers();
    		groups.add(group);
    	}
    	return groups;
    }
}