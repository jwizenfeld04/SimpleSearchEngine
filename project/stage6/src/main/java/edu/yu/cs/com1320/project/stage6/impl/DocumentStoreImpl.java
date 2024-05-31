package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;
import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;

import java.io.File;
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
    private BTree<URI, Document> store;
    private Stack<Undoable> commandStack;
    private Trie<URI> documentTrie;
    private Trie<TrieNode> metaDataTrie;
    private MinHeap<MinHeapNode> minHeap;
    private Set<URI> uriSet;
    private Set<URI> uriOnDiskSet;
    private int maxDocumentCount;
    private int maxDocumentBytes;
    private int totalDocumentBytes;
    private int totalDocCount;

    public DocumentStoreImpl() {
        this(null);
    }

    public DocumentStoreImpl(File baseDir) {
        this.store = new BTreeImpl<>();
        this.store.setPersistenceManager(new DocumentPersistenceManager(baseDir));
        this.commandStack = new StackImpl<>();
        this.documentTrie = new TrieImpl<>();
        this.metaDataTrie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.uriSet = new HashSet<>();
        this.uriOnDiskSet = new HashSet<>();
        // Default them to Max Integer because primitive types can't be null
        this.maxDocumentCount = Integer.MAX_VALUE;
        this.maxDocumentBytes = Integer.MAX_VALUE;

        this.totalDocCount = 0;
        this.totalDocumentBytes = 0;
    }

    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || uri.getPath().isBlank() || format == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            // Uses this method to prevent kicking out documents during the get
            Document checkDocument = getDoc(uri);
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
        boolean onDisk = this.uriOnDiskSet.contains(uri);
        // Document storage is adjusted in addDoc method
        Document doc = addDoc(uri, format, bytes);
        if (getDoc(uri) != null) {
            removeWordsFromTrie(uri);
            removeMetadataFromTrie(uri);
        }
        Document oldDoc = this.store.put(uri, doc);
        addWordsToTrie(uri);
        addMetadataToTrie(uri);
        handleOldDoc(uri, oldDoc, onDisk);
        Document docMovedToDisk = null;
        while (this.isOverStorageLimit()) {
            docMovedToDisk = this.storageOverflowDelete();
        }
        addPutUndoCommand(uri, oldDoc, docMovedToDisk, onDisk);
        return oldDoc == null ? 0 : oldDoc.hashCode();
    }

    private void handleOldDoc(URI uri, Document oldDoc, boolean onDisk) {
        // If oldDoc exists then remove the document count just added from addDoc
        this.uriOnDiskSet.remove(uri);
        if (oldDoc != null && onDisk == false) {
            this.totalDocCount -= 1;
            this.totalDocumentBytes -= this.getDocumentByteAmount(oldDoc);
            // In case it was on disk, it shouldn't be anymore

        } else {
            // New document and needs to be added to MinHeap
            // oldDoc will have same uri so no change to MinHeap
            this.minHeap.insert(new MinHeapNode(uri));
        }
        // If oldDoc is null, the ByteAmount is 0
        // Don't need to set the lastUsedTime again since it is set in constructor of document
        this.minHeap.reHeapify(new MinHeapNode(uri));
    }

    private void addPutUndoCommand(URI uri, Document oldDoc, Document docMovedToDisk, boolean onDisk) {
        addGenericCommand(uri, (ignoredDoc) -> {
            if (getDocumentByteAmount(oldDoc) > this.maxDocumentBytes) {
                return; //TODO: Make sure Exception shouldn't be thrown
            }
            removeWordsFromTrie(uri);
            removeMetadataFromTrie(uri);
            // Must come before put because, method calls B-Tree get
            if (oldDoc == null) {
                deleteURIFromHeap(uri);
            }
            Document replacedDoc = this.store.put(uri, oldDoc);
            this.totalDocumentBytes -= this.getDocumentByteAmount(replacedDoc);
            this.totalDocCount -= 1;
            if (oldDoc != null) {
                addWordsToTrie(uri);
                addMetadataToTrie(uri);
                if (onDisk) {
                    try {
                        this.store.moveToDisk(uri);
                        this.uriOnDiskSet.add(uri);
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                } else {
                    this.uriOnDiskSet.remove(uri);
                    this.totalDocCount += 1;
                    this.totalDocumentBytes += this.getDocumentByteAmount(oldDoc);
                    this.setNanoTimeAndReheapify(oldDoc);
                }
                while (this.isOverStorageLimit()) {
                    this.storageOverflowDelete();
                }
            } else {
                this.uriSet.remove(uri);
            }
            if (docMovedToDisk != null) {
                checkAndAddDocumentBackToStorage(docMovedToDisk);
            }
        });
    }

    private Document addDoc(URI uri, DocumentFormat format, byte[] bytes) {
        Document doc;
        if (format == DocumentFormat.TXT) {
            String txt = new String(bytes);
            doc = new DocumentImpl(uri, txt, null);
        } else {
            doc = new DocumentImpl(uri, bytes);
        }
        this.totalDocumentBytes += this.getDocumentByteAmount(doc);
        this.totalDocCount += 1;
        this.uriSet.add(uri);
        return doc;
    }

    public Document get(URI url) throws IOException {
        Document doc = getDoc(url);
        if (getDocumentByteAmount(doc) > this.maxDocumentBytes) {
            return null; //TODO: Not sure how to handle this case
        }
        if (doc != null) {
            checkAndAddDocumentBackToStorage(doc);
            this.setNanoTimeAndReheapify(doc);
        }
        return doc;
    }

    private Document getDoc(URI url) {
        return this.store.get(url);
    }

    public String setMetadata(URI uri, String key, String value) throws IOException {
        boolean onDisk = this.uriOnDiskSet.contains(uri);
        // Has to be before getDocument bc that will bring back to disk
        Document doc = getDocument(uri, key);
        String oldVal = doc.setMetadataValue(key, value);
        this.metaDataTrie.put(key, new TrieNode(uri, value));
        Document docMovedToDisk = checkAndAddDocumentBackToStorage(doc);
        setMetaDataUndoCommand(uri, doc, docMovedToDisk, key, oldVal, onDisk);
        this.setNanoTimeAndReheapify(doc);
        return oldVal;
    }

    private void setMetaDataUndoCommand(URI uri, Document doc, Document docMovedToDisk, String key, String oldVal,
            boolean onDisk) {
        addGenericCommand(uri, (ignoredVal) -> {
            String originalVal = doc.setMetadataValue(key, oldVal);
            this.metaDataTrie.delete(key, new TrieNode(uri, originalVal));
            if (oldVal != null) {
                this.metaDataTrie.put(key, new TrieNode(uri, oldVal));
            }
            if (onDisk) {
                try {
                    this.store.moveToDisk(uri);
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            } else {
                checkAndAddDocumentBackToStorage(doc);
                this.setNanoTimeAndReheapify(doc);
            }
            if (docMovedToDisk != null) {
                checkAndAddDocumentBackToStorage(docMovedToDisk);
            }
        });
    }

    public String getMetadata(URI uri, String key) throws IOException {
        // getDocument adds from disk if neccessary
        Document doc = getDocument(uri, key);
        checkAndAddDocumentBackToStorage(doc);
        if (doc != null) {
            this.setNanoTimeAndReheapify(doc);
        }
        return doc.getMetadataValue(key);
    }

    public List<Document> search(String keyword) throws IOException {
        List<Document> documents = this.searchHelper(keyword);
        for (Document doc : documents) {
            checkAndAddDocumentBackToStorage(doc);
        }
        List<Document> documentsForReheapify = new ArrayList<>(documents);
        documentsForReheapify.removeIf(d -> (this.uriOnDiskSet.contains(d.getKey())));
        this.setNanoTimeAndReheapify(documentsForReheapify);
        return documents;
    }

    private List<Document> searchHelper(String keyword) {
        List<Document> documents = new ArrayList<>();
        List<URI> uris = this.documentTrie.getSorted(keyword, new Comparator<URI>() {
            @Override
            public int compare(URI u1, URI u2) {
                return (store.get(u2).wordCount(keyword) - store.get(u1).wordCount(keyword));
            }
        });
        for (URI uri : uris) {
            documents.add(this.store.get(uri));
        }
        return documents;
    }

    public List<Document> searchByPrefix(String keywordPrefix) throws IOException {
        List<Document> documents = this.searchByPrefixHelper(keywordPrefix);
        for (Document doc : documents) {
            checkAndAddDocumentBackToStorage(doc);
        }
        List<Document> documentsForReheapify = new ArrayList<>(documents);
        documentsForReheapify.removeIf(d -> (this.uriOnDiskSet.contains(d.getKey())));
        this.setNanoTimeAndReheapify(documentsForReheapify);
        return documents;
    }

    private List<Document> searchByPrefixHelper(String keywordPrefix) {
        List<Document> documents = new ArrayList<>();
        List<URI> uris = this.documentTrie.getAllWithPrefixSorted(keywordPrefix, new Comparator<URI>() {
            @Override
            public int compare(URI u1, URI u2) {
                return (prefixWordCount(u2, keywordPrefix) - prefixWordCount(u1, keywordPrefix));
            }
        });
        for (URI uri : uris) {
            documents.add(this.store.get(uri));
        }
        return documents;
    }

    public List<Document> searchByMetadata(Map<String, String> keysValues) throws IOException {
        List<Document> searchDocuments = this.searchByMetadataHelper(keysValues);
        for (Document doc : searchDocuments) {
            checkAndAddDocumentBackToStorage(doc);
        }
        List<Document> documentsForReheapify = new ArrayList<>(searchDocuments);
        documentsForReheapify.removeIf(d -> (this.uriOnDiskSet.contains(d.getKey())));
        this.setNanoTimeAndReheapify(documentsForReheapify);
        return searchDocuments;
    }

    private List<Document> searchByMetadataHelper(Map<String, String> keysValues) {
        List<Document> searchDocuments = new ArrayList<>();
        Set<URI> searchURIs = new HashSet<>();
        if (keysValues == null || keysValues.isEmpty()) {
            return searchDocuments;
        }
        for (String key : keysValues.keySet()) {
            Set<TrieNode> nodes = this.metaDataTrie.get(key);
            for (TrieNode node : nodes) {
                boolean contains = true;
                String docValue = node.getValue();
                String keyValue = keysValues.get(key);
                if (docValue == null || keyValue == null || !docValue.equals(keyValue)) {
                    contains = false;
                    searchURIs.remove(node.getURI());
                    break;
                }
                if (contains) {
                    searchURIs.add(node.getURI());
                }
            }
        }
        for (URI uri : searchURIs) {
            searchDocuments.add(this.store.get(uri));
        }
        return searchDocuments;
    }

    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues)
            throws IOException {
        List<Document> searchDocuments = searchByKeywordAndMetadataHelper(keyword, keysValues);
        for (Document doc : searchDocuments) {
            checkAndAddDocumentBackToStorage(doc);
        }
        List<Document> documentsForReheapify = new ArrayList<>(searchDocuments);
        documentsForReheapify.removeIf(d -> (this.uriOnDiskSet.contains(d.getKey())));
        this.setNanoTimeAndReheapify(documentsForReheapify);
        return searchDocuments;
    }

    private List<Document> searchByKeywordAndMetadataHelper(String keyword, Map<String, String> keysValues)
            throws IOException {
        List<Document> searchDocuments = new ArrayList<>();
        List<Document> keywordDocuments = this.searchHelper(keyword);
        List<Document> metadataDocuments = this.searchByMetadataHelper(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        return searchDocuments;
    }

    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues)
            throws IOException {
        List<Document> searchDocuments = searchByPrefixAndMetadataHelper(keywordPrefix, keysValues);
        for (Document doc : searchDocuments) {
            checkAndAddDocumentBackToStorage(doc);
        }
        List<Document> documentsForReheapify = new ArrayList<>(searchDocuments);
        documentsForReheapify.removeIf(d -> (this.uriOnDiskSet.contains(d.getKey())));
        this.setNanoTimeAndReheapify(documentsForReheapify);
        return searchDocuments;
    }

    private List<Document> searchByPrefixAndMetadataHelper(String keywordPrefix, Map<String, String> keysValues)
            throws IOException {
        List<Document> searchDocuments = new ArrayList<>();
        List<Document> keywordDocuments = this.searchByPrefixHelper(keywordPrefix);
        List<Document> metadataDocuments = this.searchByMetadataHelper(keysValues);
        for (Document doc : keywordDocuments) {
            if (keywordDocuments.contains(doc) && metadataDocuments.contains(doc)) {
                searchDocuments.add(doc);
            }
        }
        return searchDocuments;
    }

    public boolean delete(URI url) {
        // Handles double delete
        if (!this.uriOnDiskSet.contains(url) && this.store.get(url) == null) {
            return false;
        }
        removeWordsFromTrie(url);
        removeMetadataFromTrie(url);
        this.uriSet.remove(url);
        this.deleteURIFromHeap(url);
        Document doc = this.store.put(url, null);
        if (doc == null) {
            // Document doesn't exist in Btree or on disk
            return false;
        } else {
            // Document was serialized from disk, memory was never added back
            boolean onDisk = this.uriOnDiskSet.contains(url);
            if (onDisk) {
                this.uriOnDiskSet.remove(url);
            }
            // Document existed in BTree memomry, so remove it
            else {
                this.totalDocumentBytes -= this.getDocumentByteAmount(doc);
                this.totalDocCount -= 1;
            }
            deleteUndoCommand(url, doc, onDisk);
            return true;
        }
    }

    private void deleteUndoCommand(URI url, Document doc, boolean onDisk) {
        addGenericCommand(url, (ignoredDoc) -> {
            if (getDocumentByteAmount(doc) > this.maxDocumentBytes) {
                return; //TODO: Is Exception thrown for this, stay on disk, or nothing
            }
            // Adding to Trie is done regardless of memory or disk
            // First put document in store and then move to disk if needed
            this.store.put(url, doc);
            addWordsToTrie(url);
            addMetadataToTrie(url);
            if (onDisk) {
                try {
                    this.store.moveToDisk(url);
                    this.uriOnDiskSet.add(url);
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            } else {
                this.totalDocumentBytes += this.getDocumentByteAmount(doc);
                this.totalDocCount += 1;
                while (this.isOverStorageLimit()) {
                    this.storageOverflowDelete();
                }
                this.minHeap.insert(new MinHeapNode(url));
                this.setNanoTimeAndReheapify(doc);
            }
        });
    }

    public Set<URI> deleteAll(String keyword) {
        Set<URI> uriSet = this.documentTrie.deleteAll(keyword);
        if (uriSet.isEmpty()) {
            return uriSet;
        }
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> uriSet = this.documentTrie.deleteAllWithPrefix(keywordPrefix);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) throws IOException {
        List<Document> documents = this.searchByMetadataHelper(keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) throws IOException {
        List<Document> documents = this.searchByKeywordAndMetadataHelper(keyword, keysValues);
        Set<Document> documentSet = new HashSet<>();
        documentSet.addAll(documents);
        Set<URI> uriSet = getUriFromDocuments(documentSet);
        return deleteAllCommanSet(uriSet);
    }

    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues)
            throws IOException {
        List<Document> documents = this.searchByPrefixAndMetadataHelper(keywordPrefix, keysValues);
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
        if (this.maxDocumentCount < this.totalDocCount) {
            while (this.maxDocumentCount < this.totalDocCount) {
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

    private Document storageOverflowDelete() {
        MinHeapNode node = this.minHeap.remove();
        Document doc = node.getDocument();
        URI url = doc.getKey();
        try {
            this.store.moveToDisk(url);
            this.uriOnDiskSet.add(url);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
        this.store.put(url, null);
        this.totalDocumentBytes -= getDocumentByteAmount(doc);
        this.totalDocCount -= 1;
        return doc;
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

    private int prefixWordCount(URI uri, String prefix) {
        int count = 0;
        for (String word : this.store.get(uri).getWords()) {
            if (word.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    private void removeWordsFromTrie(URI uri) {
        for (String word : this.store.get(uri).getWords()) {
            this.documentTrie.delete(word, uri);
        }
    }

    private void removeMetadataFromTrie(URI uri) {
        Document doc = this.store.get(uri);
        for (String word : doc.getMetadata().keySet()) {
            this.metaDataTrie.delete(word, new TrieNode(uri, doc.getMetadataValue(word)));
        }
    }

    private void addWordsToTrie(URI uri) {
        for (String word : this.store.get(uri).getWords()) {
            this.documentTrie.put(word, uri);
        }
    }

    private void addMetadataToTrie(URI uri) {
        Document doc = this.store.get(uri);
        for (String key : doc.getMetadata().keySet()) {
            this.metaDataTrie.put(key, new TrieNode(uri, doc.getMetadataValue(key)));
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
        removeMetadataFromTrie(uri);
        this.uriSet.remove(uri);
        this.deleteURIFromHeap(uri);
        Document oldDoc = this.store.put(uri, null);
        boolean onDisk = this.uriOnDiskSet.contains(uri);
        if (onDisk) {
            this.uriOnDiskSet.remove(uri);
        } else {
            this.totalDocumentBytes -= this.getDocumentByteAmount(oldDoc);
            this.totalDocCount -= 1;
        }
        GenericCommand<URI> command = commandsetDeleteUndo(uri, oldDoc, onDisk);
        return command;
    }

    private GenericCommand<URI> commandsetDeleteUndo(URI uri, Document oldDoc, boolean onDisk) {
        GenericCommand<URI> command = new GenericCommand<URI>(uri, (ignoredDoc) -> {
            if (getDocumentByteAmount(oldDoc) > this.maxDocumentBytes) {
                return;
            }
            this.store.put(uri, oldDoc);
            addWordsToTrie(uri);
            addMetadataToTrie(uri);
            if (onDisk) {
                try {
                    this.store.moveToDisk(uri);
                    this.uriOnDiskSet.add(uri);
                } catch (IOException e) {
                    e.printStackTrace(System.out);
                }
            } else {
                this.totalDocumentBytes += this.getDocumentByteAmount(oldDoc);
                this.totalDocCount += 1;
                while (this.isOverStorageLimit()) {
                    this.storageOverflowDelete();
                }
                this.minHeap.insert(new MinHeapNode(uri));
                this.setNanoTimeAndReheapify(oldDoc);
            }
        });
        return command;
    }

    private void setNanoTimeAndReheapify(Document doc) {
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.reHeapify(new MinHeapNode(doc.getKey()));
    }

    private void setNanoTimeAndReheapify(List<Document> documents) {
        long time = System.nanoTime();
        for (Document doc : documents) {
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(new MinHeapNode(doc.getKey()));
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
        boolean isCountOverflow = this.totalDocCount > this.maxDocumentCount;
        if (isByteOverflow || isCountOverflow) {
            return true;
        }
        return false;
    }

    private void deleteURIFromHeap(URI url) {
        if (this.uriOnDiskSet.contains(url)) {
            return;
        }
        MinHeapNode node = this.minHeap.peek();
        long time = node.getDocument().getLastUseTime();
        this.store.get(url).setLastUseTime(time - 1000);
        this.minHeap.reHeapify(new MinHeapNode(url));
        this.minHeap.remove();
    }

    private Document checkAndAddDocumentBackToStorage(Document doc) {
        URI url = doc.getKey();
        Document pushedDoc = null;
        if (this.uriOnDiskSet.contains(url)) {
            // getDoc is used to make sure it's deserialized if being added back from disk
            this.getDoc(url);
            this.uriOnDiskSet.remove(url);
            this.totalDocCount += 1;
            this.totalDocumentBytes += getDocumentByteAmount(doc);
            this.minHeap.insert(new MinHeapNode(url));
            setNanoTimeAndReheapify(doc);
            while (this.isOverStorageLimit()) {
                pushedDoc = this.storageOverflowDelete();
            }
        }
        return pushedDoc;
    }

    private class MinHeapNode implements Comparable<MinHeapNode> {
        URI uri;

        private MinHeapNode(URI uri) {
            this.uri = uri;
        }

        private Document getDocument() {
            return store.get(uri);
        }

        @Override
        public int compareTo(MinHeapNode node) {
            return this.getDocument().compareTo(node.getDocument());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            MinHeapNode compNode = (MinHeapNode) obj;
            if (this.uri.equals(compNode.uri)) {
                return true;
            }
            return false;
        }
    }

    private class TrieNode implements Comparable<TrieNode> {
        URI uri;
        String value;

        private TrieNode(URI uri, String value) {
            this.uri = uri;
            this.value = value;
        }

        private URI getURI() {
            return this.uri;
        }

        private String getValue() {
            return this.value;
        }

        @Override
        public int compareTo(TrieNode node) {
            return this.getURI().compareTo(node.getURI());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            TrieNode compNode = (TrieNode) obj;
            return uri.equals(compNode.uri);
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }
}
