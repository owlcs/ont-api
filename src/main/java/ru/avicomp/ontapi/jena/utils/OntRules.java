package ru.avicomp.ontapi.jena.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Helper to test all kind of OWL Objects (OWL Entities, OWL Class&ObjectProperty Expressions, OWL DataRanges, OWL Anonymous Individuals)
 * inside the jena model {@link Model} according to <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Specification</a>.
 * <p>
 * Created by @szuev on 28.01.2017.
 */
public class OntRules {
    private static RuleEngine rules = new StrictRuleEngine();

    public static RuleEngine getRules() {
        return rules;
    }

    public static void setRules(RuleEngine engine) {
        rules = OntJenaException.notNull(engine, "Null rule engine specified.");
    }

    public static boolean isAnnotationProperty(Model model, Resource candidate) {
        return rules.isAnnotationProperty(model, candidate);
    }

    public static boolean isDataProperty(Model model, Resource candidate) {
        return rules.isDataProperty(model, candidate);
    }

    public static boolean isObjectPropertyExpression(Model model, Resource candidate) {
        return rules.isObjectPropertyExpression(model, candidate);
    }

    public static boolean isDataRange(Model model, Resource candidate) {
        return rules.isDataRange(model, candidate);
    }

    public static boolean isClassExpression(Model model, Resource candidate) {
        return rules.isClassExpression(model, candidate);
    }

    public static boolean isIndividual(Model model, Resource candidate) {
        return rules.isIndividual(model, candidate);
    }

    public interface RuleEngine {
        boolean isAnnotationProperty(Model model, Resource candidate);

        boolean isDataProperty(Model model, Resource candidate);

        boolean isObjectPropertyExpression(Model model, Resource candidate);

        boolean isDataRange(Model model, Resource candidate);

        boolean isClassExpression(Model model, Resource candidate);

        boolean isIndividual(Model model, Resource candidate);

        default boolean isObjectProperty(Model model, Resource candidate) {
            return candidate.isURIResource() && isObjectPropertyExpression(model, candidate);
        }

        default boolean isClass(Model model, Resource candidate) {
            return candidate.isURIResource() && isClassExpression(model, candidate);
        }

        default boolean isDatatype(Model model, Resource candidate) {
            return candidate.isURIResource() && isDataRange(model, candidate);
        }
    }

    /**
     * The default Rule Engine Implementation.
     * <p>
     * Should work correctly and safe with any ontology (OWL-1, RDFS) using the "strict" approach:
     * if some resource has no clear hints about the type in the specified model than the corresponding method should return 'false'.
     * In other words the conditions to test should be necessary and sufficient.
     * This is in order not to produce unnecessary punnings.
     * But please NOTE: the punnings is not taken into account in obvious cases.
     * E.g. if the property has rdfs:range with rdf:type = rdfs:Datatype assigned then it is a Data Property,
     * even some other tips show it is Annotation Property or something else.
     */
    public static class StrictRuleEngine implements RuleEngine {
        private static final String DATA_PROPERTY_KEY = "DataProperty";
        private static final String OBJECT_PROPERTY_KEY = "ObjectProperty";
        private static final String ANNOTATION_PROPERTY_KEY = "AnnotationProperty";
        private static final String CLASS_EXPRESSION_KEY = "ClassExpression";
        private static final String DATA_RANGE_KEY = "DataRange";
        private static final String INDIVIDUAL_KEY = "Individual";

        /**
         * checks that specified resource(rdf:Property) is a Data Property (owl:DatatypeProperty).
         * It's marked as 'R' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>.
         * <p>
         * Note: the checking for "R rdfs:domain C" and "a R v" are skipped, because they could be confused with similar axioms
         * but for an annotation property ("s A t" and "A rdfs:domain U").
         * Here: 'v' is a literal, 'a' is an individual, 's' is IRI or anonymous individual, 'U' is IRI.
         * <p>
         * There are following conditions to test:
         * 0) it is an uri-resource
         * 1) it's in builtin list (owl:topDataProperty, owl:bottomDataProperty).
         * 2) it has the an explicit type owl:DatatypeProperty
         * 3) it has a data range (Datatype(DN) or DataRange(D)): "R rdfs:range D"
         * 4) it contains in negative data property assertion (owl:NegativePropertyAssertion, checks for owl:targetValue).
         * 5) it is a property in the common data property restrictions (owl:Restriction with owl:onProperty = tested object).
         * 6) it is in the list of properties (predicate owl:onProperties) from N-ary Data Range Restriction.
         * 7) recursively checks objects from the triples with predicate owl:equivalentProperty, owl:propertyDisjointWith, rdfs:subPropertyOf
         * 8) recursively checks subjects for the same statements (the candidate resource is regarded now as an object in a triple)
         * 9) recursively checks objects from owl:AllDisjointProperties(owl:members) anonymous section.
         *
         * @param model     model {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this uri-resource is a Data Property
         */
        @Override
        public boolean isDataProperty(Model model, Resource candidate) {
            return isDataProperty(model, candidate, new HashMap<>());
        }

        /**
         * checks that specified uri-resource(rdf:Property) is a Annotation Property (owl:AnnotationProperty).
         * It's marked as 'A' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>.
         * <p>
         * Note: we don't check rdfs:domain and rdfs:range statements since theirs right part is just an uri,
         * so could be treated as standalone resource or be defined in the graph as class expressions or as anything else.
         * Checking of non-existence such constructions seems contrary to the strict approach
         * because the model can be used in imports of some other ontology where that uri has explanation.
         * It seems there are no way to determine exactly("necessary and sufficient") that the candidate is an annotation property
         * if there are only domain or range axioms.
         * Also we skip the check for annotation property assertions ("s A t") since it can be confused with data property assertion ("a R v").
         * <p>
         * There are following conditions to test:
         * 0) it is an uri-resource
         * 1) it's in builtin list (rdfs:label, owl:deprecated ... etc)
         * 2) it has the an explicit type owl:AnnotationProperty (or owl:OntologyProperty for annotations in owl:Ontology section, deprecated)
         * 3) any non-builtin predicate from anon section with type owl:Axiom, owl:Annotation (bulk annotations)
         * 4) recursive object's checking for triple "A1 rdfs:subPropertyOf A2"
         * 5) recursive subject's checking for triple "A1 rdfs:subPropertyOf A2"
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @return true if this uri-resource is an Annotation Property (owl:AnnotationProperty)
         */
        @Override
        public boolean isAnnotationProperty(Model model, Resource candidate) {
            return isAnnotationProperty(model, candidate, new HashMap<>());
        }

        /**
         * checks that specified resource is an Object Property Expression.
         * The object property is marked 'PN' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>,
         * while general object property expression holds the designation of 'P'.
         * <p>
         * There are following conditions to test:
         * 1) it's from builtin list (owl:topObjectProperty, owl:bottomObjectProperty).
         * 2) it has the an explicit type owl:ObjectProperty
         * 3) it has one of the following types:
         * - owl:InverseFunctionalProperty,
         * - owl:ReflexiveProperty,
         * - owl:IrreflexiveProperty,
         * - owl:SymmetricProperty,
         * - owl:AsymmetricProperty,
         * - owl:TransitiveProperty
         * 4) it is left or right part of "P1 owl:inverseOf P2"
         * 5) the range is a class expression: "P rdfs:range C".
         * 6) a predicate in positive object property assertion "a1 PN a2".
         * 7) attached to a negative object property assertion (owl:NegativePropertyAssertion, the check for owl:targetIndividual).
         * 8) todo: checking for object property restrictions (owl:Restriction with owl:onProperty = tested object).
         * 8.1) todo: is a subject in "_:x owl:hasSelf "true"^^xsd:boolean" (local reflexivity)
         * 9) part of "P owl:propertyChainAxiom (P1 ... Pn)"
         * 10) recursively checks objects from triples with predicate owl:equivalentProperty, owl:propertyDisjointWith, rdfs:subPropertyOf
         * 11) recursively checks subjects for the same statements (the input resource is regarded as an object in the triple)
         * 12) recursively checks objects from owl:AllDisjointProperties(owl:members)
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this resource is an Object Property
         */
        @Override
        public boolean isObjectPropertyExpression(Model model, Resource candidate) {
            return isObjectPropertyExpression(model, candidate, new HashMap<>());
        }

        /**
         * checks that specified resource(rdfs:Class) is a Data Range Expression or a Datatype
         * ('D' and 'DN' in in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>).
         * <p>
         * There are following conditions to test:
         * 1) it's from builtin list (rdfs:Literal, xsd:string, owl:rational ... etc).
         * 2) it has the an explicit type rdfs:Datatype or owl:DataRange(deprecated in OWL 2)
         * 3) it is the right part of owl:onDataRange inside "_:x rdf:type owl:Restriction" (DataProperty Restriction class expression)
         * 4) it is a subject or an uri-object in the statement "_:x owl:onDatatype DN" (datatype restriction)
         * x) todo: owl:withRestrictions,
         * 5) it is a subject or an object in the statement "_:x owl:datatypeComplementOf D" (data range complement)
         * 6) it is a subject in the statement which contains a list of literals as an object "_:x owl:oneOf (v1 ... vn)"
         * Recursive checks:
         * 7) is an uri-resource and contains another data range in the right part of statement "DN owl:equivalentClass D"
         * 8) is an object in statement with predicate owl:equivalentClass where subject is uri data range
         * 9) recursively checks objects inside lists with predicates owl:intersectionOf and owl:unionOf.
         * 10) the same but for a subject. if tested resource is in the list which corresponds data range union or data range intersection.
         * xx) todo: an object in owl:someValuesFrom,  owl:allValuesFrom where onProperty is R
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this resource is an Object Property
         */
        @Override
        public boolean isDataRange(Model model, Resource candidate) {
            return isDataRange(model, candidate, new HashMap<>());
        }

        /**
         * checks that specified resource(rdfs:Class) is a Class Expression.
         * ('C' and 'CN' in in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>).
         * <p>
         * There are following conditions to test on Class Expression:
         * 1) it's from builtin list (owl:Nothing, owl:Thing).
         * 2) it has the an explicit type owl:Class or owl:Restriction(for anonymous CE)
         * x) todo: has owl:onProperties, owl:onProperty and anonymous(?), owl:maxCardinality, owl:allValuesFrom -> restriction with missing type
         * 3) it is the right part of statement with predicate owl:hasKey ("C owl:hasKey (P1 ... Pm R1 ... Rn)")
         * 4) left or right part of "C1 rdfs:subClassOf C2"
         * 5) left or right part of "C1 owl:disjointWith C2"
         * 6) an anonymous subject or an object in a statement with predicate owl:complementOf (Object complement of CE)
         * 7) left or right part of "CN owl:disjointUnionOf (C1 ... Cn)"
         * 8) an object in a statement with predicate owl:onClass (cardinality restriction)
         * <p>
         * 9) member in the list with predicate owl:members and rdf:type = owl:AllDisjointClasses
         * x) it is a subject in the statement which contains a list of individuals as an object "_:x owl:oneOf (a1 ... an)"
         * 10) 	P rdfs:domain C and R rdfs:domain C
         * 11) P rdfs:range C.
         * 12) a rdf:type C
         * d) recursively for owl:equivalentClass
         * e) recursively for owl:intersectionOf,  owl:unionOf
         * z) owl:someValuesFrom,  owl:allValuesFrom
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this resource is a Class Expression
         */
        @Override
        public boolean isClassExpression(Model model, Resource candidate) {
            return isClassExpression(model, candidate, new HashMap<>());
        }

        /**
         * todo
         * see {@link ru.avicomp.ontapi.jena.model.OntIndividual.Anonymous} description.
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this resource is an Individual
         */
        @Override
        public boolean isIndividual(Model model, Resource candidate) {
            return isIndividual(model, candidate, new HashMap<>());
        }

        /**
         * full description see here {@link #isDataProperty(Model, Resource)}
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is a Data Property (owl:DatatypeProperty)
         */
        private static boolean isDataProperty(Model model, Resource candidate, Map<String, Set<Resource>> seen) {
            Set<Resource> processed = seen.computeIfAbsent(DATA_PROPERTY_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            // 0)
            if (!candidate.isURIResource()) {
                return false;
            }
            // 1) builtin
            if (BuiltIn.DATA_PROPERTIES.contains(candidate)) {
                return true;
            }
            // 2) has an explicit type
            if (objects(model, candidate, RDF.type).anyMatch(OWL.DatatypeProperty::equals)) {
                return true;
            }
            // 3) rdfs:range is D
            if (objects(model, candidate, RDFS.range).anyMatch(r -> isDataRange(model, r, seen))) {
                return true;
            }
            // 4) negative assertion:
            if (subjects(model, OWL.assertionProperty, candidate)
                    .filter(r -> r.hasProperty(RDF.type, OWL.NegativePropertyAssertion))
                    .filter(r -> r.hasProperty(OWL.sourceIndividual))
                    .filter(r -> r.hasProperty(OWL.targetValue))
                    .map(r -> r.getProperty(OWL.targetValue)).map(Statement::getObject).anyMatch(RDFNode::isLiteral)) {
                return true;
            }
            // 5) restrictions:
            if (subjects(model, OWL.onProperty, candidate).anyMatch(StrictRuleEngine::isDataPropertyRestrictionCE)) {
                return true;
            }
            // 6) properties from nary restriction:
            if (naryDataPropertyRestrictions(model).anyMatch(l -> l.contains(candidate))) {
                return true;
            }

            // 7-9) recursive checks:
            // 7) collect objects (resources from the right side):
            Stream<Resource> objects = Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .map(p -> objects(model, candidate, p)).flatMap(Function.identity());
            // 8) collect subjects
            Stream<Resource> subjects = Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .map(p -> subjects(model, p, candidate)).flatMap(Function.identity());
            // 9) collect disjoint properties
            Stream<Resource> disjoint = filterLists(allDisjointProperties(model), candidate);
            Stream<Resource> test = Stream.of(objects, subjects, disjoint).flatMap(Function.identity());
            return test
                    .filter(p -> !processed.contains(p))
                    .anyMatch(p -> isDataProperty(model, p, seen));
        }

        /**
         * description see here {@link #isAnnotationProperty(Model, Resource)}
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is an Annotation Property (owl:AnnotationProperty)
         */
        private static boolean isAnnotationProperty(Model model, Resource candidate, Map<String, Set<Resource>> seen) {
            Set<Resource> processed = seen.computeIfAbsent(ANNOTATION_PROPERTY_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            // 0)
            if (!candidate.isURIResource()) {
                return false;
            }
            // 1) builtin
            if (BuiltIn.ANNOTATION_PROPERTIES.contains(candidate)) {
                return true;
            }
            // 2) an explicit type
            if (model.contains(candidate, RDF.type, OWL.AnnotationProperty) || model.contains(candidate, RDF.type, OWL.OntologyProperty)) {
                return true;
            }
            // 3) annotations assertions inside bulk annotation object:
            Property property = candidate.as(Property.class);
            if (subjects(model, property, null)
                    .filter(RDFNode::isAnon)
                    .filter(r -> r.hasProperty(RDF.type, OWL.Annotation) || r.hasProperty(RDF.type, OWL.Axiom))
                    .filter(r -> r.hasProperty(OWL.annotatedProperty))
                    .filter(r -> r.hasProperty(OWL.annotatedSource))
                    .filter(r -> r.hasProperty(OWL.annotatedTarget)).count() != 0) {
                return true;
            }
            // 4-5) recursion:
            Stream<Resource> objects = objects(model, candidate, RDFS.subPropertyOf);
            Stream<Resource> subjects = subjects(model, RDFS.subPropertyOf, candidate);
            Stream<Resource> test = Stream.concat(objects, subjects);
            return test
                    .filter(p -> !processed.contains(p))
                    .anyMatch(p -> isAnnotationProperty(model, p, seen));
        }

        /**
         * description see here {@link #isObjectProperty(Model, Resource)}
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is an Object Property (owl:ObjectProperty)
         */
        private static boolean isObjectPropertyExpression(Model model, Resource candidate, Map<String, Set<Resource>> seen) {
            Set<Resource> processed = seen.computeIfAbsent(OBJECT_PROPERTY_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            // 1) builtin
            if (BuiltIn.OBJECT_PROPERTIES.contains(candidate)) {
                return true;
            }
            // 2) an explicit type
            if (objects(model, candidate, RDF.type).anyMatch(OWL.ObjectProperty::equals)) {
                return true;
            }
            // 3) always Object Property
            if (Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty, OWL.AsymmetricProperty,
                    OWL.ReflexiveProperty, OWL.IrreflexiveProperty).anyMatch(r -> model.contains(candidate, RDF.type, r))) {
                return true;
            }
            // 4) any part of owl:inverseOf
            if (model.contains(candidate, OWL.inverseOf) || model.contains(null, OWL.inverseOf, candidate)) {
                return true;
            }
            // 5) rdfs:range is C
            if (objects(model, candidate, RDFS.range).anyMatch(r -> isClassExpression(model, r, seen))) {
                return true;
            }
            // 6) positive object property assertion a1 PN a2.
            if (candidate.isURIResource() && statements(model, null, candidate.as(Property.class), null)
                    .filter(s -> isIndividual(model, s.getSubject(), seen))
                    .map(Statement::getObject)
                    .filter(RDFNode::isResource)
                    .map(RDFNode::asResource).anyMatch(r -> isIndividual(model, r, seen))) {
                return true;
            }
            // 7) negative op assertion:
            if (subjects(model, OWL.assertionProperty, candidate)
                    .filter(r -> r.hasProperty(RDF.type, OWL.NegativePropertyAssertion))
                    .filter(r -> r.hasProperty(OWL.sourceIndividual))
                    .filter(r -> r.hasProperty(OWL.targetIndividual))
                    .map(r -> r.getProperty(OWL.targetIndividual)).map(Statement::getObject).anyMatch(RDFNode::isResource)) {
                return true;
            }
            // 8) restrictions:
            if (subjects(model, OWL.onProperty, candidate).anyMatch(StrictRuleEngine::isObjectPropertyRestrictionCE)) {
                return true;
            }
            // 9) part of owl:propertyChainAxiom
            if (model.contains(candidate, OWL.propertyChainAxiom) || containsInList(model, OWL.propertyChainAxiom, candidate)) {
                return true;
            }
            // 10-12) recursions:
            Stream<Resource> objects = Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .map(p -> objects(model, candidate, p)).flatMap(Function.identity());
            Stream<Resource> subjects = Stream.of(RDFS.subPropertyOf, OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .map(p -> subjects(model, p, candidate)).flatMap(Function.identity());
            Stream<Resource> disjoint = filterLists(allDisjointProperties(model), candidate);
            Stream<Resource> test = Stream.of(objects, subjects, disjoint).flatMap(Function.identity());
            return test
                    .filter(p -> !processed.contains(p))
                    .anyMatch(p -> isObjectPropertyExpression(model, p, seen));
        }

        /**
         * see description {@link #isDataRange(Model, Resource)}
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is a DataRange expression Datatype (_:x rdf:type rdfs:Datatype)
         */
        private static boolean isDataRange(Model model, Resource candidate, Map<String, Set<Resource>> seen) {
            Set<Resource> processed = seen.computeIfAbsent(DATA_RANGE_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            // 1) builtin
            if (BuiltIn.DATATYPES.contains(candidate)) {
                return true;
            }
            // 2) type
            if (model.contains(candidate, RDF.type, RDFS.Datatype) || model.contains(candidate, RDF.type, OWL.DataRange)) {
                return true;
            }
            // 3) Restriction with owl:onDataRange
            if (subjects(model, OWL.onDataRange, candidate).anyMatch(s -> s.hasProperty(RDF.type, OWL.Restriction))) {
                return true;
            }
            //4) Datatype restriction)
            if (model.contains(candidate, OWL.onDatatype) || (candidate.isURIResource() && model.contains(null, OWL.onDatatype, candidate))) {
                return true;
            }
            // 5) it is a subject or an object in the statement "_:x owl:datatypeComplementOf D" (data range complement)
            if (model.contains(candidate, OWL.datatypeComplementOf) || model.contains(null, OWL.datatypeComplementOf, candidate)) {
                return true;
            }
            // 6) literal enumeration data range expression:
            if (objects(model, candidate, OWL.oneOf)
                    .filter(r -> r.canAs(RDFList.class))
                    .map(r -> r.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .anyMatch(l -> l.stream().allMatch(RDFNode::isLiteral))) {
                return true;
            }
            // recursions:
            // 7-8) "DN owl:equivalentClass D"
            Stream<Resource> equivalentObjects = candidate.isURIResource() ? objects(model, candidate, OWL.equivalentClass) : Stream.empty();
            Stream<Resource> equivalentSubjects = subjects(model, OWL.equivalentClass, candidate);
            // 9-10) "_:x owl:unionOf (D1...Dn)" and "_:x owl:intersectionOf (D1...Dn)"
            Stream<Resource> expressionObjects = Stream.of(OWL.unionOf, OWL.intersectionOf)
                    .map(p -> objects(model, candidate, p)).flatMap(Function.identity())
                    .filter(r -> r.canAs(RDFList.class)).map(r -> r.as(RDFList.class))
                    .map(RDFList::asJavaList).map(Collection::stream).flatMap(Function.identity())
                    .filter(RDFNode::isResource).map(RDFNode::asResource);
            Stream<Resource> expressionSubjects = Stream.of(OWL.unionOf, OWL.intersectionOf)
                    .map(p -> statements(model, null, p, null)).flatMap(Function.identity())
                    .filter(s -> s.getObject().canAs(RDFList.class))
                    .filter(s -> s.getObject().as(RDFList.class).asJavaList().contains(candidate))
                    .map(Statement::getSubject);

            Stream<Resource> test = Stream.of(equivalentObjects, equivalentSubjects, expressionObjects, expressionSubjects).flatMap(Function.identity());
            return test
                    .filter(p -> !processed.contains(p))
                    .anyMatch(p -> isObjectPropertyExpression(model, p, seen));
        }

        /**
         * see description {@link #isClassExpression(Model, Resource)}
         * <p>
         * f) right of owl:hasKey
         * x) left of owl:onClass
         * f) right and left owl:complementOf
         * x) left or right part of rdfs:subClassOf, owl:disjointWith, owl:AllDisjointClasses, owl:disjointUnionOf
         * d) recursively for owl:equivalentClass
         * owl:oneOf -> individuals
         *
         * @param model
         * @param candidate
         * @param seen
         * @return
         */
        private static boolean isClassExpression(Model model, Resource candidate, Map<String, Set<Resource>> seen) { // todo:
            Set<Resource> processed = seen.computeIfAbsent(CLASS_EXPRESSION_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            if (BuiltIn.CLASSES.contains(candidate)) {
                return true;
            }
            if (Stream.of(OWL.Class, OWL.Restriction).anyMatch(p -> model.contains(candidate, RDF.type, p))) {
                return true;
            }
            // rdfs:subClassOf, owl:disjointWith, owl:AllDisjointClasses, owl:disjointUnionOf

            return false;
        }

        private static boolean isIndividual(Model model, Resource candidate, Map<String, Set<Resource>> seen) { // todo:
            Set<Resource> processed = seen.computeIfAbsent(INDIVIDUAL_KEY, c -> new HashSet<>());
            if (processed.contains(candidate)) return false;
            processed.add(candidate);
            return false;
        }

        private static boolean isRestriction(Resource root) {
            return root.isAnon() && root.hasProperty(RDF.type, OWL.Restriction);
        }

        private static boolean isDataPropertyRestrictionCE(Resource root) {
            if (!isRestriction(root)) return false;
            // todo:
            return false;
        }

        private static boolean isObjectPropertyRestrictionCE(Resource root) {
            if (!isRestriction(root)) return false;
            // todo:
            return false;
        }

        private static boolean containsInList(Model model, Property predicate, Resource member) {
            return statements(model, null, predicate, null)
                    .map(Statement::getObject)
                    .filter(n -> n.canAs(RDFList.class))
                    .map(n -> n.as(RDFList.class))
                    .map(RDFList::asJavaList).anyMatch(l -> l.contains(member));
        }

        private static Stream<Resource> filterLists(Stream<List<RDFNode>> lists, Resource member) {
            return lists
                    .filter(list -> list.contains(member))
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .filter(RDFNode::isResource)
                    .map(RDFNode::asResource);
        }

        private static Stream<List<RDFNode>> allDisjointProperties(Model model) {
            return lists(model, OWL.AllDisjointProperties, OWL.members);
        }

        private static Stream<List<RDFNode>> naryDataPropertyRestrictions(Model model) {
            return lists(model, OWL.Restriction, OWL.onProperties);
        }

        private static Stream<List<RDFNode>> lists(Model model, Resource type, Property predicate) {
            return statements(model, null, predicate, null)
                    .filter(s -> hasType(s.getSubject(), type))
                    .map(Statement::getObject)
                    .filter(n -> n.canAs(RDFList.class))
                    .map(n -> n.as(RDFList.class).asJavaList());
        }

        private static boolean hasType(Resource subject, Resource type) {
            return type == null || subject.hasProperty(RDF.type, type);
        }

        public static Stream<Statement> statements(Model m, Resource s, Property p, RDFNode o) {
            return Streams.asStream(m.listStatements(s, p, o));
        }

        public static Stream<Resource> objects(Model model, Resource subject, Property predicate) {
            return statements(model, subject, predicate, null).map(Statement::getObject).filter(RDFNode::isResource).map(RDFNode::asResource);
        }

        public static Stream<Resource> subjects(Model model, Property predicate, Resource object) {
            return statements(model, null, predicate, object).map(Statement::getSubject).filter(RDFNode::isResource).map(RDFNode::asResource);
        }
    }

}
