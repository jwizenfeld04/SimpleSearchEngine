package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    private int size;
    private Node<T> top;

    public StackImpl() {
        this.top = null;
        this.size = 0;
    }

    @SuppressWarnings("hiding")
    private class Node<T> {
        private T val;
        private Node<T> next;

        private Node(T val) {
            this.val = val;
            this.next = null;
        }
    }

    public void push(T element) {
        Node<T> newNode = new Node<T>(element);
        newNode.next = this.top;
        this.top = newNode;
        this.size++;
    }

    public T pop() {
        if (this.size == 0) {
            return null;
        }
        T val = this.top.val;
        this.top = this.top.next;
        size--;
        return val;
    }

    public T peek() {
        if (this.size == 0) {
            return null;
        }
        return this.top.val;
    }

    public int size() {
        return this.size;
    }
}
