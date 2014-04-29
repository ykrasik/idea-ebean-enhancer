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

import com.avaje.ebean.enhance.agent.ClassBytesReader;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.IOException;

/**
 * Lookup a class file by given class name.
 *
 * @author Mario Ivankovits, mario@ops.co.at
 * @author yevgenyk - Updated 28/04/2014 for IDEA 13
 */
public class IdeaClassBytesReader implements ClassBytesReader {
    final CompileContext compileContext;

    public IdeaClassBytesReader(final CompileContext compileContext) {
        this.compileContext = compileContext;
    }

    @Override
    public byte[] getClassBytes(String classNamePath, ClassLoader classLoader) {
        // create a Psi compatible classname
        final String className = classNamePath.replace('/', '.').replace('$', '.');

        final PsiClass psiClass = JavaPsiFacade.getInstance(compileContext.getProject()).findClass(
            className,
            GlobalSearchScope.allScope(compileContext.getProject()));
        if (psiClass == null) {
            return null;
        }

        final PsiFile file = psiClass.getContainingFile();
        if (file == null) {
            // not file attached!?
            return null;
        }

        final VirtualFile virtualFile;
        if (file instanceof PsiCompiledElement) {
            // usually, this element is a parsed class file already (library), so we can take it
            virtualFile = psiClass.getContainingFile().getVirtualFile();
        } else {
            virtualFile = file.getVirtualFile();
        }

        if (virtualFile == null) {
            return null;
        }

        try {
            return virtualFile.contentsToByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}