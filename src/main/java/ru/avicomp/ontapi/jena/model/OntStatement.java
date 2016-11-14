package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Statement;

/**
 * TODO: not ready yet.
 * Annotated OntStatement
 * <p>
 * Created by @szuev on 13.11.2016.
 */
public interface OntStatement extends Statement {

    GraphModel getModel();

}
