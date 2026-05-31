package org.yanhuang.plugins.intellij.exportjar.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class SettingTemplateTest {

    @Test
    public void testEqualsAndHashCodeKeyedOnName() {
        SettingTemplate t1 = new SettingTemplate();
        t1.setName("alpha");
        t1.setCreateTime(100L);
        t1.setUpdateTime(200L);

        SettingTemplate t2 = new SettingTemplate();
        t2.setName("alpha");
        t2.setCreateTime(999L);
        t2.setUpdateTime(888L);

        assertEquals("same name => equal", t1, t2);
        assertEquals("same name => same hashCode", t1.hashCode(), t2.hashCode());

        SettingTemplate t3 = new SettingTemplate();
        t3.setName("beta");
        assertNotEquals("different name => not equal", t1, t3);
    }

    @Test
    public void testCompareToOrdersByUpdateTimeDescending() {
        SettingTemplate newer = new SettingTemplate();
        newer.setName("newer");
        newer.setUpdateTime(300L);

        SettingTemplate older = new SettingTemplate();
        older.setName("older");
        older.setUpdateTime(100L);

        // newer.compareTo(older) should be negative (newer comes first)
        assertTrue("newer should sort before older", newer.compareTo(older) < 0);
        assertTrue("older should sort after newer", older.compareTo(newer) > 0);
        assertEquals("same object => 0", 0, newer.compareTo(newer));
    }

    @Test
    public void testMergeTemplateCopiesToDest() {
        SettingTemplate src = new SettingTemplate();
        src.setName("src");
        src.setCreateTime(10L);
        src.setUpdateTime(500L);
        src.setOptions(new ExportOptions[]{ExportOptions.export_class});
        src.setSelectFilesStore("store-path");
        src.setFileListTreeGroupingKeys(new String[]{"key1"});
        src.setFileListTreeExpandDirectory(true);

        SettingTemplate dest = new SettingTemplate();
        dest.setName("dest");
        dest.setCreateTime(1L);
        dest.setUpdateTime(1L);

        SettingTemplate.mergeTemplate(src, dest);

        // name and createTime on dest should be untouched
        assertEquals("dest name unchanged", "dest", dest.getName());
        assertEquals("dest createTime unchanged", 1L, dest.getCreateTime());

        // options, selectFilesStore, updateTime, groupingKeys, expandDirectory copied from src
        assertEquals("updateTime copied", 500L, dest.getUpdateTime());
        assertEquals("selectFilesStore copied", "store-path", dest.getSelectFilesStore());
        assertNotNull("options copied", dest.getOptions());
        assertEquals(1, dest.getOptions().length);
        assertArrayEquals("groupingKeys copied", new String[]{"key1"}, dest.getFileListTreeGroupingKeys());
        assertTrue("expandDirectory copied", dest.isFileListTreeExpandDirectory());
    }
}
