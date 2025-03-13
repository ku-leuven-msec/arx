package myTests;

import org.deidentifier.arx.*;
import org.deidentifier.arx.aggregates.HierarchyBuilderExternalBased;
import org.deidentifier.arx.aggregates.StatisticsFrequencyDistribution;
import org.deidentifier.arx.criteria.KAnonymity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class test {

    /**
     * Entry point.
     *
     * @param args
     *            The arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Define data
        Data data = Data.create("C:\\Users\\Jenno Verdonck\\Desktop\\arx_fork\\arx\\scripts\\test_dataset - Copy.csv", StandardCharsets.UTF_8,';');

        HierarchyBuilderExternalBased<?> builder = HierarchyBuilderExternalBased.create("C:\\Users\\Jenno Verdonck\\Desktop\\arx_fork\\arx\\scripts\\Locations\\test.bat",";");

        List<String[]> params = builder.getScriptParameters();

        String result = params.stream()
                .flatMap(Stream::of) // Flatten each String[] into a stream of strings
                .collect(Collectors.joining(","));

        System.out.println(result);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("Column separator", ",");
        parameters.put("Accuracy", "5");
        parameters.put("Order", "TD");
        parameters.put("Preferred cluster amounts", "[5, 10, 25, 50, 100]");
        parameters.put("best k means try", "1");
        parameters.put("Amount of cores", "1");

        StatisticsFrequencyDistribution distribution = data.getHandle().getStatistics().getFrequencyDistribution(0);
        // Build frequency map
        Map<String, Integer> frequency = new HashMap<>();
        if (distribution != null) {
            for (int i = 0; i < distribution.values.length; i++) {
                frequency.put(distribution.values[i], (int)(distribution.frequency[i] * (double)distribution.count));
            }
        }
        builder.setFrequency(frequency);

        builder.setParameters(parameters);
        String[] values = data.getHandle().getDistinctValues(0);
        AttributeType.Hierarchy h = builder.build(values);
        data.getDefinition().setAttributeType(data.getHandle().getAttributeName(0),h);

        ARXConfiguration config = ARXConfiguration.create();
        config.addPrivacyModel(new KAnonymity(5));

        ARXAnonymizer a = new ARXAnonymizer();

        a.anonymize(data,config);
    }
}
