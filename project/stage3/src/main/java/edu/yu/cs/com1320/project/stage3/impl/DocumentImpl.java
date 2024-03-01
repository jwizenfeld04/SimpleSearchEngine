package edu.yu.cs.com1320.project.stage3.impl;

import java.net.URI;
import java.util.Arrays;

import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;

public class DocumentImpl implements Document {
    private URI uri;
    private String txt;
    private byte[] binaryData;
    private HashTable<String, String> metaData;

    public DocumentImpl(URI uri, String txt) {
        commonConstructor(uri);
        if (nullOrEmptyString(txt)) {
            throw new IllegalArgumentException();
        }
        this.txt = txt;
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        commonConstructor(uri);
        if (binaryData == null || binaryData.length == 0) {
            throw new IllegalArgumentException();
        }
        this.binaryData = binaryData;
    }

    private void commonConstructor(URI uri) {
        if (uri == null || uri.getPath().isBlank()) {
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.metaData = new HashTableImpl<>();
    }

    private boolean nullOrEmptyString(String str) {
        if (str == null || str.isBlank()) {
            return true;
        }
        return false;
    }

    public String setMetadataValue(String key, String value) {
        if (nullOrEmptyString(key)) {
            throw new IllegalArgumentException();
        }
        return this.metaData.put(key, value);
    }

    public String getMetadataValue(String key) {
        if (nullOrEmptyString(key)) {
            throw new IllegalArgumentException();
        }
        return this.metaData.get(key);
    }

    public HashTable<String, String> getMetadata() {
        HashTable<String, String> copy = new HashTableImpl<>();
        for (String key : this.metaData.keySet()) {
            copy.put(key, this.metaData.get(key));
        }
        return copy;
    }

    public String getDocumentTxt() {
        return this.txt;
    }

    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    public URI getKey() {
        return this.uri;
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (this.txt != null ? this.txt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(this.binaryData);
        return Math.abs(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        DocumentImpl doc = (DocumentImpl) obj;
        if (doc.uri.equals(this.uri) && doc.txt.equals(this.txt) && Arrays.equals(doc.binaryData, this.binaryData)
                && doc.metaData.equals(this.metaData)) {
            return true;
        } else {
            return false;
        }
    }
}