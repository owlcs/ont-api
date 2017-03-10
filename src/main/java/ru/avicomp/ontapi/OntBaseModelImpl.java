package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.search.Filters;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.model.OntID;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectImpl;

/**
 * 'Immutable' ontology with methods to read information in the form of OWL-Objects from graph-model.
 * It's our analogy of {@link uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl}
 * <p>
 * Created by @szuev on 03.12.2016.
 */
public class OntBaseModelImpl extends OWLObjectImpl implements OWLOntology {
    // binary format to provide serialization:
    private static final OntFormat DEFAULT_SERIALIZATION_FORMAT = OntFormat.RDF_THRIFT;

    protected transient InternalModel base;
    protected OntologyManager manager;

    protected OWLOntologyID ontologyID;

    public OntBaseModelImpl(OntologyManager manager, OWLOntologyID ontologyID) {
        OntApiException.notNull(ontologyID, "Null OWL ID.");
        setOWLOntologyManager(OntApiException.notNull(manager, "Null manager."));
        setBase(new InternalModel(OntFactory.createDefaultGraph(), manager.getOntologyLoaderConfiguration().getPersonality()));
        setOntologyID(ontologyID);
    }

    public OntBaseModelImpl(OntologyManager manager, InternalModel base) {
        setOWLOntologyManager(OntApiException.notNull(manager, "Null manager."));
        setBase(OntApiException.notNull(base, "Null internal model."));
    }

    public InternalModel getBase() {
        return base;
    }

    protected void setBase(InternalModel m) {
        base = m;
    }

    @Override
    public OntologyManager getOWLOntologyManager() {
        return manager;
    }

    /**
     * Sets the manager.
     * The parameter could be null (e.g. during {@link OWLOntologyManager#clearOntologies})
     *
     * @param manager {@link OntologyManager}, nullable.
     * @throws ClassCastException in case wrong manager specified.
     */
    @Override
    public void setOWLOntologyManager(OWLOntologyManager manager) {
        this.manager = (OntologyManager) manager;
    }

    /**
     * Gets ID.
     * Does not just return cached {@link #ontologyID} to provide synchronization with encapsulated jena model ({@link #base}).
     * In the other hand we need this cached {@link #ontologyID} to be existed and relevant for owl serialization.
     *
     * @return the {@link OWLOntologyID}
     */
    @Override
    public OWLOntologyID getOntologyID() {
        OntID id = base.getID();
        if (id.isAnon()) {
            return ontologyID == null || !ontologyID.isAnonymous() ? ontologyID = new OWLOntologyID() : ontologyID;
        }
        Optional<IRI> iri = Optional.of(id.getURI()).map(IRI::create);
        Optional<IRI> version = Optional.ofNullable(id.getVersionIRI()).map(IRI::create);
        return ontologyID = new OWLOntologyID(iri, version);
    }

    /**
     * Sets ID.
     * Protected access since this is an "immutable" ontology.
     *
     * @param id {@link OWLOntologyID}
     */
    protected void setOntologyID(OWLOntologyID id) {
        try {
            if (id.isAnonymous()) {
                base.setID(null).setVersionIRI(null);
                return;
            }
            IRI iri = id.getOntologyIRI().orElse(null);
            IRI versionIRI = id.getVersionIRI().orElse(null);
            base.setID(iri == null ? null : iri.getIRIString()).setVersionIRI(versionIRI == null ? null : versionIRI.getIRIString());
        } finally {
            ontologyID = id;
        }
    }

    @Override
    public boolean isAnonymous() {
        return base.getID().isAnon();
    }

    @Override
    public boolean isEmpty() {
        return base.isOntologyEmpty();
    }

    @Override
    public Stream<OWLAnnotation> annotations() {
        return base.annotations();
    }

    /**
     * =============================
     * Methods to work with imports:
     * =============================
     */

    @Override
    public Stream<OWLOntology> imports() {
        return getOWLOntologyManager().imports(this);
    }

    @Override
    public Stream<OWLImportsDeclaration> importsDeclarations() {
        return base.importDeclarations();
    }

    @Override
    public Stream<IRI> directImportsDocuments() {
        return importsDeclarations().map(OWLImportsDeclaration::getIRI);
    }

    @Override
    public Stream<OWLOntology> directImports() {
        return manager.directImports(this);
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        return getOWLOntologyManager().importsClosure(this);
    }

    /**
     * ==========================
     * To work with OWL-entities:
     * ==========================
     */

    @Override
    public Stream<OWLClass> classesInSignature() {
        return base.classes();
    }

    @Override
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return base.anonymousIndividuals();
    }

    @Override
    public Stream<OWLAnonymousIndividual> referencedAnonymousIndividuals() {
        return anonymousIndividuals();
    }

    @Override
    public Stream<OWLNamedIndividual> individualsInSignature() {
        return base.individuals();
    }

    @Override
    public Stream<OWLDataProperty> dataPropertiesInSignature() {
        return base.dataProperties();
    }

    @Override
    public Stream<OWLObjectProperty> objectPropertiesInSignature() {
        return base.objectProperties();
    }

    @Override
    public Stream<OWLAnnotationProperty> annotationPropertiesInSignature() {
        return base.annotationProperties();
    }

    @Override
    public Stream<OWLDatatype> datatypesInSignature() {
        return base.datatypes();
    }

    @Override
    public Stream<OWLEntity> signature() {
        return Stream.of(classesInSignature(), objectPropertiesInSignature(), dataPropertiesInSignature(),
                individualsInSignature(), datatypesInSignature(), annotationPropertiesInSignature()).flatMap(Function.identity());
    }

    @Override
    public Stream<OWLEntity> entitiesInSignature(@Nullable IRI entityIRI) {
        return base.getEntities(entityIRI).stream();
    }

    @Override
    public Set<IRI> getPunnedIRIs(@Nonnull Imports imports) {
        return base.ambiguousEntities(Imports.INCLUDED.equals(imports)).map(Resource::getURI).map(IRI::create).collect(Collectors.toSet());
    }

    @Override
    public boolean isDeclared(@Nullable OWLEntity owlEntity) {
        return base.getAxioms(OWLDeclarationAxiom.class).stream().map(OWLDeclarationAxiom::getEntity)
                .anyMatch(obj -> obj.equals(owlEntity));
    }

    @Override
    public boolean containsReference(@Nonnull OWLEntity entity) {
        return signature().anyMatch(entity::equals);
    }

    @Override
    public boolean containsClassInSignature(@Nonnull IRI iri) {
        return classesInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsClassInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsClassInSignature(iri));
    }

    @Override
    public boolean containsObjectPropertyInSignature(@Nonnull IRI iri) {
        return objectPropertiesInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsObjectPropertyInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsObjectPropertyInSignature(iri));
    }

    @Override
    public boolean containsDataPropertyInSignature(@Nonnull IRI iri) {
        return dataPropertiesInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsDataPropertyInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsDataPropertyInSignature(iri));
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(@Nonnull IRI iri) {
        return annotationPropertiesInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsAnnotationPropertyInSignature(iri));
    }

    @Override
    public boolean containsDatatypeInSignature(@Nonnull IRI iri) {
        return datatypesInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsDatatypeInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsDatatypeInSignature(iri));
    }

    @Override
    public boolean containsIndividualInSignature(@Nonnull IRI iri) {
        return individualsInSignature().map(HasIRI::getIRI).anyMatch(iri::equals);
    }

    @Override
    public boolean containsIndividualInSignature(@Nonnull IRI iri, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.containsIndividualInSignature(iri));
    }

    /**
     * =======================
     * To work with OWL-Axioms
     * =======================
     */

    @Override
    public Stream<OWLAxiom> axioms() {
        return base.axioms();
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(@Nonnull AxiomType<T> axiomType) {
        return base.getAxioms(axiomType).stream();
    }

    @Override
    public Stream<OWLClassAxiom> axioms(@Nonnull OWLClass clazz) {
        return base.classAxioms().filter(a -> OwlObjects.objects(OWLClass.class, a).anyMatch(clazz::equals));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(@Nonnull OWLObjectPropertyExpression property) {
        return base.objectPropertyAxioms().filter(a -> OwlObjects.objects(OWLObjectPropertyExpression.class, a).anyMatch(property::equals));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(@Nonnull OWLDataProperty property) {
        return base.dataPropertyAxioms().filter(a -> OwlObjects.objects(OWLDataProperty.class, a).anyMatch(property::equals));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(@Nonnull OWLIndividual individual) {
        return base.individualAxioms().filter(a -> OwlObjects.objects(OWLIndividual.class, a).anyMatch(individual::equals));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(@Nonnull OWLDatatype datatype) {
        return base.getAxioms(OWLDatatypeDefinitionAxiom.class).stream().filter(a -> datatype.equals(a.getDatatype()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> Stream<T> axioms(@Nonnull OWLAxiomSearchFilter filter, @Nonnull Object key) {
        return (Stream<T>) base.axioms(StreamSupport.stream(filter.getAxiomTypes().spliterator(), false)
                .map(type -> (AxiomType<T>) type)
                .collect(Collectors.toSet())).filter(a -> filter.pass(a, key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> Stream<T> axioms(@Nonnull OWLAxiomSearchFilter filter, @Nonnull Object key, @Nonnull Imports imports) {
        return imports.stream(this).flatMap(o -> (Stream<T>) o.axioms(filter, key));
    }

    /**
     * Generic search method: results all axioms which refer object, are instances of type.
     * WARNING: it differs from original OWL-API method (see {@link uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl#axioms(Class, Class, OWLObject, Navigation)}).
     *
     * @param type           OWLAxiom Class, not null.
     * @param explicitClass  any class. it is never used.
     * @param object         OWLObject to find occurrences
     * @param forSubPosition could be {@link Navigation#IN_SUPER_POSITION} or anything else. used only to find sub-property/class axioms.
     * @return Stream of {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends OWLAxiom> Stream<A> axioms(@Nonnull Class<A> type, @Nullable Class<? extends OWLObject> explicitClass, @Nonnull OWLObject object, @Nullable Navigation forSubPosition) {
        if (OWLSubObjectPropertyOfAxiom.class.equals(type) && OWLObjectPropertyExpression.class.isInstance(object)) {
            return (Stream<A>) base.axioms(OWLSubObjectPropertyOfAxiom.class)
                    .filter(a -> object.equals(Navigation.IN_SUPER_POSITION.equals(forSubPosition) ? a.getSuperProperty() : a.getSubProperty()));
        }
        if (OWLSubDataPropertyOfAxiom.class.equals(type) && OWLDataPropertyExpression.class.isInstance(object)) {
            return (Stream<A>) base.axioms(OWLSubDataPropertyOfAxiom.class)
                    .filter(a -> object.equals(Navigation.IN_SUPER_POSITION.equals(forSubPosition) ? a.getSuperProperty() : a.getSubProperty()));
        }
        if (OWLSubAnnotationPropertyOfAxiom.class.equals(type) && OWLAnnotationProperty.class.isInstance(object)) { // the difference: this axiom type is ignored in original OWL-API method:
            return (Stream<A>) base.axioms(OWLSubAnnotationPropertyOfAxiom.class)
                    .filter(a -> object.equals(Navigation.IN_SUPER_POSITION.equals(forSubPosition) ? a.getSuperProperty() : a.getSubProperty()));
        }
        if (OWLSubClassOfAxiom.class.equals(type) && OWLClassExpression.class.isInstance(object)) {
            return (Stream<A>) base.axioms(OWLSubClassOfAxiom.class)
                    .filter(a -> object.equals(Navigation.IN_SUPER_POSITION.equals(forSubPosition) ? a.getSuperClass() : a.getSubClass()));
        }
        if (OWLClassAxiom.class.equals(type) && OWLClass.class.isInstance(object)) {
            return (Stream<A>) axioms((OWLClass) object);
        }
        if (OWLObjectPropertyAxiom.class.equals(type) && OWLObjectPropertyExpression.class.isInstance(object)) {
            return (Stream<A>) axioms((OWLObjectPropertyExpression) object);
        }
        if (OWLDataPropertyAxiom.class.equals(type) && OWLDataProperty.class.isInstance(object)) {
            return (Stream<A>) axioms((OWLDataProperty) object);
        }
        if (OWLIndividualAxiom.class.equals(type) && OWLIndividual.class.isInstance(object)) {
            return (Stream<A>) axioms((OWLIndividual) object);
        }
        return base.axioms(type).filter(a -> OwlObjects.objects(object.getClass(), a).anyMatch(object::equals));
    }

    @Override
    public Stream<OWLAxiom> tboxAxioms(@Nonnull Imports imports) {
        return AxiomType.TBoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> aboxAxioms(@Nonnull Imports imports) {
        return AxiomType.ABoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLAxiom> rboxAxioms(@Nonnull Imports imports) {
        return AxiomType.RBoxAxiomTypes.stream().flatMap(t -> axioms(t, imports));
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return base.logicalAxioms();
    }

    @Override
    public Stream<OWLClassAxiom> generalClassAxioms() {
        Stream<OWLSubClassOfAxiom> subClassOfAxioms = base.getAxioms(OWLSubClassOfAxiom.class).stream()
                .filter(a -> a.getSubClass().isAnonymous());
        Stream<? extends OWLNaryClassAxiom> naryClassAxioms = Stream.of(OWLEquivalentClassesAxiom.class, OWLDisjointClassesAxiom.class)
                .map(base::getAxioms).map(Collection::stream).flatMap(Function.identity())
                .filter(a -> a.classExpressions().allMatch(IsAnonymous::isAnonymous));
        return Stream.concat(subClassOfAxioms, naryClassAxioms);
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(@Nonnull OWLAxiom axiom) {
        return axioms(axiom.getAxiomType()).map(OWLAxiom.class::cast).filter(ax -> ax.equalsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(@Nonnull OWLAxiom axiom, @Nonnull Imports imports) {
        return imports.stream(this).flatMap(o -> o.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(@Nonnull OWLPrimitive primitive) {
        return axioms().filter(a -> OwlObjects.objects(OWLPrimitive.class, a).anyMatch(primitive::equals));
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(@Nonnull OWLAnnotationProperty property) {
        return axioms(Filters.subAnnotationWithSub, property);
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(@Nonnull OWLAnnotationProperty property) {
        return axioms(Filters.apDomainFilter, property);
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(@Nonnull OWLAnnotationProperty property) {
        return axioms(Filters.apRangeFilter, property);
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(@Nonnull OWLDatatype datatype) {
        return axioms(Filters.datatypeDefFilter, datatype);
    }

    @Override
    public int getAxiomCount() {
        return (int) axioms().count();
    }

    @Override
    public int getAxiomCount(@Nonnull Imports imports) {
        return imports.stream(this).mapToInt(OWLAxiomCollection::getAxiomCount).sum();
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(@Nullable AxiomType<T> axiomType) {
        return base.getAxioms(axiomType).size();
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(@Nonnull AxiomType<T> axiomType, @Nonnull Imports imports) {
        return imports.stream(this).mapToInt(o -> o.getAxiomCount(axiomType)).sum();
    }

    @Override
    public int getLogicalAxiomCount() {
        return (int) logicalAxioms().count();
    }

    @Override
    public int getLogicalAxiomCount(@Nonnull Imports imports) {
        return imports.stream(this).mapToInt(OWLAxiomCollection::getLogicalAxiomCount).sum();
    }

    @Override
    public boolean containsAxiom(@Nullable OWLAxiom axiom) {
        return base.axioms().anyMatch(a -> a.equals(axiom));
    }

    @Override
    public boolean containsAxiom(@Nonnull OWLAxiom axiom, @Nonnull Imports imports, @Nonnull AxiomAnnotations ignoreAnnotations) {
        return imports.stream(this).anyMatch(o -> ignoreAnnotations.contains(o, axiom));
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(@Nonnull OWLAxiom axiom) {
        return containsAxiom(axiom) || axioms(axiom.getAxiomType()).anyMatch(ax -> ax.equalsIgnoreAnnotations(axiom));
    }

    @Override
    public boolean contains(@Nonnull OWLAxiomSearchFilter filter, @Nonnull Object key) {
        return base.axioms(StreamSupport.stream(filter.getAxiomTypes().spliterator(), false)
                .map(type -> type)
                .collect(Collectors.toSet())).anyMatch(a -> filter.pass(a, key));
    }

    @Override
    public boolean contains(@Nonnull OWLAxiomSearchFilter filter, @Nonnull Object key, @Nonnull Imports imports) {
        return imports.stream(this).anyMatch(o -> o.contains(filter, key));
    }

    /**
     * ======================
     * Serialization methods:
     * ======================
     */

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Graph base = OntFactory.createDefaultGraph();
        RDFDataMgr.read(base, in, DEFAULT_SERIALIZATION_FORMAT.getLang());
        // set temporary model with default personality, it will be reset inside manager while its #readObject
        setBase(new InternalModel(base, OntModelConfig.getPersonality()));
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject(); // serialize only base graph (it will be wrapped as UnionGraph):
        RDFDataMgr.write(out, base.getBaseGraph(), DEFAULT_SERIALIZATION_FORMAT.getLang());
    }

    /**
     * Overridden {@link super#toString()} to not force axiom loading.
     *
     * @return String
     */
    @Override
    public String toString() {
        return String.format("Ontology(%s)", ontologyID);
    }
}
