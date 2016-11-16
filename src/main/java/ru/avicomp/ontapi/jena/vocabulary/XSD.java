package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * XML Schema Vocabulary
 * See <a href='http://www.w3.org/2001/XMLSchema'>XSD</a>
 * <p>
 * Created by @szuev on 16.11.2016.
 */
public class XSD extends org.apache.jena.vocabulary.XSD {

    public static final Property length = ResourceFactory.createProperty(NS + "length");
    public static final Property minLength = ResourceFactory.createProperty(NS + "minLength");
    public static final Property maxLength = ResourceFactory.createProperty(NS + "maxLength");
    public static final Property pattern = ResourceFactory.createProperty(NS + "pattern");
    public static final Property minInclusive = ResourceFactory.createProperty(NS + "minInclusive");
    public static final Property minExclusive = ResourceFactory.createProperty(NS + "minExclusive");
    public static final Property maxInclusive = ResourceFactory.createProperty(NS + "maxInclusive");
    public static final Property maxExclusive = ResourceFactory.createProperty(NS + "maxExclusive");
    public static final Property totalDigits = ResourceFactory.createProperty(NS + "totalDigits");
    public static final Property fractionDigits = ResourceFactory.createProperty(NS + "fractionDigits");
    public static final Property langRange = ResourceFactory.createProperty(NS + "langRange");
}
