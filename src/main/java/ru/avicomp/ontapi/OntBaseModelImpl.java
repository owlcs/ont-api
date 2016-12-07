package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectImpl;

/**
 * TODO:
 * It's our analogy of {@link uk.ac.manchester.cs.owl.owlapi.OWLImmutableOntologyImpl}
 *
 * Created by @szuev on 03.12.2016.
 */
public class OntBaseModelImpl extends OWLObjectImpl implements OWLOntology {

    private final OntInternalModel base;
    private OWLOntologyManager manager;

    public OntBaseModelImpl(OntologyManager manager, OWLOntologyID ontologyID) {
        OntApiException.notNull(ontologyID, "Null OWL-ID.");
        this.manager = OntApiException.notNull(manager, "Null manager.");
        this.base = new OntInternalModel(manager.getGraphFactory().create());
        base.setOwlID(ontologyID);
    }

    public OntBaseModelImpl(OntologyManager manager, OntInternalModel base) {
        this.manager = OntApiException.notNull(manager, "Null manager.");
        this.base = OntApiException.notNull(base, "Null internal model.");
    }

    @Override
    public OWLOntologyID getOntologyID() {
        return base.getOwlID();
    }

    @Override
    public OWLOntologyManager getOWLOntologyManager() {
        return manager;
    }

    @Override
    public void setOWLOntologyManager(OWLOntologyManager manager) {
        this.manager = OntApiException.notNull(manager, "Null manager.");
    }

    @Override
    public Stream<OWLOntology> imports() {
        return manager.imports(this);
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
    public boolean isEmpty() {
        return base.isOntologyEmpty();
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(@Nonnull AxiomType<T> axiomType) {
        return base.getAxioms(axiomType).stream();
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
    public Stream<OWLClassAxiom> generalClassAxioms() {
        Stream<OWLSubClassOfAxiom> subClassOfAxioms = base.getAxioms(OWLSubClassOfAxiom.class).stream()
                .filter(a -> a.getSubClass().isAnonymous());
        Stream<? extends OWLNaryClassAxiom> naryClassAxioms = Stream.of(OWLEquivalentClassesAxiom.class, OWLDisjointClassesAxiom.class)
                .map(base::getAxioms).map(Collection::stream).flatMap(Function.identity())
                .filter(a -> a.classExpressions().allMatch(IsAnonymous::isAnonymous));
        return Stream.concat(subClassOfAxioms, naryClassAxioms);
    }

    @Override
    public boolean isDeclared(@Nullable OWLEntity owlEntity) {
        return base.getAxioms(OWLDeclarationAxiom.class).stream().map(OWLDeclarationAxiom::getEntity)
                .anyMatch(obj -> obj.equals(owlEntity));
    }

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
    public int getAxiomCount() {
        return (int) axioms().count();
    }

    @Override
    public int getAxiomCount(@Nonnull Imports imports) {
        return imports.stream(this).mapToInt(OWLAxiomCollection::getAxiomCount).sum();
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
    public <T extends OWLAxiom> int getAxiomCount(@Nullable AxiomType<T> axiomType) {
        return base.getAxioms(axiomType).size();
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(@Nonnull AxiomType<T> axiomType, @Nonnull Imports imports) {
        return imports.stream(this).mapToInt(o -> o.getAxiomCount(axiomType)).sum();
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
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(@Nonnull OWLAxiom axiom) {
        return axioms(axiom.getAxiomType()).map(OWLAxiom.class::cast).filter(ax -> ax.equalsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(@Nonnull OWLAxiom axiom, @Nonnull Imports imports) {
        return imports.stream(this).flatMap(o -> o.axiomsIgnoreAnnotations(axiom));
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(@Nonnull OWLPrimitive primitive) {
        return axioms().filter(a -> OntInternalModel.objects(OWLPrimitive.class, a).anyMatch(primitive::equals));
    }

    @Override
    public Stream<OWLClassAxiom> axioms(@Nonnull OWLClass clazz) {
        return base.classAxioms().filter(a -> OntInternalModel.objects(OWLClass.class, a).anyMatch(clazz::equals));
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(@Nonnull OWLObjectPropertyExpression property) {
        return base.objectPropertyAxioms().filter(a -> OntInternalModel.objects(OWLObjectPropertyExpression.class, a).anyMatch(property::equals));
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(@Nonnull OWLDataProperty property) {
        return base.dataPropertyAxioms().filter(a -> OntInternalModel.objects(OWLDataProperty.class, a).anyMatch(property::equals));
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(@Nonnull OWLIndividual individual) {
        return base.individualAxioms().filter(a -> OntInternalModel.objects(OWLIndividual.class, a).anyMatch(individual::equals));
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(@Nonnull OWLDatatype datatype) {
        return base.getAxioms(OWLDatatypeDefinitionAxiom.class).stream().filter(a -> datatype.equals(a.getDatatype()));
    }

    @Override
    public Stream<OWLAxiom> axioms() {
        return base.axioms();
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return base.logicalAxioms();
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

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, Class<? extends OWLObject> explicitClass, OWLObject entity, Navigation forSubPosition) {
        //TODO:
        return null;
    }

    @Override
    public Stream<OWLSubAnnotationPropertyOfAxiom> subAnnotationPropertyOfAxioms(OWLAnnotationProperty subProperty) {
        return null;
    }

    @Override
    public Stream<OWLAnnotationPropertyDomainAxiom> annotationPropertyDomainAxioms(OWLAnnotationProperty property) {
        return null;
    }

    @Override
    public Stream<OWLAnnotationPropertyRangeAxiom> annotationPropertyRangeAxioms(OWLAnnotationProperty property) {
        return null;
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> datatypeDefinitions(OWLDatatype datatype) {
        return null;
    }

    @Override
    public boolean containsClassInSignature(IRI owlClassIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI owlObjectPropertyIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI owlDataPropertyIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI owlAnnotationPropertyIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsDatatypeInSignature(IRI owlDatatypeIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsIndividualInSignature(IRI owlIndividualIRI, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public boolean containsDatatypeInSignature(IRI owlDatatypeIRI) {
        return false;
    }

    @Override
    public boolean containsClassInSignature(IRI owlClassIRI) {
        return false;
    }

    @Override
    public boolean containsObjectPropertyInSignature(IRI owlObjectPropertyIRI) {
        return false;
    }

    @Override
    public boolean containsDataPropertyInSignature(IRI owlDataPropertyIRI) {
        return false;
    }

    @Override
    public boolean containsAnnotationPropertyInSignature(IRI owlAnnotationPropertyIRI) {
        return false;
    }

    @Override
    public boolean containsIndividualInSignature(IRI owlIndividualIRI) {
        return false;
    }

    @Override
    public Set<IRI> getPunnedIRIs(Imports includeImportsClosure) {
        return null;
    }

    @Override
    public boolean containsReference(OWLEntity entity) {
        return false;
    }

    @Override
    public Stream<OWLOntology> importsClosure() {
        return null;
    }
}
