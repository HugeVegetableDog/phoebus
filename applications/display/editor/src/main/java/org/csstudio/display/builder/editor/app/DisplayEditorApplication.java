/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

/** Display Runtime Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayEditorApplication implements AppResourceDescriptor
{
    public static final String NAME = "display_editor";
    public static final String DISPLAY_NAME = "Display Editor";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return DisplayModel.class.getResource("/icons/display.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return DisplayModel.FILE_EXTENSIONS;
    }

    @Override
    public DisplayEditorInstance create()
    {
        return new DisplayEditorInstance(this);
    }

    @Override
    public DisplayEditorInstance create(final URI resource)
    {
        // Turn URI into the actual file,
        // so that existing instance can be uniquely identified
        File file;
        try
        {
            file = ModelResourceUtil.getFile(resource);
            if (file == null)
            {
                // TODO For http:, offer download and the open local file
                throw new Exception("Cannot determine local file");
            }
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot load " + resource, ex);
            return null;
        }
        final URI file_resource = file.toURI();

        // Check for existing instance with that input
        final DisplayEditorInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, file_resource);
        if (existing != null)
        {   // Found one, raise it
            instance = existing.getApplication();
            instance.raise();
        }
        else
        {   // Nothing found, create new one
            instance = create();
            instance.loadDisplay(file_resource);
        }
        return instance;
    }
}
