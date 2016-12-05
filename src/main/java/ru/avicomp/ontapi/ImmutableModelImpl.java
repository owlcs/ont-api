package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.AxiomAnnotations;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.Navigation;
import org.semanticweb.owlapi.util.OWLAxiomSearchFilter;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectImpl;

/**
 * TODO:
 * Created by @szuev on 03.12.2016.
 */
public class ImmutableModelImpl extends OWLObjectImpl implements OWLOntology {

    private final OntInternalModel base;
    private OWLOntologyManager manager;

    public ImmutableModelImpl(OntologyManager manager, OWLOntologyID ontologyID) {
        OntApiException.notNull(ontologyID, "Null OWL-ID.");
        this.manager = OntApiException.notNull(manager, "Null manager.");
        this.base = new OntInternalModel(manager.getGraphFactory().create());
        base.setOwlID(ontologyID);
    }

    public ImmutableModelImpl(OntologyManager manager, OntInternalModel base) {
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
    public Stream<OWLEntity> entitiesInSignature(IRI entityIRI) { //TODO:
        return null;
    }

    @Override
    public int getAxiomCount() {
        return 0;
    }

    @Override
    public int getAxiomCount(Imports includeImportsClosure) {
        return 0;
    }

    @Override
    public int getLogicalAxiomCount() {
        return 0;
    }

    @Override
    public int getLogicalAxiomCount(Imports includeImportsClosure) {
        return 0;
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType) {
        return 0;
    }

    @Override
    public <T extends OWLAxiom> int getAxiomCount(AxiomType<T> axiomType, Imports includeImportsClosure) {
        return 0;
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom, Imports includeImportsClosure, AxiomAnnotations ignoreAnnotations) {
        return false;
    }

    @Override
    public boolean containsAxiomIgnoreAnnotations(OWLAxiom axiom) {
        return false;
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom) {
        return null;
    }

    @Override
    public Stream<OWLAxiom> axiomsIgnoreAnnotations(OWLAxiom axiom, Imports includeImportsClosure) {
        return null;
    }

    @Override
    public Stream<OWLAxiom> referencingAxioms(OWLPrimitive owlEntity) {
        return null;
    }

    @Override
    public Stream<OWLClassAxiom> axioms(OWLClass cls) {
        return null;
    }

    @Override
    public Stream<OWLObjectPropertyAxiom> axioms(OWLObjectPropertyExpression property) {
        return null;
    }

    @Override
    public Stream<OWLDataPropertyAxiom> axioms(OWLDataProperty property) {
        return null;
    }

    @Override
    public Stream<OWLIndividualAxiom> axioms(OWLIndividual individual) {
        return null;
    }

    @Override
    public Stream<OWLDatatypeDefinitionAxiom> axioms(OWLDatatype datatype) {
        return null;
    }

    @Override
    public Stream<OWLAxiom> axioms() {
        return null;
    }

    @Override
    public boolean containsAxiom(OWLAxiom axiom) {
        return false;
    }

    @Override
    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return null;
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key, Imports includeImportsClosure) {
        return null;
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(OWLAxiomSearchFilter filter, Object key) {
        return null;
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object key) {
        return false;
    }

    @Override
    public boolean contains(OWLAxiomSearchFilter filter, Object key, Imports includeImportsClosure) {
        return false;
    }

    @Override
    public <T extends OWLAxiom> Stream<T> axioms(Class<T> type, Class<? extends OWLObject> explicitClass, OWLObject entity, Navigation forSubPosition) {
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
