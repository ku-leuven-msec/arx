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

import org.deidentifier.arx.DataType;
import org.deidentifier.arx.aggregates.HierarchyBuilder;
import org.deidentifier.arx.aggregates.HierarchyBuilderExternalBased;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchyWizardModelExternal<T> extends HierarchyWizardModelAbstract<T>{
    /**
     * Creates a new instance. Here we need to implement the code that reads python files
     *
     */

    /** path to the external script */
    private String path = "";

    /** Data frequency */
    private Map<String, Integer> frequency;

    /** The map of script parameters */
    private Map<String, String> parameters = new HashMap<>();;

    /** The separator used for all script data parsing */
    private String separator = ";";

    /** Must the script be run only on unique values */
    private boolean uniqueOnly = true;

    /** A singleton of the builder to avoid the need of rebuilding each time */
    private HierarchyBuilderExternalBased<T> builder;

    /** Data type */
    private DataType<T> dataType;

    /**
     * Creates a new instance.
     *
     * @param dataType
     * @param data
     * @param frequency
     */
    public HierarchyWizardModelExternal(DataType<T> dataType,
                                        String[] data,
                                        Map<String, Integer> frequency) {
        super(data);

        this.dataType = dataType;
        this.frequency = frequency;

        // Update
        this.update();
    }


    @Override
    public HierarchyBuilderExternalBased<T> getBuilder(boolean serializable) {
        if(builder == null){
            this.builder = HierarchyBuilderExternalBased.create(path, frequency, parameters, separator, uniqueOnly);
        }
        return builder;

    }

    @Override
    public void parse(HierarchyBuilder<T> hierarchyBuilder) {
        if (!(hierarchyBuilder instanceof HierarchyBuilderExternalBased)) {
            return;
        }
        HierarchyBuilderExternalBased<T> builder = (HierarchyBuilderExternalBased<T>)hierarchyBuilder;

        this.builder = builder;
        super.hierarchy = builder.build();
        super.groupsizes = builder.getGroupSizes();
        path = builder.getPath();
        frequency = builder.getFrequency();
        parameters = builder.getParameters();
        separator = builder.getSeparator();
        uniqueOnly = builder.isUniqueOnly();

        update();
    }

    @Override
    public void updateUI(HierarchyWizard.HierarchyWizardView sender) {
        //Empty design
    }

    /**
     * Update the model and all UI components.
     */
    @Override
    public void update(){
        super.update();
        updateUI(null);
    }

    @Override
    protected void build() {
        // empty by design, our hierarchy build is expensive so we don't want to update the hierarchy automaticly
    }

    protected void buildInternal() {
        // Clear
        super.hierarchy = null;
        super.error = null;
        super.groupsizes = null;

        // Check
        if (data == null) return;
        if (path.isEmpty()) return;

        // create a new builder with the current parameters
        this.builder = null;
        HierarchyBuilderExternalBased<T> builder = getBuilder(false);

        super.groupsizes = builder.prepare(data);

        super.hierarchy = builder.build();

    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String[]> getScriptParameters(){
        HierarchyBuilderExternalBased<T> builder = getBuilder(false);
        builder.setPath(path);
        builder.setSeparator(separator);
        return this.getBuilder(false).getScriptParameters();
    }


    public String getSeparator() {
        return separator;
    }

    public boolean isUniqueOnly() {
        return uniqueOnly;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void setUniqueOnly(boolean uniqueOnly) {
        this.uniqueOnly = uniqueOnly;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getPath() {
        return path;
    }
}
