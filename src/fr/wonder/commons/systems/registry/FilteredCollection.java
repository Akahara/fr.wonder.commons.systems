package fr.wonder.commons.systems.registry;

import java.util.ArrayList;
import java.util.List;

public class FilteredCollection<T, K> {

	protected final List<FilteredElement<T, K>> masks = new ArrayList<>();

	public void addMask(FilteredElement<T, K> mask) {
		masks.add(mask);
	}
	
	public void insertMask(FilteredElement<T, K> mask, int index) {
		masks.add(index, mask);
	}
	
	public int getMaskCount() {
		return masks.size();
	}
	
	public K getAccepted(T key) {
		for(FilteredElement<T, K> mask : masks)
			if(mask.matches(key))
				return mask.get();
		return null;
	}
	
	public int getUnacceptedMaskCount(T key) {
		for(int i = 0; i < masks.size(); i++) {
			if(masks.get(i).matches(key))
				return i;
		}
		return -1;
	}
	
}
