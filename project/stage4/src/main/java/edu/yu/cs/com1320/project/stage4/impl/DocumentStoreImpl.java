package edu.yu.cs.com1320.project.stage4.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI, Document> store;
    private Stack<Undoable> commandStack;
    private Trie<Document> documentTrie;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
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
        Document doc = addDoc(uri, format, bytes);
        Document oldDoc = this.store.put(uri, doc);
        addGenericCommand(uri, (ignoredDoc) -> {
            removeWordsFromTrie(uri);
            this.store.put(uri, oldDoc);
        });
        return oldDoc == null ? 0 : oldDoc.hashCode();
    }

    private Document addDoc(URI uri, DocumentFormat format, byte[] bytes) {
        Document doc;
        if (format == DocumentFormat.TXT) {
            String txt = new String(bytes);
            doc = new DocumentImpl(uri, txt);
            for (String word : doc.getWords()) {
                documentTrie.put(word, doc);
            }
        } else {
            doc = new DocumentImpl(uri, bytes);
        }
        return doc;
    }

    public Document get(URI url) {
        return this.store.get(url);
    }

    public String setMetadata(URI uri, String key, String value) {
        Document doc = getDocument(uri, key);
        String oldVal = doc.setMetadataValue(key, value);
        addGenericCommand(uri, (ignoredVal) -> doc.setMetadataValue(key, oldVal));
        return oldVal;
    }

    public String getMetadata(URI uri, String key) {
        Document doc = getDocument(uri, key);
        return doc.getMetadataValue(key);
    }

    public List<Document> search(String keyword) {
        return this.documentTrie.getSorted(keyword, new Comparator<Document>() {
            @Override
            public int compare(Document d1, Document d2) {
                return (d2.wordCount(keyword) - d1.wordCount(keyword));
            }
        });
    }

    public List<Document> searchByPrefix(String keywordPrefix) {
        return this.documentTrie.getAllWithPrefixSorted(keywordPrefix, new Comparator<Document>() {
            @Override
            public int compare(Document d1, Document d2) {
                return (prefixWordCount(d2, keywordPrefix) - prefixWordCount(d1, keywordPrefix));
            }
        });
    }

    public List<Document> searchByMetadata(Map<String, String> keysValues) {
        List<Document> searchDocuments = new ArrayList<>();
        if (keysValues == null || keysValues.isEmpty()) {
            return searchDocuments;
        }
        List<Document> allDocuments = getAllDocuments(this.store);
        if (allDocuments.isEmpty()) {
            return searchDocuments;
        }
        for (Document doc : allDocuments) {
            boolean contains = true;
            for (String key : keysValues.keySet()) {
                String docValue = doc.getMetadataValue(key);
                String keyValue = keysValues.get(key);

                if (docValue == null || keyValue == null || !docValue.equals(keyValue)) {
                    contains = false;
                    break;
                }
            }
            if (contains) {
                searchDocuments.add(doc);
            }
        }
        return searchDocuments;
    }

    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> searchDocuments = new ArrayList<>();
        List<Document> keywordDocuments = this.search(keyword);
        List<Document> metadataDocuments = this.searchByMetadata(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        return searchDocuments;
    }

    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> searchDocuments = new ArrayList<>();
        List<Document> keywordDocuments = this.searchByPrefix(keywordPrefix);
        List<Document> metadataDocuments = this.searchByMetadata(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        return searchDocuments;
    }

    public boolean delete(URI url) {
        // Handles double delete
        if (this.store.get(url) == null) {
            return false;
        }
        removeWordsFromTrie(url);
        Document doc = this.store.put(url, null);
        if (doc == null) {
            return false;
        } else {
            addGenericCommand(url, (ignoredDoc) -> {
                this.store.put(url, doc);
                addWordsToTrie(url);
            });
            return true;
        }
    }

    public Set<URI> deleteAll(String keyword) {
        Set<Document> documents = this.documentTrie.deleteAll(keyword);
        Set<URI> uriSet = getUriFromDocuments(documents);
        if (uriSet.isEmpty()) {
            return uriSet;
        }
        return addCommandSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> documents = this.documentTrie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> uriSet = getUriFromDocuments(documents);
        return addCommandSet(uriSet);
    }

    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) {
        List<Document> documents = this.searchByMetadata(keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return addCommandSet(uriSet);
    }

    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> documents = this.searchByKeywordAndMetadata(keyword, keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return addCommandSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> documents = this.searchByPrefixAndMetadata(keywordPrefix, keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return addCommandSet(uriSet);
    }

    public void undo() throws IllegalStateException {
        if (commandStack.size() == 0) {
            throw new IllegalStateException();
        }
        Undoable command = commandStack.pop();
        if (command instanceof GenericCommand) {
            command.undo();
        } else {
            @SuppressWarnings("unchecked")
            CommandSet<URI> commandSet = (CommandSet<URI>) command;
            commandSet.undoAll();
        }
    }

    // Have to pop to a copy stack without the target Command and then repop back into the original to maintain order
    public void undo(URI url) throws IllegalStateException {
        StackImpl<Undoable> commandStackCopy = new StackImpl<>();
        Undoable undoCommand = null;
        while (this.commandStack.size() > 0) {
            Undoable popCommand = this.commandStack.pop();
            if (popCommand instanceof GenericCommand) {
                @SuppressWarnings("unchecked")
                GenericCommand<URI> genericCommand = (GenericCommand<URI>) popCommand;
                if (genericCommand.getTarget().equals(url) && undoCommand == null) {
                    undoCommand = popCommand;
                } else {
                    commandStackCopy.push(popCommand);
                }
            } else {
                undoCommand = handleCommandSet(url, commandStackCopy, undoCommand, popCommand);
            }
        }
        while (commandStackCopy.size() > 0) {
            this.commandStack.push(commandStackCopy.pop());
        }
        if (undoCommand == null) {
            throw new IllegalStateException();
        }
        undoCommand.undo();
    }

    private Undoable handleCommandSet(URI url, StackImpl<Undoable> commandStackCopy, Undoable undoCommand,
            Undoable popCommand) {
        @SuppressWarnings("unchecked")
        CommandSet<URI> commandSet = (CommandSet<URI>) popCommand;
        for (GenericCommand<URI> genericCommand : commandSet) {
            if (genericCommand.getTarget().equals(url) && undoCommand == null) {
                undoCommand = genericCommand;

            }
        }
        if (undoCommand != null) {
            commandSet.remove(undoCommand);
        }
        if (commandSet.size() > 0) {
            commandStackCopy.push(popCommand);
        }
        return undoCommand;
    }

    private Document getDocument(URI uri, String key) {
        Document doc = this.store.get(uri);
        if (uri == null || uri.getPath().isBlank() || doc == null || key == null || key.isBlank()) {
            throw new IllegalArgumentException();
        }
        return doc;
    }

    private Set<URI> getUriFromDocuments(Set<Document> documents) {
        Set<URI> uris = new HashSet<>();
        if (documents.isEmpty()) {
            return uris;
        }
        for (Document doc : documents) {
            uris.add(doc.getKey());
        }
        return uris;
    }

    private List<Document> getAllDocuments(HashTable<URI, Document> store) {
        Set<URI> uriSet = store.keySet();
        List<Document> documents = new ArrayList<>();
        for (URI uri : uriSet) {
            documents.add(store.get(uri));
        }
        return documents;
    }

    private int prefixWordCount(Document doc, String prefix) {
        int count = 0;
        for (String word : doc.getWords()) {
            if (word.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private void removeWordsFromTrie(URI uri) {
        Document doc = this.store.get(uri);
        for (String word : doc.getWords()) {
            documentTrie.delete(word, doc);
        }
    }

    private void addWordsToTrie(URI uri) {
        Document doc = this.store.get(uri);
        for (String word : doc.getWords()) {
            documentTrie.put(word, doc);
        }
    }

    private void addGenericCommand(URI uri, Consumer<URI> undo) {
        GenericCommand<URI> command = new GenericCommand<URI>(uri, undo);
        commandStack.push(command);
    }

    private Set<URI> addCommandSet(Set<URI> uriSet) {
        if (uriSet.size() == 0) {
            return uriSet;
        }
        if (uriSet.size() == 1) {
            for (URI uri : uriSet) {
                removeWordsFromTrie(uri);
                Document oldDoc = this.store.put(uri, null);
                // Handles pushing to stack
                addGenericCommand(uri, (ignoredDoc) -> {
                    this.store.put(uri, oldDoc);
                    addWordsToTrie(uri);
                });
            }
        } else {
            CommandSet<URI> commandSet = new CommandSet<>();
            for (URI uri : uriSet) {
                removeWordsFromTrie(uri);
                Document oldDoc = this.store.put(uri, null);
                GenericCommand<URI> command = new GenericCommand<URI>(uri, (ignoredDoc) -> {
                    this.store.put(uri, oldDoc);
                    addWordsToTrie(uri);
                });
                commandSet.addCommand(command);
            }
            commandStack.push(commandSet);
        }
        return uriSet;
    }
}
