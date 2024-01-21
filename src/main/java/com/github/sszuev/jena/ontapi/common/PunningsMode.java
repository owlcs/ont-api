package com.github.sszuev.jena.ontapi.common;

/**
 * A standard personality mode to manage punnings.
 */
public enum PunningsMode {
    /**
     * The following punnings are considered as illegal and are excluded:
     * <ul>
     * <li>owl:Class &lt;-&gt; rdfs:Datatype</li>
     * <li>owl:ObjectProperty &lt;-&gt; owl:DatatypeProperty</li>
     * <li>owl:ObjectProperty &lt;-&gt; owl:AnnotationProperty</li>
     * <li>owl:AnnotationProperty &lt;-&gt; owl:DatatypeProperty</li>
     * </ul>
     */
    DL2,
    /**
     * Forbidden intersections of rdf-declarations:
     * <ul>
     * <li>Class &lt;-&gt; Datatype</li>
     * <li>ObjectProperty &lt;-&gt; DataProperty</li>
     * </ul>
     */
    DL_WEAK,
    /**
     * Allow everything.
     */
    FULL,
    ;

    private OntPersonality.Punnings punnings;

    public OntPersonality.Punnings getVocabulary() {
        return punnings == null ? punnings = OntPersonalities.createPunningsVocabulary(this) : punnings;
    }
}
