package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jena.enhanced.Personality;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.*;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.*;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Settings and personalities for {@link OntGraphModel}.
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

    private static final PersonalityBuilder ONT_PERSONALITY_BUILDER = new PersonalityBuilder()
            // ont-id
            .add(OntID.class, OntIDImpl.idFactory)

            // entities:
            .add(OntObject.class, OntObjectImpl.objectFactory)
            .add(OntClass.class, OntEntityImpl.classFactory)
            .add(OntNAP.class, OntEntityImpl.annotationPropertyFactory)
            .add(OntNDP.class, OntEntityImpl.dataPropertyFactory)
            .add(OntNOP.class, OntEntityImpl.objectPropertyFactory)
            .add(OntDT.class, OntEntityImpl.datatypeFactory)
            .add(OntIndividual.Named.class, OntEntityImpl.individualFactory)
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
            .add(OntCE.NaryDataAllValuesFrom.class, OntCEImpl.naryDataAllValuesFromCEFactory)
            .add(OntCE.NaryDataSomeValuesFrom.class, OntCEImpl.naryDataSomeValuesFromCEFactory)
            .add(OntCE.ComponentsCE.class, OntCEImpl.abstractComponentsCEFactory)
            .add(OntCE.CardinalityRestrictionCE.class, OntCEImpl.abstractCardinalityRestrictionCEFactory)
            .add(OntCE.ComponentRestrictionCE.class, OntCEImpl.abstractComponentRestrictionCEFactory)
            .add(OntCE.RestrictionCE.class, OntCEImpl.abstractRestrictionCEFactory)
            .add(OntCE.class, OntCEImpl.abstractCEFactory)

            // property expressions:
            .add(OntOPE.Inverse.class, OntPEImpl.inversePropertyFactory)
            .add(OntOPE.class, OntPEImpl.abstractOPEFactory)
            .add(OntPE.class, OntPEImpl.abstractPEFactory)

            // individuals
            .add(OntIndividual.Anonymous.class, OntIndividualImpl.anonymousIndividualFactory)
            .add(OntIndividual.class, OntIndividualImpl.abstractIndividualFactory)

            // negative property assertions
            .add(OntNPA.ObjectAssertion.class, OntNPAImpl.objectNPAFactory)
            .add(OntNPA.DataAssertion.class, OntNPAImpl.dataNPAFactory)
            .add(OntNPA.class, OntNPAImpl.abstractNPAFactory)

            // disjoint anonymous collections
            .add(OntDisjoint.Classes.class, OntDisjointImpl.disjointClassesFactory)
            .add(OntDisjoint.Individuals.class, OntDisjointImpl.differentIndividualsFactory)
            .add(OntDisjoint.ObjectProperties.class, OntDisjointImpl.objectPropertiesFactory)
            .add(OntDisjoint.DataProperties.class, OntDisjointImpl.dataPropertiesFactory)
            .add(OntDisjoint.Properties.class, OntDisjointImpl.abstractPropertiesFactory)
            .add(OntDisjoint.class, OntDisjointImpl.abstractDisjointFactory)

            // facet restrictions
            .add(OntFR.Length.class, OntFRImpl.lengthFRFactory)
            .add(OntFR.MinLength.class, OntFRImpl.minLengthFRFactory)
            .add(OntFR.MaxLength.class, OntFRImpl.maxLengthFRFactory)
            .add(OntFR.MinInclusive.class, OntFRImpl.minInclusiveFRFactory)
            .add(OntFR.MaxInclusive.class, OntFRImpl.maxInclusiveFRFactory)
            .add(OntFR.MinExclusive.class, OntFRImpl.minExclusiveFRFactory)
            .add(OntFR.MaxExclusive.class, OntFRImpl.maxExclusiveFRFactory)
            .add(OntFR.Pattern.class, OntFRImpl.patternFRFactory)
            .add(OntFR.TotalDigits.class, OntFRImpl.totalDigitsFRFactory)
            .add(OntFR.FractionDigits.class, OntFRImpl.fractionDigitsFRFactory)
            .add(OntFR.LangRange.class, OntFRImpl.langRangeFRFactory)
            .add(OntFR.class, OntFRImpl.abstractFRFactory)

            // data ranges
            .add(OntDR.OneOf.class, OntDRImpl.oneOfDRFactory)
            .add(OntDR.Restriction.class, OntDRImpl.restrictionDRFactory)
            .add(OntDR.ComplementOf.class, OntDRImpl.complementOfDRFactory)
            .add(OntDR.UnionOf.class, OntDRImpl.unionOfDRFactory)
            .add(OntDR.IntersectionOf.class, OntDRImpl.intersectionOfDRFactory)
            .add(OntDR.class, OntDRImpl.abstractDRFactory)

            // SWRL objects
            .add(OntSWRL.Variable.class, OntSWRLImpl.variableSWRLFactory)
            .add(OntSWRL.IArg.class, OntSWRLImpl.iArgSWRLFactory)
            .add(OntSWRL.DArg.class, OntSWRLImpl.dArgSWRLFactory)
            .add(OntSWRL.Arg.class, OntSWRLImpl.abstractArgSWRLFactory)
            .add(OntSWRL.Atom.BuiltIn.class, OntSWRLImpl.builtInAtomSWRLFactory)
            .add(OntSWRL.Atom.OntClass.class, OntSWRLImpl.classAtomSWRLFactory)
            .add(OntSWRL.Atom.DataRange.class, OntSWRLImpl.dataRangeAtomSWRLFactory)
            .add(OntSWRL.Atom.ObjectProperty.class, OntSWRLImpl.individualAtomSWRLFactory)
            .add(OntSWRL.Atom.DataProperty.class, OntSWRLImpl.dataValuedAtomSWRLFactory)
            .add(OntSWRL.Atom.DifferentIndividuals.class, OntSWRLImpl.differentIndividualsAtomSWRLFactory)
            .add(OntSWRL.Atom.SameIndividuals.class, OntSWRLImpl.sameIndividualsAtomSWRLFactory)
            .add(OntSWRL.Atom.class, OntSWRLImpl.abstractAtomSWRLFactory)
            .add(OntSWRL.Imp.class, OntSWRLImpl.impSWRLFactory)
            .add(OntSWRL.class, OntSWRLImpl.abstractSWRLFactory);

    public static final OntPersonality ONT_PERSONALITY_LAX = ONT_PERSONALITY_BUILDER.build(STANDARD_PERSONALITY, Configurable.Mode.LAX);
    public static final OntPersonality ONT_PERSONALITY_STRICT = ONT_PERSONALITY_BUILDER.build(STANDARD_PERSONALITY, Configurable.Mode.STRICT);

    private static OntPersonality personality = ONT_PERSONALITY_STRICT;

    public static OntPersonality getPersonality() {
        return personality;
    }

    public static void setPersonality(OntPersonality p) {
        personality = OntJenaException.notNull(p, "Null personality specified.");
    }

    public static class PersonalityBuilder {
        private final Map<Class<? extends OntObject>, Configurable<? extends OntObjectFactory>> map = new LinkedHashMap<>();

        public PersonalityBuilder add(Class<? extends OntObject> key, Configurable<? extends OntObjectFactory> value) {
            map.put(key, value);
            return this;
        }

        public OntPersonality build(Personality<RDFNode> init, Configurable.Mode mode) {
            OntJenaException.notNull(mode, "Null mode.");
            OntPersonality res = new OntPersonality(init == null ? new Personality<>() : init);
            map.forEach((k, v) -> res.register(k, v.get(mode)));
            return res;
        }
    }
}
