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

package uecide.app.editors;

import uecide.app.*;
import uecide.plugin.*;
import uecide.app.debug.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.JToolBar;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

public class code extends JPanel implements EditorBase {
    File file = null;
    boolean modified = false;
    RSyntaxTextArea textArea;
    RTextScrollPane scrollPane;
    SyntaxScheme scheme;
    Sketch sketch;
    Editor editor;
    JPanel findPanel = null;
    JTextField searchTerm = new JTextField();
    JButton findButton = new JButton("Find");
    SearchContext search = new SearchContext();
    JCheckBox matchCase = new JCheckBox("Case");
    JCheckBox searchBackwards = new JCheckBox("Back");
    JTextField replaceWith = new JTextField();
    JButton replaceButton = new JButton("Replace");
    JButton replaceAllButton = new JButton("All");
    JButton findCloseButton;

    JToolBar toolbar;

    public void openFindPanel() {
        if (findPanel == null) {
            ImageIcon closeIcon = Base.loadIconFromResource("tabs/close.png");
            findCloseButton = new JButton(closeIcon);
            findCloseButton.setBorder(new EmptyBorder(0, 2, 0, 2));
            findCloseButton.setContentAreaFilled(false);
            findPanel = new JPanel();
            findPanel.setLayout(new BoxLayout(findPanel, BoxLayout.LINE_AXIS));
            if (Base.preferences.getBoolean("editor.keepfindopen") == false) {
                findPanel.add(findCloseButton);
            }
            findPanel.add(searchTerm);
            findPanel.add(findButton);
            findPanel.add(matchCase);
            findPanel.add(searchBackwards);
            findPanel.add(replaceWith);
            findPanel.add(replaceButton);
            findPanel.add(replaceAllButton);
            add(findPanel, BorderLayout.SOUTH);
            revalidate();
            repaint();
            findButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (findNext(searchTerm.getText(), matchCase.isSelected(), searchBackwards.isSelected())) {
                        searchTerm.setBackground(new Color(255, 255, 255));
                    } else {
                        searchTerm.setBackground(new Color(255, 128, 128));
                    }
                }
            });
            searchTerm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (findNext(searchTerm.getText(), matchCase.isSelected(), searchBackwards.isSelected())) {
                        searchTerm.setBackground(new Color(255, 255, 255));
                    } else {
                        searchTerm.setBackground(new Color(255, 128, 128));
                    }
                }
            });
            replaceWith.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceNext(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });
            replaceButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceNext(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });
            replaceAllButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    replaceAll(searchTerm.getText(), replaceWith.getText(), matchCase.isSelected(), searchBackwards.isSelected());
                }
            });

            findCloseButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeFindAndReplace();
                }
            });
        }
        searchTerm.requestFocus();
    }

    public void closeFindAndReplace() {
        remove(findPanel);
        findPanel = null;
        revalidate();
        repaint();
    }

    public boolean findNext(String text, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchResult res = SearchEngine.find(textArea, search);
        if (!res.wasFound()) {
            textArea.setCaretPosition(0);
            int cp = textArea.getCaretPosition();
            res = SearchEngine.find(textArea, search);
            if (!res.wasFound()) {
                textArea.setCaretPosition(cp);
                return false;
            }
        }
        return true;
    }

    public void replaceNext(String text, String rep, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setReplaceWith(rep);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchEngine.replace(textArea, search);
    }

    public void replaceAll(String text, String rep, boolean mc, boolean back) {
        search.setSearchFor(text);
        search.setReplaceWith(rep);
        search.setMatchCase(mc);
        search.setSearchForward(!back);
        SearchEngine.replaceAll(textArea, search);
    }

    public code(Sketch s, File f, Editor e) {
        editor = e;
        sketch = s;
        this.setLayout(new BorderLayout());
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(FileType.getSyntaxStyle(f.getName()));

        Document d = textArea.getDocument();
        d.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }
        });

        scrollPane = new RTextScrollPane(textArea);
        toolbar = new JToolBar();
        toolbar.setVisible(!Base.preferences.getBoolean("editor.subtoolbar.hidden"));
        this.add(toolbar, BorderLayout.NORTH);

        Editor.addToolbarButton(toolbar, "toolbar/edit-copy.png", "Copy", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.copy();
            }
        });
        Editor.addToolbarButton(toolbar, "toolbar/edit-cut.png", "Cut", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.cut();
            }
        });

        Editor.addToolbarButton(toolbar, "toolbar/edit-paste.png", "Paste", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.paste();
            }
        });

        toolbar.addSeparator();
        Editor.addToolbarButton(toolbar, "toolbar/edit-undo.png", "Undo", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.undoLastAction();
            }
        });

        JButton redoButton = Editor.addToolbarButton(toolbar, "toolbar/edit-redo.png", "Redo", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.redoLastAction();
            }
        });

        toolbar.addSeparator();
        JButton indentButton = Editor.addToolbarButton(toolbar, "toolbar/format-indent-more.png", "Increase Indent");
        indentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                increaseIndent(e);
            }
        });
        JButton outdentButton = Editor.addToolbarButton(toolbar, "toolbar/format-indent-less.png", "Decrease Indent");
        outdentButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                decreaseIndent(e);
            }
        });
        toolbar.addSeparator();
        editor.addPluginsToToolbar(toolbar, Plugin.TOOLBAR_TAB);

        refreshSettings();
        this.add(scrollPane, BorderLayout.CENTER);
        if (f != null) {
            loadFile(f);
        }
        if (Base.preferences.getBoolean("editor.keepfindopen")) {
            openFindPanel();
        }
    }

    public void requestFocus() {
        textArea.requestFocus();
    }

    public void refreshSettings() {
        boolean external = Base.preferences.getBoolean("editor.external");
        textArea.setEditable(!external);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setMarkOccurrences(true);
        if (Base.preferences.get("editor.tabsize") != null) {
            textArea.setTabSize(Base.preferences.getInteger("editor.tabsize"));
        } else {
            textArea.setTabSize(4);
        }
        textArea.setTabsEmulated(Base.preferences.getBoolean("editor.expandtabs"));
        textArea.setPaintTabLines(Base.preferences.getBoolean("editor.showtabs"));

        scrollPane.setFoldIndicatorEnabled(true);
        setBackground( external ?
            Base.theme.getColor("editor.external.bgcolor") :
            Base.theme.getColor("editor.bgcolor")
        );

        textArea.setForeground(Base.theme.getColor("editor.fgcolor"));
        textArea.setFont(Base.preferences.getFont("editor.font"));
        Gutter g = scrollPane.getGutter();
        if (Base.theme.get("editor.gutter.bgcolor") != null) {
            g.setBackground(Base.theme.getColor("editor.gutter.bgcolor"));
        }
        if (Base.theme.get("editor.gutter.fgcolor") != null) {
            g.setLineNumberColor(Base.theme.getColor("editor.gutter.fgcolor"));
        }
        if (Base.theme.get("editor.line.bgcolor") != null) {
            textArea.setCurrentLineHighlightColor(Base.theme.getColor("editor.line.bgcolor"));
        }
        textArea.setFadeCurrentLineHighlight(Base.theme.getBoolean("editor.line.fade"));
        textArea.setHighlightCurrentLine(Base.theme.getBoolean("editor.line.enabled"));
        textArea.setRoundedSelectionEdges(Base.theme.getBoolean("editor.select.rounded"));
        if (Base.theme.get("editor.caret.fgcolor") != null) {
            textArea.setCaretColor(Base.theme.getColor("editor.caret.fgcolor"));
        }

        if (Base.theme.get("editor.caret.style.insert") != null) {
            if (Base.theme.get("editor.caret.style.insert").equals("box")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_BORDER_STYLE);
            }
            if (Base.theme.get("editor.caret.style.insert").equals("block")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.BLOCK_STYLE);
            }
            if (Base.theme.get("editor.caret.style.insert").equals("line")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);
            }
            if (Base.theme.get("editor.caret.style.insert").equals("thick")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
            }
            if (Base.theme.get("editor.caret.style.insert").equals("underline")) {
                textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.UNDERLINE_STYLE);
            }
        }

        if (Base.theme.get("editor.caret.style.replace") != null) {
            if (Base.theme.get("editor.caret.style.replace").equals("box")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_BORDER_STYLE);
            }
            if (Base.theme.get("editor.caret.style.replace").equals("block")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.BLOCK_STYLE);
            }
            if (Base.theme.get("editor.caret.style.replace").equals("line")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.VERTICAL_LINE_STYLE);
            }
            if (Base.theme.get("editor.caret.style.replace").equals("thick")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.THICK_VERTICAL_LINE_STYLE);
            }
            if (Base.theme.get("editor.caret.style.replace").equals("underline")) {
                textArea.setCaretStyle(RSyntaxTextArea.OVERWRITE_MODE, CaretStyle.UNDERLINE_STYLE);
            }
        }

        if (Base.theme.get("editor.markall.bgcolor") != null) {
            textArea.setMarkOccurrencesColor(Base.theme.getColor("editor.markall.bgcolor"));
        }
        textArea.setPaintMarkOccurrencesBorder(Base.theme.getBoolean("editor.markall.border"));
        if (Base.theme.get("editor.bracket.bgcolor") != null) {
            textArea.setMatchedBracketBGColor(Base.theme.getColor("editor.bracket.bgcolor"));
        }
        if (Base.theme.get("editor.bracket.bordercolor") != null) {
            textArea.setMatchedBracketBorderColor(Base.theme.getColor("editor.bracket.bordercolor"));
        }
        textArea.setPaintMatchedBracketPair(Base.theme.getBoolean("editor.bracket.pair"));

        if (Base.theme.get("editor.select.bgcolor") != null) {
            textArea.setSelectionColor(Base.theme.getColor("editor.select.bgcolor"));
        }

        scheme = textArea.getSyntaxScheme();
        applyThemeSettings();

    }

    public boolean loadFile(File f) {
        file = f;
        textArea.setText(sketch.getFileContent(file));
        textArea.setCaretPosition(0);
        scrollTo(0);
        setModified(false);
        return true;
    }

    public boolean isModified() {
        return modified;
    }

    public String getText() {
        return textArea.getText();
    }
    
    public String getText(int s, int e) {
        try {
            return textArea.getText(s, e);
        } catch (Exception ex) {
            return "";
        }
    }
    
    public void setText(String text) {
        textArea.setText(text);
    }

    public void setModified(boolean m) {
        if (m != modified) {
            modified = m;
            int tab = editor.getTabByEditor(this);
            if (tab > -1) {
                editor.setTabModified(tab, m);
            }
        }
    }

    public void scrollTo(final int pos) {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() { 
               scrollPane.getVerticalScrollBar().setValue(pos);
           }
        });
    }

    public File getFile() {
        return file;
    }

    public void applyThemeFGColor(int index, String cs) {
        if (Base.theme.get(cs) != null) {
            Color c = Base.theme.getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.foreground = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeBGColor(int index, String cs) {
        if (Base.theme.get(cs) != null) {
            Color c = Base.theme.getColor(cs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.background = c;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeFont(int index, String fs) {
        if (Base.theme.get(fs) != null) {
            Font f = Base.theme.getFont(fs);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.font = f;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeUnderline(int index, String us) {
        if (Base.theme.get(us) != null) {
            boolean u = Base.theme.getBoolean(us);
            org.fife.ui.rsyntaxtextarea.Style s = scheme.getStyle(index);
            s.underline = u;
            scheme.setStyle(index, s);
        }
    }

    public void applyThemeSettings() {

        // Annotations
        applyThemeFGColor(SyntaxScheme.ANNOTATION,                      "editor.annotation.fgcolor");
        applyThemeBGColor(SyntaxScheme.ANNOTATION,                      "editor.annotation.bgcolor");
        applyThemeFont(SyntaxScheme.ANNOTATION,                         "editor.annotation.font");
        applyThemeUnderline(SyntaxScheme.ANNOTATION,                    "editor.annotation.underline");

        // Global comments
        applyThemeFGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_DOCUMENTATION,              "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_DOCUMENTATION,         "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_EOL,                        "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_EOL,                   "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_KEYWORD,                    "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_KEYWORD,               "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MARKUP,                     "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MARKUP,                "editor.comment.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MULTILINE,                  "editor.comment.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MULTILINE,             "editor.comment.underline");

        // Fine granied comments
        applyThemeFGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.documentation.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_DOCUMENTATION,           "editor.comment.documentation.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_DOCUMENTATION,              "editor.comment.documentation.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_DOCUMENTATION,         "editor.comment.documentation.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.eol.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_EOL,                     "editor.comment.eol.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_EOL,                        "editor.comment.eol.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_EOL,                   "editor.comment.eol.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.keyword.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_KEYWORD,                 "editor.comment.keyword.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_KEYWORD,                    "editor.comment.keyword.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_KEYWORD,               "editor.comment.keyword.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.markup.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MARKUP,                  "editor.comment.markup.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MARKUP,                     "editor.comment.markup.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MARKUP,                "editor.comment.markup.underline");
        applyThemeFGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.multiline.fgcolor");
        applyThemeBGColor(SyntaxScheme.COMMENT_MULTILINE,               "editor.comment.multiline.bgcolor");
        applyThemeFont(SyntaxScheme.COMMENT_MULTILINE,                  "editor.comment.multiline.font");
        applyThemeUnderline(SyntaxScheme.COMMENT_MULTILINE,             "editor.comment.multiline.underline");

        // Data Type
        applyThemeFGColor(SyntaxScheme.DATA_TYPE,                       "editor.datatype.fgcolor");
        applyThemeBGColor(SyntaxScheme.DATA_TYPE,                       "editor.datatype.bgcolor");
        applyThemeFont(SyntaxScheme.DATA_TYPE,                          "editor.datatype.font");
        applyThemeUnderline(SyntaxScheme.DATA_TYPE,                     "editor.datatype.underline");

        // Errors global
        applyThemeFGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_CHAR,                         "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_CHAR,                    "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_IDENTIFIER,                   "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_IDENTIFIER,              "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_NUMBER_FORMAT,                "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_NUMBER_FORMAT,           "editor.error.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_STRING_DOUBLE,                "editor.error.font");
        applyThemeUnderline(SyntaxScheme.ERROR_STRING_DOUBLE,           "editor.error.underline");

        // Errors fine grained
        applyThemeFGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.char.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_CHAR,                      "editor.error.char.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_CHAR,                         "editor.error.char.font");
        applyThemeUnderline(SyntaxScheme.ERROR_CHAR,                    "editor.error.char.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.identifier.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_IDENTIFIER,                "editor.error.identifier.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_IDENTIFIER,                   "editor.error.identifier.font");
        applyThemeUnderline(SyntaxScheme.ERROR_IDENTIFIER,              "editor.error.identifier.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.number.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_NUMBER_FORMAT,             "editor.error.number.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_NUMBER_FORMAT,                "editor.error.number.font");
        applyThemeUnderline(SyntaxScheme.ERROR_NUMBER_FORMAT,           "editor.error.number.underline");
        applyThemeFGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.string.fgcolor");
        applyThemeBGColor(SyntaxScheme.ERROR_STRING_DOUBLE,             "editor.error.string.bgcolor");
        applyThemeFont(SyntaxScheme.ERROR_STRING_DOUBLE,                "editor.error.string.font");
        applyThemeUnderline(SyntaxScheme.ERROR_STRING_DOUBLE,           "editor.error.string.underline");

        // Function
        applyThemeFGColor(SyntaxScheme.FUNCTION,                        "editor.function.fgcolor");
        applyThemeBGColor(SyntaxScheme.FUNCTION,                        "editor.function.bgcolor");
        applyThemeFont(SyntaxScheme.FUNCTION,                           "editor.function.font");
        applyThemeUnderline(SyntaxScheme.FUNCTION,                      "editor.function.underline");

        // Identifier
        applyThemeFGColor(SyntaxScheme.IDENTIFIER,                      "editor.identifier.fgcolor");
        applyThemeBGColor(SyntaxScheme.IDENTIFIER,                      "editor.identifier.bgcolor");
        applyThemeFont(SyntaxScheme.IDENTIFIER,                         "editor.identifier.font");
        applyThemeUnderline(SyntaxScheme.IDENTIFIER,                    "editor.identifier.underline");

        // Literal globals
        applyThemeFGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BACKQUOTE,                  "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BACKQUOTE,             "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BOOLEAN,                    "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BOOLEAN,               "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_CHAR,                       "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_CHAR,                  "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,         "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,    "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_FLOAT,               "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_FLOAT,          "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,         "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,    "editor.literal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,        "editor.literal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,   "editor.literal.underline");

        // Literal fine grained
        applyThemeFGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.backquote.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BACKQUOTE,               "editor.literal.backquote.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BACKQUOTE,                  "editor.literal.backquote.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BACKQUOTE,             "editor.literal.backquote.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.boolean.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_BOOLEAN,                 "editor.literal.boolean.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_BOOLEAN,                    "editor.literal.boolean.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_BOOLEAN,               "editor.literal.boolean.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.char.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_CHAR,                    "editor.literal.char.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_CHAR,                       "editor.literal.char.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_CHAR,                  "editor.literal.char.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.decimal.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,      "editor.literal.decimal.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,         "editor.literal.decimal.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_DECIMAL_INT,    "editor.literal.decimal.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.float.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_FLOAT,            "editor.literal.float.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_FLOAT,               "editor.literal.float.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_FLOAT,          "editor.literal.float.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.hex.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,      "editor.literal.hex.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,         "editor.literal.hex.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_NUMBER_HEXADECIMAL,    "editor.literal.hex.underline");
        applyThemeFGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.string.fgcolor");
        applyThemeBGColor(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,     "editor.literal.string.bgcolor");
        applyThemeFont(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,        "editor.literal.string.font");
        applyThemeUnderline(SyntaxScheme.LITERAL_STRING_DOUBLE_QUOTE,   "editor.literal.string.underline");

        // Operators
        applyThemeFGColor(SyntaxScheme.OPERATOR,                        "editor.operator.fgcolor");
        applyThemeBGColor(SyntaxScheme.OPERATOR,                        "editor.operator.bgcolor");
        applyThemeFont(SyntaxScheme.OPERATOR,                           "editor.operator.font");
        applyThemeUnderline(SyntaxScheme.OPERATOR,                      "editor.operator.underline");

        // Preprocessor
        applyThemeFGColor(SyntaxScheme.PREPROCESSOR,                    "editor.preprocessor.fgcolor");
        applyThemeBGColor(SyntaxScheme.PREPROCESSOR,                    "editor.preprocessor.bgcolor");
        applyThemeFont(SyntaxScheme.PREPROCESSOR,                       "editor.preprocessor.font");
        applyThemeUnderline(SyntaxScheme.PREPROCESSOR,                  "editor.preprocessor.underline");
        
        // Regex
        applyThemeFGColor(SyntaxScheme.REGEX,                           "editor.regex.fgcolor");
        applyThemeBGColor(SyntaxScheme.REGEX,                           "editor.regex.bgcolor");
        applyThemeFont(SyntaxScheme.REGEX,                              "editor.regex.font");
        applyThemeUnderline(SyntaxScheme.REGEX,                         "editor.regex.underline");

        // Reserved Word global
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD,                      "editor.reserved.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD,                 "editor.reserved.underline");
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD_2,                    "editor.reserved.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD_2,               "editor.reserved.underline");

        // Reserved Word fine grained
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.1.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD,                   "editor.reserved.1.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD,                      "editor.reserved.1.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD,                 "editor.reserved.1.underline");
        applyThemeFGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.2.fgcolor");
        applyThemeBGColor(SyntaxScheme.RESERVED_WORD_2,                 "editor.reserved.2.bgcolor");
        applyThemeFont(SyntaxScheme.RESERVED_WORD_2,                    "editor.reserved.2.font");
        applyThemeUnderline(SyntaxScheme.RESERVED_WORD_2,               "editor.reserved.2.underline");

        // Variable
        applyThemeFGColor(SyntaxScheme.VARIABLE,                        "editor.variable.fgcolor");
        applyThemeBGColor(SyntaxScheme.VARIABLE,                        "editor.variable.bgcolor");
        applyThemeFont(SyntaxScheme.VARIABLE,                           "editor.variable.font");
        applyThemeUnderline(SyntaxScheme.VARIABLE,                      "editor.variable.underline");

        applyThemeFGColor(SyntaxScheme.SEPARATOR,                       "editor.brackets.fgcolor");
        applyThemeBGColor(SyntaxScheme.SEPARATOR,                       "editor.brackets.bgcolor");
        applyThemeFont(SyntaxScheme.SEPARATOR,                          "editor.brackets.font");
        applyThemeUnderline(SyntaxScheme.SEPARATOR,                     "editor.brackets.underline");


        textArea.setSyntaxScheme(scheme);
    }

    public void revertFile() {
        if (!isModified()) {
            return;
        }
    
        int n = editor.twoOptionBox(JOptionPane.WARNING_MESSAGE,
            "Revert File?", 
            Translate.w("You have unsaved changes.  Reverting the file will lose those changes.  Are you sure?", 40, "\n"),
            "Yes",
            "No"
        );
        if (n != 0) {
            return;
        }

        reloadFile();
    }

    public void populateMenu(JMenu menu, int flags) {
        JMenuItem item;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();


        switch (flags) {
            case (Plugin.MENU_FILE | Plugin.MENU_MID):
                item = new JMenuItem(Translate.t("Revert File"));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        revertFile();
                    }
                });
                menu.add(item);
                break;

            case (Plugin.MENU_EDIT | Plugin.MENU_TOP):
                item = new JMenuItem(Translate.t("Copy"));
                item.setAccelerator(KeyStroke.getKeyStroke('C', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.copy();
                    }
                });
                menu.add(item);
                item = new JMenuItem(Translate.t("Cut"));
                item.setAccelerator(KeyStroke.getKeyStroke('X', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.cut();
                    }
                });
                menu.add(item);
                item = new JMenuItem(Translate.t("Paste"));
                item.setAccelerator(KeyStroke.getKeyStroke('V', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.paste();
                    }
                });
                menu.add(item);
                break;

            case (Plugin.MENU_EDIT | Plugin.MENU_MID):
                item = new JMenuItem(Translate.t("Select All"));
                item.setAccelerator(KeyStroke.getKeyStroke('A', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.selectAll();
                    }
                });
                menu.add(item);
                break;

            case (Plugin.MENU_EDIT | Plugin.MENU_BOTTOM):
                item = new JMenuItem(Translate.t("Find & Replace"));
                item.setAccelerator(KeyStroke.getKeyStroke('F', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        openFindPanel();
                    }
                });
                menu.add(item);
                item = new JMenuItem(Translate.t("Undo"));
                item.setAccelerator(KeyStroke.getKeyStroke('Z', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.undoLastAction();
                    }
                });
                menu.add(item);
                item = new JMenuItem(Translate.t("Redo"));
                item.setAccelerator(KeyStroke.getKeyStroke('Y', modifiers));
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        textArea.redoLastAction();
                    }
                });
                menu.add(item);
                break;
        }
    }

    // Save the file contents to disk
    public boolean save() {
        if (file == null) {
            return false;
        }

        try {
            PrintWriter pw = new PrintWriter(file);
            pw.write(textArea.getText());
            pw.close();
            setModified(false);
            return true;
        } catch (Exception e) {
            sketch.error(e);
        }
        return false;
    }

    public boolean saveTo(File f) {
        file = f;
        return save();
    }
    
    public void reloadFile() {
        int cp = textArea.getCaretPosition();
        textArea.setText(sketch.getFileContent(file, true));
        textArea.setCaretPosition(cp);
     //   scrollTo(0);
        setModified(false);
    }

    public void insertAtCursor(String text) {
        textArea.insert(text, textArea.getCaretPosition());
    }

    public void insertAtStart(String text) {
        textArea.insert(text, 0);
    }

    public void insertAtEnd(String text) {
        textArea.append(text);
    }

    public void increaseIndent(ActionEvent e) {
        boolean wasSelected = false;
        ActionMap map = textArea.getActionMap();
        Action act;
        act = map.get(DefaultEditorKit.insertTabAction);
        String selection = textArea.getSelectedText();
        if (selection == null || selection.equals("")) {
            int off = textArea.getCaretPosition();
            textArea.setCaretPosition(textArea.getLineStartOffsetOfCurrentLine());
            act.actionPerformed(e);
            textArea.setCaretPosition(off+1); 
        } else {
            act.actionPerformed(e);
        }
    }

    public void decreaseIndent(ActionEvent e) {
        ActionMap map = textArea.getActionMap();
        Action di = map.get(RSyntaxTextAreaEditorKit.rstaDecreaseIndentAction);
        di.actionPerformed(e);
    }


    public void setSelection(int start, int end) {
        textArea.setSelectionStart(start);
        textArea.setSelectionEnd(end);
    }

    public int getSelectionStart() {
        return textArea.getSelectionStart();
    }

    public int getSelectionEnd() {
        return textArea.getSelectionEnd();
    }

    public void setSelectedText(String text) {
        textArea.replaceSelection(text);
    }

    public String getSelectedText() {
        return textArea.getSelectedText();
    }
}
