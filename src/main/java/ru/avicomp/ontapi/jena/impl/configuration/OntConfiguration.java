package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.*;

import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.jena.impl.OntEntityImpl;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.impl.OntPEImpl;
import ru.avicomp.ontapi.jena.model.*;

/**
 * TODO: make "factory" with built-in OntObject's, implement interface.
 * personalities here
 * <p>
 * Created by @szuev on 04.11.2016.
 */
public class OntConfiguration {

    private static final Personality<RDFNode> PERSONALITY = new Personality<RDFNode>()
            // standard resources:
            .add(Resource.class, ResourceImpl.factory)
            .add(Property.class, PropertyImpl.factory)
            .add(Literal.class, LiteralImpl.factory)
            .add(Container.class, ResourceImpl.factory)
            .add(Alt.class, AltImpl.factory)
            .add(Bag.class, BagImpl.factory)
            .add(Seq.class, SeqImpl.factory)
            .add(ReifiedStatement.class, ReifiedStatementImpl.reifiedStatementFactory)
            .add(RDFList.class, RDFListImpl.factory)
            .add(RDFNode.class, ResourceImpl.rdfNodeFactory)

            // entities:
            .add(OntObject.class, OntObjectImpl.objectFactory)
            .add(OntClassEntity.class, OntEntityImpl.classFactory)
            .add(OntAPEntity.class, OntEntityImpl.annotationPropertyFactory)
            .add(OntDPEntity.class, OntEntityImpl.dataPropertyFactory)
            .add(OntOPEntity.class, OntEntityImpl.objectPropertyFactory)
            .add(OntDatatypeEntity.class, OntEntityImpl.datatypeFactory)
            .add(OntIndividualEntity.class, OntEntityImpl.individualFactory)
            .add(OntEntity.class, OntEntityImpl.abstractEntityFactory)

            // class expressions:
            .add(OntCE.ObjectSomeValuesFrom.class, OntCEImpl.objectSomeValuesOfCEFactory)
            .add(OntCE.DataSomeValuesFrom.class, OntCEImpl.dataSomeValuesOfCEFactory)
            .add(OntCE.ObjectAllValuesFrom.class, OntCEImpl.objectAllValuesOfCEFactory)
            .add(OntCE.DataAllValuesFrom.class, OntCEImpl.dataAllValuesOfCEFactory)
            .add(OntCE.ObjectHasValue.class, OntCEImpl.objectHasValueCEFactory)
            .add(OntCE.DataHasValue.class, OntCEImpl.dataHasValueCEFactory)
            .add(OntCE.ObjectMinCardinality.class, OntCEImpl.objectMinCardinalityCEFactory)
            .add(OntCE.DataMinCardinality.class, OntCEImpl.dataMinCardinalityCEFactory)
            .add(OntCE.ObjectMaxCardinality.class, OntCEImpl.objectMaxCardinalityCEFactory)
            .add(OntCE.DataMaxCardinality.class, OntCEImpl.dataMaxCardinalityCEFactory)
            .add(OntCE.ObjectCardinality.class, OntCEImpl.objectCardinalityCEFactory)
            .add(OntCE.DataCardinality.class, OntCEImpl.dataCardinalityCEFactory)
            .add(OntCE.HasSelf.class, OntCEImpl.hasSelfCEFactory)
            .add(OntCE.UnionOf.class, OntCEImpl.unionOfCEFactory)
            .add(OntCE.OneOf.class, OntCEImpl.oneOfCEFactory)
            .add(OntCE.IntersectionOf.class, OntCEImpl.intersectionOfCEFactory)
            .add(OntCE.ComplementOf.class, OntCEImpl.complementOfCEFactory)
            .add(OntCE.class, OntCEImpl.abstractCEFactory)
            .add(OntCE.ComponentsCE.class, OntCEImpl.abstractComponentsCEFactory)
            .add(OntCE.CardinalityRestrictionCE.class, OntCEImpl.abstractCardinalityRestrictionCEFactory)
            .add(OntCE.ComponentRestrictionCE.class, OntCEImpl.abstractComponentRestrictionCEFactory)
            .add(OntCE.RestrictionCE.class, OntCEImpl.abstractRestrictionCEFactory) //todo: add nary CEs

            // property expressions:
            .add(OntOPE.Inverse.class, OntPEImpl.inversePropertyFactory)
            .add(OntOPE.class, OntPEImpl.abstractOPEFactory)
            .add(OntPE.class, OntPEImpl.abstractPEFactory)
            ;

    // todo: replace with out resources. ontology additions
/*            .add(OntResource.class, OntResourceImpl.factory)
            .add(Ontology.class, OntologyImpl.factory)
            .add(OntClass.class, org.apache.jena.ontology.impl.OntClassImpl.factory)
            .add(EnumeratedClass.class, EnumeratedClassImpl.factory)
            .add(IntersectionClass.class, IntersectionClassImpl.factory)
            .add(UnionClass.class, UnionClassImpl.factory)
            .add(ComplementClass.class, ComplementClassImpl.factory)
            .add(DataRange.class, DataRangeImpl.factory)

            .add(Restriction.class, RestrictionImpl.factory)
            .add(HasValueRestriction.class, HasValueRestrictionImpl.factory)
            .add(AllValuesFromRestriction.class, AllValuesFromRestrictionImpl.factory)
            .add(SomeValuesFromRestriction.class, SomeValuesFromRestrictionImpl.factory)
            .add(CardinalityRestriction.class, CardinalityRestrictionImpl.factory)
            .add(MinCardinalityRestriction.class, MinCardinalityRestrictionImpl.factory)
            .add(MaxCardinalityRestriction.class, MaxCardinalityRestrictionImpl.factory)
            .add(QualifiedRestriction.class, QualifiedRestrictionImpl.factory)
            .add(MinCardinalityQRestriction.class, MinCardinalityQRestrictionImpl.factory)
            .add(MaxCardinalityQRestriction.class, MaxCardinalityQRestrictionImpl.factory)
            .add(CardinalityQRestriction.class, CardinalityQRestrictionImpl.factory)

            .add(OntProperty.class, OntPropertyImpl.factory)
            .add(ObjectProperty.class, ObjectPropertyImpl.factory)
            .add(DatatypeProperty.class, DatatypePropertyImpl.factory)
            .add(TransitiveProperty.class, TransitivePropertyImpl.factory)
            .add(SymmetricProperty.class, SymmetricPropertyImpl.factory)
            .add(FunctionalProperty.class, FunctionalPropertyImpl.factory)
            .add(InverseFunctionalProperty.class, InverseFunctionalPropertyImpl.factory)
            .add(AllDifferent.class, AllDifferentImpl.factory)
            .add(Individual.class, IndividualImpl.factory)
            .add(AnnotationProperty.class, AnnotationPropertyImpl.factory);*/

    /**
     * register new OntObject if needed
     *
     * @param view    Interface
     * @param factory Factory to crete object
     */
    public static void register(Class<? extends OntObject> view, OntObjectFactory factory) {
        PERSONALITY.add(view, factory);
    }

    public static Personality<RDFNode> getPersonality() {
        return PERSONALITY;
    }

}
