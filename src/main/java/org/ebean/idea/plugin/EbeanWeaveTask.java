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

import com.avaje.ebean.enhance.agent.InputStreamTransform;
import com.avaje.ebean.enhance.agent.Transformer;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ActionRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * This task actually hand all successfully compiled classes over to the Ebean weaver.
 *
 * @author Mario Ivankovits, mario@ops.co.at
 */
public class EbeanWeaveTask implements CompileTask {
    private final EbeanActionComponent ebeanActionComponent;

    public EbeanWeaveTask(EbeanActionComponent ebeanActionComponent) {
        this.ebeanActionComponent = ebeanActionComponent;
    }

    /**
     * Given a content path and a (source or class) file path, resolve the package name of the file
     */
    public static String resolveClassName(String rootPath, String classPath) {
        if (rootPath == null || classPath == null) {
            return null;
        }

        String relativePath = classPath.substring(rootPath.length() + 1);
        int extensionPos = relativePath.lastIndexOf('.');
        return (extensionPos != -1) ? relativePath.substring(0, extensionPos).replace('/', '.') : null;
    }

    private boolean processItems(final CompileContext compileContext,
                                 final List<RecentlyCompiledSink.CompiledItem> compiledItems) {
        try {
            return ActionRunner.runInsideWriteAction(
                new ActionRunner.InterruptibleRunnableWithResult<Boolean>() {
                    public Boolean run() throws Exception {
                        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Ebean weaving started ...", null, -1, -1);

                        IdeaClassBytesReader icbr = new IdeaClassBytesReader(compileContext);

                        Transformer transformer = new Transformer(icbr, "detect=true;debug=0");

                        transformer.setLogout(new PrintStream(new ByteArrayOutputStream()) {
                            @Override
                            public void print(String message) {
                                compileContext.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
                            }

                            @Override
                            public void println(String message) {
                                compileContext.addMessage(CompilerMessageCategory.INFORMATION, message, null, -1, -1);
                            }
                        });

                        ProgressIndicator pi = compileContext.getProgressIndicator();
                        pi.setIndeterminate(true);
                        pi.setText("Ebean weaving");

                        InputStreamTransform isTransform = new InputStreamTransform(transformer, this.getClass().getClassLoader());

                        for (int i = 0; i < compiledItems.size(); i++) {
                            RecentlyCompiledSink.CompiledItem processingItem = compiledItems.get(i);

                            // create a className from the compiled filename
                            String className = resolveClassName(
                                processingItem.getOutputRoot(),
                                processingItem.getOutputPath());
                            if (className == null) {
                                continue;
                            }

                            VirtualFile outputFile = VfsUtil.findFileByURL(
                                VfsUtil.convertToURL(VfsUtil.pathToUrl(processingItem.getOutputPath())));

                            if (!isJavaClass(compileContext, outputFile)) {
                                continue;
                            }

                            pi.setText2(className);

                            InputStream is = outputFile.getInputStream();
                            byte[] transformed = isTransform.transform(className, is);
                            if (transformed != null) {
                                outputFile.setBinaryContent(transformed);
                            }
                        }

                        compileContext.addMessage(CompilerMessageCategory.INFORMATION, "Ebean weaving done!", null, -1, -1);

                        return Boolean.TRUE;
                    }
                }
            );
        } catch (Exception e) {
            compileContext.addMessage(CompilerMessageCategory.ERROR, e.getClass().getName() + ":" + e.getMessage(), null, -1, -1);
        }

        return Boolean.FALSE;
    }

    /**
     * Check if the file is a java class by peeking the first two magic bytes and see if we need a 0xCAFE ;-)
     */
    public static boolean isJavaClass(CompileContext compileContext, VirtualFile content) {
        byte[] buf = new byte[2];
        int read;
        InputStream is = null;
        try {
            is = content.getInputStream();
            read = is.read(buf, 0, 2);
        } catch (IOException e) {
            compileContext.addMessage(CompilerMessageCategory.ERROR, e.getClass().getName() + ":" + e.getMessage(), null, -1, -1);

            // problems reading file, treat as non-java file
            return false;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore this useless exception
                }
            }
        }
        if (read < buf.length) {
            return false;
        }
        return buf[0] == (byte) 0xCA && buf[1] == (byte) 0xFE;
    }

    public boolean execute(CompileContext compileContext) {
        if (!ebeanActionComponent.isActivated()) {
            return true;
        }

        return processItems(compileContext, RecentlyCompiledCollector.getRecentlyCompiled(compileContext));
    }
}
