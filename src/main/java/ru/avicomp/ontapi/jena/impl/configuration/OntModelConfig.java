package ru.avicomp.ontapi.jena.impl.configuration;

import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.*;

import ru.avicomp.ontapi.jena.impl.*;
import ru.avicomp.ontapi.jena.model.*;

/**
 * personalities here.
 * <p>
 * Created by @szuev on 04.11.2016.
 */
public class OntModelConfig {

    // standard resources:
    public static final Personality<RDFNode> STANDARD_PERSONALITY = new Personality<RDFNode>()
            .add(Resource.class, ResourceImpl.factory)
            .add(Property.class, PropertyImpl.factory)
            .add(Literal.class, LiteralImpl.factory)
            .add(Container.class, ResourceImpl.factory)
            .add(Alt.class, AltImpl.factory)
            .add(Bag.class, BagImpl.factory)
            .add(Seq.class, SeqImpl.factory)
            .add(ReifiedStatement.class, ReifiedStatementImpl.reifiedStatementFactory)
            .add(RDFList.class, RDFListImpl.factory)
            .add(RDFNode.class, ResourceImpl.rdfNodeFactory);

    // ont-resources:
    public static final OntPersonality ONT_PERSONALITY = new OntPersonality(STANDARD_PERSONALITY)
            // ont-id
            .register(OntID.class, OntIDImpl.idFactory)

            // entities:
            .register(OntObject.class, OntObjectImpl.objectFactory)
            .register(OntClass.class, OntEntityImpl.classFactory)
            .register(OntNAP.class, OntEntityImpl.annotationPropertyFactory)
            .register(OntNDP.class, OntEntityImpl.dataPropertyFactory)
            .register(OntNOP.class, OntEntityImpl.objectPropertyFactory)
            .register(OntDT.class, OntEntityImpl.datatypeFactory)
            .register(OntIndividual.Named.class, OntEntityImpl.individualFactory)
            .register(OntEntity.class, OntEntityImpl.abstractEntityFactory)

            // class expressions:
            .register(OntCE.ObjectSomeValuesFrom.class, OntCEImpl.objectSomeValuesOfCEFactory)
            .register(OntCE.DataSomeValuesFrom.class, OntCEImpl.dataSomeValuesOfCEFactory)
            .register(OntCE.ObjectAllValuesFrom.class, OntCEImpl.objectAllValuesOfCEFactory)
            .register(OntCE.DataAllValuesFrom.class, OntCEImpl.dataAllValuesOfCEFactory)
            .register(OntCE.ObjectHasValue.class, OntCEImpl.objectHasValueCEFactory)
            .register(OntCE.DataHasValue.class, OntCEImpl.dataHasValueCEFactory)
            .register(OntCE.ObjectMinCardinality.class, OntCEImpl.objectMinCardinalityCEFactory)
            .register(OntCE.DataMinCardinality.class, OntCEImpl.dataMinCardinalityCEFactory)
            .register(OntCE.ObjectMaxCardinality.class, OntCEImpl.objectMaxCardinalityCEFactory)
            .register(OntCE.DataMaxCardinality.class, OntCEImpl.dataMaxCardinalityCEFactory)
            .register(OntCE.ObjectCardinality.class, OntCEImpl.objectCardinalityCEFactory)
            .register(OntCE.DataCardinality.class, OntCEImpl.dataCardinalityCEFactory)
            .register(OntCE.HasSelf.class, OntCEImpl.hasSelfCEFactory)
            .register(OntCE.UnionOf.class, OntCEImpl.unionOfCEFactory)
            .register(OntCE.OneOf.class, OntCEImpl.oneOfCEFactory)
            .register(OntCE.IntersectionOf.class, OntCEImpl.intersectionOfCEFactory)
            .register(OntCE.ComplementOf.class, OntCEImpl.complementOfCEFactory)
            .register(OntCE.class, OntCEImpl.abstractCEFactory)
            .register(OntCE.ComponentsCE.class, OntCEImpl.abstractComponentsCEFactory)
            .register(OntCE.CardinalityRestrictionCE.class, OntCEImpl.abstractCardinalityRestrictionCEFactory)
            .register(OntCE.ComponentRestrictionCE.class, OntCEImpl.abstractComponentRestrictionCEFactory)
            .register(OntCE.RestrictionCE.class, OntCEImpl.abstractRestrictionCEFactory) //todo: add nary CEs

            // property expressions:
            .register(OntOPE.Inverse.class, OntPEImpl.inversePropertyFactory)
            .register(OntOPE.class, OntPEImpl.abstractOPEFactory)
            .register(OntPE.class, OntPEImpl.abstractPEFactory)

            // individuals
            .register(OntIndividual.Anonymous.class, OntIndividualImpl.anonymousIndividualFactory)
            .register(OntIndividual.class, OntIndividualImpl.abstractIndividualFactory)

            // negative property assertions
            .register(OntNPA.ObjectAssertion.class, OntNPAImpl.objectNPAFactory)
            .register(OntNPA.DataAssertion.class, OntNPAImpl.dataNPAFactory)
            .register(OntNPA.class, OntNPAImpl.abstractNPAFactory)

            // disjoint anonymous collections
            .register(OntDisjoint.Classes.class, OntDisjointImpl.disjointClassesFactory)
            .register(OntDisjoint.Individuals.class, OntDisjointImpl.differentIndividualsFactory)
            .register(OntDisjoint.ObjectProperties.class, OntDisjointImpl.objectPropertiesFactory)
            .register(OntDisjoint.DataProperties.class, OntDisjointImpl.dataPropertiesFactory)
            .register(OntDisjoint.Properties.class, OntDisjointImpl.abstractPropertiesFactory)
            .register(OntDisjoint.class, OntDisjointImpl.abstractDisjointFactory)

            // facet restrictions
            .register(OntFR.Length.class, OntFRImpl.lengthFRFactory)
            .register(OntFR.MinLength.class, OntFRImpl.minLengthFRFactory)
            .register(OntFR.MaxLength.class, OntFRImpl.maxLengthFRFactory)
            .register(OntFR.MinInclusive.class, OntFRImpl.minInclusiveFRFactory)
            .register(OntFR.MaxInclusive.class, OntFRImpl.maxInclusiveFRFactory)
            .register(OntFR.MinExclusive.class, OntFRImpl.minExclusiveFRFactory)
            .register(OntFR.MaxExclusive.class, OntFRImpl.maxExclusiveFRFactory)
            .register(OntFR.Pattern.class, OntFRImpl.patternFRFactory)
            .register(OntFR.TotalDigits.class, OntFRImpl.totalDigitsFRFactory)
            .register(OntFR.FractionDigits.class, OntFRImpl.fractionDigitsFRFactory)
            .register(OntFR.LangRange.class, OntFRImpl.langRangeFRFactory)
            .register(OntFR.class, OntFRImpl.abstractFRFactory)

            // data ranges
            .register(OntDR.OneOf.class, OntDRImpl.oneOfDRFactory)
            .register(OntDR.Restriction.class, OntDRImpl.restrictionDRFactory)
            .register(OntDR.ComplementOf.class, OntDRImpl.complementOfDRFactory)
            .register(OntDR.UnionOf.class, OntDRImpl.unionOfDRFactory)
            .register(OntDR.IntersectionOf.class, OntDRImpl.intersectionOfDRFactory)
            .register(OntDR.class, OntDRImpl.abstractDRFactory)

            // SWRL objects
            .register(OntSWRL.Variable.class, OntSWRLImpl.variableSWRLFactory)
            .register(OntSWRL.IArg.class, OntSWRLImpl.iArgSWRLFactory)
            .register(OntSWRL.DArg.class, OntSWRLImpl.dArgSWRLFactory)
            .register(OntSWRL.Arg.class, OntSWRLImpl.abstractArgSWRLFactory)
            .register(OntSWRL.Atom.BuiltIn.class, OntSWRLImpl.builtInAtomSWRLFactory)
            .register(OntSWRL.Atom.OwlClass.class, OntSWRLImpl.classAtomSWRLFactory)
            .register(OntSWRL.Atom.DataRange.class, OntSWRLImpl.dataRangeAtomSWRLFactory)
            .register(OntSWRL.Atom.ObjectProperty.class, OntSWRLImpl.individualAtomSWRLFactory)
            .register(OntSWRL.Atom.DataProperty.class, OntSWRLImpl.dataValuedAtomSWRLFactory)
            .register(OntSWRL.Atom.DifferentIndividuals.class, OntSWRLImpl.differentIndividualsAtomSWRLFactory)
            .register(OntSWRL.Atom.SameIndividuals.class, OntSWRLImpl.sameIndividualsAtomSWRLFactory)
            .register(OntSWRL.Atom.class, OntSWRLImpl.abstractAtomSWRLFactory)
            .register(OntSWRL.Imp.class, OntSWRLImpl.impSWRLFactory)
            .register(OntSWRL.class, OntSWRLImpl.abstractSWRLFactory);

}
