package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Function;

/**
 * A DataFactory without cache.
 * <p>
 * Created by @szuev on 15.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class NoCacheDataFactory implements InternalDataFactory {
    protected final ConfigProvider.Config config;

    public NoCacheDataFactory(ConfigProvider.Config config) {
        this.config = config;
    }

    @Override
    public void clear() {
        // nothing
    }

    protected IRI toIRI(Resource r) {
        return toIRI(r.getURI());
    }

    @Override
    public InternalObject<? extends OWLClassExpression> get(OntCE ce) {
        return ReadHelper.calcClassExpression(ce, this, new HashSet<>());
    }

    @Override
    public InternalObject<? extends OWLDataRange> get(OntDR dr) {
        return ReadHelper.calcDataRange(dr, this, new HashSet<>());
    }

    @Override
    public InternalObject<OWLAnnotationProperty> get(OntNAP nap) {
        IRI iri = toIRI(OntApiException.notNull(nap, "Null annotation property."));
        return InternalObject.create(getOWLDataFactory().getOWLAnnotationProperty(iri), nap);
    }

    @Override
    public InternalObject<OWLDataProperty> get(OntNDP ndp) {
        IRI iri = toIRI(OntApiException.notNull(ndp, "Null data property."));
        return InternalObject.create(getOWLDataFactory().getOWLDataProperty(iri), ndp);
    }

    @Override
    public InternalObject<? extends OWLObjectPropertyExpression> get(OntOPE ope) {
        OntApiException.notNull(ope, "Null object property.");
        if (ope.isAnon()) { //todo: handle inverse of inverseOf (?)
            OWLObjectProperty op = getOWLDataFactory().getOWLObjectProperty(toIRI(ope.as(OntOPE.Inverse.class).getDirect()));
            return InternalObject.create(op.getInverseProperty(), ope);
        }
        return InternalObject.create(getOWLDataFactory().getOWLObjectProperty(toIRI(ope)), ope);
    }

    @Override
    public InternalObject<? extends OWLIndividual> get(OntIndividual individual) {
        if (OntApiException.notNull(individual, "Null individual").isURIResource()) {
            return InternalObject.create(getOWLDataFactory().getOWLNamedIndividual(toIRI(individual)), individual);
        }
        String label = //NodeFmtLib.encodeBNodeLabel(individual.asNode().getBlankNodeLabel());
                individual.asNode().getBlankNodeLabel();
        return InternalObject.create(getOWLDataFactory().getOWLAnonymousIndividual(label), individual);

    }

    @Override
    public InternalObject<? extends OWLAnnotationValue> get(RDFNode value) {
        if (OntApiException.notNull(value, "Null node").isLiteral()) {
            return get(value.asLiteral());
        }
        if (value.isURIResource()) {
            return asIRI(value.as(OntObject.class));
        }
        if (value.isAnon()) {
            return get(Models.asAnonymousIndividual(value));
        }
        throw new OntApiException("Not an AnnotationValue " + value);

    }

    @Override
    public InternalObject<? extends OWLAnnotationSubject> get(OntObject subject) {
        if (OntApiException.notNull(subject, "Null resource").isURIResource()) {
            return asIRI(subject);
        }
        if (subject.isAnon()) {
            return get(Models.asAnonymousIndividual(subject));
        }
        throw new OntApiException("Not an AnnotationSubject " + subject);

    }

    @Override
    public InternalObject<OWLLiteral> get(Literal literal) {
        String txt = OntApiException.notNull(literal, "Null literal").getLexicalForm();
        String lang = literal.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            txt = txt + "@" + lang;
        }
        OntDT dt = literal.getModel().getResource(literal.getDatatypeURI()).as(OntDT.class);
        InternalObject<OWLDatatype> owl;
        if (dt.isBuiltIn()) {
            owl = InternalObject.create(getOWLDataFactory().getOWLDatatype(toIRI(dt)));
        } else {
            owl = get(dt);
        }
        OWLLiteral res = getOWLDataFactory().getOWLLiteral(txt, owl.getObject());
        return InternalObject.create(res).append(owl);
    }

    @Override
    public InternalObject<? extends SWRLAtom> get(OntSWRL.Atom atom) {
        return ReadHelper.calcSWRLAtom(atom, this);
    }

    @Override
    public Collection<InternalObject<OWLAnnotation>> get(OntStatement statement) {
        return ReadHelper.getAnnotations(statement, this);
    }


    public CacheMap<OntCE, InternalObject<? extends OWLClassExpression>> classExpressionStore() {
        return new NoOpCacheMap<>();
    }

    public CacheMap<OntDR, InternalObject<? extends OWLDataRange>> dataRangeStore() {
        return new NoOpCacheMap<>();
    }

    @Override
    public InternalObject<IRI> asIRI(OntObject object) {
        return InternalObject.create(toIRI(object), object.canAs(OntEntity.class) ? object.as(OntEntity.class) : object);
    }

    @Override
    public OWLDataFactory getOWLDataFactory() {
        return config.dataFactory();
    }

    public interface CacheMap<K, V> {
        V get(K key);

        void put(K key, V value);

        default V get(K key, Function<? super K, ? extends V> map) {
            V v = get(key);
            if (v != null) return v;
            v = Objects.requireNonNull(Objects.requireNonNull(map, "Null mapping function.").apply(key),
                    "Null map result, key: " + key);
            put(key, v);
            return v;
        }
    }

    public static class NoOpCacheMap<K, V> implements CacheMap<K, V> {
        @Override
        public V get(K key) {
            return null;
        }

        @Override
        public void put(K key, V value) {
            // nothing
        }
    }
}
