package com.github.owlcs.ontapi;

import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.common.OntConfigs;
import org.apache.jena.ontapi.common.OntPersonalities;
import org.apache.jena.ontapi.common.OntPersonality;

public class TestOntSpecifications {

    public static final OntPersonality ONT_PERSONALITY_FULL = OntPersonalities.OWL2_ONT_PERSONALITY()
            .setBuiltins(OntPersonalities.OWL2_FULL_BUILTINS)
            .setReserved(OntPersonalities.OWL2_RESERVED)
            .setConfig(OntConfigs.OWL2_CONFIG)
            .setPunnings(OntPersonalities.OWL_NO_PUNNINGS)
            .build();

    public static final OntPersonality ONT_PERSONALITY_DL = OntPersonalities.OWL2_ONT_PERSONALITY()
            .setBuiltins(OntPersonalities.OWL2_FULL_BUILTINS)
            .setReserved(OntPersonalities.OWL2_RESERVED)
            .setConfig(OntConfigs.OWL2_CONFIG)
            .setPunnings(OntPersonalities.OWL_DL2_PUNNINGS)
            .build();

    public static final OntPersonality ONT_PERSONALITY_DL_WEAK = OntPersonalities.OWL2_ONT_PERSONALITY()
            .setBuiltins(OntPersonalities.OWL2_FULL_BUILTINS)
            .setReserved(OntPersonalities.OWL2_RESERVED)
            .setConfig(OntConfigs.OWL2_CONFIG)
            .setPunnings(OntPersonalities.OWL_DL_WEAK_PUNNINGS)
            .build();

    public static final OntSpecification OWL2_FULL_NO_INF = new OntSpecification(ONT_PERSONALITY_FULL, null);
    public static final OntSpecification OWL2_DL_NO_INF = new OntSpecification(ONT_PERSONALITY_DL, null);
    public static final OntSpecification OWL2_DL_WEAK_NO_INF = new OntSpecification(ONT_PERSONALITY_DL_WEAK, null);

}
