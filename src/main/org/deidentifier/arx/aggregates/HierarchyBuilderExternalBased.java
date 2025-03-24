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
package org.deidentifier.arx.aggregates;

import org.deidentifier.arx.AttributeType.Hierarchy;

import java.io.*;
import java.util.*;

/**
 * This class enables building hierarchies within external custom scripts.
 *
 * @author Fabian Prasser
 * @param <T>
 */
public class HierarchyBuilderExternalBased<T> extends HierarchyBuilder<T> implements Serializable { // NO_UCD

    /**  SVUID */
    private static final long serialVersionUID = 9201730872884242173L;

    public static <T> HierarchyBuilderExternalBased<T> create(String path, Map<String, Integer> frequency, Map<String, String> parameters, String separator, boolean uniqueOnly){
        return new HierarchyBuilderExternalBased<>(path, frequency, parameters, separator, uniqueOnly);
    }

    public static <T> HierarchyBuilderExternalBased<T> create(String path, String separator){
        return new HierarchyBuilderExternalBased<>(path, new HashMap<>(), new HashMap<>(), separator, false);
    }

    /**
     * Copies the given builder
     * @param builder
     * @return
     */
    public static <T> HierarchyBuilderExternalBased<T> create(HierarchyBuilderExternalBased<T> builder){
        return new HierarchyBuilderExternalBased<T>(builder);
    }

    /**
     * Loads a builder specification from the given file.
     *
     * @param <T>
     * @param file
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static <T> HierarchyBuilderExternalBased<T> create(File file) throws IOException{
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            HierarchyBuilderExternalBased<T> result = (HierarchyBuilderExternalBased<T>)ois.readObject();
            return result;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (ois != null) ois.close();
        }
    }

    /**
     * Loads a builder specification from the given file.
     *
     * @param <T>
     * @param file
     * @return
     * @throws IOException
     */
    public static <T> HierarchyBuilderExternalBased<T> create(String file) throws IOException{
        return create(new File(file));
    }

    /** path to the external script */
    private String path;

    /** Data frequency */
    private Map<String, Integer> frequency;

    /** The map of script parameters */
    private Map<String, String> parameters;

    /** The separator used for all script data parsing */
    private String separator;

    /** Must the script be run only on unique values */
    private boolean uniqueOnly;

    /** Result. We also save this one as a cache*/
    private String[][] result;

    private HierarchyBuilderExternalBased(String path, Map<String, Integer> frequency, Map<String, String> parameters, String separator, boolean uniqueOnly) {
        super(Type.EXTERNAL_BASED);
        this.path = path;
        this.frequency = frequency;
        this.parameters = parameters;
        this.separator = separator;
        this.uniqueOnly = uniqueOnly;
    }

    /**
     * Copy constructor
     * @param builder
     */
    private HierarchyBuilderExternalBased(HierarchyBuilderExternalBased<T> builder){
        super(Type.EXTERNAL_BASED);
        this.path = builder.path;
        this.frequency = new HashMap<>(builder.frequency);
        this.parameters = new HashMap<>(builder.parameters);
        this.uniqueOnly = builder.uniqueOnly;
        this.separator = builder.separator;
        this.result = new String[builder.result.length][];
        for (int i = 0; i < builder.result.length; i++) {
            this.result[i] = builder.result[i].clone();
        }
        this.groupSizes = builder.groupSizes.clone();
    }

    /**
     * Creates a new hierarchy, based on the predefined specification.
     *
     * @return
     */
    public Hierarchy build(){

        // Check
        if (result == null) {
            throw new IllegalArgumentException("Please call prepare() first");
        }

        // Return
        Hierarchy h = Hierarchy.create(result);
        // Do not reset the result, we cache it
        //this.result = null;
        return h;
    }

    /**
     * Creates a new hierarchy, based on the predefined specification.
     *
     * @param data
     * @return
     */
    public Hierarchy build(String[] data){
        prepare(data);
        return build();
    }

    private int[] groupSizes;

    /**
     * Prepares the builder. Returns a list of the number of equivalence classes per level
     *
     * @param data
     * @return
     */
    public int[] prepare(String[] data){
        // create the data for which the script must generate a hierarchy
        String[] fullData;
        if (uniqueOnly) {
            fullData = data;
        }else{
            if(this.frequency.isEmpty()){
                throw new IllegalArgumentException("Please set frequencies when not using unique only");
            }
            int totalAmount = this.frequency.values().stream().mapToInt(Integer::intValue).sum();
            fullData = new String[totalAmount];
            int index = 0;
            for (String entry:data){
                for (int i = 0; i < this.frequency.getOrDefault(entry,0); i++){
                    fullData[index] = entry;
                    index++;
                }
            }
        }

        List<String[]> hierarchyRows = new ScriptExecutor().runScript(path, separator, parameters, fullData);

        result = hierarchyRows.toArray(new String[0][]);
        groupSizes = new int[result[0].length];
        for (int i = 0; i < groupSizes.length; i++) {
            Set<String> uniques = new HashSet<>();
            for(String[] row : result){
                uniques.add(row[i]);
            }
            groupSizes[i] = uniques.size();
        }
        return groupSizes;
    }

    /**
     * @return the script path
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the frequency
     */
    public Map<String, Integer> getFrequency() {
        return frequency;
    }

    /**
     * @return the given parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @return the given separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * @return the given parameters
     */
    public boolean isUniqueOnly() {
        return uniqueOnly;
    }

    public int[] getGroupSizes(){
        return this.groupSizes;
    }

    /**
     * @param path the script path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @param separator the script separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * @param frequency the frequency to set
     */
    public void setFrequency(Map<String, Integer> frequency) {
        this.frequency = frequency;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the parameters expected by the script
     */
    public List<String[]> getScriptParameters() {
        return new ScriptExecutor().getParameters(path, separator);
    }


    public class ScriptExecutor {

        public List<String[]> getParameters(String path, String separator){
            if(!new File(path).exists()){
                throw new IllegalArgumentException("Script path " + path + " does not exist");
            }
            // requesting parameters is a flag with a given data separator
            ProcessBuilder builder = new ProcessBuilder(path, separator, "-parameters");
            builder.directory(new File(path).getParentFile());
            Process p;
            try {
                p = builder.start();
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot run the script at path " + path + ": ");
            }

            List<String[]> params = this.readOutput(p);

            // test if the parameters got parsed properly
            if(params.size()>1){
                for(int i=0;i< params.size()-1;i++){
                    String[] par = params.get(i);
                    if(par.length!=4){
                        throw new IllegalArgumentException("Could not parse the parameter: " + Arrays.toString(par) + " Try changing the delimiter.");
                    }
                }
            }

            try{
                p.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return params;
        }


        public List<String[]> runScript(String path, String separator, Map<String, String> parameters, String[] fullData){
            Process p = getProcess(path, separator);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));

            // first pass all the parameters and check if the script received them all
            // write amount of parameters
            writer.println(parameters.size());
            for (Map.Entry<String, String> param:parameters.entrySet()){
                writer.println(param.getKey() + separator + param.getValue());
            }

            // write the data itself
            writer.println(fullData.length);
            for (String row:fullData){
                writer.println(row);
            }

            writer.close();
            List<String[]> splitRows = this.readOutput(p);

            if (splitRows.size() != fullData.length){
                throw new IllegalArgumentException("Wrong number of hierarchy rows returned. Expected: " + fullData.length + ", Actual: " + splitRows.size());
            }

            try {
                p.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return splitRows;
        }

        private Process getProcess(String path, String separator) {
            if(!new File(path).exists()){
                throw new IllegalArgumentException("Script path (\"" + path + "\") does not exist");
            }
            ProcessBuilder builder = new ProcessBuilder(path, separator);
            builder.directory(new File(path).getParentFile());
            Process p;
            try {
                p = builder.start();
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot run the script at path " + path + ": " + e.getMessage());
            }
            return p;
        }

        private List<String[]> readOutput(Process p){
            // read all the output our script returns
            List<String> scriptResult = new ArrayList<>();
            List<String> scriptError = new ArrayList<>();
            List<String[]> splitRows = new ArrayList<>();
            String line;
            try(BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                while ((line = output.readLine()) != null) {
                    scriptResult.add(line);
                }

                while ((line = error.readLine()) != null) {
                    scriptError.add(line);
                }

                // parsing first error line as message, others are for debugging reasons
                if(!scriptError.isEmpty()){
                    throw new IllegalArgumentException("Script did not finish properly: " + scriptError.get(0));
                }

                // check if the script properly stopped with printing (detects script not finishing properly without exceptions)
                if (scriptResult.get(scriptResult.size()-1).equals("DONE")) {
                    scriptResult.remove(scriptResult.size()-1);
                    for (String row:scriptResult){
                        splitRows.add(row.split(separator));
                    }
                }else{
                    throw new IllegalArgumentException("Script did not finish properly but no error message was provided");
                }

            }catch (IOException e) {
                throw new IllegalArgumentException("Cannot run the script at path " + path + ": ");
            }
            return splitRows;
        }
    }
}
