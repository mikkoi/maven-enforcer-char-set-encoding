package com.github.mikkoi.maven.plugins.enforcer.rule.charsetencoding;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.plugins.enforcer.EnforcerTestUtils;
import org.junit.Test;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertTrue;

public final class TestCharacterSetEncodingRule {

    @Test
    public void testValidFiles1() {
        testFiles(true, 0, "UTF-8", "src/test/resources", "utf-8.txt", StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles1_1() {
        testFiles(true, 0, "ISO-8859-1", "src/test/resources", "iso.txt", StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles1_2() {
        testFiles(false, 1, "UTF-8", "src/test/resources", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles2() {
        testFiles(false, 2, "ASCII", "src/test/resources", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles3() {
        // TestClassInUTF8 can be interpreted as iso-8859-1
        testFiles(true, 0, "iso-8859-1", "src/test/java", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles4() {
        // This file (officially - defined by Maven - as utf-8) has no 8-bit chars so it can be ANY encoding!
        testFiles(true, 0, "utf-8", "src/test/java", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles5() {
        testFiles(false, 1, "ascii", "src/test/java", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    @Test
    public void testValidFiles6() {
        testFiles(true, 0, "ISO-8859-1", "src/test/resources", StringUtils.EMPTY, StringUtils.EMPTY);
    }

    private void testFiles(
            boolean expected,
            int expectedFaultyFilesAmount,
            @Nonnull String encoding,
            @Nonnull String dir,
            @Nonnull String includeRegex,
            @Nonnull String excludeRegex
    ) {
        boolean isValid;
        CharacterSetEncodingRule rule = new CharacterSetEncodingRule();
        rule.setRequireEncoding(encoding);
        rule.setDirectory(dir);
        rule.setIncludeRegex(includeRegex);
        rule.setExcludeRegex(excludeRegex);
        try {
            EnforcerRuleHelper helper = EnforcerTestUtils.getHelper();
            rule.execute(helper);
            isValid = true;
        } catch (EnforcerRuleException e) {
            // e.printStackTrace();
            isValid = false;
        }
        assertTrue(isValid == expected && rule.getFaultyFiles().size() == expectedFaultyFilesAmount);
    }

}
