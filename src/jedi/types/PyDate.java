package jedi.types;

@SuppressWarnings({"deprecation"})
public class PyDate extends java.util.Date {

	private static final long serialVersionUID = -1598588999773456561L;

	public PyDate() {
		super();
	}
	
	public PyDate(java.util.Date date) {
		// Utilizando os setters da super-classe.
		super.setYear(date.getYear() );
		super.setMonth(date.getMonth() );
		super.setDate(date.getDate() );
		super.setHours(date.getHours() );
		super.setMinutes(date.getMinutes() );
		super.setSeconds(date.getSeconds() );
	}
	
	public PyDate(int year, int month, int date) {
		super(year - 1900, month - 1, date);
	}
	
	public PyDate(int year, int month, int date, int hrs, int min) {
		super(year - 1900, month - 1, date, hrs, min);
	}
	
	public PyDate(int year, int month, int date, int hrs, int min, int sec) {
		super(year - 1900, month - 1, date, hrs, min, sec);
	}
	
	// Getters
	
	public int year() {
		return this.getYear() + 1900;
	}
	
	public int month() {
		return this.getMonth() + 1;
	}
	
	public int date() {
		return this.getDate();
	}
	
	public int hours() {
		return this.getHours();
	}
	
	public int minutes() {
		return this.getMinutes();
	}
	
	public int seconds() {
		return this.getSeconds();
	}
	
	public String toString(String format) {
		return PyString.to_string(this, format);
	}
	
	public String toString() {
		
		return String.format(
				
			"%d-%d-%d %s:%s:%s", 
			
			this.getYear() + 1900,
			
			this.getMonth() + 1,
			
			this.getDate(),
			
			this.getHours(),
			
			this.getMinutes(),
			
			this.getSeconds()
		);
	}
	
	public String to_string(String format) {
		// Por questoes de desempenho nao fiz uma chamada a toString(format) nessa linha
		// pois o mesmo iria criar uma nova pilha de metodo na memória.
		return PyString.to_string(this, format);
	}
	
	
	// Setters
	
	public void year(int year) {
		this.setYear(year - 1900);
	}
	
	public void month(int month) {
		this.setMonth(month - 1);
	}
	
	public void date(int date) {
		this.setDate(date);
	}
	
	public void hours(int hours) {
		this.setHours(hours);
	}
	
	public void minutes(int minutes) {
		this.setMinutes(minutes);
	}
	
	public void seconds(int seconds) {
		this.setSeconds(seconds);
	}
}