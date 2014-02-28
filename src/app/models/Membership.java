/***********************************************************************************************
 * @(#)Membership.java
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

import java.util.Date;

import jedi.db.Models;
import jedi.db.annotations.fields.CharField;
import jedi.db.annotations.fields.DateField;
import jedi.db.annotations.fields.ForeignKeyField;
import jedi.db.models.Manager;
import jedi.db.models.Model;

public class Membership extends Model {	
	private static final long serialVersionUID = -1658988303242454439L;
	
	@ForeignKeyField(model="Person", 
			constraint_name="fk_memberships_people", 
			references="people", 
			on_delete=Models.CASCADE)
	private Person person;
	
	@ForeignKeyField(
			model="Group",
			constraint_name="fk_memberships_groups",
			references="groups",
			on_delete=Models.CASCADE)
	private Group group;

	@DateField
	private Date dateJoined;
	
	@CharField(max_length=64)
	private String inviteReason;
	
	public static Manager objects = new Manager(Membership.class);

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Date getDateJoined() {
		return dateJoined;
	}

	public void setDateJoined(Date dateJoined) {
		this.dateJoined = dateJoined;
	}

	public String getInviteReason() {
		return inviteReason;
	}

	public void setInviteReason(String inviteReason) {
		this.inviteReason = inviteReason;
	}	
}