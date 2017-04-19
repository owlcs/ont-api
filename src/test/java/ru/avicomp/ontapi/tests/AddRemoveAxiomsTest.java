package ru.avicomp.ontapi.tests;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * The ontology and test scenario (in some general terms) was taken from
 * <a href='https://github.com/Galigator/openllet'>openllet</a> (it is an alive fork of Pellet).
 * <p>
 * Created by @szuev on 19.04.2017.
 */
public class AddRemoveAxiomsTest {
    private static final Logger LOGGER = Logger.getLogger(AddRemoveAxiomsTest.class);

    @Test
    public void main() throws Exception {
        OWLOntologyManager m = OntManagers.createConcurrentONT();

        OWLDataFactory df = m.getOWLDataFactory();

        IRI iri = IRI.create("http://www.example.org/xxx");

        OWLNamedIndividual a = df.getOWLNamedIndividual("http://www.example.org/test#a");
        OWLDataProperty dr = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dr"));
        OWLDataProperty dp = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dp"));
        OWLDataProperty dq = df.getOWLDataProperty(IRI.create("http://www.example.org/test#dq"));

        OWLDatatype integer = OWL2Datatype.XSD_INTEGER.getDatatype(df);
        OWLLiteral l = df.getOWLLiteral("1", integer);
        OWLClassExpression c = df.getOWLDataSomeValuesFrom(dr, integer);

        OWLAxiom FunctionalDataProperty = df.getOWLFunctionalDataPropertyAxiom(dp);
        OWLAxiom SubDataPropertyOf_dq = df.getOWLSubDataPropertyOfAxiom(dq, dp);
        OWLAxiom SubDataPropertyOf_dr = df.getOWLSubDataPropertyOfAxiom(dr, dp);
        OWLAxiom DataPropertyAssertion = df.getOWLDataPropertyAssertionAxiom(dq, a, l);
        OWLAxiom ClassAssertion = df.getOWLClassAssertionAxiom(c, a);
        OWLAxiom Declaration_a = df.getOWLDeclarationAxiom(a);
        OWLAxiom Declaration_dq = df.getOWLDeclarationAxiom(dq);
        OWLAxiom Declaration_dp = df.getOWLDeclarationAxiom(dp);
        OWLAxiom Declaration_dr = df.getOWLDeclarationAxiom(dr);
        OWLAxiom Declaration_integer = df.getOWLDeclarationAxiom(integer);

        OWLOntology o = m.createOntology(iri);

        o.add(FunctionalDataProperty);
        o.add(SubDataPropertyOf_dq);
        o.add(SubDataPropertyOf_dr);
        o.add(DataPropertyAssertion);
        o.add(ClassAssertion);

        o.remove(DataPropertyAssertion);

        o.add(Declaration_a);
        o.add(Declaration_dq);
        o.add(Declaration_integer);

        o.remove(Declaration_a);
        o.remove(Declaration_dq);

        o.remove(Declaration_integer);
        o.add(DataPropertyAssertion);
        o.remove(SubDataPropertyOf_dq);
        o.add(Declaration_dq);
        o.add(Declaration_dp);

        o.remove(Declaration_dq);

        o.remove(Declaration_dp);
        o.add(SubDataPropertyOf_dq);

        o.remove(DataPropertyAssertion);

        o.add(Declaration_a);
        o.add(Declaration_dq);
        o.add(Declaration_integer);

        o.remove(Declaration_a);
        o.remove(Declaration_dq);
        o.remove(Declaration_integer);

        o.add(DataPropertyAssertion);

        o.remove(SubDataPropertyOf_dr);
        o.add(Declaration_dp);
        o.add(Declaration_dr);


        o.remove(Declaration_dp);

        o.remove(Declaration_dr);

        o.add(SubDataPropertyOf_dr);
        o.remove(SubDataPropertyOf_dq);
        o.add(Declaration_dq);
        o.add(Declaration_dp);

        o.remove(Declaration_dq);
        o.remove(Declaration_dp);
        o.add(SubDataPropertyOf_dq);
        o.remove(FunctionalDataProperty);
        o.add(Declaration_dp);

        o.remove(Declaration_dp);

        o.add(FunctionalDataProperty);

        o.remove(ClassAssertion);

        o.add(Declaration_a);

        o.add(Declaration_integer);
        o.add(Declaration_dr);

        o.remove(Declaration_a);

        o.remove(Declaration_integer);
        o.remove(Declaration_dr);
        o.add(ClassAssertion);

        ReadWriteUtils.print(o);
        o.axioms().forEach(LOGGER::info);
    }

}
