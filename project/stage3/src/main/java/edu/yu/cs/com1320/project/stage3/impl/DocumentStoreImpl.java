package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.undo.Command;
import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage3.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;

public class DocumentStoreImpl implements DocumentStore {
    private HashTable<URI, Document> store;
    private Stack<Command> commandStack;

    public DocumentStoreImpl() {
        this.store = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
    }

    public String setMetadata(URI uri, String key, String value) {
        Document doc = getDocument(uri, key);
        String oldVal = doc.setMetadataValue(key, value);
        addCommandStack(uri, (ignoredVal) -> doc.setMetadataValue(key, oldVal));
        return oldVal;
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
        addCommandStack(uri, (ignoredDoc) -> this.store.put(uri, oldDoc));
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
            addCommandStack(url, (ignoredDoc) -> this.store.put(url, doc));
            return true;
        }
    }

    private void addCommandStack(URI uri, Consumer<URI> undo) {
        Command command = new Command(uri, undo);
        commandStack.push(command);
    }

    public void undo() throws IllegalStateException {
        if (commandStack.size() == 0) {
            throw new IllegalStateException();
        }
        Command command = commandStack.pop();
        command.undo();
    }

    // Have to pop to a copy stack without the target Command and then repop back into the original to maintain order
    public void undo(URI url) throws IllegalStateException {
        StackImpl<Command> commandStackCopy = new StackImpl<>();
        Command undoCommand = null;
        while (this.commandStack.size() > 0) {
            Command popCommand = this.commandStack.pop();
            if (popCommand.getUri() == url && undoCommand == null) {
                undoCommand = popCommand;
            } else {
                commandStackCopy.push(popCommand);
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
}
