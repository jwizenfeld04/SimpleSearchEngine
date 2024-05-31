package edu.yu.cs.com1320.project.stage6;

import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage6.impl.DocumentImpl;
import edu.yu.cs.com1320.project.MinHeap;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;

public class MinHeapImplTest {
    MinHeap<Integer> minHeap;

    @BeforeEach
    void setup() {
        this.minHeap = new MinHeapImpl<>();
    }

    @Test
    public void testMinHeapProperty() {
        this.minHeap.insert(10);
        this.minHeap.insert(8);
        this.minHeap.insert(12);
        assertEquals(this.minHeap.remove(), Integer.valueOf(8));
        assertEquals(this.minHeap.remove(), Integer.valueOf(10));
        assertEquals(this.minHeap.remove(), Integer.valueOf(12));
        assertThrows(NoSuchElementException.class, () -> this.minHeap.remove());
    }

    @Test
    public void testReheapifyNull() {
        MinHeapImpl<Integer> heap = new MinHeapImpl<>();
        assertThrows(IllegalArgumentException.class, () -> heap.reHeapify(null));
    }

    @Test
    public void testReheapifyElementNotInHeap() {
        MinHeapImpl<Integer> heap = new MinHeapImpl<>();
        heap.insert(10);
        assertThrows(NoSuchElementException.class, () -> heap.reHeapify(5));
    }

    @Test
    public void testReheapifyMoveUp() {
        MinHeapImpl<Document> heap = new MinHeapImpl<>();
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        Document doc1 = new DocumentImpl(uri1, "Hi my name is Jeremy", null);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        Document doc2 = new DocumentImpl(uri2, "Hi my name is Jeremy", null);
        URI uri3 = URI.create("http://www.github.com/jwizenf3");
        Document doc3 = new DocumentImpl(uri3, "Hi my name is Jeremy", null);
        heap.insert(doc1);
        heap.insert(doc2);
        heap.insert(doc3);

        assertEquals(doc1, heap.peek());
        doc3.setLastUseTime(System.nanoTime() - 1000000);
        heap.reHeapify(doc3);
        assertEquals(doc3, heap.peek());
    }

    @Test
    public void testReheapifyMoveDown() {
        MinHeapImpl<Document> heap = new MinHeapImpl<>();
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        Document doc1 = new DocumentImpl(uri1, "Hi my name is Jeremy", null);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        Document doc2 = new DocumentImpl(uri2, "Hi my name is Jeremy", null);
        URI uri3 = URI.create("http://www.github.com/jwizenf3");
        Document doc3 = new DocumentImpl(uri3, "Hi my name is Jeremy", null);
        heap.insert(doc1);
        heap.insert(doc2);
        heap.insert(doc3);

        assertEquals(doc1, heap.peek());
        doc1.setLastUseTime(System.nanoTime() + 1000000);
        heap.reHeapify(doc1);
        assertEquals(doc2, heap.peek());
    }

    @Test
    public void testReheapifyStay() {
        MinHeapImpl<Document> heap = new MinHeapImpl<>();
        URI uri1 = URI.create("http://www.github.com/jwizenf1");
        Document doc1 = new DocumentImpl(uri1, "Hi my name is Jeremy", null);
        URI uri2 = URI.create("http://www.github.com/jwizenf2");
        Document doc2 = new DocumentImpl(uri2, "Hi my name is Jeremy", null);
        URI uri3 = URI.create("http://www.github.com/jwizenf3");
        Document doc3 = new DocumentImpl(uri3, "Hi my name is Jeremy", null);
        heap.insert(doc1);
        heap.insert(doc2);
        heap.insert(doc3);

        assertEquals(doc1, heap.peek());
        doc1.setLastUseTime(doc1.getLastUseTime() + 4000);
        heap.reHeapify(doc1);
        assertEquals(doc1, heap.peek());
    }

    @Test
    public void testArrayDoublingMinHeap() {
        for (int i = 0; i < 25; i++) {
            this.minHeap.insert(i);
        }
        assertEquals(Integer.valueOf(0), this.minHeap.peek());
    }
}
