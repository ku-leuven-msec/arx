package org.deidentifier.arx.aggregates;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.DataType;

import java.io.*;

public class HierarchyBuilderSuppressionBased<T> extends HierarchyBuilder<T> implements Serializable {

    /** Result. */
    private transient String[][] result;

    /**  SVUID */
    private static final long serialVersionUID = -1199092308823592969L;


    /**
     * Creates a hierarchy only containing a suppression level
     */
    protected HierarchyBuilderSuppressionBased() {
        super(Type.SUPPRESSION_BASED);
    }


    protected HierarchyBuilderSuppressionBased(HierarchyBuilderSuppressionBased<T> builder) {
        super(Type.SUPPRESSION_BASED);
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> HierarchyBuilderSuppressionBased<T> create(){
        return new HierarchyBuilderSuppressionBased<T>();
    }

    /**
     *
     * Create copy of the builder
     *
     * @param builder
     * @param <T>
     * @return
     */
    public static <T> HierarchyBuilderSuppressionBased<T> create(HierarchyBuilderSuppressionBased<T> builder){
        return new HierarchyBuilderSuppressionBased<T>(builder);
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
    public static <T> HierarchyBuilderSuppressionBased<T> create(File file) throws IOException{
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            HierarchyBuilderSuppressionBased<T> result = (HierarchyBuilderSuppressionBased<T>)ois.readObject();
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
    public static <T> HierarchyBuilderSuppressionBased<T> create(String file) throws IOException{
        return create(new File(file));
    }

    @Override
    public Hierarchy build() {
        // Check
        if (result == null) {
            throw new IllegalArgumentException("Please call prepare() first");
        }

        // Return
        Hierarchy h = Hierarchy.create(result);
        this.result = null;
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

    @Override
    public int[] prepare(String[] data) {

        // Create hierarchy
        result = new String[data.length][0];
        for (int i = 0; i < result.length; i++) {
            result[i] = new String[] { data[i], DataType.ANY_VALUE };
        }
        return new int[] {data.length,1};
    }
}
