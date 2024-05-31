package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;

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

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI, Document> store;
    private Stack<Undoable> commandStack;
    private Trie<Document> documentTrie;
    private MinHeap<Document> minHeap;
    private int maxDocumentCount;
    private int maxDocumentBytes;
    private int totalDocumentBytes;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        // Default them to Max Integer because primitive types can't be null
        this.maxDocumentCount = Integer.MAX_VALUE;
        this.maxDocumentBytes = Integer.MAX_VALUE;

        // Note that totalDocumentCount = this.store.size() so no need for additional tracking
        this.totalDocumentBytes = 0;
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
        if (bytes.length > this.maxDocumentBytes) {
            throw new IllegalArgumentException();
        }
        Document doc = addDoc(uri, format, bytes);
        Document oldDoc = this.store.put(uri, doc);
        if (oldDoc != null) {
            deleteDocumentFromHeap(oldDoc);
        }
        // If oldDoc is null, the ByteAmount is 0
        this.totalDocumentBytes -= this.getDocumentByteAmount(oldDoc);
        this.minHeap.reHeapify(doc); // Don't need to set the lastUsedTime again as it is set in constructor of document
        addPutUndoCommand(uri, oldDoc);
        while (this.isOverStorageLimit()) {
            this.storageOverflowDelete();
        }
        return oldDoc == null ? 0 : oldDoc.hashCode();
    }

    private void addPutUndoCommand(URI uri, Document oldDoc) {
        addGenericCommand(uri, (ignoredDoc) -> {
            if (getDocumentByteAmount(oldDoc) > this.maxDocumentBytes) {
                return;
            }
            removeWordsFromTrie(uri);
            Document replacedDoc = this.store.put(uri, oldDoc);
            deleteDocumentFromHeap(replacedDoc);
            this.totalDocumentBytes += this.getDocumentByteAmount(oldDoc);
            this.totalDocumentBytes -= this.getDocumentByteAmount(replacedDoc);
            if (oldDoc != null) {
                addWordsToTrie(uri);
                this.minHeap.insert(oldDoc);
                this.setNanoTimeAndReheapify(oldDoc);
            }
            while (this.isOverStorageLimit()) {
                this.storageOverflowDelete();
            }
        });
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
        this.totalDocumentBytes += this.getDocumentByteAmount(doc);
        this.minHeap.insert(doc);
        return doc;
    }

    public Document get(URI url) {
        Document doc = this.store.get(url);
        if (doc != null) {
            this.setNanoTimeAndReheapify(doc);
        }
        return doc;
    }

    public String setMetadata(URI uri, String key, String value) {
        Document doc = getDocument(uri, key);
        String oldVal = doc.setMetadataValue(key, value);
        addGenericCommand(uri, (ignoredVal) -> {
            doc.setMetadataValue(key, oldVal);
            this.setNanoTimeAndReheapify(doc);
        });
        this.setNanoTimeAndReheapify(doc);
        return oldVal;
    }

    public String getMetadata(URI uri, String key) {
        Document doc = getDocument(uri, key);
        if (doc != null) {
            this.setNanoTimeAndReheapify(doc);
        }
        return doc.getMetadataValue(key);
    }

    public List<Document> search(String keyword) {
        List<Document> documents = this.searchHelper(keyword);
        this.setNanoTimeAndReheapify(documents);
        return documents;
    }

    private List<Document> searchHelper(String keyword) {
        List<Document> documents = this.documentTrie.getSorted(keyword, new Comparator<Document>() {
            @Override
            public int compare(Document d1, Document d2) {
                return (d2.wordCount(keyword) - d1.wordCount(keyword));
            }
        });
        return documents;
    }

    public List<Document> searchByPrefix(String keywordPrefix) {
        List<Document> documents = this.searchByPrefixHelper(keywordPrefix);
        this.setNanoTimeAndReheapify(documents);
        return documents;
    }

    private List<Document> searchByPrefixHelper(String keywordPrefix) {
        List<Document> documents = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, new Comparator<Document>() {
            @Override
            public int compare(Document d1, Document d2) {
                return (prefixWordCount(d2, keywordPrefix) - prefixWordCount(d1, keywordPrefix));
            }
        });
        return documents;
    }

    public List<Document> searchByMetadata(Map<String, String> keysValues) {
        List<Document> searchDocuments = this.searchByMetadataHelper(keysValues);
        this.setNanoTimeAndReheapify(searchDocuments);
        return searchDocuments;
    }

    private List<Document> searchByMetadataHelper(Map<String, String> keysValues) {
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
        List<Document> keywordDocuments = this.searchHelper(keyword);
        List<Document> metadataDocuments = this.searchByMetadataHelper(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        this.setNanoTimeAndReheapify(searchDocuments);
        return searchDocuments;
    }

    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> searchDocuments = new ArrayList<>();
        List<Document> keywordDocuments = this.searchByPrefixHelper(keywordPrefix);
        List<Document> metadataDocuments = this.searchByMetadataHelper(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        this.setNanoTimeAndReheapify(searchDocuments);
        return searchDocuments;
    }

    public boolean delete(URI url) {
        // Handles double delete
        if (this.store.get(url) == null) {
            return false;
        }
        removeWordsFromTrie(url);
        Document doc = this.store.put(url, null);
        this.deleteDocumentFromHeap(doc);
        if (doc == null) {
            return false;
        } else {
            this.totalDocumentBytes -= this.getDocumentByteAmount(doc);
            addGenericCommand(url, (ignoredDoc) -> {
                if (getDocumentByteAmount(doc) > this.maxDocumentBytes) {
                    return;
                }
                this.store.put(url, doc);
                this.totalDocumentBytes += this.getDocumentByteAmount(doc);
                addWordsToTrie(url);
                while (this.isOverStorageLimit()) {
                    this.storageOverflowDelete();
                }
                this.minHeap.insert(doc);
                this.setNanoTimeAndReheapify(doc);
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
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> documents = this.documentTrie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> uriSet = getUriFromDocuments(documents);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) {
        List<Document> documents = this.searchByMetadata(keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> documents = this.searchByKeywordAndMetadata(keyword, keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> documents = this.searchByPrefixAndMetadata(keywordPrefix, keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return deleteAllCommanSet(uriSet);
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
            List<Document> docList = new ArrayList<>();
            for (GenericCommand<URI> cmd : commandSet) {
                URI url = (URI) cmd.getTarget();
                docList.add(this.store.get(url));
            }
            this.setNanoTimeAndReheapify(docList);
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

    public void setMaxDocumentCount(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException();
        }
        this.maxDocumentCount = limit;
        if (this.maxDocumentCount < this.store.size()) {
            while (this.maxDocumentCount < this.store.size()) {
                storageOverflowDelete();
            }
        }
    }

    public void setMaxDocumentBytes(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException();
        }
        this.maxDocumentBytes = limit;
        if (this.maxDocumentBytes < this.totalDocumentBytes) {
            while (this.maxDocumentBytes < this.totalDocumentBytes) {
                storageOverflowDelete();
            }
        }
    }

    private void storageOverflowDelete() {
        Document doc = this.minHeap.remove();
        URI url = doc.getKey();
        removeWordsFromTrie(url);
        this.store.put(url, null);
        this.totalDocumentBytes -= getDocumentByteAmount(doc);
        removeDocFromCommandStack(url);
    }

    private void removeDocFromCommandStack(URI url) {
        Stack<Undoable> tempStack = new StackImpl<>();
        while (this.commandStack.size() > 0) {
            Undoable tempCommand = this.commandStack.pop();
            if (tempCommand instanceof GenericCommand) {
                @SuppressWarnings("unchecked")
                GenericCommand<URI> genericCommand = (GenericCommand<URI>) tempCommand;
                if (!genericCommand.getTarget().equals(url)) {
                    tempStack.push(genericCommand);
                }
            } else {
                @SuppressWarnings("unchecked")
                CommandSet<URI> commandSet = (CommandSet<URI>) tempCommand;
                while (commandSet.iterator().hasNext()) {
                    if (commandSet.iterator().next().getTarget().equals(url)) {
                        commandSet.iterator().remove();
                    }
                }
                tempStack.push(commandSet);
            }
        }
        while (tempStack.size() > 0) {
            this.commandStack.push(tempStack.pop());
        }
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

    private Set<URI> deleteAllCommanSet(Set<URI> uriSet) {
        if (uriSet.size() == 0) {
            return uriSet;
        }
        if (uriSet.size() == 1) {
            for (URI uri : uriSet) {
                GenericCommand<URI> command = deleteDocAndGetCommand(uri);
                commandStack.push(command);
            }
        } else {
            CommandSet<URI> commandSet = new CommandSet<>();
            for (URI uri : uriSet) {
                GenericCommand<URI> command = deleteDocAndGetCommand(uri);
                commandSet.addCommand(command);
            }
            commandStack.push(commandSet);
        }
        return uriSet;
    }

    private GenericCommand<URI> deleteDocAndGetCommand(URI uri) {
        removeWordsFromTrie(uri);
        Document oldDoc = this.store.put(uri, null);
        this.deleteDocumentFromHeap(oldDoc);
        this.totalDocumentBytes -= this.getDocumentByteAmount(oldDoc);
        GenericCommand<URI> command = new GenericCommand<URI>(uri, (ignoredDoc) -> {
            if (getDocumentByteAmount(oldDoc) > this.maxDocumentBytes) {
                return;
            }
            this.store.put(uri, oldDoc);
            this.totalDocumentBytes += this.getDocumentByteAmount(oldDoc);
            addWordsToTrie(uri);
            while (this.isOverStorageLimit()) {
                this.storageOverflowDelete();
            }
            this.minHeap.insert(oldDoc);
            this.setNanoTimeAndReheapify(oldDoc);
        });
        return command;
    }

    private void setNanoTimeAndReheapify(Document doc) {
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.reHeapify(doc);
    }

    private void setNanoTimeAndReheapify(List<Document> documents) {
        long time = System.nanoTime();
        for (Document doc : documents) {
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(doc);
        }
    }

    private int getDocumentByteAmount(Document doc) {
        if (doc == null) {
            return 0;
        }
        String documentText = doc.getDocumentTxt();
        int documentBytes = 0;
        if (documentText != null) {
            documentBytes = documentText.getBytes().length;
        } else {
            documentBytes = doc.getDocumentBinaryData().length;
        }
        return documentBytes;
    }

    private boolean isOverStorageLimit() {
        boolean isByteOverflow = this.totalDocumentBytes > this.maxDocumentBytes;
        boolean isCountOverflow = this.store.size() > this.maxDocumentCount;
        if (isByteOverflow || isCountOverflow) {
            return true;
        }
        return false;
    }

    private Document deleteDocumentFromHeap(Document doc) {
        Document topDoc = this.minHeap.peek();
        long time = topDoc.getLastUseTime();
        doc.setLastUseTime(time - 1000);
        this.minHeap.reHeapify(doc);
        return this.minHeap.remove();
    }
}
