package edu.yu.cs.com1320.project.impl;

import java.util.NoSuchElementException;

import edu.yu.cs.com1320.project.MinHeap;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {

    @SuppressWarnings("unchecked")
    public MinHeapImpl() {
        // Needs to be Comparable and not Object to deal with casting to extended generics
        this.elements = (E[]) new Comparable[10];
    }

    public void reHeapify(E element) {
        if (element == null) {
            throw new IllegalArgumentException();
        }
        int idx = this.getArrayIndex(element);
        boolean isLeftChild = idx * 2 < this.elements.length && this.elements[idx * 2] != null;
        boolean isRightChild = idx * 2 + 1 < this.elements.length && this.elements[idx * 2 + 1] != null;
        // Handle updated element is less than parent in MinHeap and needs to be shifted up
        if (idx > 1 && this.isGreater(idx / 2, idx)) {
            this.upHeap(idx);
        }
        // Handle updated element is greater than child in MinHeap and needs to be shifted down
        else if ((isLeftChild && this.isGreater(idx, idx * 2)) || (isRightChild && this.isGreater(idx, idx * 2 + 1))) {
            this.downHeap(idx);
        }
    }

    protected int getArrayIndex(E element) {
        if (element == null) {
            throw new IllegalArgumentException();
        }
        int idx = 0;
        for (int i = 1; i < this.elements.length; i++) {
            E curElement = this.elements[i];
            if (curElement == null) {
                break;
            }
            if (curElement.equals(element)) {
                idx = i;
                break;
            }
        }
        if (idx == 0) {
            throw new NoSuchElementException();
        }
        return idx;
    }

    @SuppressWarnings("unchecked")
    protected void doubleArraySize() {
        E[] oldElements = this.elements;
        this.elements = (E[]) new Comparable[this.elements.length * 2];
        for (int i = 1; i < oldElements.length; i++) {
            this.elements[i] = oldElements[i];
        }
    }
}
