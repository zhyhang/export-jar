package org.yanhuang.plugins.intellij.exportjar.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExportJarInfoTest {

    @Test
    public void testEqualsAndHashCodeKeyedOnPath() {
        ExportJarInfo a = new ExportJarInfo();
        a.setPath("/a/out.jar");
        a.setCreateTime(100L);

        ExportJarInfo b = new ExportJarInfo();
        b.setPath("/a/out.jar");
        b.setCreateTime(999L);

        assertEquals("same path => equal", a, b);
        assertEquals("same path => same hashCode", a.hashCode(), b.hashCode());

        ExportJarInfo c = new ExportJarInfo();
        c.setPath("/b/out.jar");
        assertNotEquals("different path => not equal", a, c);
    }

    @Test
    public void testCompareToOrdersByCreateTimeDescending() {
        ExportJarInfo newer = new ExportJarInfo();
        newer.setPath("/newer.jar");
        newer.setCreateTime(300L);

        ExportJarInfo older = new ExportJarInfo();
        older.setPath("/older.jar");
        older.setCreateTime(100L);

        assertTrue("newer should sort before older", newer.compareTo(older) < 0);
        assertTrue("older should sort after newer", older.compareTo(newer) > 0);
        assertEquals("same object => 0", 0, newer.compareTo(newer));
    }

    @Test
    public void testGetCreationAliasesCreateTime() {
        ExportJarInfo info = new ExportJarInfo();
        info.setCreateTime(12345L);
        assertEquals("getCreation() should return createTime", 12345L, info.getCreation());

        info.setCreation(99999L);
        assertEquals("setCreation() should update createTime", 99999L, info.getCreateTime());
    }
}
