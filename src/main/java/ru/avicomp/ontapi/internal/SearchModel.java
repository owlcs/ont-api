/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.impl.conf.BaseFactoryImpl;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.impl.conf.PersonalityBuilder;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntSWRL;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Objects;

/**
 * Model with optimizations including nodes cache.
 * It is used in various operations of collecting axioms, each of them must be isolated by R/W lock,
 * which guarantees that underlying graph is not changed.
 * <p>
 * Created by @ssz on 16.02.2019.
 *
 * @since 1.4.0
 */
@SuppressWarnings("WeakerAccess")
public class SearchModel extends OntGraphModelImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchModel.class);
    /**
     * Average {@link Node} (uri and blank) size is about 160 bytes (internal string ~ 150byte),
     * Experiments show that for the limit = 100_000, the sum of all cache sizes is not more than 190_000
     * (it is for teleost and galen, significantly less for the rest it),
     * This means about 30 MB.
     * Tested ontologies:
     * <ul>
     * <li>teleost(59mb, 336_291 axioms, 650_339 triples)</li>
     * <li>hp(38mb, 143_855 axioms, 367_315 triples)</li>
     * <li>galen(33mb, 96_463 axioms, 281_492 triples)</li>
     * <li>psychology(4mb, 38_872 axioms, 38_873 triples)</li>
     * <li>family(0.2mb, 2_845 axioms)</li>
     * <li>pizza(0.1mb, 945 axioms)</li>
     * </ul>
     * todo: move to config
     */
    private static final int CACHE_SIZE = 50_000;

    protected final InternalConfig conf;
    /**
     * Original personality.
     */
    protected final OntPersonality personality;
    private Boolean hasAnnotations;
    private Boolean hasSubAnnotations;

    public SearchModel(Graph graph, OntPersonality personality, InternalConfig conf) {
        this(graph, personality, conf, true);
    }

    protected SearchModel(Graph graph,
                          OntPersonality personality,
                          InternalConfig conf,
                          boolean withCache) {
        super(graph, withCache ? cachedPersonality(personality, conf) : personality);
        this.conf = Objects.requireNonNull(conf);
        this.personality = personality;
    }

    static <X> X handleFetchNodeAsException(OntJenaException error,
                                            Node node,
                                            Class<? extends RDFNode> type,
                                            OntGraphModel m,
                                            InternalConfig conf) throws OntApiException {
        if (!conf.isIgnoreAxiomsReadErrors()) {
            throw new OntApiException(error);
        }
        LOGGER.warn("Can't wrap node <{}> as {}: found a problem inside <{}>: '{}'",
                node, OntObjectImpl.viewAsString(type), m.getID(), error.getMessage());
        return null;
    }

    public static OntPersonality cachedPersonality(OntPersonality from, InternalConfig conf) {
        PersonalityBuilder res = PersonalityBuilder.from(from);
        from.types(OntObject.class)
                // do not cache SWRL.DArg (and, therefore, SWRL.Arg) since an instance of this type
                // can be Literal with unpredictable length
                .filter(x -> x != OntSWRL.DArg.class && x != OntSWRL.Arg.class)
                .forEach(x -> CachedFactory.cache(res, from, x, CACHE_SIZE, conf.parallel()));
        return res.build();
    }

    @Override
    public OntGraphModelImpl getTopModel() {
        if (independent()) {
            return this;
        }
        // do not cache, since the top model is used only to list local objects,
        // and, also, because these objects may differ from those retrieved from the full model,
        // (for example sub model may contain declaration '<a> a owl:Class',
        // while in the local graph there is '<a> a rdfs:Datatype' - i.e. a punning for the same entity <a>).
        // A shared cache for this case will lead to wrong result,
        // and a separated cache will not give a performance gain
        return new SearchModel(getBaseGraph(), personality, conf, false);
    }

    /**
     * Answers {@code true} if the model contains bulk annotations.
     *
     * @return {@code true} if the model contains predicate {@link OWL#annotatedSource owl:annotatedSource}
     */
    public boolean hasAnnotations() {
        return hasAnnotations == null ?
                hasAnnotations = contains(null, OWL.annotatedSource, (RDFNode) null) :
                hasAnnotations;
    }

    /**
     * Answers {@code true} if the model contains bulk sub-annotations
     * (i.e. {@code rdf:type} = {@link OWL#Annotation owl:Annotation}).
     *
     * @return {@code true} if the model contains sub-annotations
     */
    public boolean hasSubAnnotations() {
        return hasSubAnnotations == null ?
                hasSubAnnotations = contains(null, RDF.type, OWL.Annotation) :
                hasSubAnnotations;
    }

    @Override
    public ExtendedIterator<Resource> listAnnotations(Resource t, Resource s, Property p, RDFNode o) {
        if (!hasAnnotations()) return NullIterator.instance();
        if (OWL.Annotation == t && !hasSubAnnotations()) {
            return NullIterator.instance();
        }
        return super.listAnnotations(t, s, p, o);
    }

    @Override
    public <N extends RDFNode> N fetchNodeAs(Node node, Class<N> type) {
        try {
            return super.fetchNodeAs(node, type);
        } catch (OntJenaException e) {
            return handleFetchNodeAsException(e, node, type, this, conf);
        }
    }

    /**
     * A {@link ObjectFactory} impl with nodes cache.
     */
    public static class CachedFactory extends BaseFactoryImpl {
        private final ObjectFactory from;
        private final Class<? extends OntObject> type;
        private final InternalCache<Node, Boolean> canWrapCache;

        public CachedFactory(Class<? extends OntObject> type, ObjectFactory from, int limit, boolean parallel) {
            this.type = Objects.requireNonNull(type);
            this.from = Objects.requireNonNull(from);
            this.canWrapCache = InternalCache.createBounded(parallel, limit);
        }

        private static CachedFactory create(Class<? extends OntObject> type,
                                            ObjectFactory from,
                                            int limit,
                                            boolean parallel) {
            return new CachedFactory(type,
                    from instanceof CachedFactory ? ((CachedFactory) from).from : from,
                    limit,
                    parallel);
        }

        static void cache(PersonalityBuilder res,
                          OntPersonality from,
                          Class<? extends OntObject> type,
                          int limit,
                          boolean parallel) {
            res.add(type, create(type, from.getObjectFactory(type), limit, parallel));
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return from.iterator(eg);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            if (node.isLiteral()) return from.canWrap(node, eg);
            return canWrapCache.get(node, n -> from.canWrap(n, eg));
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            return from.createInstance(node, eg);
        }

        @Override
        public String toString() {
            return String.format("CachedFactory[%s]", OntObjectImpl.viewAsString(type));
        }
    }
}
