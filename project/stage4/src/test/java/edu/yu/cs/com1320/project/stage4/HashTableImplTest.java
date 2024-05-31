package edu.yu.cs.com1320.project.stage4;

import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;

public class HashTableImplTest {
    private HashTable<Integer, String> table;

    @BeforeEach
    void setup() {
        table = new HashTableImpl<>();
        table.put(10, "Today");
        table.put(31, "Yesterday");
        table.put(12, "Tomorrow");
        table.put(11, "Now");

    }

    @Test
    public void testGetValue() {
        String val1 = "Yesterday";
        String val2 = "Today";

        assertEquals(val1, this.table.get(31));
        assertEquals(val2, this.table.get(10));
        assertNull(this.table.get(9));

    }

    @Test
    public void testContainsKey() {
        assertTrue(this.table.containsKey(31));
        assertFalse(this.table.containsKey(29));
    }

    @Test
    public void testTableSize() {
        assertEquals(4, this.table.size());
        this.table.put(9, "Yes");
        assertEquals(5, this.table.size());
        this.table.put(9, "No");
        assertEquals(5, this.table.size());
        this.table.put(9, null);
        assertEquals(4, this.table.size());
    }

    @Test
    public void testFullKeyset() {
        Set<Integer> keySet = new HashSet<>();
        keySet.add(10);
        keySet.add(31);
        keySet.add(12);
        keySet.add(11);
        assertEquals(keySet, this.table.keySet());
        this.table.put(52, "Hi");
        keySet.add(52);
        assertEquals(keySet, this.table.keySet());
        keySet.remove(52);
        this.table.put(52, null);
        assertEquals(keySet, this.table.keySet());
    }

    @Test
    public void testFullValuelist() {
        Collection<String> valueList = new ArrayList<>();
        Collection<String> compareList = new ArrayList<>();
        valueList.add("Today");
        valueList.add("Yesterday");
        valueList.add("Now");
        valueList.add("Tomorrow");
        compareList.addAll(this.table.values());
        assertEquals(valueList, compareList);
        valueList.add("Hi");
        this.table.put(52, "Hi");
        compareList.clear();
        compareList.addAll(this.table.values());
        assertEquals(valueList, compareList);
        valueList.remove("Hi");
        this.table.put(52, null);
        compareList.clear();
        compareList.addAll(this.table.values());
        assertEquals(valueList, compareList);
    }

    @Test
    public void testPutNewKey() {
        String result = this.table.put(1, "Value1");
        assertEquals("Value1", this.table.get(1));
        assertNull(result);
    }

    @Test
    public void testUpdateExistingKey() {
        this.table.put(1, "Value1");
        String result = this.table.put(1, "Value2");
        assertEquals("Value1", result);
        assertEquals("Value2", this.table.get(1));
    }

    @Test
    public void testHandleCollision() {
        this.table.put(1, "Value1");
        this.table.put(6, "Value2");
        assertEquals("Value1", this.table.get(1));
        assertEquals("Value2", this.table.get(6));
    }

    @Test
    public void testDeleteKeySettingNull() {
        this.table.put(1, "Value1");
        this.table.put(1, null);
        assertNull(this.table.get(1));
        this.table.put(1, "Value1");
        this.table.put(2, "Value2");
        this.table.put(3, "Value3");
        this.table.put(2, null);
        assertEquals("Value3", this.table.get(3));
        assertNull(this.table.get(2));

    }

    @Test
    public void testArrayDoubling() {
        this.table = new HashTableImpl<>();
        for (int i = 1; i < 51; i++) {
            this.table.put(i, "Value " + i);
        }
        assertEquals(50, this.table.size());
        assertEquals("Value 1", this.table.get(1));
        assertEquals("Value 11", this.table.get(11));
        assertEquals("Value 21", this.table.get(21));
        assertEquals("Value 32", this.table.get(32));
        assertEquals("Value 33", this.table.get(33));
        assertEquals("Value 34", this.table.get(34));
        assertEquals("Value 50", this.table.get(50));

    }

}
