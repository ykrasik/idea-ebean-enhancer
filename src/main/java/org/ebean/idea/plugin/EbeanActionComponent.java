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

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.ebean.idea.plugin.EbeanActionComponent.EbeanWeavingState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Maintains the per project activate flag and setup the compiler stuff appropriate
 *
 * @author Mario Ivankovits, mario@ops.co.at
 * @author yevgenyk - Updated 28/04/2014 for IDEA 13
 */
@State(name = "ebeanWeaving", storages = {
    @Storage(id = "ebeanWeaving", file = StoragePathMacros.WORKSPACE_FILE)
})
public class EbeanActionComponent implements ProjectComponent, PersistentStateComponent<EbeanWeavingState> {
    private static final Key<List<File>> COMPILED_FILES = new Key<>(EbeanActionComponent.class.getName() + ".COMPILED_FILES");

    private final Project project;
    private final CompiledFileCollector compiledFileCollector;

    private EbeanWeavingState ebeanWeavingState;

    public EbeanActionComponent(Project project) {
        this.project = project;
        this.compiledFileCollector = new CompiledFileCollector();
        this.ebeanWeavingState = new EbeanWeavingState();
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "Ebean Action Component";
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {
    }

    @Override
    public void projectOpened() {
    }

    @Override
    public void projectClosed() {
        setActivated(false);
    }

    public boolean isActivated() {
        return ebeanWeavingState.activated;
    }

    public void setActivated(boolean activated) {
        if (!this.ebeanWeavingState.activated && activated) {
            getCompilerManager().addCompilationStatusListener(compiledFileCollector);
        } else if (this.ebeanWeavingState.activated && !activated) {
            getCompilerManager().removeCompilationStatusListener(compiledFileCollector);
        }
        this.ebeanWeavingState.activated = activated;
    }

    private CompilerManager getCompilerManager() {
        return CompilerManager.getInstance(project);
    }

    @Nullable
    @Override
    public EbeanWeavingState getState() {
        return ebeanWeavingState;
    }

    @Override
    public void loadState(EbeanWeavingState ebeanWeavingState) {
        setActivated(ebeanWeavingState.activated);
        XmlSerializerUtil.copyBean(ebeanWeavingState, this.ebeanWeavingState);
    }

    public static class EbeanWeavingState {
        public boolean activated;
    }
}
