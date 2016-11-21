package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * For SWRL addition.
 * See <a href='https://www.w3.org/Submission/SWRL'>specification</a>.
 * <p>
 * Created by @szuev on 02.11.2016.
 */
public interface OntSWRL extends OntObject {

    interface Imp extends OntSWRL {
        Stream<Atom> head();

        Stream<Atom> body();
    }

    interface Variable extends OntSWRL {
    }

    /**
     * It is not SWRL Object, but plain {@link OntObject}.
     * Wrapper for either {@link org.apache.jena.rdf.model.Literal}, {@link Variable} or {@link OntIndividual}
     */
    interface Arg extends OntObject {
    }

    /**
     * wrapper for Literal and Variable
     */
    interface DArg extends Arg {
    }

    /**
     * wrapper for Literal and OntIndividual
     */
    interface IArg extends Arg {
    }

    interface Atom<P extends RDFNode> extends OntSWRL {

        /**
         * returns one of the following: OntDR, OntOPE, OntNDP, OntCE, Resource(uri), Property.
         *
         * @return RDFNode
         */
        P getPredicate();

        interface BuiltIn extends Atom<Resource> {
            Stream<DArg> arguments();
        }

        interface OntClass extends Unary<OntCE, IArg> {
        }

        interface DataRange extends Unary<OntDR, DArg> {
        }

        interface DataProperty extends Binary<OntNDP, IArg, DArg> {
        }

        interface ObjectProperty extends Binary<OntOPE, IArg, IArg> {
        }

        interface DifferentIndividuals extends Binary<Property, IArg, IArg> {
        }

        interface SameIndividuals extends Binary<Property, IArg, IArg> {
        }

        interface Binary<P extends Resource, F extends Arg, S extends Arg> extends Atom<P> {
            F getFirstArg();

            S getSecondArg();
        }

        interface Unary<P extends Resource, A extends Arg> extends Atom<P> {
            A getArg();
        }

    }
}
