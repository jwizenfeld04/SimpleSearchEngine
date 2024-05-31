package edu.yu.cs.com1320.project.stage6;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.yu.cs.com1320.project.stage6.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage6.impl.DocumentStoreImpl;

public class DocumentStoreImplTest {

    private URI uri;
    private byte[] binaryData = { 65, 66, 67, 68, 69 };
    private DocumentStoreImpl store;

    @BeforeEach
    void setup() {
        uri = URI.create("http://www.github.com/jwizenf2");
        store = new DocumentStoreImpl();
    }

    @Test
    public void testValidGetDocument() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        store.put(input, uri, DocumentFormat.TXT);
        Document doc = store.get(uri);
        assertEquals("ABCDE", doc.getDocumentTxt());

        input.reset();
        URI uri2 = URI.create("http://www.github.com/jwizenf");
        store.put(input, uri2, DocumentFormat.BINARY);
        Document docBinary = store.get(uri2);
        boolean equalByteArrays = Arrays.equals(binaryData, docBinary.getDocumentBinaryData());
        assertTrue(equalByteArrays);
    }

    @Test
    public void testValidDeleteDocument() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        store.put(input, uri, DocumentFormat.TXT);
        Document doc = store.get(uri);
        assertTrue(doc.getKey().toString().equals("http://www.github.com/jwizenf2"));
        boolean delete = store.delete(uri);
        assertTrue(delete);
        assertNull(store.get(uri));
    }

    @Test
    public void testSetMetaDataInvalid() throws IOException {
        String key = "test";
        String value = "test2";
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        assertThrows(IllegalArgumentException.class, () -> {
            store.setMetadata(uri, key, value);
        });
        this.store.put(input, uri, DocumentFormat.TXT);
        String oldValue = store.setMetadata(uri, key, value);
        assertNull(oldValue);
    }

    @Test
    public void testInvalidPutDocument() throws IOException {
        InputStream input = new ByteArrayInputStream(new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            store.put(input, uri, DocumentFormat.TXT);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            store.put(input, null, DocumentFormat.TXT);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            store.put(input, uri, null);
        });

        CustomInputStream input2 = new CustomInputStream();
        assertThrows(IOException.class, () -> {
            store.put(input2, uri, DocumentFormat.TXT);
        });
    };

    @Test
    public void testNullPutDocument() throws IOException {
        ByteArrayInputStream input2 = new ByteArrayInputStream(binaryData);

        store.put(input2, uri, DocumentFormat.TXT);
        int docHashCode = store.get(uri).hashCode();
        int oldDocHashCode = store.put(null, uri, DocumentFormat.TXT);

        assertEquals(docHashCode, oldDocHashCode);
    };

    class CustomInputStream extends InputStream {
        @Override
        public byte[] readAllBytes() throws IOException {
            throw new IOException("Simulate IOException");
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(byte[] b) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testUndoDelete() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        Document doc = this.store.get(uri);
        assertEquals("ABCDE", doc.getDocumentTxt());
        this.store.delete(uri);
        doc = this.store.get(uri);
        assertNull(doc);
        this.store.undo();
        doc = this.store.get(uri);
        assertEquals("ABCDE", doc.getDocumentTxt());
    }

    @Test
    public void testUndoSpecificDocumentDelete() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        input.reset();
        URI uri2 = URI.create("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        input.reset();
        URI uri3 = URI.create("http://www.github.com/jwizenf4");
        this.store.put(input, uri3, DocumentFormat.TXT);
        input.reset();
        URI uri4 = URI.create("http://www.github.com/jwizenf5");
        this.store.put(input, uri4, DocumentFormat.TXT);
        this.store.delete(uri);
        this.store.delete(uri2);
        this.store.delete(uri4);
        assertEquals("ABCDE", this.store.get(uri3).getDocumentTxt());
        assertNull(this.store.get(uri));
        assertNull(this.store.get(uri2));
        this.store.undo(uri);
        assertNull(this.store.get(uri2));
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        this.store.undo(uri2);
        assertEquals("ABCDE", this.store.get(uri2).getDocumentTxt());
    }

    @Test
    public void testUndoPut() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        input.reset();
        URI uri2 = URI.create("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        assertEquals("ABCDE", this.store.get(uri2).getDocumentTxt());
        this.store.undo();
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        assertNull(this.store.get(uri2));
    }

    @Test
    public void testUndoSpecficDocumentPut() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        input.reset();
        URI uri2 = URI.create("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        assertEquals("ABCDE", this.store.get(uri2).getDocumentTxt());
        this.store.undo(uri);
        assertEquals("ABCDE", this.store.get(uri2).getDocumentTxt());
        assertNull(this.store.get(uri));
    }

    @Test
    public void testUndoSetMetadata() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        Document doc = this.store.get(uri);
        store.setMetadata(uri, "Test Key", "Test Value");
        store.setMetadata(uri, "Test Key", "Test Value 2");
        assertEquals("Test Value 2", doc.getMetadataValue("Test Key"));
        this.store.undo();
        assertEquals("Test Value", doc.getMetadataValue("Test Key"));
        this.store.undo();
        assertNull(doc.getMetadataValue("Test Key"));
    }

    @Test
    public void testUndoSpecificDocumentSetMetadata() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        input.reset();
        URI uri2 = URI.create("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        Document doc = this.store.get(uri);
        Document doc2 = this.store.get(uri2);
        store.setMetadata(uri, "TestKey", "Test Value");
        store.setMetadata(uri2, "TestKey", "Test Value 2");
        assertEquals("Test Value", doc.getMetadataValue("TestKey"));
        assertEquals("Test Value 2", doc2.getMetadataValue("TestKey"));
        this.store.undo(uri);
        assertNull(doc.getMetadataValue("TestKey"));
        assertEquals("Test Value 2", doc2.getMetadataValue("TestKey"));
    }

    @Test
    public void invalidUndo() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        this.store.undo();
        assertThrows(IllegalStateException.class, () -> {
            this.store.undo();
        });
    }

    @Test
    public void invalidUndoSpecificDocument() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        this.store.put(input, uri, DocumentFormat.TXT);
        assertEquals("ABCDE", this.store.get(uri).getDocumentTxt());
        URI uri2 = URI.create("http://www.github.com/jwizenf3");
        assertThrows(IllegalStateException.class, () -> {
            this.store.undo(uri2);
        });
    }

    @Test
    public void testOrderedSearch() throws IOException, URISyntaxException {
        String originalString = "This is a test for the trie impl, is it correct";
        byte[] byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(byteSequence);
        this.store.put(input, uri, DocumentFormat.TXT);
        originalString = "Here is another test, let's see if it does better at searching";
        byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        input = new ByteArrayInputStream(byteSequence);
        URI uri2 = new URI("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        List<Document> documentSet = new ArrayList<>();
        documentSet.addAll(this.store.search("is"));
        List<Document> expectedSet = new ArrayList<>();
        expectedSet.add(this.store.get(uri));
        expectedSet.add(this.store.get(uri2));
        for (int i = 0; i < expectedSet.size(); i++) {
            assertEquals(expectedSet.get(i), documentSet.get(i));
        }
    }

    @Test
    public void testNonValidCharSearch() throws IOException, URISyntaxException {
        String originalString = "This-is a;test for)the trie-impl, is it&correct";
        byte[] byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(byteSequence);
        this.store.put(input, uri, DocumentFormat.TXT);
        originalString = "Here is another a;test, let's see if it does better at searching This*is";
        byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        input = new ByteArrayInputStream(byteSequence);
        URI uri2 = new URI("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        List<Document> documentSet = new ArrayList<>();
        documentSet.addAll(this.store.search("a;test"));
        List<Document> expectedSet = new ArrayList<>();
        for (int i = 0; i < expectedSet.size(); i++) {
            assertEquals(expectedSet.get(i), documentSet.get(i));
        }
        assertEquals(new ArrayList<>(), this.store.search("Here is"));
        assertEquals(new ArrayList<>(), this.store.search("*&@$^#%"));
    }

    @Test
    public void testOrderedSearchByPrefix() throws IOException, URISyntaxException {
        String originalString = "This is a test for the trie impl, let's see if it is correct";
        byte[] byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(byteSequence);
        this.store.put(input, uri, DocumentFormat.TXT);

        originalString = "Here is another test, let's see if it does better at searching";
        byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        input = new ByteArrayInputStream(byteSequence);
        URI uri2 = new URI("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);

        List<Document> documentSet = new ArrayList<>();
        documentSet.addAll(this.store.searchByPrefix("se"));
        List<Document> expectedSet = new ArrayList<>();
        expectedSet.add(this.store.get(uri2));
        expectedSet.add(this.store.get(uri));
        for (int i = 0; i < expectedSet.size(); i++) {
            assertEquals(expectedSet.get(i), documentSet.get(i));
        }
        documentSet.clear();
        documentSet.addAll(this.store.searchByPrefix("b"));
        expectedSet.remove(this.store.get(uri));
        for (int i = 0; i < expectedSet.size(); i++) {
            assertEquals(expectedSet.get(i), documentSet.get(i));
        }
    }

    @Test
    public void testSearchByMetadata() throws IOException {
        String originalString = "This is a test for the trie impl, is it correct";
        byte[] byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(byteSequence);
        this.store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);
        store.setMetadata(uri, "Bye", "Bye");

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        searchMap.put("Bye", "Bye");

        List<Document> results = store.searchByMetadata(searchMap);

        assertEquals(1, results.size());

        Document resultDoc = results.get(0);
        assertEquals(uri, resultDoc.getKey());

        assertEquals(value, resultDoc.getMetadataValue(key));
    }

    @Test
    public void testSearchByKeywordAndMetadata() throws IOException {
        String content = "This is a sample content";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
        store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        List<Document> results = store.searchByKeywordAndMetadata("sample", searchMap);
        assertFalse(results.isEmpty());
        assertEquals(uri, results.get(0).getKey());
    }

    @Test
    public void testSearchByPrefixAndMetadata() throws IOException {
        String content = "This is a sample content";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
        store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        List<Document> results = store.searchByPrefixAndMetadata("sam", searchMap);
        assertFalse(results.isEmpty());
        assertEquals(uri, results.get(0).getKey());
    }

    @Test
    public void testDeleteAllWithMetadata() throws IOException {
        String content = "This is a sample content";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
        store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        Set<URI> deletedUris = store.deleteAllWithMetadata(searchMap);
        assertFalse(deletedUris.isEmpty());
        assertTrue(deletedUris.contains(uri));
    }

    @Test
    public void testDeleteAllWithKeywordAndMetadata() throws IOException {
        String content = "This is a sample content";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
        store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        Set<URI> deletedUris = store.deleteAllWithKeywordAndMetadata("sample", searchMap);
        assertFalse(deletedUris.isEmpty());
        assertTrue(deletedUris.contains(uri));
    }

    @Test
    public void testDeleteAllWithPrefixAndMetadata() throws IOException {
        String content = "This is a sample content";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes());
        store.put(input, uri, DocumentFormat.TXT);

        String key = "Hi";
        String value = "Yes";
        store.setMetadata(uri, key, value);

        Map<String, String> searchMap = new HashMap<>();
        searchMap.put(key, value);
        Set<URI> deletedUris = store.deleteAllWithPrefixAndMetadata("sam", searchMap);
        assertFalse(deletedUris.isEmpty());
        assertTrue(deletedUris.contains(uri));
    }

    @Test
    public void testUndoDeleteAll() throws IOException, URISyntaxException {
        String originalString = "This is a test for the trie impl, is it correct";
        byte[] byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(byteSequence);
        this.store.put(input, uri, DocumentFormat.TXT);
        originalString = "test your knowledge";
        byteSequence = originalString.getBytes(StandardCharsets.UTF_8);
        input = new ByteArrayInputStream(byteSequence);
        URI uri2 = new URI("http://www.github.com/jwizenf3");
        this.store.put(input, uri2, DocumentFormat.TXT);
        List<Document> docSet = new ArrayList<>();
        docSet.add(this.store.get(uri2));
        docSet.add(this.store.get(uri));
        assertEquals(docSet, this.store.search("test"));
        this.store.deleteAll("test");
        docSet.clear();
        assertEquals(docSet, this.store.search("test"));
        this.store.undo();
        docSet.add(this.store.get(uri2));
        docSet.add(this.store.get(uri));
        assertEquals(docSet, this.store.search("test"));
    }

    @Test
    public void testUndoSpecificDeleteAll() throws IOException {
        // Add two documents with a similar word
        String doc1Text = "This is a test for undo functionality";
        ByteArrayInputStream input1 = new ByteArrayInputStream(doc1Text.getBytes(StandardCharsets.UTF_8));
        URI uri1 = URI.create("http://example.com/doc1");
        this.store.put(input1, uri1, DocumentFormat.TXT);

        String doc2Text = "Another test document for undo functionality";
        ByteArrayInputStream input2 = new ByteArrayInputStream(doc2Text.getBytes(StandardCharsets.UTF_8));
        URI uri2 = URI.create("http://example.com/doc2");
        this.store.put(input2, uri2, DocumentFormat.TXT);

        // Check words are in  trie
        assertTrue(this.store.search("test").size() == 2);
        assertTrue(this.store.search("undo").size() == 2);

        this.store.deleteAll("test");

        // Assert both documents removed and words removed from trie
        assertNull(this.store.get(uri1));
        assertNull(this.store.get(uri2));
        assertTrue(this.store.search("test").isEmpty());
        assertTrue(this.store.search("undo").isEmpty());

        this.store.undo(uri1);

        // Assert first document back in the store and words in trie
        assertNotNull(this.store.get(uri1));
        assertEquals(doc1Text, this.store.get(uri1).getDocumentTxt());
        assertTrue(this.store.search("test").contains(this.store.get(uri1)));
        assertTrue(this.store.search("undo").contains(this.store.get(uri1)));

        // Assert that the CommandSet is still in the CommandStack by checking the second document is still missing
        assertNull(this.store.get(uri2));

        this.store.undo(uri2);

        // Assert second document back in the store and words in trie
        assertNotNull(this.store.get(uri2));
        assertEquals(doc2Text, this.store.get(uri2).getDocumentTxt());
        assertTrue(this.store.search("test").contains(this.store.get(uri2)));
        assertTrue(this.store.search("undo").contains(this.store.get(uri2)));

        // Undo puts for the Document
        this.store.undo(uri1);
        this.store.undo(uri2);

        // Assert that there are no more undos for uris
        assertThrows(IllegalStateException.class, () -> this.store.undo(uri1));
        assertThrows(IllegalStateException.class, () -> this.store.undo(uri2));
    }

    @Test
    public void testInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> this.store.setMaxDocumentBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> this.store.setMaxDocumentCount(-1));
    }

    @Test
    public void testPutNewVersionOfDocumentBinary() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(binaryData);
        URI uri = URI.create("http://www.github.com/jwizenf1");
        this.store.put(input, uri, DocumentFormat.BINARY);
        int hashCode = this.store.get(uri).hashCode();
        byte[] newData = { 5, 53, 43 };
        input = new ByteArrayInputStream(newData);
        int oldHash = this.store.put(input, uri, DocumentFormat.BINARY);
        assertEquals(hashCode, oldHash);
        assertNull(this.store.get(uri).getDocumentTxt());
    }

}
