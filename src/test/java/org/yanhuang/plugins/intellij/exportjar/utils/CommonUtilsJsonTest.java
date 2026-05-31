package org.yanhuang.plugins.intellij.exportjar.utils;

import org.junit.Test;
import org.yanhuang.plugins.intellij.exportjar.model.ExportJarInfo;
import org.yanhuang.plugins.intellij.exportjar.model.ExportOptions;
import org.yanhuang.plugins.intellij.exportjar.model.SettingHistory;
import org.yanhuang.plugins.intellij.exportjar.model.SettingSelectFile;
import org.yanhuang.plugins.intellij.exportjar.model.SettingTemplate;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class CommonUtilsJsonTest {

    @Test
    public void testSettingHistoryRoundTrip() {
        SettingHistory h = new SettingHistory();
        h.setCreateTime(1000L);
        h.setUpdateTime(2000L);

        String json = CommonUtils.toJson(h);
        SettingHistory h2 = CommonUtils.fromJson(json, SettingHistory.class);

        assertEquals(1000L, h2.getCreateTime());
        assertEquals(2000L, h2.getUpdateTime());
    }

    @Test
    public void testSettingTemplateRoundTrip() {
        SettingTemplate t = new SettingTemplate();
        t.setName("my-template");
        t.setCreateTime(100L);
        t.setUpdateTime(200L);
        t.setOptions(new ExportOptions[]{ExportOptions.export_class, ExportOptions.export_java});

        String json = CommonUtils.toJson(t);
        SettingTemplate t2 = CommonUtils.fromJson(json, SettingTemplate.class);

        assertEquals("my-template", t2.getName());
        assertEquals(100L, t2.getCreateTime());
        assertEquals(200L, t2.getUpdateTime());
        assertNotNull(t2.getOptions());
        assertEquals(2, t2.getOptions().length);
    }

    @Test
    public void testExportJarInfoRoundTrip() {
        ExportJarInfo info = new ExportJarInfo();
        info.setPath("/some/path/out.jar");
        info.setCreateTime(999L);

        String json = CommonUtils.toJson(info);
        ExportJarInfo info2 = CommonUtils.fromJson(json, ExportJarInfo.class);

        assertEquals("/some/path/out.jar", info2.getPath());
        assertEquals(999L, info2.getCreateTime());
    }

    @Test
    public void testTransientFieldsAbsentFromJson() {
        SettingSelectFile sf = new SettingSelectFile();
        sf.setFilePath("/some/file.java");
        sf.putMappingVf(Path.of("/some/file.java"), new Object());

        String json = CommonUtils.toJson(sf);

        assertFalse("virtualFile should not be serialized", json.contains("\"virtualFile\""));
        assertFalse("nioPath should not be serialized", json.contains("\"nioPath\""));
        assertFalse("mappingVfs should not be serialized", json.contains("\"mappingVfs\""));
    }

    @Test
    public void testExportJarInfoLegacyAlias() {
        // getCreation() and setCreation() are Java-level aliases for createTime.
        // Gson serializes the backing field "createTime", so round-trip via toJson/fromJson
        // preserves the value through the alias getter.
        ExportJarInfo info = new ExportJarInfo();
        info.setCreation(12345L);  // alias setter
        assertEquals(12345L, info.getCreateTime());

        String json = CommonUtils.toJson(info);
        ExportJarInfo info2 = CommonUtils.fromJson(json, ExportJarInfo.class);
        assertEquals(12345L, info2.getCreation());
        assertEquals(12345L, info2.getCreateTime());
    }
}
