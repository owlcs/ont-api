package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href='https://www.w3.org/TR/owl2-syntax/'>OWL 2 Web Ontology Language</a>
 * See <a href='http://www.w3.org/2002/07/owl#'>schema(ttl)</a>
 * Note: owl:real and owl:rational are absent in the schema.
 * <p>
 * Created by @szuev on 21.12.2016.
 */
public class OWL extends org.apache.jena.vocabulary.OWL2 {

    public final static Resource real = resource("real");
    public final static Resource rational = resource("rational");

}
