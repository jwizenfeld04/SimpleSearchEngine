package edu.yu.cs.com1320.project.stage3;

import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.stage3.impl.DocumentImpl;
import edu.yu.cs.com1320.project.HashTable;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;

public class DocumentImplTest {

    private URI uri;
    private String txt;
    private byte[] binaryData = { 10, 20, 30, 50, 80, 43 };
    private DocumentImpl documentText;
    private DocumentImpl documentBinary;

    @BeforeEach
    void setup() {
        uri = URI.create("http://www.github.com/jwizenf2");
        txt = "Hello my name is Jeremy and I am a computer science major at Yeshiva University";
        documentText = new DocumentImpl(uri, txt);
        documentBinary = new DocumentImpl(uri, binaryData);
    }

    @Test
    public void testValidTextConstructor() {
        String txt2 = "To be or not to be that is the question";
        DocumentImpl doc = new DocumentImpl(uri, txt2);
        assertEquals(doc.getKey(), uri);
        assertEquals(doc.getDocumentTxt(), txt2);
    }

    @Test
    public void testValidBinaryDataConstructor() {
        byte[] binaryData2 = { 1, 2, 3, 4, 5 };
        DocumentImpl doc = new DocumentImpl(uri, binaryData2);
        assertEquals(doc.getKey(), uri);
        assertEquals(doc.getDocumentBinaryData(), binaryData2);
    }

    @Test
    public void testInvalidConstructors() {
        byte[] binaryData2 = { 1, 2, 3, 4, 5 };
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(null, binaryData2);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(null, "binaryData2");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, "");
        });
        byte[] bytes = {};
        assertThrows(IllegalArgumentException.class, () -> {
            new DocumentImpl(uri, bytes);
        });
    }

    @Test
    public void testGetMetaDataCopy() {
        String key = "name";
        String value = "Jeremy";
        documentText.setMetadataValue(key, value);

        HashTable<String, String> copy = documentText.getMetadata();

        String newValue = "update";
        copy.put(key, newValue);

        assertNotEquals(newValue, documentText.getMetadata().get(key));
    }

    @Test
    public void testGetMetaDataCopyInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            documentText.getMetadataValue("");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            documentText.getMetadataValue(null);
        });
        String meta = documentText.getMetadataValue("no");
        assertNull(meta);
        documentText.setMetadataValue("yes", null);

        String metaNull = documentText.getMetadataValue("yes");
        assertNull(metaNull);
    }

    @Test
    public void testExtraDocumentVariablesNull() {
        assertNull(documentText.getDocumentBinaryData());
        assertNull(documentBinary.getDocumentTxt());
    }

    @Test
    public void testEqualDocumentImpl() {
        URI newUri = URI.create("http://www.github.com/jwizenf2");
        String newTxt = "Hello my name is Jeremy and I am a computer science major at Yeshiva University";
        DocumentImpl newDoc = new DocumentImpl(newUri, newTxt);
        assertEquals(newDoc.hashCode(), documentText.hashCode());
        assertEquals(newDoc, documentText);
    }

    @Test
    public void testSettingMetaData() {
        String first = documentText.setMetadataValue("name", "JEREMY");
        String old = documentText.setMetadataValue("name", "Wizenfeld");
        assertEquals(old, "JEREMY");
        assertEquals(null, first);
        assertThrows(IllegalArgumentException.class, () -> {
            documentText.setMetadataValue("", "new");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            documentText.setMetadataValue(null, "new");
        });
    }

}
