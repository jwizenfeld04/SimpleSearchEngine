package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.stage2.DocumentStore;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage2.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI, Document> store;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
    }

    public String setMetadata(URI uri, String key, String value) {
        Document doc = getDocument(uri, key);
        return doc.setMetadataValue(key, value);
    }

    public String getMetadata(URI uri, String key) {
        Document doc = getDocument(uri, key);
        return doc.getMetadataValue(key);
    }

    private Document getDocument(URI uri, String key) {
        Document doc = this.store.get(uri);
        if (uri == null || uri.getPath().isBlank() || doc == null || key == null || key.isBlank()) {
            throw new IllegalArgumentException();
        }
        return doc;
    }

    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || uri.getPath().isBlank() || format == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            Document checkDocument = this.get(uri);
            if (checkDocument == null) {
                return 0;
            }
            int deletedDocHashcode = checkDocument.hashCode();
            this.delete(uri);
            return deletedDocHashcode;
        }
        byte[] bytes = input.readAllBytes();
        Document doc;
        if (format == DocumentFormat.TXT) {
            String txt = new String(bytes);
            doc = new DocumentImpl(uri, txt);
        } else {
            doc = new DocumentImpl(uri, bytes);
        }

        Document oldDoc = this.store.put(uri, doc);
        if (oldDoc == null) {
            return 0;
        }
        return oldDoc.hashCode();
    }

    public Document get(URI url) {
        return this.store.get(url);
    }

    public boolean delete(URI url) {
        Document doc = this.store.put(url, null);
        if (doc == null) {
            return false;
        } else {
            return true;
        }
    }
}
