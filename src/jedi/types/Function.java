package jedi.types;

public class Function<T> implements Runnable {
	
	public int index;
	
	public T value;
	
	public Object[] objects = null;
	
	public Object object = null;
	
	public Function() {}
	
	public <E> Function(E ... objects) {
		this.objects = objects;
	}
	
	public <E> Function(E object) {		
		this.object = object;
	}

	public void run() {}
}
