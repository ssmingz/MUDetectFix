package de.tu_darmstadt.stg.mudetect;

import aug.model.APIUsageExample;
import aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.model.Overlap;

import java.util.List;

public interface OverlapsFinder {
    List<Overlap> findOverlaps(APIUsageExample target, APIUsagePattern pattern);
}
