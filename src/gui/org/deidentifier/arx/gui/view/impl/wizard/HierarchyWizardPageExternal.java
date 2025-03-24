/*
 * ARX Data Anonymization Tool
 * Copyright 2012 - 2023 Fabian Prasser and contributors
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

package org.deidentifier.arx.gui.view.impl.wizard;

import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A page for configuring the custom builder.
 */
public class HierarchyWizardPageExternal<T> extends HierarchyWizardPageBuilder<T> {

    /** Var. */
    private final HierarchyWizardModelExternal<T>  model;

    private Button btnChoose;
    private Button btnApplyHierarchy;
    private Combo comboLocation;

    private final Controller controller;
    private Group group;
    private Button useDistinctOnly;
    private Text txtDelimiter;
    private Label txtDescription;
    private boolean canFinish = false;

    /**
     * Creates a new instance.
     *
     * @param wizard
     * @param model
     * @param finalPage
     */
    public HierarchyWizardPageExternal(final Controller controller, final HierarchyWizard<T> wizard,
                                       final HierarchyWizardModel<T> model,
                                       final HierarchyWizardPageFinal<T> finalPage) {
        super(wizard, model.getExternalModel(), finalPage);
        this.model = model.getExternalModel();
        this.controller = controller;
        setTitle(Resources.getMessage("HierarchyWizardPageScript.0")); //$NON-NLS-1$
        setDescription(Resources.getMessage("HierarchyWizardPageScript.1")); //$NON-NLS-1$
        setPageComplete(true);
    }

    @Override
    public void createControl(final Composite parent) {

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(4, false));

        Label lblLocation = new Label(composite, SWT.NONE);
        lblLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        lblLocation.setText(Resources.getMessage("HierarchyWizardPageScript.3")); //$NON-NLS-

        comboLocation = new Combo(composite, SWT.READ_ONLY);
        comboLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        comboLocation.addSelectionListener(new SelectionAdapter() {
            /**
             * Resets {@link customSeparator} and evaluates page
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                if(!model.getPath().equals(comboLocation.getText())) {
                    model.setPath(comboLocation.getText());
                    Display d = composite.getDisplay();
                    Cursor cursor = new Cursor(d,SWT.CURSOR_WAIT);
                    Shell s = composite.getShell();
                    s.setCursor(cursor);

                    evaluatePage();

                    cursor = new Cursor(d, SWT.CURSOR_ARROW);
                    s.setCursor(cursor);
                }
            }
        });


        btnChoose = new Button(composite, SWT.NONE);
        btnChoose.setText(Resources.getMessage("HierarchyWizardPageScript.4"));
        btnChoose.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));//$NON-NLS-1$
        btnChoose.addSelectionListener(new SelectionAdapter() {

            /**
             * Opens a file selection dialog for exe files
             *
             * If a valid exe file was selected, it is added to
             * {@link #comboLocation} when it wasn't already there. It is then
             * preselected within {@link #comboLocation} and the page is
             * evaluated {@see #evaluatePage}.
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {

                /* Open file dialog */
                final String path = controller.actionShowOpenFileDialog(getShell(),
                        "*"); //$NON-NLS-1$
                if (path == null) {
                    return;
                }

                /* Check whether path was already added */
                if (comboLocation.indexOf(path) == -1) {
                    comboLocation.add(path, 0);
                }

                /* Select path and notify comboLocation about change */
                comboLocation.select(comboLocation.indexOf(path));
                comboLocation.notifyListeners(SWT.Selection, null);
            }
        });

        Label lblDelimiter = new Label(composite, SWT.NONE);
        lblDelimiter.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblDelimiter.setText(Resources.getMessage("HierarchyWizardPageScript.2"));

        txtDelimiter = new Text(composite, SWT.BORDER);
        txtDelimiter.setLayoutData(SWTUtil.createFillHorizontallyGridData());
        //txtDelimiter.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        txtDelimiter.setText(model.getSeparator());
        txtDelimiter.setEditable(true);
        txtDelimiter.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if(!model.getSeparator().equals(txtDelimiter.getText())) {
                    model.setSeparator(txtDelimiter.getText());
                    Display d = composite.getDisplay();
                    Cursor cursor = new Cursor(d,SWT.CURSOR_WAIT);
                    Shell s = composite.getShell();
                    s.setCursor(cursor);

                    evaluatePage();

                    cursor = new Cursor(d, SWT.CURSOR_ARROW);
                    s.setCursor(cursor);
                }
            }
        });
        decorate(txtDelimiter);

        useDistinctOnly = new Button(composite,SWT.CHECK);
        useDistinctOnly.setText(Resources.getMessage("HierarchyWizardPageScript.5"));
        useDistinctOnly.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1) );
        useDistinctOnly.setSelection(true);
        useDistinctOnly.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setPageComplete(false);
                model.setUniqueOnly(useDistinctOnly.getSelection());
                canFinish = false;
                setPageComplete(true);
            }
        });

        group = new Group(composite, SWT.SHADOW_ETCHED_IN);
        group.setText(Resources.getMessage("HierarchyWizardPageScript.6"));
        group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
        GridLayout layout = SWTUtil.createGridLayout(3, false);
        layout.horizontalSpacing = 10;
        group.setLayout(layout);

        txtDescription = new Label(composite, SWT.WRAP);
        txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true, 4, 4) );

        btnApplyHierarchy = new Button(composite, SWT.NONE);
        btnApplyHierarchy.setText(Resources.getMessage("HierarchyWizardPageScript.8"));
        btnApplyHierarchy.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false, 1, 1));//$NON-NLS-1$
        btnApplyHierarchy.setEnabled(false);
        btnApplyHierarchy.addSelectionListener(new SelectionAdapter() {

            /**
             * Opens a file selection dialog for exe files
             *
             * If a valid exe file was selected, it is added to
             * {@link #comboLocation} when it wasn't already there. It is then
             * preselected within {@link #comboLocation} and the page is
             * evaluated {@see #evaluatePage}.
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                setPageComplete(false);

                Display d = composite.getDisplay();
                Cursor cursor = new Cursor(d,SWT.CURSOR_WAIT);
                Shell s = composite.getShell();
                s.setCursor(cursor);

                setErrorMessage(null);
                try {
                    model.buildInternal();
                }catch (Exception e) {
                    canFinish = false;
                    model.update();
                    cursor = new Cursor(d, SWT.CURSOR_ARROW);
                    s.setCursor(cursor);
                    updateApplyButton();
                    setErrorMessage(e.getMessage());
                    return;
                }

                canFinish = true;
                model.update();
                cursor = new Cursor(d, SWT.CURSOR_ARROW);
                s.setCursor(cursor);
                updateApplyButton();
                setPageComplete(true);

            }
        });

        updatePage();
        setControl(composite);
    }

    @Override
    public boolean isPageComplete() {
        return canFinish;
    }

    @Override
    public void setVisible(boolean value){
        super.setVisible(value);
        model.setVisible(value);
    }

    @Override
    public void updatePage() {
        txtDelimiter.setText(model.getSeparator());
        comboLocation.add(model.getPath(),0);
        comboLocation.select(0);
        useDistinctOnly.setSelection(model.isUniqueOnly());
        evaluatePage();

        if(model.getHierarchy()!=null){
            canFinish = true;
            updateApplyButton();
            setPageComplete(true);
        }

        //model.update();
    }

    public void updateApplyButton() {
        if (comboLocation.getText().isEmpty() || txtDelimiter.getText().isEmpty()) {
            btnApplyHierarchy.setEnabled(false);
        } else {
            for(String paramValue:model.getParameters().values()) {
                if(paramValue.isEmpty()){
                    btnApplyHierarchy.setEnabled(false);
                    return;
                }
            }
            btnApplyHierarchy.setEnabled(!canFinish);
        }
    }

    /**
     * Decorates a text field for domain properties.
     *
     * @param text
     */
    private void decorate(final Text text) {
        final ControlDecoration decoration = new ControlDecoration(text, SWT.RIGHT);
        text.addModifyListener(new ModifyListener(){
            @Override
            public void modifyText(ModifyEvent arg0) {
                if (text.getText().isEmpty()) {
                    decoration.setDescriptionText(Resources.getMessage("HierarchyWizardPageScript.9")); //$NON-NLS-1$
                    Image image = FieldDecorationRegistry.getDefault()
                            .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
                            .getImage();
                    decoration.setImage(image);
                    decoration.show();
                } else {
                    decoration.hide();
                }
                setPageComplete(isPageComplete());
            }
        });
    }

    private void evaluatePage() {

        setPageComplete(false);
        setErrorMessage(null);

        if (comboLocation.getText().isEmpty() || txtDelimiter.getText().isEmpty()) { //$NON-NLS-1$
            return;
        }

        List<String[]> parameters;
        try {
            parameters = model.getScriptParameters();

        } catch (Exception e) {
            canFinish = false;
            updateApplyButton();
            setErrorMessage(e.getMessage());
            return;
        }



        if(parameters.isEmpty()){
            canFinish = false;
            updateApplyButton();
            setErrorMessage(Resources.getMessage("HierarchyWizardPageScript.7"));
            return;
        }

        /* Remove old data from view*/
        Control[] c = group.getChildren();

        for (Control child: c){
            child.dispose();
        }

        Map<String,String> paramValues = new HashMap<>();

        /* Put data into view */
        for (int i = 0; i < parameters.size() - 1; i++) {
            String[] s = parameters.get(i);
            Text t = new Text(group, SWT.NONE);
            t.setText(s[0] + " (" + s[1] + ")");
            t.setEnabled(true);
            t.setEditable(false);
            Text input = new Text(group, SWT.BORDER);
            String value = model.getParameters().containsKey(s[0]) ? model.getParameters().get(s[0]) : s[2];
            model.getParameters().put(s[0],value);
            input.setText(value);
            input.setEditable(true);
            input.setEnabled(true);
            input.setLayoutData(SWTUtil.createFillHorizontallyGridData());
            paramValues.put(s[0],s[2]);

            decorate(input);

            input.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent arg0) {
                    setPageComplete(false);
                    model.getParameters().put(s[0], input.getText());
                    canFinish = false;
                    updateApplyButton();
                    setPageComplete(true);
                }
            });

            Text description = new Text(group, SWT.NONE);
            description.setText(s[3]);
            description.setEditable(false);
            description.setEnabled(true);
            //description.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
        }

        model.setParameters(paramValues);
        txtDescription.setText(parameters.get(parameters.size() - 1)[0]);

        group.update();
        group.layout();
        group.pack();
        group.getParent().layout();

        /* Mark page as completed */
        canFinish = false;
        updateApplyButton();
        setPageComplete(true);
    }
}
