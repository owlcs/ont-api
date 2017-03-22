package ru.avicomp.ontapi.transforms;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * The base class for any graph-converter.
 * todo: add configurable logger to record all changes.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Transform {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Transform.class);

    protected final Graph graph;
    private Model model;
    private Model base;

    protected Transform(Graph graph) {
        this.graph = graph;
    }

    /**
     * performs the graph transformation.
     */
    public abstract void perform();

    /**
     * decides is the transformation needed or not.
     *
     * @return true to process, false to skip
     */
    public boolean test() {
        return true;
    }

    public void process() {
        if (test()) {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(String.format("Process <%s> on <%s>", name(), Graphs.getName(getBaseGraph())));
            perform();
        }
    }

    public String name() {
        return getClass().getSimpleName();
    }

    public Graph getGraph() {
        return graph;
    }

    public Graph getBaseGraph() {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
    }

    public Model getModel() {
        return model == null ? model = ModelFactory.createModelForGraph(getGraph()) : model;
    }

    public Model getBaseModel() {
        return base == null ? base = ModelFactory.createModelForGraph(getBaseGraph()) : base;
    }

    public void changeType(Resource realType, Resource newType) {
        Set<Resource> toFix = statements(null, RDF.type, realType).map(Statement::getSubject).collect(Collectors.toSet());
        toFix.forEach(subject -> {
            undeclare(subject, realType);
            declare(subject, newType);
        });
    }

    public void declare(Resource subject, Resource type) {
        subject.addProperty(RDF.type, type);
    }

    public void undeclare(Resource subject, Resource type) {
        getBaseModel().removeAll(subject, RDF.type, type);
    }

    public boolean hasType(Resource resource, Resource type) {
        return resource.hasProperty(RDF.type, type);
    }

    protected boolean containsType(Resource type) {
        return getBaseModel().contains(null, RDF.type, type);
    }

    public static Stream<Statement> statements(Model m, Resource s, Property p, RDFNode o) {
        return Iter.asStream(m.listStatements(s, p, o));
    }

    public Stream<Statement> statements(Resource s, Property p, RDFNode o) {
        return statements(getBaseModel(), s, p, o).map(st -> getModel().asStatement(st.asTriple()));
    }
}
