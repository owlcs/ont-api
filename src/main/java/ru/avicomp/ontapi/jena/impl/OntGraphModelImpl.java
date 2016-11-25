package ru.avicomp.ontapi.jena.impl;

import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Base model to work through jena only.
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work in accordance with OWL2 DL specification.
 * <p>
 * Created by @szuev on 27.10.2016.
 */
public class OntGraphModelImpl extends ModelCom implements OntGraphModel {

    /**
     * fresh ontology.
     */
    public OntGraphModelImpl() {
        this(new GraphMem());
    }

    public OntGraphModelImpl(Graph graph) {
        this(graph instanceof UnionGraph ? graph : new UnionGraph(graph), OntModelConfig.ONT_PERSONALITY);
    }

    public OntGraphModelImpl(Graph graph, OntPersonality personality) {
        this(graph, personality, true);
    }

    public OntGraphModelImpl(Graph graph, OntPersonality personality, boolean syncImports) {
        super(graph instanceof UnionGraph ? graph : new UnionGraph(graph), personality);
        if (syncImports)
            syncGraphRefs(personality);
    }

    protected void syncGraphRefs(OntPersonality personality) {
        removeAll(getID(), OWL2.imports, null);
        models(personality).map(OntGraphModel::getID).filter(Resource::isURIResource).forEach(id -> addImport(id.getURI()));
    }

    @Override
    public OntID getID() {
        List<Resource> prev = ontologyStatements().collect(Collectors.toList());
        Resource res;
        if (prev.isEmpty()) {
            res = createResource(); // anon id
            add(res, RDF.type, OWL2.Ontology);
        } else {
            res = prev.get(0); // choose first.
        }
        return getNodeAs(res.asNode(), OntID.class);
    }

    @Override
    public OntID setID(String uri) {
        List<Statement> tmp = ontologyStatements().map(s -> JenaUtils.asStream(listStatements(s, null, (RDFNode) null))).flatMap(Function.identity()).distinct().collect(Collectors.toList());
        remove(tmp);
        Resource subject;
        if (uri == null) {
            subject = tmp.stream().map(Statement::getSubject).filter(Resource::isAnon).findFirst().orElse(createResource());
        } else {
            subject = createResource(uri);
        }
        add(subject, RDF.type, OWL2.Ontology);
        tmp.forEach(s -> add(subject, s.getPredicate(), s.getObject()));
        return getNodeAs(subject.asNode(), OntID.class);
    }

    private Stream<Resource> ontologyStatements() {
        return JenaUtils.asStream(listStatements(null, RDF.type, OWL2.Ontology).mapWith(Statement::getSubject)).distinct();
    }

    @Override
    public void addImport(OntGraphModel m) {
        if (!OntJenaException.notNull(m, "Null model.").getID().isURIResource()) {
            throw new OntJenaException("Anonymous sub models are not allowed");
        }
        getGraph().addGraph(m.getGraph());
        addImport(m.getID());
    }

    @Override
    public void removeImport(OntGraphModel m) {
        getGraph().removeGraph(OntJenaException.notNull(m, "Null model.").getGraph());
        removeImport(m.getID());
    }

    @Override
    public void addImport(String uri) {
        addImport(createResource(uri));
    }

    public void addImport(Resource uri) {
        add(getID(), OWL2.imports, uri);
    }

    public void removeImport(Resource uri) {
        removeAll(getID(), OWL2.imports, uri);
    }

    @Override
    public void removeImport(String uri) {
        removeImport(createResource(uri));
    }

    @Override
    public Stream<Resource> imports() {
        return JenaUtils.asStream(listStatements(null, OWL2.imports, (RDFNode) null)
                .filterKeep(this::isInBaseModel)
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isURIResource)
                .mapWith(RDFNode::asResource));
    }

    @Override
    public Stream<OntGraphModel> models() {
        return models(getPersonality());
    }

    public Stream<OntGraphModel> models(OntPersonality personality) {
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

    public Model getBaseModel() {
        return ModelFactory.createModelForGraph(getBaseGraph());
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

    public boolean isInBaseModel(Statement stmt) {
        return isInBaseModel(stmt.getSubject(), stmt.getPredicate(), stmt.getObject());
    }

    protected boolean isInBaseModel(Resource s, Property p, RDFNode o) {
        return getBaseGraph().contains(s.asNode(), p.asNode(), o.asNode());
    }

    @Override
    public OntGraphModelImpl remove(Resource s, Property p, RDFNode o) { // todo: removing is allowed only for base graph
        graph.delete(Triple.create(s.asNode(), p.asNode(), o.asNode()));
        return this;
    }

    @Override
    public Stream<OntEntity> ontEntities() {
        return ontObjects(OntEntity.class);
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
    public <T extends OntEntity> T getOntEntity(Class<T> type, String uri) {
        Node n = NodeFactory.createURI(OntJenaException.notNull(uri, "Null uri."));
        try { // returns not null in case it is present in graph or built-in.
            return getNodeAs(n, type);
        } catch (ConversionException ignore) {
            // ignore
            return null;
        }
    }

    /**
     * to create any OntObject resource
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
        return createOntObject(type, uri);
    }

    @Override
    public void removeOntObject(OntObject obj) {
        obj.clearAnnotations();
        removeAll(obj, null, null);
    }

    @Override
    public <T extends OntEntity> Stream<T> ontEntities(Class<T> type) {
        return ontObjects(type);
    }

    @Override
    protected OntPersonality getPersonality() {
        return (OntPersonality) super.getPersonality();
    }

    @Override
    public OntDisjoint.Classes createDisjointClasses(Stream<OntCE> classes) {
        return OntDisjointImpl.createDisjointClasses(this, classes);
    }

    @Override
    public OntDisjoint.Individuals createDifferentIndividuals(Stream<OntIndividual> individuals) {
        return OntDisjointImpl.createDifferentIndividuals(this, individuals);
    }

    @Override
    public OntDisjoint.ObjectProperties createDisjointObjectProperties(Stream<OntOPE> properties) {
        return OntDisjointImpl.createDisjointObjectProperties(this, properties);
    }

    @Override
    public OntDisjoint.DataProperties createDisjointDataProperties(Stream<OntNDP> properties) {
        return OntDisjointImpl.createDisjointDataProperties(this, properties);
    }

    @Override
    public <T extends OntFR> T createFacetRestriction(Class<T> view, Literal literal) {
        return OntFRImpl.create(this, view, literal);
    }

    @Override
    public OntDR.OneOf createOneOfDataRange(Stream<Literal> values) {
        return OntDRImpl.createOneOf(this, values);
    }

    @Override
    public OntDR.Restriction createRestrictionDataRange(OntDR property, Stream<OntFR> values) {
        return OntDRImpl.createRestriction(this, property, values);
    }

    @Override
    public OntDR.ComplementOf createComplementOfDataRange(OntDR other) {
        return OntDRImpl.createComplementOf(this, other);
    }

    @Override
    public OntDR.UnionOf createUnionOfDataRange(Stream<OntDR> values) {
        return OntDRImpl.createUnionOf(this, values);
    }

    @Override
    public OntDR.IntersectionOf createIntersectionOfDataRange(Stream<OntDR> values) {
        return OntDRImpl.createIntersectionOf(this, values);
    }

    @Override
    public OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectSomeValuesFrom.class, onProperty, other, OWL2.someValuesFrom);
    }

    @Override
    public OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataSomeValuesFrom.class, onProperty, other, OWL2.someValuesFrom);
    }

    @Override
    public OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE onProperty, OntCE other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectAllValuesFrom.class, onProperty, other, OWL2.allValuesFrom);
    }

    @Override
    public OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP onProperty, OntDR other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataAllValuesFrom.class, onProperty, other, OWL2.allValuesFrom);
    }

    @Override
    public OntCE.ObjectHasValue createObjectHasValue(OntOPE onProperty, OntIndividual other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.ObjectHasValue.class, onProperty, other, OWL2.hasValue);
    }

    @Override
    public OntCE.DataHasValue createDataHasValue(OntNDP onProperty, Literal other) {
        return OntCEImpl.createComponentRestrictionCE(this, OntCE.DataHasValue.class, onProperty, other, OWL2.hasValue);
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
    public OntCE.UnionOf createUnionOf(Stream<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.UnionOf.class, OWL2.unionOf, classes);
    }

    @Override
    public OntCE.IntersectionOf createIntersectionOf(Stream<OntCE> classes) {
        return OntCEImpl.createComponentsCE(this, OntCE.IntersectionOf.class, OWL2.intersectionOf, classes);
    }

    @Override
    public OntCE.OneOf createOneOf(Stream<OntIndividual> individuals) {
        return OntCEImpl.createComponentsCE(this, OntCE.OneOf.class, OWL2.oneOf, individuals);
    }

    @Override
    public OntCE.HasSelf createHasSelf(OntOPE onProperty) {
        return OntCEImpl.createHasSelf(this, onProperty);
    }

    @Override
    public OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Stream<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException("Unsupported " + OntCE.NaryDataAllValuesFrom.class);
    }

    @Override
    public OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Stream<OntNDP> onProperties, OntDR other) {
        //todo
        throw new OntJenaException("Unsupported " + OntCE.NaryDataSomeValuesFrom.class);
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
    public OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Stream<OntSWRL.DArg> arguments) {
        return OntSWRLImpl.createBuiltInAtom(this, predicate, arguments);
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
    public OntSWRL.Imp createSWRLImp(Stream<OntSWRL.Atom> head, Stream<OntSWRL.Atom> body) {
        return OntSWRLImpl.createImp(this, head, body);
    }
}
