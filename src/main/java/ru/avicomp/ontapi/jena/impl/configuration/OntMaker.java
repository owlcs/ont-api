package ru.avicomp.ontapi.jena.impl.configuration;

import java.lang.reflect.InvocationTargetException;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * To make some preparation while creating (create main triple).
 * Also to create new instance of resource.
 * Used in factory ({@link CommonOntObjectFactory}).
 * <p>
 * Created by szuev on 07.11.2016.
 */
public interface OntMaker {

    void make(Node node, EnhGraph eg);

    EnhNode instance(Node node, EnhGraph eg);

    Class<?> getInstanceClass();

    /**
     * Creation in graph is disabled for this maker
     */
    class Default implements OntMaker {
        protected final Class<? extends OntObjectImpl> impl;

        /**
         * Class must be public and have a public constructor with parameters {@link Node} and {@link EnhGraph}.
         *
         * @param impl {@link ru.avicomp.ontapi.jena.model.OntObject} implementation.
         */
        public Default(Class<? extends OntObjectImpl> impl) {
            this.impl = OntJenaException.notNull(impl, "Null implementation class.");
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            throw new OntJenaException("Creation is not allowed for node " + node + " and class " + impl.getSimpleName());
        }

        @Override
        public EnhNode instance(Node node, EnhGraph eg) {
            try {
                return impl.getDeclaredConstructor(Node.class, EnhGraph.class).newInstance(node, eg);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new OntJenaException(e);
            }
        }

        @Override
        public Class<? extends OntObjectImpl> getInstanceClass() {
            return impl;
        }
    }

    class WithType extends Default {
        protected final Node type;

        public WithType(Class<? extends OntObjectImpl> impl, Resource type) {
            super(impl);
            this.type = OntJenaException.notNull(type, "Null type.").asNode();
        }

        @Override
        public void make(Node node, EnhGraph eg) {
            eg.asGraph().add(Triple.create(node, RDF.type.asNode(), type));
        }
    }
}
