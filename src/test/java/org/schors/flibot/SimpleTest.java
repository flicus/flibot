package org.schors.flibot;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleTest {

    @Test
    public void test1() {
        Search s = new Search("aaa", SearchType.AUTHOR);
        assertEquals("aaa", s.getToSearch());
    }
}
