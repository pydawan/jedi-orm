/***********************************************************************************************
 * @(#)Group.java
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

import java.util.List;

import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.ManyToManyField;
import jedi.db.models.Model;
import jedi.db.models.manager.Manager;

public class Group extends Model {
	private static final long serialVersionUID = 4512798759309714169L;
	
	@CharField(max_length=128)
	private String name;
	
	@ManyToManyField(through="Membership")
	private List<Person> members;
	
	public static Manager objects = new Manager(Group.class);
	
	public Group() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    public List<Person> getMembers() {
        List<Membership> memberships = Membership.objects.getSet(Group.class, this.id);
        members = new java.util.ArrayList<Person>();
        Person person = null;
        for (Membership membership : memberships) {
            person = Person.objects.<Person>get("id", membership.getPerson().getId());
            members.add(person);
        }
        return members;
    }

    public void setMembers(List<Person> members) {
        this.members = members;
    }
}