package edu.yu.cs.com1320.project.stage4;

import org.junit.jupiter.api.Test;

import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.Stack;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

public class StackImplTest {
    private Stack<String> stack;

    @BeforeEach
    void setup() {
        stack = new StackImpl<>();
    }

    @Test
    public void testStackPush() {
        stack.push("val1");
        stack.push("val2");
        stack.push("val3");
        assertEquals(3, stack.size());
        assertEquals("val3", stack.peek());
    }

    @Test
    public void testStackPop() {
        stack.push("val1");
        stack.push("val2");
        stack.push("val3");
        assertEquals(3, stack.size());
        assertEquals("val3", stack.pop());
        assertEquals("val2", stack.pop());
        assertEquals("val1", stack.pop());
        assertNull(stack.pop());
    }
}
