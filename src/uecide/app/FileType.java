package uecide.app;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;
import java.util.jar.*;
import uecide.plugin.*;


import javax.swing.*;
import javax.imageio.*;

import uecide.app.Compiler;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;


public class FileType {

    public static class FileTypeInfo {
        public int type;
        public String style;
        public String editor;
        public String icon;
        public int group;
        public FileTypeInfo(int t, String e, String s, String i, int g) {
            type = t;
            editor = e;
            style = s;
            icon = i;
            group = g;
        }
    };
        

    public static final int INVALID = 0;
    public static final int HEADER = 1;
    public static final int CSOURCE = 2;
    public static final int CPPSOURCE = 3;
    public static final int ASMSOURCE = 4;
    public static final int OBJECT = 5;
    public static final int LIBRARY = 6;
    public static final int SKETCH = 7;
    public static final int TEXT = 8;
    public static final int GRAPHIC = 9;

    public static final int GROUP_OTHER = 0;
    public static final int GROUP_SOURCE = 1;
    public static final int GROUP_HEADER = 2;
    public static final int GROUP_BINARY = 3;
    public static final int GROUP_GRAPHIC = 4;

    public static TreeMap<String, FileTypeInfo> fileTypeList;
    static {
        fileTypeList = new TreeMap<String, FileTypeInfo>();
        fileTypeList.put("c",   new FileTypeInfo(FileType.CSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_C, "files/text-x-c.png", GROUP_SOURCE));

        fileTypeList.put("cpp", new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("c++", new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("cc",  new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("cp",  new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("cxx", new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("CPP", new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("C",   new FileTypeInfo(FileType.CPPSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));

        fileTypeList.put("S",   new FileTypeInfo(FileType.ASMSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c.png", GROUP_SOURCE));
        fileTypeList.put("sx",  new FileTypeInfo(FileType.ASMSOURCE, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c.png", GROUP_SOURCE));

        fileTypeList.put("h",   new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("H",   new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("hh",  new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("hp",  new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("hxx", new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("hpp", new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("HPP", new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("h++", new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));
        fileTypeList.put("tcc", new FileTypeInfo(FileType.HEADER, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_HEADER));


        fileTypeList.put("pde", new FileTypeInfo(FileType.SKETCH, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));
        fileTypeList.put("ino", new FileTypeInfo(FileType.SKETCH, "uecide.app.editors.code", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "files/text-x-c++.png", GROUP_SOURCE));

        fileTypeList.put("lss", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("hex", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("cfg", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("ini", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("txt", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("dat", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("plist", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));
        fileTypeList.put("properties", new FileTypeInfo(FileType.TEXT, "uecide.app.editors.text", null, "files/text-x-generic.png", GROUP_OTHER));

        fileTypeList.put("o", new FileTypeInfo(FileType.OBJECT, "uecide.app.editors.object", null, "files/application-octet-stream.png", GROUP_BINARY));
        fileTypeList.put("a", new FileTypeInfo(FileType.OBJECT, "uecide.app.editors.object", null, "files/application-octet-stream.png", GROUP_BINARY));
        fileTypeList.put("elf", new FileTypeInfo(FileType.OBJECT, "uecide.app.editors.object", null, "files/application-octet-stream.png", GROUP_BINARY));

        fileTypeList.put("bmp", new FileTypeInfo(FileType.GRAPHIC, "uecide.app.editors.bitmap", null, "files/image-x-generic.png", GROUP_GRAPHIC));

    }

    public static int getType(File f) { return getType(f.getName()); }
    public static int getType(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).type;
            }
        }
        return FileType.INVALID;
    }

    public static boolean isValid(File f) { return isValid(f.getName()); }
    public static boolean isValid(String f) {
        return getType(f) != FileType.INVALID;
    }

    public static String getEditor(File f) { return getEditor(f.getName()); }
    public static String getEditor(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).editor;
            }
        }
        return null;
    }

    public static String getSyntaxStyle(File f) { return getSyntaxStyle(f.getName()); }
    public static String getSyntaxStyle(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).style;
            }
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    public static String getIcon(File f) { return getIcon(f.getName()); }
    public static String getIcon(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).icon;
            }
        }
        return "unknown.png";
    }

    public static int getGroup(File f) { return getGroup(f.getName()); }
    public static int getGroup(String f) {
        for (String extension : fileTypeList.keySet()) {
            if (f.endsWith("." + extension)) {
                return fileTypeList.get(extension).group;
            }
        }
        return GROUP_OTHER;
    }

    
};
