package org.apache.maven.plugins.enforcer.rule.charsetencoding;

/**
 * Created by someone with Ä, Ö and Å in his name.
 */
public class TestClassInUTF8 {
    static private boolean ÄäkkösFunction() {
        boolean scandinavianÄÖÅVar = true;
        return false;
    }
}
