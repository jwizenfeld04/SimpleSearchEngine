package edu.yu.cs.com1320.project.stage1.impl;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.yu.cs.com1320.project.stage1.Document;

public class DocumentImpl implements Document {
    private URI uri;
    private String txt;
    private byte[] binaryData;
    private Map<String, String> metaData;

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
        this.metaData = new HashMap<>();
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

    public HashMap<String, String> getMetadata() {
        HashMap<String, String> copy = new HashMap<>();
        copy.putAll(this.metaData);
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
        return result;
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