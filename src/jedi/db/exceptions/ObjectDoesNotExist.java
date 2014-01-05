package jedi.db.exceptions;

/**
 * 
 * @author Thiago Alexandre Martins Monteiro
 *
 * Exceção que é lançada quando nenhum objeto é retornado por uma consulta no banco de dados.
 */
public class ObjectDoesNotExist extends Exception {

// Atributos
	
	private static final long serialVersionUID = 5830724599642403525L;
 
// Construtores
	
	public ObjectDoesNotExist() { 
		super(); 
	}
	  
	public ObjectDoesNotExist(String message) { 
		super(message); 
	}
  
	public ObjectDoesNotExist(String message, Throwable cause) { 
		super(message, cause); 
	}
	 
	public ObjectDoesNotExist(Throwable cause) { 
		super(cause); 
	}

}
