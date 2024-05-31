package edu.yu.cs.com1320.project.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.yu.cs.com1320.project.Trie;

public class TrieImpl<Value> implements Trie<Value> {
    private Node<Value> root;
    private final int ALPHABET_SIZE = 63;

    public TrieImpl() {
        this.root = new Node<Value>();
    }

    @SuppressWarnings("hiding")
    private class Node<Value> {
        private Set<Value> val;
        private Node<Value>[] links;

        @SuppressWarnings("unchecked")
        private Node() {
            this.val = new HashSet<>();
            this.links = new Node[ALPHABET_SIZE];
        }
    }

    public void put(String key, Value val) {
        if (val != null) {
            this.root = put(this.root, key, val, 0);
        }
    }

    private Node<Value> put(Node<Value> x, String key, Value val, int d) {
        //create a new node
        if (x == null) {
            x = new Node<>();
        }
        //we've reached the last node in the key, set the value for the key and return the node
        if (d == key.length()) {
            x.val.add(val);
            return x;
        }
        //proceed to the next node in the chain of nodes that forms the desired key
        char c = key.charAt(d);
        int idx = getNodeIdx(c);
        x.links[idx] = this.put(x.links[idx], key, val, d + 1);
        return x;
    }

    private int getNodeIdx(char c) {
        int idx = 0;
        if (Character.isDigit(c)) {
            idx = Character.getNumericValue(c);
        } else if (Character.isUpperCase(c)) {
            idx = c - 55;
        } else if (Character.isSpaceChar(c)) {
            idx = 62;
        } else {
            idx = c - 61;
        }
        return idx;
    }

    public List<Value> getSorted(String key, Comparator<Value> comparator) {
        if (key == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        List<Value> valueList = new ArrayList<>();
        if (key.matches(".*[^A-Za-z0-9 ].*")) {
            return valueList;
        }
        valueList.addAll(this.get(key));
        if (valueList.size() == 0) {
            return valueList;
        }
        Collections.sort(valueList, comparator);
        return valueList;
    }

    public Set<Value> get(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> valueSet = new HashSet<>();
        if (key.matches(".*[^A-Za-z0-9 ].*")) {
            return valueSet;
        }
        Node<Value> node = this.get(this.root, key, 0);
        if (node != null) {
            valueSet.addAll(node.val);
        }
        return valueSet;
    }

    private Node<Value> get(Node<Value> x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        char c = key.charAt(d);
        int idx = getNodeIdx(c);
        return this.get(x.links[idx], key, d + 1);
    }

    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if (prefix == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        Node<Value> prefixNode = this.get(this.root, prefix, 0);
        List<Value> prefixWords = new ArrayList<>();
        if (prefixNode != null) {
            Set<Value> results = new HashSet<>();
            prefixWords.addAll(getPrefixWords(prefixNode, 0, results));
            Collections.sort(prefixWords, comparator);
        }
        return prefixWords;
    }

    private Set<Value> getPrefixWords(Node<Value> rootNode, int d, Set<Value> results) {
        if (rootNode.val != null) {
            results.addAll(rootNode.val);
        }
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (rootNode.links[i] != null) {
                this.getPrefixWords(rootNode.links[i], d, results);
            }
        }
        return results;
    }

    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        Node<Value> node = this.get(this.root, prefix, 0);
        Set<Value> values = new HashSet<>();
        if (node != null) {
            this.deletePrefixNode(node, values);
            this.root = this.cleanupNode(this.root, prefix, 0);
        }
        return values;
    }

    public Set<Value> deleteAll(String key) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        Set<Value> deletedValues = new HashSet<>();
        this.root = deleteAll(this.root, key, 0, deletedValues);
        return deletedValues;
    }

    public Value delete(String key, Value val) {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        Node<Value> node = this.get(root, key, 0);
        if (node == null || !node.val.contains(val)) {
            return null;
        }
        // If node only has one val, then it uses the cleanupDelete to remove unnecessary nodes
        if (node.val.size() == 1) {
            // New Hashset isn't needed for this method, but used in other methods
            this.root = deleteAll(this.root, key, 0, new HashSet<>());
        } else {
            node.val.remove(val);
        }
        return val;
    }

    private Node<Value> deleteAll(Node<Value> node, String key, int d, Set<Value> deletedValues) {
        if (node == null) {
            return null;
        }
        if (d == key.length()) {
            deletedValues.addAll(node.val);
            node.val.clear();
        } else {
            char c = key.charAt(d);
            int idx = getNodeIdx(c);
            node.links[idx] = deleteAll(node.links[idx], key, d + 1, deletedValues);
        }

        if (!node.val.isEmpty()) {
            return node;
        }

        for (Node<Value> link : node.links) {
            if (link != null) {
                return node;
            }
        }
        // All links are null and values are cleared, so return null to delete this node.
        return null;
    }

    private Node<Value> deletePrefixNode(Node<Value> prefixNode, Set<Value> values) {
        if (prefixNode == null) {
            return null;
        }
        if (prefixNode.val != null) {
            values.addAll(prefixNode.val);
            prefixNode.val = null;
        }
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (prefixNode.links[i] != null) {
                prefixNode.links[i] = this.deletePrefixNode(prefixNode.links[i], values);
            }
        }
        if (prefixNode.val != null) {
            return prefixNode;
        }
        //otherwise, check if subtrie rooted at x is completely empty
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (prefixNode.links[i] != null) {
                return prefixNode; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    private Node<Value> cleanupNode(Node<Value> node, String key, int d) {
        if (node == null) {
            return null;
        }

        char c = key.charAt(d);
        int idx = getNodeIdx(c);
        // Loop down the root to the key - 1 to separate the link
        if (d == key.length() - 1) {
            node.links[idx] = null;
        } else {
            node.links[idx] = cleanupNode(node.links[idx], key, d + 1);
        }
        // Delete the key, if the current node has no children then delete that node
        // Recurse back until either the node has a child, a value, or is the root itself
        if (node.val != null || !node.val.isEmpty() || hasNonNullChild(node) || node == this.root) {
            return node;
        }

        return null;
    }

    private boolean hasNonNullChild(Node<Value> node) {
        for (int i = 0; i < ALPHABET_SIZE; i++) {
            if (node.links[i] != null) {
                return true;
            }
        }
        return false;
    }
}
