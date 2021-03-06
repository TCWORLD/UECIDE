/*
 * Copyright (c) 2014, Majenko Technologies
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * 
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uecide.app;

import uecide.app.preproc.*;
import java.util.regex.*;


import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class Library implements Comparable {
    public ArrayList<String> requiredLibraries;
    public File folder;
    public String name;
    public File examplesFolder = null;
    public ArrayList<File> sourceFiles;
    public ArrayList<File> headerFiles;
    public ArrayList<File> archiveFiles;
    public File mainInclude;
    public File utilityFolder;
    public File librariesFolder;
    public TreeMap<String, File>examples;
    public String type;
    public String core;
    public int compiledPercent = 0;

    public boolean valid = false;

    public boolean buildLibrary = false;

    PropertyFile properties = null;
    File propertyFile = null;
    
    boolean utilRecurse = false;

    public Library(File hdr, String t, String c) {
        type = t;
        core = c;
        File root = hdr.getParentFile();
        name = hdr.getName().substring(0, hdr.getName().indexOf(".h"));;
        folder = root;
        mainInclude = hdr;
        if (!mainInclude.exists()) {
            return;
        }

        propertyFile = new File(folder, "library.txt");
        if (propertyFile.exists()) {
            properties = new PropertyFile(propertyFile);
            utilityFolder = new File(root, properties.get("utility", "utility"));
            utilRecurse = properties.getBoolean("utility.recurse");
            examplesFolder = new File(root, properties.get("examples", "examples"));
            librariesFolder = new File(root, properties.get("libraries", "libraries"));
            core = properties.get("core", "all");
        } else {
            utilityFolder = new File(root, "utility");
            examplesFolder = new File(root, "examples");
            librariesFolder = new File(root, "libraries");
        }
        rescan();
        valid = true;
    }

    public void rescan() {
        requiredLibraries = new ArrayList<String>();
        sourceFiles = new ArrayList<File>();
        archiveFiles = new ArrayList<File>();
        headerFiles = new ArrayList<File>();
        examples = new TreeMap<String, File>();

        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "cpp", false));
        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "c", false));
        sourceFiles.addAll(Sketch.findFilesInFolder(folder, "S", false));
        archiveFiles.addAll(Sketch.findFilesInFolder(folder, "a", false));
        headerFiles.addAll(Sketch.findFilesInFolder(folder, "h", false));

        if (utilityFolder.exists() && utilityFolder.isDirectory()) {
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "cpp", utilRecurse));
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "c", utilRecurse));
            sourceFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "S", utilRecurse));
            archiveFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "a", utilRecurse));
            headerFiles.addAll(Sketch.findFilesInFolder(utilityFolder, "h", utilRecurse));
        }

        if (examplesFolder.exists() && examplesFolder.isDirectory()) {
            File[] list = examplesFolder.listFiles();
            for (File f : list) {
                if (f.isDirectory()) {
                    String sketchName = f.getName();
                    File sketchFile = new File(f, sketchName + ".pde");
                    if (sketchFile.exists()) {
                        examples.put(sketchName, f);
                    } else {
                        sketchFile = new File(f, sketchName + ".ino");
                        if (sketchFile.exists()) {
                            examples.put(sketchName, f);
                        }
                    }
                }
            }
        }

        probedFiles = new ArrayList<String>();

        gatherIncludes(mainInclude);

        for (File f : sourceFiles) {
            gatherIncludes(f);
        }
    }

    ArrayList<String> probedFiles;

    public boolean hasHeader(String header) {
        for (File f : headerFiles) {
            if (f.getName().equals(header)) {
                return true;
            }
        }
        return false;
    }

    public File getHeader(String header) {
        for (File f : headerFiles) {
            if (f.getName().equals(header)) {
                return f;
            }
        }
        return null;
    }

    public void gatherIncludes(File f) {
        String[] data;
        try {
            FileReader in = new FileReader(f);
            StringBuilder contents = new StringBuilder();
            char[] buffer = new char[4096];
            int read = 0;
            do {
                contents.append(buffer, 0, read);
                read = in.read(buffer);
            } while (read >= 0);
            data = contents.toString().split("\n");
        } catch (Exception e) {
            Base.error(e);
            return;
        }
        ArrayList<String> includes = new ArrayList<String>();

        for (String line : data) {
            line = line.trim();
            if (line.startsWith("#include")) {
                int qs = line.indexOf("<");
                if (qs == -1) {
                    qs = line.indexOf("\"");
                }
                if (qs == -1) {
                    continue;
                }
                qs++;
                int qe = line.indexOf(">");
                if (qe == -1) {
                    qe = line.indexOf("\"", qs);
                }
                String i = line.substring(qs, qe);

                // If the file is not local to the library then go ahead and add it.
                // Local files override other libraries.
                File localFile = new File(folder, i);
                if (!hasHeader(localFile.getName())) {
                    if (requiredLibraries.indexOf(i) == -1) {
                        requiredLibraries.add(i);
                    }
                } else {
                    // This is a local header.  We should check it for libraries.
                    localFile = getHeader(localFile.getName());
                    if (probedFiles.indexOf(localFile.getAbsolutePath()) == -1) {
                        probedFiles.add(localFile.getAbsolutePath());
                        gatherIncludes(localFile);
                    }
                }
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    public String getName() {
        return name;
    }

    public File getFolder() {
        return folder;
    }

    public File getUtilityFolder() {
        return utilityFolder;
    }

    public File getLibrariesFolder() {
        return librariesFolder;
    }

    public String getInclude() {
        return "#include <" + mainInclude.getName() + ">\n";
    }

    public ArrayList<File> getSourceFiles() {
        return sourceFiles;
    }

    public ArrayList<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public String getType() {
        return type;
    }

    public boolean isCore() {
        return type.startsWith("core:");
    }

    public boolean isContributed() {
        return type.startsWith("cat");
    }

    public boolean isSketch() {
        return type.equals("sketch");
    }

    public String getCore() {
        return core;
    }

    public boolean worksWith(String c) {
        if (core.equals("all")) {
            return true;
        }
        return core.equals(c);
    }

    public String getArchiveName() {
        String[] bits = type.split(":");
        return "lib" + bits[0] + "_" + name + ".a";
    }

    public String getLinkName() {
        String[] bits = type.split(":");
        return bits[0] + "_" + name;
    }

    public File getExamplesFolder() {
        return examplesFolder;
    }

    public String toString() {
        return getName();
    }

    public void setCompiledPercent(int p) {
        compiledPercent = p;
    }

    public int getCompiledPercent() {
        return compiledPercent;
    }

    public boolean isHeaderOnly() {
        return sourceFiles.size() == 0;
    }

    public boolean isLocal(File sketchFolder) {
        File wouldBeLocalLibs = new File(sketchFolder, "libraries");
        File wouldBeLocal = new File(wouldBeLocalLibs, folder.getName());
        if (wouldBeLocal.equals(folder)) {
            return true;
        }
        return false;
    }

    public int compareTo(Object o) {
        if (o instanceof Library) {
            Library ol = (Library)o;
            return this.toString().compareTo(ol.toString());
        }
        if (o instanceof String) {
            String os = (String)o;
            return this.toString().compareTo(os);
        }
        return 0;
    }


    // =================================================================
    // Static library management portion
    // =================================================================
    
    public static TreeMap<String, TreeSet<Library>> libraryList = new TreeMap<String, TreeSet<Library>>();
    public static TreeMap<String, String> categoryNames = new TreeMap<String, String>();

    // A "group" consists of a type and a subtype separated by a colon.  Valid types are:
    // core, compiler, board, cat.  Subtypes are dependant on the type.
    //   core:<core name>
    //   compiler:<compiler name>
    //   board:<board name>
    //   cat:<category name>

    public static void addLibrary(String group, Library lib) {
        TreeSet<Library> setData = libraryList.get(group);
        if (setData == null) {
            setData = new TreeSet<Library>();
        }
        setData.add(lib);
        libraryList.put(group, setData);
    }

    public static TreeSet<Library> getLibraries(String group) {
        TreeSet<Library> outList = new TreeSet<Library>();

        TreeSet<Library> dataSet = libraryList.get(group);
        if (dataSet == null) {
            return null;
        }
        for (Library lib : dataSet) {
            outList.add(lib);
        }
        return outList;
    }

    public static TreeSet<Library> getLibraries(String group, String core) {
        TreeSet<Library> outList = new TreeSet<Library>();

        TreeSet<Library> dataSet = libraryList.get(group);
        if (dataSet == null) {
            return null;
        }
        for (Library lib : dataSet) {
            if (lib.worksWith(core)) {
                outList.add(lib);
            }
        }
        return outList;
    }

    public static TreeMap<String, TreeSet<Library>> getFilteredLibraries(String core) {
        TreeMap<String, TreeSet<Library>> outList = new TreeMap<String, TreeSet<Library>>();
        for (String cat : libraryList.keySet()) {
            TreeSet <Library> group = getLibraries(cat, core);
            if (group != null && group.size() > 0) {
                outList.put(cat, group);
            }
        }
        return outList;
    }

    public static void setCategoryName(String cat, String name) {
        categoryNames.put(cat, name);
    }

    public static String getCategoryName(String cat) {
        return categoryNames.get(cat);
    }

    public static void loadLibrariesFromFolder(File folder, String group) {
        loadLibrariesFromFolder(folder, group, "all");
    }

    public static void loadLibrariesFromFolder(File folder, String group, String core) {
        File[] list = folder.listFiles();
        if (list == null) {
            return;
        }
        Debug.message("Loading libraries from " + folder.getAbsolutePath());
        for (File f : list) {
            if (f.isDirectory()) {
                if (core.equals("all")) {
                    boolean sub = false;
                    for (String c : Base.cores.keySet()) {
                        if (f.getName().equals(c)) {
                            Debug.message("  Found sub-library core group " + f);
                            loadLibrariesFromFolder(f, group, f.getName());
                            sub = true;
                        }
                    }
                    if (sub) continue;
                }

                File files[] = f.listFiles();
                for (File sf : files) {
                    if ((sf.getName().equals(f.getName() + ".h") || (sf.getName().startsWith(f.getName() + "_") && sf.getName().endsWith(".h")))) {
                        Library newLibrary = new Library(sf, group, core);
                        if (newLibrary.isValid()) {
                            addLibrary(group, newLibrary);
                            Debug.message("    Adding new library " + newLibrary.getName() + " from " + f.getAbsolutePath());
                            if (newLibrary.getLibrariesFolder().exists()) {
                                loadLibrariesFromFolder(newLibrary.getLibrariesFolder(), group, core);
                            }
                        } else {
                            Debug.message("    Skipping invalid library " + f.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    // Load all the libraries from everywhere.

    public static void loadLibraries() {
        libraryList = new TreeMap<String, TreeSet<Library>>();
        categoryNames = new TreeMap<String, String>();
        // Start with the compiler.  It's rare that there would be any here.
        for (Compiler c : Base.compilers.values()) {
            setCategoryName("compiler:" + c.getName(), c.getDescription());
            loadLibrariesFromFolder(c.getLibrariesFolder(), "compiler:" + c.getName());
        }

        // Now we'll do the cores.  This is almost guaranteed to have libraries.
        for (Core c : Base.cores.values()) {
            setCategoryName("core:" + c.getName(), c.getDescription());
            loadLibrariesFromFolder(c.getLibrariesFolder(), "core:" + c.getName(), c.getName());
        }

        // And now boards.
        for (Board c : Base.boards.values()) {
            setCategoryName("board:" + c.getName(), c.getDescription());
            loadLibrariesFromFolder(c.getLibrariesFolder(), "core:" + c.getName());
        }

        // And finally let's work through the categories.
        for (String k : Base.preferences.childKeysOf("library")) {
            String cName = Base.preferences.get("library." + k + ".name");
            String cPath = Base.preferences.get("library." + k + ".path");
            if (cName != null && cPath != null) {
                File f = new File(cPath);
                if (f.exists() && f.isDirectory()) {
                    setCategoryName("cat:" + k, cName);
                    loadLibrariesFromFolder(f, "cat:" + k);
                }
            }
        }
    }

    public static Library getLibraryByName(String name, String core, String group) {
        TreeSet<Library> dataSet = libraryList.get(group);
        if (dataSet == null) {
            return null;
        }
        for (Library l : dataSet) {
            if (l.toString().equals(name)) {
                if (l.worksWith(core)) {
                    return l;
                }
            }
        }
        return null;
    }

    public static Library getLibraryByName(String name, String core) {
        for (String group : libraryList.keySet()) {
            Library l = getLibraryByName(name, core, group);
            if  (l != null) {
                return l;
            }
        }
        return null;
    }

    public static Library getLibraryByInclude(String include, String core) {
        if (!include.endsWith(".h")) {
            return null;
        }

        // Exact match?
        String name = include.substring(0, include.lastIndexOf("."));
        Library lib = getLibraryByName(name, core);
        if (lib != null) {
            return lib;
        }

        for (TreeSet<Library> dataSet : libraryList.values()) {
            for (Library l : dataSet) {
                if (l.worksWith(core)) {
                    if (l.hasHeader(include)) {
                        return l;
                    }
                }
            }
        }
        return null;
    }

    public static TreeSet<String> getLibraryCategories() {
        TreeSet<String> cats = new TreeSet<String>();
        for (String cat : libraryList.keySet()) {
            cats.add(cat);
        }
        return cats;
    }

    public static File getCategoryLocation(String group) {
        String[] bits = group.split(":");

        if (bits[0] == "cat") {
            return Base.preferences.getFile("library." + bits[1] + ".path");
        }

        if (bits[0] == "core") {
            Core c = Base.cores.get(bits[1]);
            if (c == null) {
                return null;
            }
            return c.getLibrariesFolder();
        }

        if (bits[0] == "compiler") {
            Compiler c = Base.compilers.get(bits[1]);
            if (c == null) {
                return null;
            }
            return c.getLibrariesFolder();
        }

        if (bits[0] == "board") {
            Board c = Base.boards.get(bits[1]);
            if (c == null) {
                return null;
            }
            return c.getLibrariesFolder();
        }
        return null;
    }
}

