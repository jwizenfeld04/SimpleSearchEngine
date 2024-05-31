package edu.yu.cs.com1320.project.stage6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.stage6.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage6.impl.DocumentStoreImpl;

public class DocumentStoreImplMemoryTest {

    private DocumentStoreImpl store;

    @BeforeEach
    void setup() {
        store = new DocumentStoreImpl();
    }

    @AfterEach
    public void cleanup() throws IOException {
        for (int i = 1; i <= 6; i++) {
            URI uri = URI.create("http://www.github.com/jwizenf" + i);
            deleteFile(uri);
        }
        deleteDirectoryIfEmpty("http://www.github.com");
    }

    private void deleteFile(URI uri) throws IOException {
        String dir = System.getProperty("user.dir");
        Path path = Paths.get(dir, uri.getHost(), uri.getPath() + ".json");
        Files.deleteIfExists(path);
    }

    private void deleteDirectoryIfEmpty(String baseUri) throws IOException {
        String dir = System.getProperty("user.dir");
        Path path = Paths.get(dir, URI.create(baseUri).getHost());
        if (Files.isDirectory(path) && Files.list(path).findAny().isEmpty()) {
            Files.delete(path);
        }
    }

    private boolean isOnDisk(URI uri) {
        String dir = System.getProperty("user.dir");
        String path = Paths.get(dir, uri.getHost(), uri.getPath() + ".json").toString();
        return Files.exists(Paths.get(path));
    }

    private int getTotalBytes(List<Document> documents) {
        int bytes = 0;
        for (Document doc : documents) {
            if (doc.getDocumentTxt() != null) {
                bytes += doc.getDocumentTxt().getBytes().length;
            } else {
                bytes += doc.getDocumentBinaryData().length;
            }
        }
        return bytes;
    }

    @Test
    public void testSetMaxDocumentLimitLessThanCurrent() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 6; i++) {
            URI uri = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri, DocumentFormat.TXT);
            input.reset();
        }

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        URI uri3 = URI.create("http://www.github.com/jwizenf3");

        this.store.setMaxDocumentCount(3);
        // Assert documents are deleted from store BTree
        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));

        assertNotNull(this.store.get(uri1));
        assertTrue(isOnDisk(uri3));
    }

    @Test
    public void testSetMaxByteLimitLessThanCurrent() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 6; i++) {
            URI uri = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri, DocumentFormat.TXT);
            input.reset();
        }

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        URI uri3 = URI.create("http://www.github.com/jwizenf3");
        this.store.setMaxDocumentBytes(110);

        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));

        assertNotNull(this.store.get(uri1));
        assertTrue(isOnDisk(uri3));
    }

    // Ensure document bytes properly track across put, delete, and undo
    @Test
    public void testDocumentBytesTrackProperly() throws IOException {
        String testString1 = "Hi my name is Jeremy Wizenfeld"; // 30 bytes
        ByteArrayInputStream input1 = new ByteArrayInputStream(testString1.getBytes());
        String testString2 = "This is test code for Stage 5 of DataStructures"; // 47 bytes
        ByteArrayInputStream input2 = new ByteArrayInputStream(testString2.getBytes());
        String testString3 = "I is ready to begin practicing LeetCode"; // 39 bytes
        ByteArrayInputStream input3 = new ByteArrayInputStream(testString3.getBytes());
        String testString4 = "Will Algorithms or Computer Organization is harder next year?"; // 61 bytes
        ByteArrayInputStream input4 = new ByteArrayInputStream(testString4.getBytes());

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input1, uri1, DocumentFormat.TXT);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        this.store.put(input2, uri2, DocumentFormat.TXT);
        URI uri3 = URI.create("http://www.github.com/jwizenf3");
        this.store.put(input3, uri3, DocumentFormat.TXT);
        URI uri4 = URI.create("http://www.github.com/jwizenf4");
        this.store.put(input4, uri4, DocumentFormat.TXT);

        assertEquals(177, getTotalBytes(this.store.search("is")));
        this.store.get(uri2);
        this.store.get(uri3);
        this.store.get(uri4);
        this.store.setMaxDocumentBytes(140);
        assertEquals(177, getTotalBytes(this.store.search("is")));

        assertTrue(isOnDisk(uri3));

        this.store.delete(uri3);
        List<Document> searchDocs = this.store.search("is");
        List<URI> searchUris = new ArrayList<>();
        for (Document doc : searchDocs) {
            searchUris.add(doc.getKey());
        }
        assertTrue(searchUris.contains(uri1));
        assertTrue(searchUris.contains(uri2));
        assertTrue(searchUris.contains(uri4));
        assertEquals(138, getTotalBytes(searchDocs));
        this.store.undo();

        assertTrue(isOnDisk(uri3));
        assertEquals(177, getTotalBytes(this.store.search("is")));
        this.store.undo();
        assertEquals(116, getTotalBytes(this.store.search("is")));
    }

    // Set Max Byte Limit and try to add another one, make sure proper one is deleted
    @Test
    public void testSetMaxByteLimitThenExceedingLimit() throws IOException {
        this.store.setMaxDocumentBytes(100);
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 5; i++) {
            URI uri = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri, DocumentFormat.TXT);
            input.reset();
        }

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        URI uri3 = URI.create("http://www.github.com/jwizenf3");

        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));

        URI uri5 = URI.create("http://www.github.com/jwizenf5");
        this.store.put(input, uri5, DocumentFormat.TXT);

        assertTrue(isOnDisk(uri3));
    }

    // Set Max Document Limit and try to add another one, make sure proper one is deleted
    @Test
    public void testSetMaxDocumentLimitThenExceedingLimit() throws IOException {
        this.store.setMaxDocumentCount(2);
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 5; i++) {
            URI uri2 = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri2, DocumentFormat.TXT);
            input.reset();
        }

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        URI uri3 = URI.create("http://www.github.com/jwizenf3");

        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));

        URI uri5 = URI.create("http://www.github.com/jwizenf5");
        this.store.put(input, uri5, DocumentFormat.TXT);

        assertTrue(isOnDisk(uri3));
    }

    // Make sure single document above limit is not put into store and throws IllegalArgumentException
    @Test
    public void testStorageSideEffectsDocumentExceedsTotalLimit() throws IOException {
        String testString1 = "I is ready to begin practicing LeetCode"; // 39 bytes
        ByteArrayInputStream input1 = new ByteArrayInputStream(testString1.getBytes());
        String testString2 = "Will Algorithms or Computer Organization is harder next year?"; // 61 bytes
        ByteArrayInputStream input2 = new ByteArrayInputStream(testString2.getBytes());

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input1, uri1, DocumentFormat.TXT);
        this.store.setMaxDocumentBytes(50);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        assertThrows(IllegalArgumentException.class, () -> this.store.put(input2, uri2, DocumentFormat.TXT));
        assertNotNull(this.store.get(uri1));
        assertNull(this.store.get(uri2));
    }

    // Make sure undo handles limits and makes space if undo will exceed limit. Undo side effects
    @Test
    public void testUndoStorageSideEffects() throws IOException {
        String testString1 = "I is ready to begin practicing LeetCode"; // 39 bytes
        ByteArrayInputStream input1 = new ByteArrayInputStream(testString1.getBytes());
        String testString2 = "Will Algorithms or Computer Organization is harder next year?"; // 61 bytes
        ByteArrayInputStream input2 = new ByteArrayInputStream(testString2.getBytes());

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input1, uri1, DocumentFormat.TXT);
        this.store.setMaxDocumentBytes(75);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        this.store.put(input2, uri2, DocumentFormat.TXT);

        assertTrue(isOnDisk(uri1));
        assertNotNull(this.store.get(uri2));

        this.store.undo();
        // URI1 should be back in memory because that was state before undo
        assertNull(this.store.get(uri2));
        assertFalse(isOnDisk(uri1));
    }

    @Test
    public void testDeleteDocumentUndoPutsBackInHeap() throws IOException {
        String testString1 = "I am ready to begin practicing LeetCode"; // 39 bytes
        ByteArrayInputStream input1 = new ByteArrayInputStream(testString1.getBytes());
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input1, uri1, DocumentFormat.TXT);
        String testString2 = "I am ready to begin practicing LeetCode2"; // 39 bytes
        ByteArrayInputStream input2 = new ByteArrayInputStream(testString2.getBytes());
        this.store.put(input2, uri1, DocumentFormat.TXT);
        this.store.undo();
        this.store.delete(uri1);
        this.store.undo();
        assertEquals(uri1, this.store.get(uri1).getKey());
    }

    @Test
    public void testMaxDocCountAfterPut() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 6; i++) {
            URI uri2 = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri2, DocumentFormat.TXT);
            input.reset();
        }
        this.store.setMaxDocumentCount(3);

        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf1")));
        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf2")));
        URI newUri = URI.create("http://www.github.com/jwizenf6");
        this.store.put(input, newUri, DocumentFormat.TXT);
        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf3")));
    }

    @Test
    public void testMaxDocCountAfterMultiDocSearch() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 6; i++) {
            URI uri2 = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri2, DocumentFormat.TXT);
            input.reset();
        }
        Map<String, String> metaDataMap = new HashMap<>();
        metaDataMap.put("hi", "bye");
        this.store.setMetadata(URI.create("http://www.github.com/jwizenf1"), "hi", "bye");
        this.store.setMetadata(URI.create("http://www.github.com/jwizenf2"), "hi", "bye");
        List<Document> searchList = this.store.searchByKeywordAndMetadata("is", metaDataMap);
        assertEquals(2, searchList.size());
        this.store.setMaxDocumentCount(4);
        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf3")));
        URI newUri = URI.create("http://www.github.com/jwizenf6");
        this.store.put(input, newUri, DocumentFormat.TXT);
        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf4")));
        this.store.setMaxDocumentCount(1);
        searchList = this.store.searchByKeywordAndMetadata("is", metaDataMap);
        assertEquals(2, searchList.size());
        assertTrue(isOnDisk(URI.create("http://www.github.com/jwizenf1")));
    }

    @Test
    public void testPreviousStateAfterUndoPut() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input, uri1, DocumentFormat.TXT);
        this.store.setMaxDocumentCount(1);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        input.reset();
        this.store.put(input, uri2, DocumentFormat.TXT);
        assertTrue(isOnDisk(uri1));
        this.store.undo();
        assertFalse(isOnDisk(uri1));
        assertNull(this.store.get(uri2));
    }

    @Test
    public void testPreviousStateAfterUndoSetMetadata() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input, uri1, DocumentFormat.TXT);
        this.store.setMaxDocumentCount(1);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        input.reset();
        this.store.put(input, uri2, DocumentFormat.TXT);
        assertTrue(isOnDisk(uri1));
        this.store.setMetadata(uri1, "key", "value");
        assertFalse(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));
        this.store.undo();
        Map<String, String> metaData = new HashMap<>();
        metaData.put("key", "value");
        assertTrue(isOnDisk(uri1));
        assertFalse(isOnDisk(uri2));
        assertFalse(this.store.searchByMetadata(metaData).contains(this.store.get(uri1)));
    }

    @Test
    public void testSearchByPrefixWithDocumentsOnDisk() throws IOException {
        this.store.setMaxDocumentCount(3);
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        for (int i = 1; i < 6; i++) {
            URI uri = URI.create("http://www.github.com/jwizenf" + i);
            this.store.put(input, uri, DocumentFormat.TXT);
            input.reset();
        }

        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        URI uri3 = URI.create("http://www.github.com/jwizenf3");

        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri2));

        List<Document> docs = this.store.searchByPrefix("na");
        assertEquals(5, docs.size());

        assertTrue(isOnDisk(uri1));
        assertTrue(isOnDisk(uri3));
    }

    @Test
    public void testPutWhenDocumentOnDiskAlready() throws IOException {
        String testString = "Hi my name is Jeremy Wizenfeld and this is a test";
        ByteArrayInputStream input = new ByteArrayInputStream(testString.getBytes());
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input, uri1, DocumentFormat.TXT);
        int hashcode = this.store.get(uri1).hashCode();
        this.store.setMaxDocumentCount(1);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        input.reset();
        this.store.put(input, uri2, DocumentFormat.TXT);
        assertTrue(isOnDisk(uri1));
        String testString2 = "Does this work?";
        ByteArrayInputStream input2 = new ByteArrayInputStream(testString2.getBytes());
        int oldHashcode = this.store.put(input2, uri1, DocumentFormat.TXT);
        assertTrue(isOnDisk(uri2));
        assertFalse(isOnDisk(uri1));
        assertEquals(hashcode, oldHashcode);
        this.store.undo();
        assertTrue(isOnDisk(uri1));
        assertFalse(isOnDisk(uri2));
    }
}
