package com.github.owlcs.ontapi.internal;

import org.apache.jena.ontapi.OntModelControls;
import org.apache.jena.ontapi.common.OntEnhGraph;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObject;

public class OntModelSupport {
    /**
     * Answers {@code true} if the feature is supported.
     * @param model {@link OntModel}
     * @param control {@link  OntModelControls}
     * @return boolean
     */
    public static boolean supports(OntModel model, OntModelControls control) {
        return OntEnhGraph.asPersonalityModel(model).getOntPersonality().getConfig().getBoolean(control);
    }

    /**
     * Answers {@code true} if the language construct is supported
     * @param model {@link OntModel}
     * @param view type of language construct
     * @return boolean
     */
    public static boolean supports(OntModel model, Class<? extends OntObject> view) {
        return OntEnhGraph.asPersonalityModel(model).getOntPersonality().supports(view);
    }

    /**
     * Returns profile's name.
     * @param model {@link OntModel}
     * @return {@code String}
     */
    public static String profileName(OntModel model) {
        return OntEnhGraph.asPersonalityModel(model).getOntPersonality().getName();
    }
}
