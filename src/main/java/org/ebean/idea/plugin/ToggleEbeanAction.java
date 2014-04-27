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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

/**
 * Action for toggling Ebean weaving on and off
 *
 * @author Mario Ivankovits, mario@ops.co.at
 */
public class ToggleEbeanAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent anActionEvent) {
        Project currentProject = (Project) anActionEvent.getDataContext().getData(DataConstants.PROJECT);
        if (currentProject != null && currentProject.hasComponent(EbeanActionComponent.class)) {
            EbeanActionComponent action = currentProject.getComponent(EbeanActionComponent.class);
            return action.isActivated();
        }
        return false;
    }

    @Override
    public void setSelected(AnActionEvent anActionEvent, boolean selected) {
        Project currentProject = (Project) anActionEvent.getDataContext().getData(DataConstants.PROJECT);
        if (currentProject != null && currentProject.hasComponent(EbeanActionComponent.class)) {
            EbeanActionComponent action = currentProject.getComponent(EbeanActionComponent.class);
            action.setActivated(selected);
        }
    }
}