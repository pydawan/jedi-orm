package jedi.db.models;

import java.util.ArrayList;
import java.util.List;

public abstract class Field {
	protected int maxLength;
	protected String comment;
	protected boolean primaryKey;
	protected boolean required;
	protected boolean unique;
	protected boolean dbIndex;
	protected boolean editable;	
	protected String dbColumn;
    protected String dbTablespace;
    protected String defaultValue;        
    protected String helpText;
    protected List<String> choices = new ArrayList<String>();
    protected List<String> errorMessages = new ArrayList<String>();
    protected String uniqueForDate;
    protected String uniqueForMonth;
    protected String uniqueForYear;
    protected String verboseName;
    
    public Field() {}

	public int getMaxLength() {
		return maxLength;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isDbIndex() {
		return dbIndex;
	}

	public void setDbIndex(boolean dbIndex) {
		this.dbIndex = dbIndex;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public String getDbColumn() {
		return dbColumn;
	}

	public void setDbColumn(String dbColumn) {
		this.dbColumn = dbColumn;
	}

	public String getDbTablespace() {
		return dbTablespace;
	}

	public void setDbTablespace(String dbTablespace) {
		this.dbTablespace = dbTablespace;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getHelpText() {
		return helpText;
	}

	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}

	public List<String> getChoices() {
		return choices;
	}

	public void setChoices(List<String> choices) {
		this.choices = choices;
	}

	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public String getUniqueForDate() {
		return uniqueForDate;
	}

	public void setUniqueForDate(String uniqueForDate) {
		this.uniqueForDate = uniqueForDate;
	}

	public String getUniqueForMonth() {
		return uniqueForMonth;
	}

	public void setUniqueForMonth(String uniqueForMonth) {
		this.uniqueForMonth = uniqueForMonth;
	}

	public String getUniqueForYear() {
		return uniqueForYear;
	}

	public void setUniqueForYear(String uniqueForYear) {
		this.uniqueForYear = uniqueForYear;
	}

	public String getVerboseName() {
		return verboseName;
	}

	public void setVerboseName(String verboseName) {
		this.verboseName = verboseName;
	}
}