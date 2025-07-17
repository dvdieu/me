package org.example.v4.matching.context;


import java.util.Collections;
import java.util.Map;

public class MatchingConfig {

    public int proRataMin = 1;
    public int lmmMinFill = 1;
    public int totalLmmPerThousand = 400;
    public Map<Long, Integer> lmmPerThousands = Collections.emptyMap();

}
