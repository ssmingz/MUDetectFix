package de.tu_darmstadt.stg.mustudies;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import aug.model.APIUsageGraph;

import java.util.Collection;

public class UsageUtils {
    public static Multiset<String> countNumberOfUsagesPerType(Collection<? extends APIUsageGraph> usageGraphs) {
        HashMultiset<String> numberOfUsagesPerType = HashMultiset.create();
        for (APIUsageGraph usageGraph : usageGraphs) {
            numberOfUsagesPerType.addAll(usageGraph.getAPIs());
        }
        return numberOfUsagesPerType;
    }
}
