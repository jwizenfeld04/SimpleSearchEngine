package edu.yu.cs.com1320.project.stage3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import edu.yu.cs.com1320.project.stage3.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage3.impl.DocumentStoreImpl;

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
        store.setMetadata(uri, "Test Key", "Test Value");
        store.setMetadata(uri2, "Test Key", "Test Value 2");
        assertEquals("Test Value", doc.getMetadataValue("Test Key"));
        assertEquals("Test Value 2", doc2.getMetadataValue("Test Key"));
        this.store.undo(uri);
        assertNull(doc.getMetadataValue("Test Key"));
        assertEquals("Test Value 2", doc2.getMetadataValue("Test Key"));
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
}
