package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.model.GraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * TODO:
 * Created by @szuev on 12.11.2016.
 */
public class OntStatementImpl extends StatementImpl implements OntStatement {

    public OntStatementImpl(Resource subject, Property predicate, RDFNode object, GraphModel model) {
        super(subject, predicate, object, (ModelCom) model);
    }

    OntStatementImpl(Statement s) {
        this(s.getSubject(), s.getPredicate(), s.getObject(), (GraphModel) s.getModel());
    }

    @Override
    public GraphModel getModel() {
        return (GraphModel) super.getModel();
    }

    protected void changeSubject(Resource resource) {
        this.subject = OntException.notNull(resource, "Null subject.").inModel(getModel());
    }
}
