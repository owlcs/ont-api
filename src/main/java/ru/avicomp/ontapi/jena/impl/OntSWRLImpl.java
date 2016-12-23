package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.LiteralImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;

/**
 * Ont SWRL Object Implementation
 * <p>
 * Created by @szuev on 18.11.2016.
 */
public class OntSWRLImpl extends OntObjectImpl implements OntSWRL {
    private static final OntFilter VAR_SWRL_FILTER = OntFilter.URI.and(new OntFilter.HasType(SWRL.Variable));

    public static OntObjectFactory variableSWRLFactory = new CommonOntObjectFactory(
            new OntMaker.WithType(VariableImpl.class, SWRL.Variable),
            new OntFinder.ByType(SWRL.Variable), VAR_SWRL_FILTER);

    public static OntObjectFactory dArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(DArgImpl.class),
            OntFinder.ANY_SUBJECT_AND_OBJECT, VAR_SWRL_FILTER.or(LiteralImpl.factory::canWrap));
    public static OntObjectFactory iArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(IArgImpl.class),
            OntFinder.ANY_SUBJECT, VAR_SWRL_FILTER.or(OntIndividualImpl.abstractIndividualFactory::canWrap));
    public static OntObjectFactory abstractArgSWRLFactory = new MultiOntObjectFactory(dArgSWRLFactory, iArgSWRLFactory);

    public static OntObjectFactory builtInAtomSWRLFactory = makeAtomFactory(BuiltInAtomImpl.class, SWRL.BuiltinAtom);
    public static OntObjectFactory classAtomSWRLFactory = makeAtomFactory(OntClassAtomImpl.class, SWRL.ClassAtom);
    public static OntObjectFactory dataRangeAtomSWRLFactory = makeAtomFactory(DataRangeAtomImpl.class, SWRL.DataRangeAtom);
    public static OntObjectFactory dataValuedAtomSWRLFactory = makeAtomFactory(DataPropertyAtomImpl.class, SWRL.DatavaluedPropertyAtom);
    public static OntObjectFactory individualAtomSWRLFactory = makeAtomFactory(ObjectPropertyAtomImpl.class, SWRL.IndividualPropertyAtom);
    public static OntObjectFactory differentIndividualsAtomSWRLFactory = makeAtomFactory(DifferentIndividualsAtomImpl.class, SWRL.DifferentIndividualsAtom);
    public static OntObjectFactory sameIndividualsAtomSWRLFactory = makeAtomFactory(SameIndividualsAtomImpl.class, SWRL.SameIndividualAtom);
    public static OntObjectFactory abstractAtomSWRLFactory = new MultiOntObjectFactory(OntFinder.TYPED,
            builtInAtomSWRLFactory, classAtomSWRLFactory, dataRangeAtomSWRLFactory, dataValuedAtomSWRLFactory,
            individualAtomSWRLFactory, differentIndividualsAtomSWRLFactory, sameIndividualsAtomSWRLFactory);

    public static OntObjectFactory impSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(ImpImpl.class), new OntFinder.ByType(SWRL.Imp), new OntFilter.HasType(SWRL.Imp));
    public static OntObjectFactory abstractSWRLFactory = new MultiOntObjectFactory(variableSWRLFactory, abstractAtomSWRLFactory, impSWRLFactory);

    private static OntObjectFactory makeAtomFactory(Class<? extends AtomImpl> view, Resource type) {
        return new CommonOntObjectFactory(new OntMaker.Default(view),
                new OntFinder.ByType(type), OntFilter.BLANK.and(new OntFilter.HasType(type)));
    }

    public OntSWRLImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static Variable createVariable(OntGraphModelImpl model, String uri) {
        return model.createOntObject(Variable.class, uri);
    }

    public static Atom.BuiltIn createBuiltInAtom(OntGraphModelImpl model, Resource predicate, Stream<DArg> arguments) {
        OntObjectImpl.checkNamed(predicate);
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.BuiltinAtom);
        model.add(res, SWRL.builtin, predicate);
        model.add(predicate, RDF.type, SWRL.Builtin); // ?
        model.add(res, SWRL.arguments, model.createList(arguments.iterator()));
        return model.getNodeAs(res.asNode(), Atom.BuiltIn.class);
    }

    public static Atom.OntClass createClassAtom(OntGraphModelImpl model, OntCE clazz, IArg arg) {
        OntJenaException.notNull(clazz, "Null class");
        OntJenaException.notNull(arg, "Null i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.ClassAtom);
        model.add(res, SWRL.classPredicate, clazz);
        model.add(res, SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.OntClass.class);
    }

    public static Atom.DataRange createDataRangeAtom(OntGraphModelImpl model, OntDR range, DArg arg) {
        OntJenaException.notNull(range, "Null data range");
        OntJenaException.notNull(arg, "Null d-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DataRangeAtom);
        model.add(res, SWRL.dataRange, range);
        model.add(res, SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.DataRange.class);
    }

    public static Atom.DataProperty createDataPropertyAtom(OntGraphModelImpl model, OntNDP dataProperty, IArg firstArg, DArg secondArg) {
        OntJenaException.notNull(dataProperty, "Null data property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second d-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DatavaluedPropertyAtom);
        model.add(res, SWRL.propertyPredicate, dataProperty);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DataProperty.class);
    }

    public static Atom.ObjectProperty createObjectPropertyAtom(OntGraphModelImpl model, OntOPE objectProperty, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(objectProperty, "Null object property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.IndividualPropertyAtom);
        model.add(res, SWRL.propertyPredicate, objectProperty);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.ObjectProperty.class);
    }

    public static Atom.DifferentIndividuals createDifferentIndividualsAtom(OntGraphModelImpl model, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DifferentIndividualsAtom);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DifferentIndividuals.class);
    }

    public static Atom.SameIndividuals createSameIndividualsAtom(OntGraphModelImpl model, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.SameIndividualAtom);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.SameIndividuals.class);
    }

    public static Imp createImp(OntGraphModelImpl model, Stream<Atom> head, Stream<Atom> body) {
        OntJenaException.notNull(head, "Null head");
        OntJenaException.notNull(body, "Null body");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.Imp);
        model.add(res, SWRL.head, Models.createTypedList(model, SWRL.AtomList, head));
        model.add(res, SWRL.body, Models.createTypedList(model, SWRL.AtomList, body));
        return model.getNodeAs(res.asNode(), Imp.class);
    }

    public static class VariableImpl extends OntSWRLImpl implements Variable {
        public VariableImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class DArgImpl extends OntObjectImpl implements DArg {
        public DArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class IArgImpl extends OntObjectImpl implements IArg {
        public IArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static abstract class AtomImpl<P extends RDFNode> extends OntSWRLImpl implements Atom<P> {
        public AtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class BuiltInAtomImpl extends AtomImpl<Resource> implements Atom.BuiltIn {
        public BuiltInAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Resource getPredicate() { // should be uri
            return getRequiredOntProperty(SWRL.builtin, Resource.class);
        }

        @Override
        public Stream<DArg> arguments() {
            return rdfList(SWRL.arguments, DArg.class);
        }
    }

    public static class OntClassAtomImpl extends AtomImpl<OntCE> implements Atom.OntClass {
        public OntClassAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntCE getPredicate() {
            return getRequiredOntProperty(SWRL.classPredicate, OntCE.class);
        }

        @Override
        public IArg getArg() {
            return getRequiredOntProperty(SWRL.argument1, IArg.class);
        }
    }

    public static class DataRangeAtomImpl extends AtomImpl<OntDR> implements Atom.DataRange {
        public DataRangeAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntDR getPredicate() {
            return getRequiredOntProperty(SWRL.dataRange, OntDR.class);
        }

        @Override
        public DArg getArg() {
            return getRequiredOntProperty(SWRL.argument1, DArg.class);
        }
    }

    public static class DataPropertyAtomImpl extends AtomImpl<OntNDP> implements Atom.DataProperty {
        public DataPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntNDP getPredicate() {
            return getRequiredOntProperty(SWRL.propertyPredicate, OntNDP.class);
        }

        @Override
        public IArg getFirstArg() {
            return getRequiredOntProperty(SWRL.argument1, IArg.class);
        }

        @Override
        public DArg getSecondArg() {
            return getRequiredOntProperty(SWRL.argument2, DArg.class);
        }
    }

    public static class ObjectPropertyAtomImpl extends AtomImpl<OntOPE> implements Atom.ObjectProperty {
        public ObjectPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntOPE getPredicate() {
            return getRequiredOntProperty(SWRL.propertyPredicate, OntOPE.class);
        }

        @Override
        public IArg getFirstArg() {
            return getRequiredOntProperty(SWRL.argument1, IArg.class);
        }

        @Override
        public IArg getSecondArg() {
            return getRequiredOntProperty(SWRL.argument2, IArg.class);
        }
    }

    public static class DifferentIndividualsAtomImpl extends AtomImpl<Property> implements Atom.DifferentIndividuals {

        public DifferentIndividualsAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Property getPredicate() {
            return OWL.differentFrom;
        }

        @Override
        public IArg getFirstArg() {
            return getRequiredOntProperty(SWRL.argument1, IArg.class);
        }

        @Override
        public IArg getSecondArg() {
            return getRequiredOntProperty(SWRL.argument2, IArg.class);
        }
    }

    public static class SameIndividualsAtomImpl extends AtomImpl<Property> implements Atom.SameIndividuals {
        public SameIndividualsAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Property getPredicate() {
            return OWL.sameAs;
        }

        @Override
        public IArg getFirstArg() {
            return getRequiredOntProperty(SWRL.argument1, IArg.class);
        }

        @Override
        public IArg getSecondArg() {
            return getRequiredOntProperty(SWRL.argument2, IArg.class);
        }
    }

    public static class ImpImpl extends OntSWRLImpl implements Imp {
        public ImpImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        private Stream<Atom> list(Property predicate) {
            Statement st = getProperty(predicate);
            if (st == null || !st.getObject().isResource())
                return Stream.empty();
            Resource list = st.getObject().asResource();
            return Models.rdfListContent(getModel(), list).filter(n -> n.canAs(Atom.class)).map(n -> n.as(Atom.class)).distinct();
        }

        @Override
        public Stream<Atom> head() {
            return list(SWRL.head);
        }

        @Override
        public Stream<Atom> body() {
            return list(SWRL.body);
        }
    }
}

