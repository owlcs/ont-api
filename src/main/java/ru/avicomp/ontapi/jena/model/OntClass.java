package ru.avicomp.ontapi.jena.model;

/**
 * OWLClass
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntClass extends OntEntity, OntCE {

    OntIndividual createIndividual();

    OntNIndividual createIndividual(String uri);
}
