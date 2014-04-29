package org.ebean.idea.plugin;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yevgenyk - Updated 28/04/2014 for IDEA 13
 */
public class CompiledFileCollector implements CompilationStatusListener {
    private List<CompiledFile> compiledFiles;

    public CompiledFileCollector() {
        this.compiledFiles = new ArrayList<>();
    }

    @Override
    public void compilationFinished(boolean aborted,
                                    int errors,
                                    int warnings,
                                    CompileContext compileContext) {
        new EbeanWeaveTask(compileContext, compiledFiles).process();
        compiledFiles = new ArrayList<>();
    }

    @Override
    public void fileGenerated(String outputRoot, String relativePath) {
        // Collect all valid compiled '.class' files
        final CompiledFile compiledFile = createCompiledFile(outputRoot, relativePath);
        if (compiledFile != null) {
            compiledFiles.add(compiledFile);
        }
    }

    private CompiledFile createCompiledFile(String outputRoot, String relativePath) {
        if (outputRoot == null || relativePath == null || !relativePath.endsWith(".class")) {
            return null;
        }

        final File file = new File(outputRoot, relativePath);
        if (!file.exists() || !isJavaClass(file)) {
            return null;
        }

        final String className = resolveClassName(relativePath);

        return new CompiledFile(file, className);
    }

    /**
     * Given a content path and a class file path, resolve the fully qualified class name
     */
    private String resolveClassName(String relativePath) {
        final int extensionPos = relativePath.lastIndexOf('.');
        return relativePath.substring(0, extensionPos).replace('/', '.');
    }

    /**
     * Check if the file is a java class by peeking the first two magic bytes and see if we need a 0xCAFE ;-)
     */
    private boolean isJavaClass(File file) {
        try (InputStream is = new FileInputStream(file)) {
            final byte[] buf = new byte[2];
            final int read = is.read(buf, 0, 2);
            if (read < buf.length) {
                return false;
            }
            return buf[0] == (byte) 0xCA &&
                   buf[1] == (byte) 0xFE;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class CompiledFile {
        private final File file;
        private final String className;

        private CompiledFile(File file, String className) {
            this.file = file;
            this.className = className;
        }

        public File getFile() {
            return file;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String toString() {
            return "CompiledFile{" +
                "file=" + file +
                ", className='" + className + '\'' +
                '}';
        }
    }
}
