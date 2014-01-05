package jedi.db.exceptions;

/**
 * 
 * @author Thiago Alexandre Martins Monteiro
 * 
 * Exceção lançada quando mais de um objeto é retornado por uma consulta no banco de dados.
 *
 */

public class MultipleObjectsReturned extends Exception {

// 	Atributos
	
	private static final long serialVersionUID = 271849121410861140L;
	
//	Construtores
	
	public MultipleObjectsReturned() {
		super();
	}

	public MultipleObjectsReturned(String message) {
		super(message);
	}
	
	public MultipleObjectsReturned(Throwable cause) {
		super(cause);
	}
	
	public MultipleObjectsReturned(String message, Throwable cause) {
		super(message, cause);
	}
}
