package ru.avicomp.ontapi.jena.converters;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * TODO: not ready yet.
 *
 * Helper to test all kind of OWL Objects (OWL Entities, OWL Class&ObjectProperty Expressions, OWL DataRanges, OWL Anonymous Individuals)
 * inside the jena model {@link Model} according to <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Specification</a>.
 * To use inside {@link ru.avicomp.ontapi.jena.converters.TransformAction} mostly.
 * <p>
 * Created by @szuev on 28.01.2017.
 */
public class OntRules {
    private static RuleEngine rules = new DefaultRuleEngine();

    public static RuleEngine getRules() {
        return rules;
    }

    public static void setRules(RuleEngine engine) {
        rules = OntJenaException.notNull(engine, "Null rule engine specified.");
    }

    public static boolean isAnnotationProperty(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testAnnotationProperty(model, candidate));
    }

    public static boolean isDataProperty(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testDataProperty(model, candidate));
    }

    public static boolean isObjectProperty(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testObjectProperty(model, candidate));
    }

    public static boolean isObjectPropertyExpression(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testObjectPropertyExpression(model, candidate));
    }

    public static boolean isDatatype(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testDatatype(model, candidate));
    }

    public static boolean isDataRange(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testDataRange(model, candidate));
    }

    public static boolean isClass(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testClass(model, candidate));
    }

    public static boolean isClassExpression(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testClassExpression(model, candidate));
    }

    public static boolean isNamedIndividual(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testNamedIndividual(model, candidate));
    }

    public static boolean isIndividual(Model model, Resource candidate) {
        return RuleEngine.Result.TRUE.equals(rules.testIndividual(model, candidate));
    }

    public interface RuleEngine {
        Result testAnnotationProperty(Model model, Resource candidate);

        Result testDataProperty(Model model, Resource candidate);

        Result testObjectPropertyExpression(Model model, Resource candidate);

        Result testDataRange(Model model, Resource candidate);

        Result testClassExpression(Model model, Resource candidate);

        Result testIndividual(Model model, Resource candidate);

        default Result testObjectProperty(Model model, Resource candidate) {
            return candidate.isURIResource() ? testObjectPropertyExpression(model, candidate) : Result.FALSE;
        }

        default Result testClass(Model model, Resource candidate) {
            return candidate.isURIResource() ? testClassExpression(model, candidate) : Result.FALSE;
        }

        default Result testDatatype(Model model, Resource candidate) {
            return candidate.isURIResource() ? testDataRange(model, candidate) : Result.FALSE;
        }

        default Result testNamedIndividual(Model model, Resource candidate) {
            return candidate.isURIResource() ? testIndividual(model, candidate) : Result.FALSE;
        }

        enum Result {
            TRUE,
            FALSE,
            UNKNOWN,
        }
    }

    /**
     * TODO: seems it is wrong. need to remove or change
     * The engine that prefers annotation property more than Object and Data properties, since it has more commonality.
     * Ignores parsing of range statement for Object(O) and Data(R) Property in favour of Annotation Property(A)
     * ("P rdfs:range C", "R rdfs:range D" => "A rdfs:range U").
     */
    public static class DefaultRuleEngine extends StrictRuleEngine {
        @Override
        public Result testAnnotationProperty(Model model, Resource candidate) {
            Result res = super.testAnnotationProperty(model, candidate);
            if (!Result.UNKNOWN.equals(res)) return res;
            if (!isProperty(model, candidate)) return Result.FALSE;
            if (Result.TRUE.equals(testDataProperty(model, candidate))) return Result.FALSE;
            if (Result.TRUE.equals(testObjectPropertyExpression(model, candidate))) return Result.FALSE;
            return Result.TRUE;
        }

        @Override
        protected boolean checkDataPropertyRange(Model model, Resource candidate) {
            return false;
        }

        @Override
        protected boolean checkObjectPropertyRange(Model model, Resource candidate) {
            return !candidate.isURIResource();
        }
    }

    /**
     * TODO: seems need to fix behaviour.
     * The 'strict' Rule Engine Implementation.
     * <p>
     * Should work correctly and safe with any ontology (OWL1, RDFS, incomplete OWL2) using the "strict" approach:
     * if some resource has no clear univocal hints about the type in the specified model than the corresponding method should return 'false'.
     * In other words the conditions to test should be necessary and sufficient.
     * This is in order to not produce unnecessary punnings.
     * <p>
     * But please NOTE: the punnings is not taken into account in obvious cases and still allowed by this reasoner.
     * E.g. if the property has rdfs:range with rdf:type = rdfs:Datatype assigned then it is a Data Property,
     * even some other tips show it is Annotation Property also or something else.
     * One more example about 'strict' approach: the statement "_:x rdfs:subPropertyOf _:y" shows that '_:x' is
     * a rdfs:Property and has the same type as '_:y".
     * But it is used for all kind of properties (annotation, data and object).
     * To make a strict derivation we should check that '_:y' should have only one of the above property type.
     * In terms of axioms we have three 'sub-property' axiom statements with the same semantic,
     * and with the "strict" approach we choose only those which do not admit of doubt.
     * By the similar reasons checking for positive property assertion is skipped.
     */
    @SuppressWarnings("WeakerAccess")
    public static class StrictRuleEngine extends BaseRuleEngine {
        @Override
        public Result testAnnotationProperty(Model model, Resource candidate) {
            return testAnnotationProperty(model, candidate, new HashMap<>());
        }

        @Override
        public Result testDataProperty(Model model, Resource candidate) {
            return testDataProperty(model, candidate, new HashMap<>());
        }

        @Override
        public Result testObjectPropertyExpression(Model model, Resource candidate) {
            return testObjectPropertyExpression(model, candidate, new HashMap<>());
        }

        @Override
        public Result testDataRange(Model model, Resource candidate) {
            return testDataRange(model, candidate, new HashMap<>());
        }

        @Override
        public Result testClassExpression(Model model, Resource candidate) {
            return testClassExpression(model, candidate, new HashMap<>());
        }

        @Override
        public Result testIndividual(Model model, Resource candidate) {
            return testIndividual(model, candidate, new HashMap<>());
        }

        protected boolean checkDataPropertyRange(Model model, Resource candidate) {
            return true;
        }

        protected boolean checkObjectPropertyRange(Model model, Resource candidate) {
            return true;
        }

        /**
         * checks that specified uri-resource(rdf:Property) is a Annotation Property (owl:AnnotationProperty).
         * It's marked as 'A' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>.
         * <p>
         * Note: we skip the checking for annotation property assertions ("s A t", where 's' is an anonymous individual or iri ('U'),
         * 't' is an anonymous individual, any iri ('U') or literal ('v')) since it can be confused with data and object property assertion
         * ("a1 P a2" and "a R v", where 'a' is an individual, 'v' is a literal).
         * It seems any object or property assertion can be treated as annotation assertion also,
         * therefore we can not allow this checking within the 'strict' approach.
         * <p>
         * In addition to the {@link super#testAnnotationProperty(Model, Resource)} there are following conditions to test:
         * 5) recursive checking for rdfs:domain: the object should not be class expression to not mess with P or R.
         * 6) recursive checking for rdfs:range: the range should not be C or D (otherwise it may belong to object/data property)
         * 7) recursive checking for rdfs:subPropertyOf: the tested resource is annotation property if it is bonded with some another annotation property.
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is an Annotation Property (owl:AnnotationProperty)
         */
        protected Result testAnnotationProperty(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(OWL.AnnotationProperty, c -> new HashMap<>());
            if (processed.containsKey(candidate)) {
                return processed.get(candidate);
            }
            processed.put(candidate, Result.UNKNOWN);
            Result res = super.testAnnotationProperty(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // 5) "A rdfs:domain U", but not "P rdfs:domain C", "R rdfs:domain C"
            if (objects(model, candidate, RDFS.domain)
                    .filter(RDFNode::isURIResource)
                    .anyMatch(r -> !Result.TRUE.equals(testClassExpression(model, r, seen)))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 6) "A rdfs:range U", but not "P rdfs:range C" or "R rdfs:range D"
            if (objects(model, candidate, RDFS.range)
                    .filter(RDFNode::isURIResource)
                    .anyMatch(r -> isNotRDFSClass(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 7) rdfs:subPropertyOf
            if (testTheSameTypeByPredicate(model, candidate, RDFS.subPropertyOf, p -> isAnnotationPropertyOnly(model, p, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            return Result.UNKNOWN;
        }

        /**
         * checks that specified resource(rdf:Property) is a Data Property (owl:DatatypeProperty).
         * It's marked as 'R' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>.
         * <p>
         * Note: the checking for "R rdfs:domain C" and "a R v" are skipped now, because they could be confused with similar axioms
         * but for an annotation property ("s A t" and "A rdfs:domain U").
         * Here: 'v' is a literal, 'a' is an individual, 's' is IRI or anonymous individual, 'U' is IRI.
         * <p>
         * See base method {@link super#testDataProperty(Model, Resource)}
         * In addition there are following conditions to test (the sequence are important because it depends on other recursive methods):
         * 8) it is a property in the universal and existential data property restrictions (checking for owl:someValuesFrom or owl:allValuesFrom = D).
         * 9) recursively check that object or subject with predicate owl:equivalentProperty or owl:propertyDisjointWith has the same type
         * 10) recursive check the objects from owl:AllDisjointProperties(owl:members) anonymous section.
         * 11) if the right part of statement "R rdfs:range D" is a DataRange then R is definitely Data Property
         * 12) recursively checks that some part of "_:x rdfs:subPropertyOf _:y" is another data property
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is a Data Property (owl:DatatypeProperty)
         */
        protected Result testDataProperty(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(OWL.DatatypeProperty, c -> new HashMap<>());
            if (processed.containsKey(candidate)) {
                return processed.get(candidate);
            }
            processed.put(candidate, Result.UNKNOWN);
            Result res = super.testDataProperty(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // 8) test object with owl:someValuesFrom or owl:allValuesFrom is D:
            if (subjects(model, OWL.onProperty, candidate)
                    .filter(StrictRuleEngine::isRestriction)
                    .map(r -> r.hasProperty(OWL.someValuesFrom) ? objects(model, r, OWL.someValuesFrom) : objects(model, r, OWL.allValuesFrom))
                    .flatMap(Function.identity())
                    .anyMatch(r -> isDataRangeOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 9) test owl:equivalentProperty and owl:propertyDisjointWith
            if (Stream.of(OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .anyMatch(p -> testTheSameTypeByPredicate(model, candidate, p, r -> isDataNotObjectProperty(model, r, seen)))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 10) test disjoint properties
            if (allDisjointPropertiesByMember(model, candidate).anyMatch(r -> isDataNotObjectProperty(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 11) test rdfs:range is D
            if (checkDataPropertyRange(model, candidate) && objects(model, candidate, RDFS.range).anyMatch(r -> isDataRangeOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 12) test rdfs:subPropertyOf
            if (testTheSameTypeByPredicate(model, candidate, RDFS.subPropertyOf, r -> isDataPropertyOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // then it is not R
            return Result.UNKNOWN;
        }

        /**
         * checks that specified resource is an Object Property Expression.
         * The object property is marked 'PN' in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>,
         * while general object property expression holds the designation of 'P'.
         * <p>
         * In addition to the base {@link super#testObjectPropertyExpression(Model, Resource)} there are following conditions to test:
         * 11) it is a property in the universal and existential object property restrictions (checking for owl:someValuesFrom or owl:allValuesFrom = C).
         * 12) test owl:equivalentProperty and owl:propertyDisjointWith
         * 14) recursively checks objects from owl:AllDisjointProperties(owl:members)
         * 14) the range is a class expression: "P rdfs:range C".
         * 15) checking for rdfs:subPropertyOf (if one part of this statement is the object and only object property, then another is also)
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this uri-resource is an Object Property (owl:ObjectProperty)
         */
        protected Result testObjectPropertyExpression(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(OWL.ObjectProperty, c -> new HashMap<>());
            if (processed.containsKey(candidate)) return processed.get(candidate);
            processed.put(candidate, Result.UNKNOWN);
            Result res = super.testObjectPropertyExpression(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // 11) owl:someValuesFrom or owl:allValuesFrom = C.
            if (subjects(model, OWL.onProperty, candidate)
                    .filter(StrictRuleEngine::isRestriction)
                    .map(r -> r.hasProperty(OWL.someValuesFrom) ? objects(model, r, OWL.someValuesFrom) : objects(model, r, OWL.allValuesFrom))
                    .flatMap(Function.identity())
                    .anyMatch(r -> isClassExpressionOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 12) test owl:equivalentProperty and owl:propertyDisjointWith
            if (Stream.of(OWL.equivalentProperty, OWL.propertyDisjointWith)
                    .anyMatch(p -> testTheSameTypeByPredicate(model, candidate, p, r -> isObjectNotDataProperty(model, r, seen)))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 13) test disjoint properties
            if (allDisjointPropertiesByMember(model, candidate).anyMatch(r -> isObjectNotDataProperty(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 14) rdfs:range is C
            if (checkObjectPropertyRange(model, candidate) && objects(model, candidate, RDFS.range).anyMatch(r -> isClassExpressionOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 15) test rdfs:subPropertyOf
            if (testTheSameTypeByPredicate(model, candidate, RDFS.subPropertyOf, r -> isObjectPropertyOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // not an object property
            return Result.UNKNOWN;
        }

        /**
         * checks that specified resource(rdfs:Class) is a Data Range Expression or a Datatype
         * ('D' and 'DN' in in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>).
         * <p>
         * In addition to the base {@link super#testDataRange(Model, Resource)} there are following conditions to test:
         * 9) it is attached on predicate owl:someValuesFrom or owl:allValuesFrom where onProperty is R (recursive checking for R)
         * 10) it is left(uri) or right part of statement "DN owl:equivalentClass D" which contains another data range.
         * 11) recursively checks objects inside lists with predicates owl:intersectionOf and owl:unionOf.
         * 12) it is a range for data property: "R rdfs:range D" (checking that the subject is a data property only)
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true if this resource is a DataRange expression or a Datatype (_:x rdf:type rdfs:Datatype)
         */
        protected Result testDataRange(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(RDFS.Datatype, c -> new HashMap<>());
            if (processed.containsKey(candidate)) return processed.get(candidate);
            processed.put(candidate, Result.UNKNOWN);
            Result res = super.testDataRange(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // 9) universal and existential data property restrictions
            if (multiSubjects(model, candidate, OWL.someValuesFrom, OWL.allValuesFrom)
                    .filter(r -> r.hasProperty(OWL.onProperty))
                    .map(r -> r.getProperty(OWL.onProperty)).map(Statement::getObject)
                    .filter(RDFNode::isResource).map(RDFNode::asResource)
                    .anyMatch(r -> isDataNotObjectProperty(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 10) "DN owl:equivalentClass D"
            Stream<Resource> equivalentObjects = candidate.isURIResource() ? objects(model, candidate, OWL.equivalentClass) : Stream.empty();
            Stream<Resource> equivalentSubjects = subjects(model, OWL.equivalentClass, candidate);
            // 11) "_:x owl:unionOf (D1...Dn)" and "_:x owl:intersectionOf (D1...Dn)"
            Stream<Resource> expressionObjects = objectsFromLists(model, candidate, OWL.unionOf, OWL.intersectionOf);
            Stream<Resource> expressionSubjects = subjectsByListMember(model, candidate, OWL.unionOf, OWL.intersectionOf);
            Stream<Resource> test = Stream.of(equivalentObjects, equivalentSubjects, expressionObjects, expressionSubjects).flatMap(Function.identity());
            if (test.anyMatch(p -> isDataRangeOnly(model, p, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 12) "R rdfs:range D"
            if (subjects(model, RDFS.range, candidate).anyMatch(r -> isDataPropertyOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // not a Data Range
            return Result.UNKNOWN;
        }

        /**
         * checks that specified resource(rdfs:Class) is a Class Expression.
         * ('C' and 'CN' in in the <a href='https://www.w3.org/TR/owl2-quick-reference/'>reminder</a>).
         * <p>
         * Base checking see {@link super#testClassExpression(Model, Resource)}
         * In addition there are following conditions to test on Class Expression:
         * 13) it is attached on predicate owl:someValuesFrom or owl:allValuesFrom where onProperty is P
         * 14) recursively for owl:equivalentClass (the right and left parts should have the same type)
         * 15) recursively for owl:intersectionOf, owl:unionOf (analogically for data ranges)
         * 16) if subject of domain statement is an object or a data property ("P rdfs:domain C" and "R rdfs:domain C")
         * 15) if subject of range statement is an object property ("P rdfs:range C") (checking that P is object property only)
         * 17) class assertion ("a rdf:type C"); it is last, because an individual has declaration only in OWL2 and only if is named,
         * it is easy to confuse it with blank node.
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true it is a Class Expression
         */
        protected Result testClassExpression(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(OWL.Class, c -> new HashMap<>());
            if (processed.containsKey(candidate)) return processed.get(candidate);
            processed.put(candidate, Result.UNKNOWN);
            Result res = super.testClassExpression(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // Recursions:
            // 13) universal and existential object property restrictions
            if (multiSubjects(model, candidate, OWL.someValuesFrom, OWL.allValuesFrom)
                    .filter(r -> r.hasProperty(OWL.onProperty))
                    .map(r -> r.getProperty(OWL.onProperty)).map(Statement::getObject)
                    .filter(RDFNode::isResource).map(RDFNode::asResource)
                    .anyMatch(r -> isObjectNotDataProperty(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 14) "Cj owl:equivalentClass Cj+1"
            Stream<Resource> equivalentObjects = objects(model, candidate, OWL.equivalentClass);
            Stream<Resource> equivalentSubjects = subjects(model, OWL.equivalentClass, candidate);
            // 15) "_:x owl:intersectionOf (C1 ... Cn)" & "_:x owl:unionOf (C1 ... Cn)"
            Stream<Resource> expressionObjects = objectsFromLists(model, candidate, OWL.unionOf, OWL.intersectionOf);
            Stream<Resource> expressionSubjects = subjectsByListMember(model, candidate, OWL.unionOf, OWL.intersectionOf);
            Stream<Resource> test = Stream.of(equivalentObjects, equivalentSubjects, expressionObjects, expressionSubjects).flatMap(Function.identity());
            if (test.anyMatch(p -> isClassExpressionOnly(model, p, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 16) "P rdfs:domain C" or "R rdfs:domain C"
            if (subjects(model, RDFS.domain, candidate)
                    .anyMatch(r -> isDataOrObjectProperty(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 17) "P rdfs:range C"
            if (subjects(model, RDFS.range, candidate)
                    .anyMatch(r -> isObjectPropertyOnly(model, r, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 18) "a rdf:type C"
            if (subjects(model, RDF.type, candidate).map(r -> testIndividual(model, r, seen))
                    .anyMatch(Result.TRUE::equals)) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // not a CE:
            return Result.UNKNOWN;
        }

        /**
         * checks that resource is an anonymous or named individual.
         * see also {@link ru.avicomp.ontapi.jena.model.OntIndividual.Anonymous} description.
         * <p>
         * In addition to the {@link super#testIndividual(Model, Resource)} there are following conditions to test on Individual:
         * 8) class assertion (declaration) "_:a rdf:type C"
         * 9) positive data property assertion "a R v", where R is data and only data property (annotation property could be attached to any entity "s A t")
         * 10) positive object property assertion "a1 PN a2"
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @param seen      the map to avoid recursion
         * @return true it is a named or anonymous individual
         */
        protected Result testIndividual(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            Map<Resource, Result> processed = seen.computeIfAbsent(OWL.NamedIndividual, c -> new HashMap<>());
            if (processed.containsKey(candidate)) return processed.get(candidate);
            processed.put(candidate, Result.FALSE);
            Result res = super.testIndividual(model, candidate);
            if (!Result.UNKNOWN.equals(res)) {
                return processed.compute(candidate, (r, b) -> res);
            }
            // Recursions:
            // 8) class assertion (declaration) "_:a rdf:type C"
            if (objects(model, candidate, RDF.type).map(r -> testClassExpression(model, r, seen)).anyMatch(Result.TRUE::equals)) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 9) positive data property assertion "a R v" , but not annotation assertion "a A v"
            if (statements(model, candidate, null, null)
                    .filter(s -> s.getObject().isLiteral())
                    .map(Statement::getPredicate)
                    .anyMatch(p -> isDataPropertyOnly(model, p, seen))) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // 10) positive object property assertion "a1 PN a2", but not annotation assertion "a1 A a2"
            Stream<Resource> left = statements(model, null, null, candidate)
                    .filter(s -> isObjectPropertyOnly(model, s.getPredicate(), seen))
                    .map(Statement::getSubject);
            Stream<Resource> right = statements(model, candidate, null, null)
                    .filter(s -> s.getObject().isResource())
                    .filter(s -> isObjectPropertyOnly(model, s.getPredicate(), seen))
                    .map(Statement::getObject).map(RDFNode::asResource);
            Stream<Resource> test = Stream.concat(left, right);
            if (test.map(p -> testIndividual(model, p, seen)).anyMatch(Result.TRUE::equals)) {
                return processed.compute(candidate, (r, b) -> Result.TRUE);
            }
            // Not an individual:
            return Result.UNKNOWN;
        }

        protected boolean isNotRDFSClass(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return !Result.TRUE.equals(testClassExpression(model, candidate, seen)) && !Result.TRUE.equals(testDataRange(model, candidate, seen));
        }

        protected boolean isDataRangeOnly(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testDataRange(model, candidate, seen)) &&
                    !Result.TRUE.equals(testClassExpression(model, candidate, seen));
        }

        protected boolean isClassExpressionOnly(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testClassExpression(model, candidate, seen)) &&
                    !Result.TRUE.equals(testDataRange(model, candidate, seen));
        }

        protected boolean isObjectPropertyOnly(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen)) &&
                    !Result.TRUE.equals(testDataProperty(model, candidate, seen)) &&
                    !Result.TRUE.equals(testAnnotationProperty(model, candidate, seen));
        }

        protected boolean isAnnotationPropertyOnly(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testAnnotationProperty(model, candidate, seen)) &&
                    !Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen)) &&
                    !Result.TRUE.equals(testDataProperty(model, candidate, seen));
        }

        protected boolean isDataPropertyOnly(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testDataProperty(model, candidate, seen)) &&
                    !Result.TRUE.equals(testAnnotationProperty(model, candidate, seen)) &&
                    !Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen));
        }

        protected boolean isDataNotObjectProperty(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testDataProperty(model, candidate, seen)) &&
                    !Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen));
        }

        protected boolean isObjectNotDataProperty(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen)) &&
                    !Result.TRUE.equals(testDataProperty(model, candidate, seen));
        }

        protected boolean isDataOrObjectProperty(Model model, Resource candidate, Map<Resource, Map<Resource, Result>> seen) {
            return Result.TRUE.equals(testDataProperty(model, candidate, seen)) ||
                    Result.TRUE.equals(testObjectPropertyExpression(model, candidate, seen));
        }

        protected boolean testTheSameTypeByPredicate(Model model, Resource candidate, Property predicate, Predicate<Resource> tester) {
            Stream<Resource> objects = objects(model, candidate, predicate);
            Stream<Resource> subjects = subjects(model, predicate, candidate);
            return Stream.concat(objects, subjects).anyMatch(tester);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class BaseRuleEngine implements RuleEngine {

        /**
         * There are following conditions to test:
         * 0) it is an uri-resource
         * 1) it's in builtin list (rdfs:label, owl:deprecated ... etc)
         * 2) it's not from reserved vocabulary
         * 3) it has the an explicit type owl:AnnotationProperty (or owl:OntologyProperty for annotations in owl:Ontology section, deprecated)
         * 4) any non-builtin predicate from anon section with type owl:Axiom, owl:Annotation (bulk annotations)
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @return true if this uri-resource is an Annotation Property (owl:AnnotationProperty)
         */
        @Override
        public Result testAnnotationProperty(Model model, Resource candidate) {
            // 0) annotation property is always IRI
            if (!candidate.isURIResource()) {
                return Result.FALSE;
            }
            // 1) builtin
            if (BuiltIn.ANNOTATION_PROPERTIES.contains(candidate)) {
                return Result.TRUE;
            }
            // 2) not reserved:
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 3) an explicit type
            if (model.contains(candidate, RDF.type, OWL.AnnotationProperty) || model.contains(candidate, RDF.type, OWL.OntologyProperty)) {
                return Result.TRUE;
            }
            // 4) annotations assertion inside bulk annotation object:
            Property property = candidate.inModel(model).as(Property.class);
            if (subjects(model, property)
                    .filter(RDFNode::isAnon)
                    .filter(r -> r.hasProperty(RDF.type, OWL.Annotation) || r.hasProperty(RDF.type, OWL.Axiom))
                    .filter(r -> r.hasProperty(OWL.annotatedProperty))
                    .filter(r -> r.hasProperty(OWL.annotatedSource))
                    .anyMatch(r -> r.hasProperty(OWL.annotatedTarget))) {
                return Result.TRUE;
            }
            // no A
            return Result.UNKNOWN;
        }

        /**
         * There are following conditions to test:
         * 0) it is an uri-resource
         * 1) it's in builtin list (owl:topDataProperty, owl:bottomDataProperty).
         * 2) it is from not reserved vocabulary
         * 3) it has the an explicit type owl:DatatypeProperty
         * 4) it contains in negative data property assertion (owl:NegativePropertyAssertion, checks for owl:targetValue).
         * 5) it is in the list of properties (predicate owl:onProperties) from N-ary Data Range Restriction.
         * 6) it is attached on predicate owl:onProperty to some qualified cardinality restriction (checking for owl:onDataRange predicate)
         * 7) it is part of owl:Restriction with literal on predicate owl:hasValue.
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @return true if this uri-resource is a Data Property (owl:DatatypeProperty)
         */
        @Override
        public Result testDataProperty(Model model, Resource candidate) {
            // 0) data property expression is always named
            if (!candidate.isURIResource()) {
                return Result.FALSE;
            }
            // 1) builtin
            if (BuiltIn.DATA_PROPERTIES.contains(candidate)) {
                return Result.TRUE;
            }
            // 2) not a reserved uri
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 3) has an explicit type
            if (objects(model, candidate, RDF.type).anyMatch(OWL.DatatypeProperty::equals)) {
                return Result.TRUE;
            }
            // 4) negative assertion:
            if (subjects(model, OWL.assertionProperty, candidate)
                    .filter(r -> r.hasProperty(RDF.type, OWL.NegativePropertyAssertion))
                    .filter(r -> r.hasProperty(OWL.sourceIndividual))
                    .filter(r -> r.hasProperty(OWL.targetValue))
                    .map(r -> r.getProperty(OWL.targetValue)).map(Statement::getObject).anyMatch(RDFNode::isLiteral)) {
                return Result.TRUE;
            }
            // 5) properties from nary restriction:
            if (Models.isInList(model, candidate) && naryDataPropertyRestrictions(model).anyMatch(l -> l.contains(candidate))) {
                return Result.TRUE;
            }
            // 6) qualified maximum/minimum/exact cardinality data property restrictions
            // 7) literal value data property restriction
            if (subjects(model, OWL.onProperty, candidate)
                    .filter(StrictRuleEngine::isRestriction)
                    .anyMatch(r -> r.hasProperty(OWL.onDataRange) ||
                            (r.hasProperty(OWL.hasValue) && r.getProperty(OWL.hasValue).getObject().isLiteral()))) {
                return Result.TRUE;
            }
            // can't determine type
            return Result.UNKNOWN;
        }

        /**
         * There are following conditions to test:
         * 1) it's from builtin list (owl:topObjectProperty, owl:bottomObjectProperty).
         * 2) it is not from reserved vocabulary
         * 3) it has the an explicit type owl:ObjectProperty
         * 4) it has one of the following types:
         * - owl:InverseFunctionalProperty,
         * - owl:ReflexiveProperty,
         * - owl:IrreflexiveProperty,
         * - owl:SymmetricProperty,
         * - owl:AsymmetricProperty,
         * - owl:TransitiveProperty
         * 5) it is left or right part of "P1 owl:inverseOf P2"
         * 6) part of "P owl:propertyChainAxiom (P1 ... Pn)"
         * 7) attached to a negative object property assertion (owl:NegativePropertyAssertion, the check for owl:targetIndividual).
         * 8) it is a subject in "_:x owl:hasSelf "true"^^xsd:boolean" (local reflexivity object property restriction)
         * 9) it is attached on predicate owl:onProperty to some qualified cardinality object property restriction (checking for owl:onClass predicate)
         * 10) it is part of owl:Restriction with non-literal on predicate owl:hasValue (it has to be an individual).
         *
         * @param model     {@link Model}
         * @param candidate the {@link Resource} to test.
         * @return true if this uri-resource is an Object Property (owl:ObjectProperty)
         */
        @Override
        public Result testObjectPropertyExpression(Model model, Resource candidate) {
            // 1) builtin
            if (BuiltIn.OBJECT_PROPERTIES.contains(candidate)) {
                return Result.TRUE;
            }
            // 2) not builtin
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 3) an explicit type
            if (objects(model, candidate, RDF.type).anyMatch(OWL.ObjectProperty::equals)) {
                return Result.TRUE;
            }
            // 4) always Object Property
            if (Stream.of(OWL.InverseFunctionalProperty, OWL.TransitiveProperty, OWL.SymmetricProperty, OWL.AsymmetricProperty,
                    OWL.ReflexiveProperty, OWL.IrreflexiveProperty).anyMatch(r -> model.contains(candidate, RDF.type, r))) {
                return Result.TRUE;
            }
            // 5) any part of owl:inverseOf
            if (model.contains(candidate, OWL.inverseOf) || model.contains(null, OWL.inverseOf, candidate)) {
                return Result.TRUE;
            }
            // 6) part of owl:propertyChainAxiom
            if (model.contains(candidate, OWL.propertyChainAxiom) || containsInList(model, OWL.propertyChainAxiom, candidate)) {
                return Result.TRUE;
            }
            // 7) negative object property assertion:
            if (subjects(model, OWL.assertionProperty, candidate)
                    .filter(r -> r.hasProperty(RDF.type, OWL.NegativePropertyAssertion))
                    .filter(r -> r.hasProperty(OWL.sourceIndividual))
                    .filter(r -> r.hasProperty(OWL.targetIndividual))
                    .map(r -> r.getProperty(OWL.targetIndividual)).map(Statement::getObject).anyMatch(RDFNode::isResource)) {
                return Result.TRUE;
            }
            // 8) it is a subject in "_:x owl:hasSelf "true"^^xsd:boolean" (local reflexivity object property restriction)
            // 9) it is attached on predicate owl:onProperty to some qualified cardinality object property restriction (checking for owl:onClass predicate)
            // 10) it is part of owl:Restriction with non-literal on predicate owl:hasValue (it has to be an individual).
            if (subjects(model, OWL.onProperty, candidate)
                    .filter(StrictRuleEngine::isRestriction)
                    .anyMatch(r -> r.hasProperty(OWL.onClass) ||
                            r.hasProperty(OWL.hasSelf, Models.TRUE) ||
                            (r.hasProperty(OWL.hasValue) && r.getProperty(OWL.hasValue).getObject().isResource()))) {
                return Result.TRUE;
            }
            // unknown type
            return Result.UNKNOWN;
        }

        /**
         * There are following conditions to test:
         * 1) it's from builtin list (rdfs:Literal, xsd:string, owl:rational ... etc).
         * 2) not reserved uri
         * 3) it has the an explicit type rdfs:Datatype or owl:DataRange(deprecated in OWL 2)
         * 4) it is a subject or an object in the statement "_:x owl:datatypeComplementOf D" (data range complement)
         * 5) it is a subject or an uri-object in the statement "_:x owl:onDatatype DN; _:x owl:withRestrictions (...)" (datatype restriction)
         * 6) it is a subject in the statement which contains a list of literals as an object "_:x owl:oneOf (v1 ... vn)"
         * 7) it is the right part of owl:onDataRange inside "_:x rdf:type owl:Restriction" (data property qualified cardinality restriction)
         * 8) it is an object with predicate owl:someValuesFrom or owl:allValuesFrom inside N-ary Data Property Restrictions
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true if this resource is a DataRange expression or a Datatype (_:x rdf:type rdfs:Datatype)
         */
        @Override
        public Result testDataRange(Model model, Resource candidate) {
            // 1) is builtin
            if (BuiltIn.DATATYPES.contains(candidate)) {
                return Result.TRUE;
            }
            // 2) is not builtin
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 3) explicit type
            if (model.contains(candidate, RDF.type, RDFS.Datatype) || model.contains(candidate, RDF.type, OWL.DataRange)) {
                return Result.TRUE;
            }
            // 4) data range complement expression
            if (model.contains(candidate, OWL.datatypeComplementOf) || model.contains(null, OWL.datatypeComplementOf, candidate)) {
                return Result.TRUE;
            }
            // 5) Datatype restriction
            if (subjects(model, OWL.onDatatype, candidate)
                    .map(r -> objects(model, r, OWL.withRestrictions))
                    .flatMap(Function.identity())
                    .anyMatch(r -> r.canAs(RDFList.class))) {
                return Result.TRUE;
            }
            if (model.contains(candidate, OWL.onDatatype) && objects(model, candidate, OWL.withRestrictions).anyMatch(r -> r.canAs(RDFList.class))) {
                return Result.TRUE;
            }
            // 6) literal enumeration data range expression:
            if (objects(model, candidate, OWL.oneOf)
                    .filter(r -> r.canAs(RDFList.class))
                    .map(r -> r.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .anyMatch(l -> !l.isEmpty() && l.stream().allMatch(RDFNode::isLiteral))) {
                return Result.TRUE;
            }
            // 7) data property qualified cardinality restriction
            if (subjects(model, OWL.onDataRange, candidate).anyMatch(StrictRuleEngine::isRestriction)) {
                return Result.TRUE;
            }
            // 8) n-ary data property restrictions
            if (multiSubjects(model, candidate, OWL.someValuesFrom, OWL.allValuesFrom)
                    .map(r -> objects(model, r, OWL.onProperties)).flatMap(Function.identity())
                    .anyMatch(r -> r.canAs(RDFList.class))) {
                return Result.TRUE;
            }
            // unknown type
            return Result.UNKNOWN;
        }


        /**
         * There are following conditions to test on Class Expression:
         * 1) it's from builtin list (owl:Nothing, owl:Thing).
         * 2) not a reserved uri
         * 3) it has the an explicit type owl:Class or owl:Restriction(for anonymous CE)
         * 4) it is the right part of statement with predicate owl:hasKey ("C owl:hasKey (P1 ... Pm R1 ... Rn)")
         * 5) left or right part of "C1 rdfs:subClassOf C2"
         * 6) left or right part of "C1 owl:disjointWith C2"
         * 7) a subject (anonymous in OWL2) or an object in a statement with predicate owl:complementOf (Object complement of CE)
         * 8) left or right part of "CN owl:disjointUnionOf (C1 ... Cn)"
         * 9) an object in a statement with predicate owl:onClass (cardinality restriction)
         * 10) member in the list with predicate owl:members and rdf:type = owl:AllDisjointClasses
         * 11) it is a subject in the statement which contains a list of individuals as an object "_:x owl:oneOf (a1 ... an)"
         * 12) it could be owl:Restriction but with missed rdf:type
         * - local reflexivity object property restriction
         * - universal/existential data/object property restriction
         * - individual/literal value object/data property restriction
         * - maximum/minimum/exact cardinality object/data property restriction
         * - maximum/minimum/exact qualified cardinality object/data property restriction
         * - n-ary universal/existential data property restriction
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true it is a Class Expression
         */
        @Override
        public Result testClassExpression(Model model, Resource candidate) {
            // 1)
            if (BuiltIn.CLASSES.contains(candidate)) {
                return Result.TRUE;
            }
            // 2)
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 3) type
            if (model.contains(candidate, RDF.type, OWL.Class) || isRestriction(candidate.inModel(model))) {
                return Result.TRUE;
            }
            // 4) "C owl:hasKey (P1 ... Pm R1 ... Rn)"
            if (objects(model, candidate, OWL.hasKey).anyMatch(r -> r.canAs(RDFList.class))) {
                return Result.TRUE;
            }
            // 5-7) "C1 rdfs:subClassOf C2", "C1 owl:disjointWith C2", "_:x rdf:type owl:Class; _:x owl:complementOf C"
            if (Stream.of(RDFS.subClassOf, OWL.disjointWith, OWL.complementOf)
                    .map(p -> Stream.concat(subjects(model, p, candidate), objects(model, candidate, p)))
                    .flatMap(Function.identity())
                    .anyMatch(r -> true)) {
                return Result.TRUE;
            }
            // 8) "CN owl:disjointUnionOf (C1 ... Cn)"
            if ((candidate.isURIResource() && model.contains(candidate, OWL.disjointUnionOf)) || containsInList(model, OWL.disjointUnionOf, candidate)) {
                return Result.TRUE;
            }
            // 9) object property qualified cardinality restriction
            if (subjects(model, OWL.onClass, candidate).anyMatch(StrictRuleEngine::isRestriction)) {
                return Result.TRUE;
            }
            // 10) member in the list with predicate owl:members and rdf:type = owl:AllDisjointClasses
            if (Models.isInList(model, candidate) && allDisjointClasses(model).anyMatch(list -> list.contains(candidate))) {
                return Result.TRUE;
            }
            // 11) individuals enumeration:
            if (objects(model, candidate, OWL.oneOf)
                    .filter(r -> r.canAs(RDFList.class))
                    .map(r -> r.as(RDFList.class))
                    .map(RDFList::asJavaList)
                    .anyMatch(l -> !l.isEmpty() && l.stream().allMatch(RDFNode::isResource))) {
                return Result.TRUE;
            }
            // 12) test parts of owl:Restriction.
            if (objects(model, candidate, OWL.onProperty).anyMatch(RDFNode::isResource)) {
                if (model.contains(candidate, OWL.hasSelf, Models.TRUE)) {
                    return Result.TRUE;
                }
                if (multiObjects(model, candidate, OWL.someValuesFrom, OWL.allValuesFrom)
                        .anyMatch(RDFNode::isResource)) {
                    return Result.TRUE;
                }
                if (model.contains(candidate, OWL.hasValue)) {
                    return Result.TRUE;
                }
                if (Stream.of(OWL.cardinality, OWL.maxCardinality, OWL.minCardinality)
                        .map(p -> objects(model, candidate, p)).flatMap(Function.identity())
                        .anyMatch(RDFNode::isLiteral)) {
                    return Result.TRUE;
                }
                if (Stream.of(OWL.qualifiedCardinality, OWL.maxQualifiedCardinality, OWL.minQualifiedCardinality)
                        .map(p -> objects(model, candidate, p)).flatMap(Function.identity()).anyMatch(RDFNode::isLiteral) &&
                        Stream.of(OWL.onClass, OWL.onDataRange)
                                .map(p -> objects(model, candidate, p)).flatMap(Function.identity()).anyMatch(r -> true)) {
                    return Result.TRUE;
                }
            }
            if (objects(model, candidate, OWL.onProperties).anyMatch(r -> r.canAs(RDFList.class)) &&
                    multiObjects(model, candidate, OWL.someValuesFrom, OWL.allValuesFrom).anyMatch(RDFNode::isResource)) {
                return Result.TRUE;
            }
            // unknown type
            return Result.UNKNOWN;
        }

        /**
         * There are following conditions to test on Individual:
         * 1) it is not a reserved uri
         * 2) it is an uri-resource and has an explicit type owl:NamedIndividual
         * 3) it is a subject or an object in a statement with predicates owl:sameAs or owl:differentFrom
         * 4) it is contained in a owl:AllDifferent (rdf:List with predicate owl:distinctMembers or owl:members)
         * 5) it is contained in a rdf:List with predicate owl:oneOf
         * 6) it is a part of owl:NegativePropertyAssertion section with predicates owl:sourceIndividual or owl:targetIndividual
         * 7) object property restriction "_:x owl:hasValue a."
         *
         * @param model     {@link Model}
         * @param candidate {@link Resource} to test.
         * @return true it is a named or anonymous individual
         */
        @Override
        public Result testIndividual(Model model, Resource candidate) {
            // 1)
            if (isReserved(candidate)) {
                return Result.FALSE;
            }
            // 2) declaration:
            if (candidate.isURIResource() && model.contains(candidate, RDF.type, OWL.NamedIndividual)) {
                return Result.TRUE;
            }
            // 3) "aj owl:sameAs aj+1", "a1 owl:differentFrom a2"
            if (Stream.of(OWL.sameAs, OWL.differentFrom).anyMatch(p -> model.contains(candidate, p) || model.contains(null, p, candidate))) {
                return Result.TRUE;
            }
            // 4) owl:AllDifferent
            if (Models.isInList(model, candidate) && allDifferent(model).anyMatch(list -> list.contains(candidate))) {
                return Result.TRUE;
            }
            // 5) owl:oneOf
            if (containsInList(model, OWL.oneOf, candidate)) {
                return Result.TRUE;
            }
            // 6) owl:NegativePropertyAssertion
            if (multiSubjects(model, candidate, OWL.sourceIndividual, OWL.targetIndividual)
                    .filter(r -> r.hasProperty(OWL.assertionProperty))
                    .anyMatch(r -> r.hasProperty(RDF.type, OWL.NegativePropertyAssertion))) {
                return Result.TRUE;
            }
            // 7) object property restriction "_:x owl:hasValue a."
            if (subjects(model, OWL.hasValue, candidate)
                    .filter(StrictRuleEngine::isRestriction)
                    .anyMatch(r -> r.hasProperty(OWL.onProperty))) {
                return Result.TRUE;
            }
            // unknown type
            return Result.UNKNOWN;
        }

        protected boolean isReserved(Resource candidate) {
            return BuiltIn.ALL.contains(candidate);
        }

        public static Stream<Statement> statements(Model m, Resource s, Property p, RDFNode o) {
            return Iter.asStream(m.listStatements(s, p, o));
        }

        public static Stream<Resource> objects(Model model, Resource subject, Property predicate) {
            return statements(model, subject, predicate, null).map(Statement::getObject).filter(RDFNode::isResource).map(RDFNode::asResource);
        }

        public static Stream<Resource> subjects(Model model, Property predicate, Resource object) {
            return statements(model, null, predicate, object).map(Statement::getSubject).filter(RDFNode::isResource).map(RDFNode::asResource);
        }

        public static Stream<Resource> subjects(Model model, Property predicate) {
            return subjects(model, predicate, null);
        }

        public static Stream<Resource> multiSubjects(Model model, Resource object, Property... predicates) {
            return Stream.of(predicates).map(p -> subjects(model, p, object)).flatMap(Function.identity());
        }

        public static Stream<Resource> multiObjects(Model model, Resource subject, Property... predicates) {
            return Stream.of(predicates).map(p -> objects(model, subject, p)).flatMap(Function.identity());
        }

        public static boolean isRestriction(Resource root) {
            return root.isAnon() && root.hasProperty(RDF.type, OWL.Restriction);
        }

        public static Stream<Resource> objectsFromLists(Model model, Resource subject, Property... predicates) {
            return Stream.of(predicates).map(p -> objects(model, subject, p)).flatMap(Function.identity())
                    .filter(r -> r.canAs(RDFList.class)).map(r -> r.as(RDFList.class))
                    .map(RDFList::asJavaList).map(Collection::stream).flatMap(Function.identity())
                    .filter(RDFNode::isResource).map(RDFNode::asResource);
        }

        public static Stream<Resource> subjectsByListMember(Model model, Resource member, Property... predicates) {
            return Models.isInList(model, member) ?
                    Stream.of(predicates).map(p -> statements(model, null, p, null)).flatMap(Function.identity())
                            .filter(s -> s.getObject().canAs(RDFList.class))
                            .filter(s -> s.getObject().as(RDFList.class).asJavaList().contains(member))
                            .map(Statement::getSubject) : Stream.empty();
        }

        public static boolean containsInList(Model model, Property predicate, Resource member) {
            return Models.isInList(model, member) && statements(model, null, predicate, null)
                    .map(Statement::getObject)
                    .filter(n -> n.canAs(RDFList.class))
                    .map(n -> n.as(RDFList.class))
                    .map(RDFList::asJavaList).anyMatch(l -> l.contains(member));
        }

        public static Stream<Resource> allDisjointPropertiesByMember(Model model, Resource member) {
            return Models.isInList(model, member) ?
                    allDisjointProperties(model)
                            .filter(list -> list.contains(member))
                            .map(Collection::stream)
                            .flatMap(Function.identity())
                            .filter(RDFNode::isResource)
                            .map(RDFNode::asResource)
                    : Stream.empty();
        }

        public static Stream<List<RDFNode>> allDisjointProperties(Model model) {
            return lists(model, OWL.AllDisjointProperties, OWL.members);
        }

        public static Stream<List<RDFNode>> allDisjointClasses(Model model) {
            return lists(model, OWL.AllDisjointClasses, OWL.members);
        }

        public static Stream<List<RDFNode>> allDifferent(Model model) {
            return Stream.concat(lists(model, OWL.AllDifferent, OWL.members), lists(model, OWL.AllDifferent, OWL.distinctMembers));
        }

        public static Stream<List<RDFNode>> naryDataPropertyRestrictions(Model model) {
            return lists(model, OWL.Restriction, OWL.onProperties);
        }

        private static Stream<List<RDFNode>> lists(Model model, Resource type, Property predicate) {
            return statements(model, null, predicate, null)
                    .filter(s -> type == null || s.getSubject().hasProperty(RDF.type, type))
                    .map(Statement::getObject)
                    .filter(n -> n.canAs(RDFList.class))
                    .map(n -> n.as(RDFList.class).asJavaList());
        }

        public static boolean isProperty(Model model, Resource candidate) {
            return candidate.isURIResource()
                    && (model.contains(candidate, RDF.type, RDF.Property)
                    || model.contains(candidate, RDFS.subPropertyOf)
                    || model.contains(candidate, RDFS.range)
                    || model.contains(candidate, RDFS.domain));
        }

    }

}
