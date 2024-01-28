package com.github.owlcs.ontapi;

import com.github.sszuev.jena.ontapi.common.OntConfigs;
import com.github.sszuev.jena.ontapi.common.OntPersonalities;
import com.github.sszuev.jena.ontapi.common.OntPersonality;

public class TestOntPersonalities {

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
}
