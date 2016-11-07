package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.impl.GraphModelImpl;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;

/**
 * Factory to create typed resources.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class TypedOntObjectFactory extends CommonOntObjectFactory {
    public TypedOntObjectFactory(Class<? extends OntObjectImpl> impl, Resource type, Filter... filters) {
        super(impl, new TypeMaker(type), new TypeFinder(type), new TypeFilter(type), filters);
    }

    private abstract static class TypeWrapper {
        static final Node RDF_TYPE = RDF.type.asNode();
        protected final Node type;

        TypeWrapper(Resource type) {
            this.type = OntException.notNull(type, "Null type.").asNode();
        }
    }

    public static class TypeMaker extends TypeWrapper implements Maker {
        public TypeMaker(Resource type) {
            super(type);
        }

        @Override
        public void prepare(Node node, EnhGraph eg) {
            eg.asGraph().add(Triple.create(node, RDF_TYPE, type));
        }
    }

    public static class TypeFinder extends TypeWrapper implements Finder {
        public TypeFinder(Resource type) {
            super(type);
        }

        @Override
        public Stream<Node> find(EnhGraph eg) {
            return GraphModelImpl.asStream(eg.asGraph().find(Node.ANY, RDF_TYPE, type).mapWith(Triple::getSubject));
        }
    }

    public static class TypeFilter extends TypeWrapper implements Filter {
        public TypeFilter(Resource type) {
            super(type);
        }

        @Override
        public boolean test(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF_TYPE, type);
        }
    }
}
