package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the standard RDF.
 * See <a href='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>schema</a>.
 * <p>
 * Created by @szuev on 21.12.2016.
 */
public class RDF extends org.apache.jena.vocabulary.RDF {

    public final static Resource PlainLiteral = resource("PlainLiteral");
}
