package jedi.db;

/** 
 * @author Thiago Alexandre Martins Monteiro
 *
 */
public enum Models {
	CASCADE("CASCADE"),
	PROTECT("PROTECT"),
	SET_NULL("SET NULL"),
	SET_DEFAULT("SET DEFAULT"),
	DO_NOTHING("");
	
	private final String valor;
	
	private Models(String valor) {
		this.valor = valor;
	}
	
	public String getValor() {
		return valor;
	}
	
	public String valor() {
		return valor;
	}
}
