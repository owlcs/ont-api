package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.RDFNode;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * Personality (mappings from [interface] Class objects of RDFNode to {@link Implementation} factories)
 * <p>
 * Created by @szuev on 10.11.2016.
 */
public class OntPersonality extends Personality<RDFNode> {

    public OntPersonality(Personality<RDFNode> other) {
        super(other);
    }

    /**
     * registers new OntObject if needed
     *
     * @param view    Interface (OntObject)
     * @param factory Factory to crete object
     */
    public OntPersonality register(Class<? extends OntObject> view, OntObjectFactory factory) {
        return (OntPersonality) super.add(OntApiException.notNull(view, "Null view."), OntApiException.notNull(factory, "Null factory."));
    }

    /**
     * removes factory.
     *
     * @param view Interface (OntObject)
     */
    public void unregister(Class<? extends OntObject> view) {
        getMap().remove(view);
    }

    /**
     * gets factory for OntObject
     *
     * @param view Interface (OntObject)
     * @return {@link OntObjectFactory} factory.
     */
    public OntObjectFactory getOntImplementation(Class<? extends OntObject> view) {
        return (OntObjectFactory) OntApiException.notNull(getImplementation(view), "Can't find factory for object " + view);
    }


    @Override
    public OntPersonality copy() {
        return new OntPersonality(this);
    }
}
