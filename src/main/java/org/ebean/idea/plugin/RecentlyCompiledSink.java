/*
 * Copyright 2009 Mario Ivankovits
 *
 *     This file is part of Ebean-idea-plugin.
 *
 *     Ebean-idea-plugin is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Ebean-idea-plugin is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Ebean-idea-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ebean.idea.plugin;

import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Sink wrapper to get access to the recently compiled files
 */
public class RecentlyCompiledSink implements TranslatingCompiler.OutputSink {
    private final TranslatingCompiler.OutputSink delegate;

    private final List<CompiledItem> compiledItems = new ArrayList<CompiledItem>();

    public static class CompiledItem {
        private final String outputRoot;
        private final String outputPath;

        public CompiledItem(String outputRoot, String outputPath) {
            this.outputRoot = outputRoot;
            this.outputPath = outputPath;
        }

        public String getOutputRoot() {
            return outputRoot;
        }

        public String getOutputPath() {
            return outputPath;
        }
    }

    public RecentlyCompiledSink(TranslatingCompiler.OutputSink delegate) {
        this.delegate = delegate;
    }

    public void add(String outputRoot,
                    Collection<TranslatingCompiler.OutputItem> items,
                    VirtualFile[] filesToRecompile) {
        if (items != null && outputRoot != null) {
            for (TranslatingCompiler.OutputItem item : items) {
                if (item.getOutputPath() == null) {
                    continue;
                }

                compiledItems.add(new CompiledItem(outputRoot, item.getOutputPath()));
            }
        }

        if (delegate != null) {
            delegate.add(outputRoot, items, filesToRecompile);
        }
    }

    public List<CompiledItem> getCompiledItems() {
        return compiledItems;
    }
}
