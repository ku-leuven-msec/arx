/*
 * ARX Data Anonymization Tool
 * Copyright 2012 - 2024 Fabian Prasser and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.gui.view.impl.menu;

import org.apache.commons.math3.util.Pair;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.gui.model.Model;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import java.util.stream.IntStream;

/**
 * A dialog for finding and replacing data items
 * 
 * @author Fabian Prasser
 */
public class DialogMergeWith extends TitleAreaDialog {

    /** Model */
    private String               title;
    /** Model */
    private String               message;
    /** Model */
    private Pair<String, String> value    = null;
    /** Model */
    private String[]             columns;

    /** View */
    private Button               okButton;
    /** View */
    private Combo                mergeColumn;
    /** View */
    private Text                 separator;
    /** View */
    private Label                errorMessage;

    /**
     * Creates a new instance
     *
     * @param parentShell
     * @param handle
     * @param column
     */
    public DialogMergeWith(Shell parentShell,
                           Model model,
                           DataHandle handle,
                           int column) {
        super(parentShell);
        this.title = Resources.getMessage("DialogMergeWith.0"); //$NON-NLS-1$
        this.message = Resources.getMessage("DialogMergeWith.1") + //$NON-NLS-1$
                       handle.getAttributeName(column) + Resources.getMessage("DialogMergeWith.2"); //$NON-NLS-1$
        this.columns = IntStream.range(0, handle.getNumColumns()).filter(i -> i!=column).mapToObj(handle::getAttributeName).toArray(String[]::new);
    }

    /**
     * Returns a pair containing the column name to merge with and the separator
     * 
     * @return the value
     */
    public Pair<String, String> getValue() {
        return value;
    }

    @Override
    public void setErrorMessage(String message) {
        
        // Check
        if (this.errorMessage.isDisposed()) return;
        
        // Set
        if (message != null) {
            this.errorMessage.setText(message);
        } else {
            this.errorMessage.setText(""); //$NON-NLS-1$
        }

        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
        Control button = getButton(IDialogConstants.OK_ID);
        if (button != null) {
            button.setEnabled(message == null);
        }
    }

    /**
     * Checks if all input is valid
     */
    private void checkValidity() {
        if(separator.getText().isEmpty()) {
            setErrorMessage(Resources.getMessage("DialogMergeWith.7"));
        }else{
            setErrorMessage(null);
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            value = new Pair<String, String>(columns[mergeColumn.getSelectionIndex()], separator.getText());
        } else {
            value = null;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setImages(Resources.getIconSet(newShell.getDisplay()));
    }


    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent,
                                IDialogConstants.OK_ID,
                                IDialogConstants.OK_LABEL,
                                true);
        okButton.setEnabled(true); //$NON-NLS-1$
        createButton(parent,
                     IDialogConstants.CANCEL_ID,
                     IDialogConstants.CANCEL_LABEL,
                     false);
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        setTitle(title); 
        setMessage(message);
        return contents;
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {

        Composite composite = (Composite) super.createDialogArea(parent);
        Composite base = new Composite(composite, SWT.NONE);
        base.setLayoutData(SWTUtil.createFillGridData());
        base.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());

        Label messageLabel = new Label(base, SWT.NONE);
        messageLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        messageLabel.setText(Resources.getMessage("DialogMergeWith.3")); //$NON-NLS-1$

        Label mergeColumnLabel = new Label(base, SWT.NONE);
        mergeColumnLabel.setText(Resources.getMessage("DialogMergeWith.4")); //$NON-NLS-1$

        this.mergeColumn = new Combo(base, SWT.READ_ONLY);
        mergeColumn.setItems(columns);
        mergeColumn.select(0);
        mergeColumn.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        mergeColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                checkValidity();
            }
        });

        Label separatorLabel = new Label(base, SWT.NONE);
        separatorLabel.setText(Resources.getMessage("DialogMergeWith.5")); //$NON-NLS-1$

        this.separator = new Text(base, SWT.BORDER);
        this.separator.setText("::");
        this.separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        this.separator.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                checkValidity();
            }
        });

        errorMessage = new Label(base, SWT.NONE);
        errorMessage.setLayoutData(GridDataFactory.fillDefaults()
                                                  .grab(true, true)
                                                  .span(2, 1)
                                                  .create());
        errorMessage.setBackground(errorMessage.getDisplay()
                                               .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessage.setForeground(GUIHelper.COLOR_RED);
        applyDialogFont(base);
        checkValidity();
        return composite;
    }

    @Override
    protected ShellListener getShellListener() {
        return new ShellAdapter() {
            @Override
            public void shellClosed(final ShellEvent event) {
                value = null;
                setReturnCode(Window.CANCEL);
            }
        };
    }
}
