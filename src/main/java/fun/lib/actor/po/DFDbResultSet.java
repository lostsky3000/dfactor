package fun.lib.actor.po;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public final class DFDbResultSet<T> implements List<T>{
	private final LinkedList<T> _lsRaw = new LinkedList<>();
	private int _size = 0;
	public DFDbResultSet() {
		// TODO Auto-generated constructor stub
	}
	
	public void setSize(int size){
		this._size = size;
	}
	public int getSize(){
		return this._size;
	}
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return _lsRaw.size();
	}

	@Override
	public boolean isEmpty() {
		return _lsRaw.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return _lsRaw.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return _lsRaw.iterator();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return _lsRaw.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return _lsRaw.toArray(a);
	}

	@Override
	public boolean add(T e) {
		// TODO Auto-generated method stub
		return _lsRaw.add(e);
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return _lsRaw.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return _lsRaw.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		// TODO Auto-generated method stub
		return _lsRaw.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		// TODO Auto-generated method stub
		return _lsRaw.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return _lsRaw.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return _lsRaw.retainAll(c);
	}

	@Override
	public void clear() {
		_lsRaw.clear();
	}

	@Override
	public T get(int index) {
		// TODO Auto-generated method stub
		return _lsRaw.get(index);
	}

	@Override
	public T set(int index, T element) {
		// TODO Auto-generated method stub
		return _lsRaw.set(index, element);
	}

	@Override
	public void add(int index, T element) {
		// TODO Auto-generated method stub
		_lsRaw.add(index, element);
	}

	@Override
	public T remove(int index) {
		// TODO Auto-generated method stub
		return _lsRaw.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return _lsRaw.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return _lsRaw.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		// TODO Auto-generated method stub
		return _lsRaw.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		// TODO Auto-generated method stub
		return _lsRaw.listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return _lsRaw.subList(fromIndex, toIndex);
	}

}
