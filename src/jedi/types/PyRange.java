package jedi.types;

import java.util.ArrayList;


public class PyRange extends ArrayList<Integer> {

	private static final long serialVersionUID = 1L;

	public int start;
	
	public int end;
	
	public int increment;
	
	public int[] values;
	
	public PyRange() {}
	
	public PyRange(int start, int end) {
		this(start, end, 1);
	}
	
	public PyRange(int start, int end, int increment) {
		this.start = start;
		this.end = end;
		this.increment = increment;

		for (int i = start; i < end; i += increment) {
			this.add(i);
		}
	}
	
	public PyRange(String range) {
		this(range, 0);
	}
	
	public PyRange(String range, int increment) {		
		System.out.println(range);
	}
	
	public PyRange reverse() {
		
		PyRange r = new PyRange();
			
		for (int i = this.size() - 1; i >= 0; i--) {
			r.add(this.get(i) );
		}
		
		return r;
	}
	
	public void each(Function<Integer> function) {
		
		int index = 0;
		
		if (function != null) {
			for (Integer object : this) {
				function.index = index++;
				function.value = object;
				function.run();
			}
		}
	}
}
