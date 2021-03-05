package edu.iastate.cs.mudetect.mining;

import aug.model.APIUsageExample;

import java.util.Collection;

public interface AUGMiner {
    Model mine(Collection<APIUsageExample> examples);
}
