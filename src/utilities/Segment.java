package utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Segment<A> implements Iterable<A> {
	
	List<A> segment;
	int size;
	
	public Segment(List<A> segment) {
		this.segment = segment;
		this.size = segment.size();
	}

	public Segment(A[] segment) {
		this.segment = Arrays.asList(segment);
		this.size = segment.length;
	}
	
	public int size() {
		return this.size;
	}
	
	public List<A> getElements() {
		return this.segment;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Segment<?>)) {
			return false;
		}
		
		Segment<?> otherSegment = (Segment<?>) other; 
		if (this.size != otherSegment.size()) {
			return false;
		} else {
			Iterator<A> it = iterator();
			for (Object element : otherSegment) {
				if (!element.equals(it.next())) {
					return false;
				}
			}
		}
		
		return true;
	}

	@Override
	public Iterator<A> iterator() {
		return segment.iterator();
	}
	
	@Override
	public String toString() {
		String s = "[";
		for (A element : this.segment) {
			s += element + ", ";
		}
		return s.substring(0, s.length() - 2) + "]";
	}
	
	@Override
	public int hashCode() {
		int result = 3;
		for (A element : this.segment) {
			result = 37*result + element.hashCode();
		}
		return result;
	}
 	
	public Segment<A> compose(Segment<A> other) {
		List<A> newSegment = new ArrayList<A>(getElements());
		newSegment.addAll(other.getElements());
		return new Segment<A>(newSegment);
	}
}
