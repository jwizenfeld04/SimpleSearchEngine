package edu.yu.cs.com1320.project.stage1;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import edu.yu.cs.com1320.project.stage1.DocumentStore.DocumentFormat;
import edu.yu.cs.com1320.project.stage1.impl.DocumentStoreImpl;

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
}
