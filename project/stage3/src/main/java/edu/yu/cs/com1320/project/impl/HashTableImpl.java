package edu.yu.cs.com1320.project.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.yu.cs.com1320.project.HashTable;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {
    private Node<Key, Value>[] nodes;
    private int size;
    private boolean isLoadFactor;

    @SuppressWarnings("unchecked")
    public HashTableImpl() {
        this.nodes = new Node[10];
        this.size = 0;
        this.isLoadFactor = false;
    }

    @SuppressWarnings("hiding")
    private class Node<Key, Value> {
        private Key key;
        private Value value;
        private Node<Key, Value> next;

        private Node(Key key, Value value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
    }

    public Value get(Key k) {
        int idx = getHashIndex(k);
        Node<Key, Value> curNode = this.nodes[idx];
        while (curNode != null) {
            if (curNode.key.equals(k)) {
                return curNode.value;
            }
            curNode = curNode.next;
        }
        return null;
    }

    public Value put(Key k, Value v) {
        Node<Key, Value> newNode = new Node<>(k, v);
        int idx = getHashIndex(k);
        // Handles a new node at new index
        if (this.nodes[idx] == null) {
            if (v != null) {
                this.nodes[idx] = newNode;
                this.size++;
            }
            return null;
        }
        Node<Key, Value> curNode = this.nodes[idx];
        Node<Key, Value> prevNode = null;
        // Handles a replace or delete of an exisitng node
        while (curNode != null) {
            if (curNode.key.equals(k)) {
                return putUpdateValue(prevNode, curNode, v, idx);
            }
            prevNode = curNode;
            curNode = curNode.next;
        }
        // Handles adding a new node with a collision
        handleNodeCollision(prevNode, newNode, v);
        return null;
    }

    private void handleNodeCollision(Node<Key, Value> prevNode, Node<Key, Value> newNode, Value v) {
        if (v != null) {
            prevNode.next = newNode;
            this.size++;
            if (this.size() / this.nodes.length > .7) {
                this.isLoadFactor = true;
                if (this.isLoadFactor) {
                    doubleArray();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void doubleArray() {
        this.isLoadFactor = false;
        Node<Key, Value>[] curNodes = this.nodes;
        int originalSize = this.size;
        Node<Key, Value>[] doubleNodes = new Node[this.nodes.length * 2];
        this.nodes = doubleNodes;
        for (int i = 0; i < curNodes.length; i++) {
            Node<Key, Value> curNode = curNodes[i];
            if (curNode != null) {
                this.put(curNode.key, curNode.value);
                while (curNode.next != null) {
                    curNode = curNode.next;
                    this.put(curNode.key, curNode.value);
                }
            }
        }
        this.size = originalSize;
    }

    private Value putUpdateValue(Node<Key, Value> prevNode, Node<Key, Value> curNode, Value v, int idx) {
        Value prevVal = curNode.value;
        if (v == null) {
            if (prevNode != null) {
                prevNode.next = curNode.next;
            } else {
                this.nodes[idx] = curNode.next;
            }
            this.size--;
        } else {
            curNode.value = v;
        }
        return prevVal;
    }

    public boolean containsKey(Key key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Value val = this.get(key);
        if (val == null) {
            return false;
        }
        return true;
    }

    public Set<Key> keySet() {
        Set<Key> keySet = new HashSet<>();
        for (int i = 0; i < nodes.length; i++) {
            Node<Key, Value> curNode = this.nodes[i];
            if (curNode != null) {
                keySet.add(curNode.key);
                while (curNode.next != null) {
                    curNode = curNode.next;
                    keySet.add(curNode.key);
                }
            }
        }
        return Collections.unmodifiableSet(keySet);
    }

    public Collection<Value> values() {
        Collection<Value> valueSet = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            Node<Key, Value> curNode = this.nodes[i];
            if (curNode != null) {
                valueSet.add(curNode.value);
                while (curNode.next != null) {
                    curNode = curNode.next;
                    valueSet.add(curNode.value);
                }
            }
        }
        return Collections.unmodifiableCollection(valueSet);
    }

    public int size() {
        return this.size;
    }

    private int getHashIndex(Key key) {
        return (key.hashCode() & 0x7fffffff) % this.nodes.length;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Key key : this.keySet()) {
            result = 31 * result + key.hashCode();
        }
        return Math.abs(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        HashTableImpl<Key, Value> table = (HashTableImpl<Key, Value>) obj;
        if (this.size() != table.size()) {
            return false;
        }
        for (int i = 0; i < this.nodes.length; i++) {
            Node<Key, Value> currentNode = this.nodes[i];
            while (currentNode != null) {
                Value otherValue = table.get(currentNode.key);
                if (otherValue == null || !otherValue.equals(currentNode.value)) {
                    return false;
                }
                currentNode = currentNode.next;
            }
        }
        return true;
    }
}
