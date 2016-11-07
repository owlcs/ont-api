package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntOPEntity;

/**
 * owl:ObjectProperty (could be also Annotation, InverseFunctional, Transitive or SymmetricProperty)
 * <p>
 * Created by szuev on 03.11.2016.
 */
class OntOPropertyImpl extends OntEntityImpl implements OntOPEntity {

    OntOPropertyImpl(Resource inModel) {
        super(inModel);
    }

    OntOPropertyImpl(Node n, EnhGraph g) {
        super(n, g);
    }

    @Override
    public Class<? extends OntEntity> getActualClass() {
        return OntOPEntity.class;
    }

    @Override
    public Resource getRDFType() {
        return OWL.ObjectProperty;
    }

    @Override
    public Stream<OntCE> domain() {
        return getModel().classExpressions(this, RDFS.domain);
    }

    @Override
    public Stream<OntCE> range() {
        return getModel().classExpressions(this, RDFS.range);
    }

    private boolean hasType(Resource type) {
        return types().filter(type::equals).findAny().isPresent();
    }

    public boolean isInverseFunctional() {
        return hasType(OWL.InverseFunctionalProperty);
    }

    public boolean isTransitive() {
        return hasType(OWL.TransitiveProperty);
    }

    public boolean isSymetric() {
        return hasType(OWL.SymmetricProperty);
    }

}

