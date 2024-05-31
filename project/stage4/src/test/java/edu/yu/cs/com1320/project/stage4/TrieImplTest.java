package edu.yu.cs.com1320.project.stage4;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.impl.TrieImpl;

public class TrieImplTest {
    private Trie<Integer> trie;

    @BeforeEach
    void setup() {
        this.trie = new TrieImpl<>();
        this.trie.put("She", 12);
        this.trie.put("She", 5);
        this.trie.put("She", 10);
        this.trie.put("Sells", 25);
        this.trie.put("Seashells", 37);
        this.trie.put("Seashore", 28);
        this.trie.put("Sel", 58);
        this.trie.put("Bye", 10);
        this.trie.put("Bye", 12);
    }

    @Test
    public void testTriePut() {
        String key1 = "test";
        String key2 = "YES";
        String key3 = "No04";
        String key4 = "123";
        Set<Integer> nums = new HashSet<>();
        int num1 = 5;
        nums.add(num1);
        trie.put(key1, 5);
        trie.put(key2, 5);
        trie.put(key3, 5);
        trie.put(key4, 5);
        assertEquals(nums, trie.get(key1));
        assertEquals(nums, trie.get(key2));
        assertEquals(nums, trie.get(key3));
        assertEquals(nums, trie.get(key4));
    }

    @Test
    public void testTrieGetSorted() {
        List<Integer> sortedValues = this.trie.getSorted("She", new Comparator<Integer>() {
            @Override
            public int compare(Integer int1, Integer int2) {
                return int2 - int1;
            }
        });
        List<Integer> nums = new ArrayList<>();
        nums.add(12);
        nums.add(10);
        nums.add(5);
        assertEquals(nums, sortedValues);
    }

    @Test
    public void testTrieGetPrefixSorted() {
        List<Integer> sortedValues = this.trie.getAllWithPrefixSorted("Se", new Comparator<Integer>() {
            @Override
            public int compare(Integer int1, Integer int2) {
                return int2 - int1;
            }
        });
        List<Integer> nums = new ArrayList<>();
        nums.add(58);
        nums.add(37);
        nums.add(28);
        nums.add(25);
        assertEquals(nums, sortedValues);
    }

    @Test
    public void testTrieDeleteAllWithPrefix() {
        List<Integer> sortedValues = this.trie.getAllWithPrefixSorted("S", new Comparator<Integer>() {
            @Override
            public int compare(Integer int1, Integer int2) {
                return int2 - int1;
            }
        });
        Set<Integer> setValues = new HashSet<>();
        setValues.addAll(sortedValues);
        Set<Integer> deletedValues = this.trie.deleteAllWithPrefix("S");
        assertEquals(setValues, deletedValues);
        List<Integer> emptySet = new ArrayList<>();
        assertEquals(emptySet, this.trie.getAllWithPrefixSorted("S", new Comparator<Integer>() {
            @Override
            public int compare(Integer int1, Integer int2) {
                return int2 - int1;
            }
        }));
    }

    @Test
    public void testTrieDeleteAll() {
        Set<Integer> values = this.trie.get("Bye");
        Set<Integer> deletedValues = this.trie.deleteAll("Bye");
        assertEquals(values, deletedValues);
        Set<Integer> emptySet = new HashSet<>();
        values = this.trie.get("Bye");
        assertEquals(emptySet, values);
    }

    @Test
    public void testTrieDelete() {
        Set<Integer> values = this.trie.get("Bye");
        Integer deletedValue = this.trie.delete("Bye", 10);
        assertEquals(10, deletedValue);
        values = this.trie.get("Bye");
        Set<Integer> expectedVales = new HashSet<>();
        expectedVales.add(12);
        assertEquals(expectedVales, values);
        deletedValue = this.trie.delete("Bye", 12);
        assertEquals(12, deletedValue);
        expectedVales.remove(12);
        assertEquals(expectedVales, this.trie.get("Bye"));
    }

    @Test
    public void testTrieDeleteAndGetEmpty() {
        Set<Integer> emptySet = new HashSet<>();
        assertEquals(emptySet, this.trie.deleteAllWithPrefix("l"));
        assertEquals(emptySet, this.trie.deleteAll("exist"));
        assertNull(this.trie.delete("l", 5));
        // Checked case sensitivity
        assertEquals(emptySet, this.trie.get("bye"));
    }
}
