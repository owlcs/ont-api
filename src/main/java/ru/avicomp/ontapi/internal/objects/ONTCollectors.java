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

package ru.avicomp.ontapi.internal.objects;

import org.semanticweb.owlapi.model.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.Set;

/**
 * A collection of {@link OWLObjectVisitorEx OWL-API Object Visitor}s used internally.
 * Created by @ssz on 30.08.2019.
 *
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public abstract class ONTCollectors {

    /**
     * Creates a visitor that collects {@code OWLClass}es into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLClass}es
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLClass>> forClasses(Set<OWLClass> res) {
        return new SetCollector<OWLClass>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainNamedClasses() || x.isNamedClass();
            }

            @Override
            public Set<OWLClass> visit(OWLClass x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects {@code OWLAnonymousIndividual}s into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLAnonymousIndividual}s
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLAnonymousIndividual>> forAnonymousIndividuals(Set<OWLAnonymousIndividual> res) {
        return new SetCollector<OWLAnonymousIndividual>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainAnonymousIndividuals() || x.isAnonymousIndividual();
            }

            @Override
            public Set<OWLAnonymousIndividual> visit(OWLAnonymousIndividual x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects {@code OWLNamedIndividual}s into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLNamedIndividual}s
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLNamedIndividual>> forNamedIndividuals(Set<OWLNamedIndividual> res) {
        return new SetCollector<OWLNamedIndividual>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainNamedIndividuals() || x.isNamedIndividual();
            }

            @Override
            public Set<OWLNamedIndividual> visit(OWLNamedIndividual x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects {@code OWLDatatype}s into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLDatatype}s
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLDatatype>> forDatatypes(Set<OWLDatatype> res) {
        return new SetCollector<OWLDatatype>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainDatatypes() || x.isDatatype();
            }

            @Override
            public Set<OWLDatatype> visit(OWLDatatype x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects Object Properties into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLObjectProperty}es
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLObjectProperty>> forObjectProperties(Set<OWLObjectProperty> res) {
        return new SetCollector<OWLObjectProperty>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainObjectProperties() || x.isObjectProperty();
            }

            @Override
            public Set<OWLObjectProperty> visit(OWLObjectProperty x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects Data Properties into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLDataProperty}es
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLDataProperty>> forDataProperties(Set<OWLDataProperty> res) {
        return new SetCollector<OWLDataProperty>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainDataProperties() || x.isDataProperty();
            }

            @Override
            public Set<OWLDataProperty> visit(OWLDataProperty x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects Annotation Properties into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLAnnotationProperty}es
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLAnnotationProperty>> forAnnotationProperties(Set<OWLAnnotationProperty> res) {
        return new SetCollector<OWLAnnotationProperty>(res) {
            @Override
            protected boolean testComponent(ONTComposite x) {
                return x.canContainAnnotationProperties() || x.isAnnotationProperty();
            }

            @Override
            public Set<OWLAnnotationProperty> visit(OWLAnnotationProperty x) {
                res.add(x);
                return res;
            }
        };
    }

    /**
     * Creates a visitor that collects {@code OWLClassExpression}s into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLClassExpression}s
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLClassExpression>> forClassExpressions(Set<OWLClassExpression> res) {
        return new ForClassExpressions(res);
    }

    /**
     * Creates a visitor that collects {@code OWLEntity}es into the specified {@code Set}.
     *
     * @param res {@code Set} of {@link OWLEntity}es
     * @return {@link OWLObjectVisitorEx}
     */
    public static OWLObjectVisitorEx<Set<OWLEntity>> forEntities(Set<OWLEntity> res) {
        return new ForEntities(res);
    }

    /**
     * A base visitor that collects a {@code Set}.
     *
     * @param <X> - subtype {@link OWLObject}, that must be also {@link ONTComposite}
     */
    public static abstract class SetCollector<X extends OWLObject> implements OWLObjectVisitorEx<Set<X>> {
        protected final Set<X> res;

        public SetCollector(Set<X> res) {
            this.res = Objects.requireNonNull(res);
        }

        @Override
        public Set<X> doDefault(Object object) {
            ((ONTComposite) object).objects().forEach(x -> {
                if (testComponent((ONTComposite) x)) {
                    x.getOWLObject().accept(SetCollector.this);
                }
            });
            return res;
        }

        /**
         * Tests that the component is acceptable to pass down.
         *
         * @param x {@link ONTComposite}, not {@code null}
         * @return boolean
         */
        protected boolean testComponent(ONTComposite x) {
            return true;
        }
    }

    /**
     * A visitor to collect {@link OWLEntity Entities} in the form of {@code Set}.
     *
     * @see org.semanticweb.owlapi.util.OWLEntityCollector
     * @see ru.avicomp.ontapi.jena.model.OntEntity
     */
    protected static class ForEntities extends SetCollector<OWLEntity> {

        protected ForEntities(Set<OWLEntity> res) {
            super(res);
        }

        @Override
        public Set<OWLEntity> visit(OWLClass ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLEntity> visit(OWLObjectProperty property) {
            res.add(property);
            return res;
        }

        @Override
        public Set<OWLEntity> visit(OWLDataProperty property) {
            res.add(property);
            return res;
        }

        @Override
        public Set<OWLEntity> visit(OWLNamedIndividual individual) {
            res.add(individual);
            return res;
        }

        @Override
        public Set<OWLEntity> visit(OWLDatatype node) {
            res.add(node);
            return res;
        }

        @Override
        public Set<OWLEntity> visit(OWLAnnotationProperty property) {
            res.add(property);
            return res;
        }
    }

    /**
     * A visitor to collect {@link OWLClassExpression Class Expressions} in the form of {@code Set}.
     *
     * @see org.semanticweb.owlapi.util.OWLClassExpressionCollector
     * @see ru.avicomp.ontapi.jena.model.OntCE
     */
    protected static class ForClassExpressions extends SetCollector<OWLClassExpression> {

        protected ForClassExpressions(Set<OWLClassExpression> res) {
            super(res);
        }

        @Override
        protected boolean testComponent(ONTComposite x) {
            return x.canContainClassExpressions() || x.isClassExpression();
        }

        @Override
        public Set<OWLClassExpression> visit(OWLClass ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectIntersectionOf ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectUnionOf ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectOneOf ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectComplementOf ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectHasSelf ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectSomeValuesFrom ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataSomeValuesFrom ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectAllValuesFrom ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataAllValuesFrom ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectHasValue ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataHasValue ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectMinCardinality ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectExactCardinality ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLObjectMaxCardinality ce) {
            res.add(ce);
            return doDefault(ce);
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataMinCardinality ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataExactCardinality ce) {
            res.add(ce);
            return res;
        }

        @Override
        public Set<OWLClassExpression> visit(OWLDataMaxCardinality ce) {
            res.add(ce);
            return res;
        }
    }

}
