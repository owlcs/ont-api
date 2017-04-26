package ru.avicomp.ontapi.jena.impl;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.InfModelImpl;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.shared.PrefixMapping;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Base model to work through jena only.
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work in accordance with OWL2 DL specification.
 * <p>
 * Created by @szuev on 27.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntGraphModelImpl extends ModelCom implements OntGraphModel {

    /**
     * The main constructor.
     * @param graph {@link Graph}
     * @param personality {@link OntPersonality}
     */
    public OntGraphModelImpl(Graph graph, OntPersonality personality) {
        super(graph instanceof UnionGraph ? graph : new UnionGraph(graph), OntJenaException.notNull(personality, "Null personality"));
    }

    public void syncImports() {
        syncImports(getPersonality());
    }

    protected void syncImports(OntPersonality personality) {
        OntID id = getID();
        id.removeAll(OWL.imports);
        imports(personality).map(OntGraphModel::getID).filter(Resource::isURIResource).map(Resource::getURI).forEach(id::addImport);
    }

    @Override
    protected OntPersonality getPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    @Override
    public OntID getID() {
        return getNodeAs(Graphs.getOntology(getBaseGraph())
                .orElseGet(() -> createResource().addProperty(RDF.type, OWL.Ontology).asNode()), OntID.class);
    }

    @Override
    public OntID setID(String uri) {
        List<Statement> prev = Iter.asStream(getBaseModel().listResourcesWithProperty(RDF.type, OWL.Ontology))
                .map(s -> Iter.asStream(s.listProperties())).flatMap(Function.identity()).collect(Collectors.toList());
        if (prev.stream().filter(s -> OWL.imports.equals(s.getPredicate()))
                .map(Statement::getObject)
                .filter(RDFNode::isURIResource)
                .map(RDFNode::asResource)
                .map(Resource::getURI).anyMatch(u -> u.equals(uri))) {
            throw new OntJenaException("Can't create ontology: <" + uri + "> is present in the imports.");
        }
        remove(prev);
        Resource res = createResource(uri).addProperty(RDF.type, OWL.Ontology);
        prev.forEach(s -> res.addProperty(s.getPredicate(), s.getObject()));
        return getNodeAs(res.asNode(), OntID.class);
    }

    @Override
    public void addImport(OntGraphModel m) {
        if (!OntJenaException.notNull(m, "Null model.").getID().isURIResource()) {
            throw new OntJenaException("Anonymous sub models are not allowed.");
        }
        getGraph().addGraph(m.getGraph());
        getID().addImport(m.getID().getURI());
    }

    @Override
    public void removeImport(OntGraphModel m) {
        getGraph().removeGraph(OntJenaException.notNull(m, "Null model.").getGraph());
        getID().removeImport(m.getID().getURI());
    }

    @Override
    public Stream<OntGraphModel> imports() {
        return imports(getPersonality());
    }

    public Stream<OntGraphModel> imports(OntPersonality personality) {
        return getGraph().getUnderlying().graphs().map(g -> new OntGraphModelImpl(g, personality));
    }

    @Override
    public UnionGraph getGraph() {
        return (UnionGraph) super.getGraph();
    }

    @Override
    public Graph getBaseGraph() {
        return getGraph().getBaseGraph();
    }

    @Override
    public Model getBaseModel() {
        return new ModelCom(getBaseGraph());
    }

    @Override
    public InfModel asInferenceModel(Reasoner reasoner) {
        return new InfModelImpl(OntJenaException.notNull(reasoner, "Null reasoner.").bind(getGraph()));
    }

    @Override
    public PrefixMapping setNsPrefix(String prefix, String uri) {
        getBaseGraph().getPrefixMapping().setNsPrefix(prefix, uri);
        return this;
    }

    @Override
    public Model write(Writer writer) {
        return getBaseModel().write(writer);
    }

    @Override
    public Model write(Writer writer, String lang) {
        return getBaseModel().write(writer, lang);
    }

    @Override
    public Model write(Writer writer, String lang, String base) {
        return getBaseModel().write(writer, lang, base);
    }

    @Override
    public Model write(OutputStream out) {
        return getBaseModel().write(out);
    }

    @Override
    public Model write(OutputStream out, String lang) {
        return getBaseModel().write(out, lang);
    }

    @Override
    public Model write(OutputStream out, String lang, String base) {
        return getBaseModel().write(out, lang, base);
    }

    @Override
    public boolean isInBaseModel(Statement stmt) {
        return isInBaseModel(OntApiException.notNull(stmt, "Null statement.").getSubject(), stmt.getPredicate(), stmt.getObject());
    }

    protected boolean isInBaseModel(Resource s, Property p, RDFNode o) {
        return getBaseGraph().contains(s.asNode(), p.asNode(), o.asNode());
    }

    /**
     * to retrieve the stream of {@link OntObject}s
     *
     * @param type Class
     * @return Stream
     */
    @Override
    public <T extends OntObject> Stream<T> ontObjects(Class<T> type) {
        return getPersonality().getOntImplementation(type).find(this).map(e -> getNodeAs(e.asNode(), type));
    }

    @Override
    public Stream<OntEntity> ontEntities() {
        return Stream.of(OntClass.class, OntDT.class, OntIndividual.Named.class, OntNOP.class, OntNAP.class, OntNDP.class).map(this::ontEntities).flatMap(Function.identity());
    }

    @Override
    public <T extends OntEntity> T getOntEntity(Class<T> type, String uri) {
        Node n = NodeFactory.createURI(OntJenaException.notNull(uri, "Null uri."));
        try { // returns not null in case it is present in graph or built-in.
            return getNodeAs(n, type);
        } catch (OntJenaException.Conversion ignore) {
            // ignore
            return null;
        }
    }

    @Override
    public <N extends RDFNode> N getNodeAs(Node n, Class<N> view) {
        try {
            return super.getNodeAs(OntJenaException.notNull(n, "Null node"), OntJenaException.notNull(view, "Null class view."));
        } catch (ConversionException e) {
            throw new OntJenaException.Conversion(String.format("Failed to convert node <%s> to <%s>", n, view), e);
        }
    }

    /**
     * to create any OntObject resource.
     *
     * @param type Class
     * @param uri  String
     * @return OntObject
     */
    public <T extends OntObject> T createOntObject(Class<T> type, String uri) {
        Resource res = uri == null ? createResource() : createResource(uri);
        return getPersonality().getOntImplementation(type).create(res.asNode(), this).as(type);
    }

    @Override
    public <T extends OntEntity> T createOntEntity(Class<T> type, String uri) {
        try {
            return createOntObject(type, uri);
        } catch (OntJenaException.Creation e) { // illegal punning:
            throw new OntJenaException(String.format("Can't add entity [%s: %s]: perhaps it's illegal punning.", type.getSimpleName(), uri), e);
        }
    }

    @Override
    public void removeOntObject(OntObject obj) {
        obj.clearAnnotations();
        obj.content().forEach(this::remove);
    }

    @Override
    public Stream<OntStatement> statements() {
        return Iter.asStream(listStatements()).map(st -> toOntStatement(null, st));
    }

    @Override
    public Stream<OntStatement> statements(Resource s, Property p, RDFNode o) {
        return Iter.asStream(listStatements(s, p, o)).map(st -> toOntStatement(null, st));
    }

    protected OntStatement toOntStatement(OntStatement main, Statement st) {
        if (st.equals(main)) return main;
        if (main != null && st.getPredicate().canAs(OntNAP.class)) {
            return new OntStatementImpl(main.getSubject(), st.getPredicate().as(OntNAP.class), st.getObject(), this);
        }
        return new OntStatementImpl(st.getSubject(), st.getPredicate(), st.getObject(), this);
    }

    @Override
    public OntDisjoint.Classes createDisjointClasses(Collection<OntCE> classes) {
        return OntDisjointImpl.createDisjointClasses(this, classes.stream());
    }

    @Override
    public OntDisjoint.Individuals createDifferentIndividuals(Collection<OntIndividual> individuals) {
        return OntDisjointImpl.createDifferentIndividuals(this, individuals.stream());
    }

    @Override
    public OntDisjoint.ObjectProperties createDisjointObjectProperties(Collection<OntOPE> properties) {
        return OntDisjointImpl.createDisjointObjectProperties(this, properties.stream());
    }

    @Override
    public OntDisjoint.DataProperties createDisjointDataProperties(Collection<OntNDP> properties) {
        return OntDisjointImpl.createDisjointDataProperties(this, properties.stream());
    }

    @Override
    public <T extends OntFR> T createFacetRestriction(Class<T> view, Literal literal) {
        return OntFRImpl.create(this, view, literal);
    }

    @Override
    public OntDR.OneOf createOneOfDataRange(Collection<Literal> values) {
        return OntDRImpl.createOneOf(this, values.stream());
    }

    @Override
    public OntDR.Restriction createRestrictionDataRange(OntDR property, Collection<OntFR> values) {
        return OntDRImpl.createRestriction(this, property, values.stream());
    }

    @Override
    public OntDR.ComplementOf createComplementOfDataRange(OntDR other) {
        return OntDRImpl.createComplementOf(this, other);
    }

    @Override
    public OntDR.UnionOf createUnionOfDataRange(Collection<OntDR> values) {
        return OntDRImpl.createUnionOf(this, values.stream());
    }

    @Override
    public OntDR.IntersectionOf createIntersectionOfDataRange(Collection<OntDR> values) {
        return OntDRImpl.createIntersectionOf(this, values.stream());
    }

    @Override
    public OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectSomeValuesFrom.class, onProperty, other, OWL.someValuesFrom);
    }

    @Override
    public OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataSomeValuesFrom.class, onProperty, other, OWL.someValuesFrom);
    }

    @Override
    public OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectAllValuesFrom.class, onProperty, other, OWL.allValuesFrom);
    }

    @Override
    public OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataAllValuesFrom.class, onProperty, other, OWL.allValuesFrom);
    }

    @Override
    public OntCE.ObjectHasValue createObjectHasValue(OntOPE onProperty, OntIndividual other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectHasValue.class, onProperty, other, OWL.hasValue);
    }

    @Override
    public OntCE.DataHasValue createDataHasValue(OntNDP onProperty, Literal other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataHasValue.class, onProperty, other, OWL.hasValue);
    }

    @Override
    public OntCE.ObjectMinCardinality createObjectMinCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMinCardinality createDataMinCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMinCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectMaxCardinality createObjectMaxCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataMaxCardinality createDataMaxCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataMaxCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.ObjectCardinality createObjectCardinality(OntOPE onProperty, int cardinality, OntCE onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.ObjectCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.DataCardinality createDataCardinality(OntNDP onProperty, int cardinality, OntDR onObject) {
        return OntCEImpl.createCardinalityRestrictionCE(this, OntCE.DataCardinality.class, onProperty, cardinality, onObject);
    }

    @Override
    public OntCE.UnionOf createUnionOf(Collection<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.UnionOf.class, OWL.unionOf, classes.stream());
    }

    @Override
    public OntCE.IntersectionOf createIntersectionOf(Collection<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.IntersectionOf.class, OWL.intersectionOf, classes.stream());
    }

    @Override
    public OntCE.OneOf createOneOf(Collection<OntIndividual> individuals) {
        return OntCEImpl.createComponentsCE(this, OntCE.OneOf.class, OWL.oneOf, individuals.stream());
    }

    @Override
    public OntCE.HasSelf createHasSelf(OntOPE onProperty) {
        return OntCEImpl.createHasSelf(this, onProperty);
    }

    @Override
    public OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Collection<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException.Unsupported("TODO: " + OntCE.NaryDataAllValuesFrom.class);
    }

    @Override
    public OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Collection<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException.Unsupported("TODO: " + OntCE.NaryDataSomeValuesFrom.class);
    }

    @Override
    public OntCE.ComplementOf createComplementOf(OntCE other) {
        return OntCEImpl.createComplementOf(this, other);
    }

    @Override
    public OntSWRL.Variable createSWRLVariable(String uri) {
        return OntSWRLImpl.createVariable(this, uri);
    }

    @Override
    public OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Collection<OntSWRL.DArg> arguments) {
        return OntSWRLImpl.createBuiltInAtom(this, predicate, arguments.stream());
    }

    @Override
    public OntSWRL.Atom.OntClass createClassSWRLAtom(OntCE clazz, OntSWRL.IArg arg) {
        return OntSWRLImpl.createClassAtom(this, clazz, arg);
    }

    @Override
    public OntSWRL.Atom.DataRange createDataRangeSWRLAtom(OntDR range, OntSWRL.DArg arg) {
        return OntSWRLImpl.createDataRangeAtom(this, range, arg);
    }

    @Override
    public OntSWRL.Atom.DataProperty createDataPropertySWRLAtom(OntNDP dataProperty, OntSWRL.IArg firstArg, OntSWRL.DArg secondArg) {
        return OntSWRLImpl.createDataPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.ObjectProperty createObjectPropertySWRLAtom(OntOPE dataProperty, OntSWRL.IArg firstArg, OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createObjectPropertyAtom(this, dataProperty, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.DifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createDifferentIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Atom.SameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg) {
        return OntSWRLImpl.createSameIndividualsAtom(this, firstArg, secondArg);
    }

    @Override
    public OntSWRL.Imp createSWRLImp(Collection<OntSWRL.Atom> head, Collection<OntSWRL.Atom> body) {
        return OntSWRLImpl.createImp(this, head.stream(), body.stream());
    }

}
