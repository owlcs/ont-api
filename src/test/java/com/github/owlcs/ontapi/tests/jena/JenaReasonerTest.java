/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Derivation;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.RDFSRuleReasonerFactory;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.ReasonerVocabulary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * The simplest reasoner test.
 * Modified copy-paste from jena-core-tests (org.apache.jena.reasoner.test.ManualExample)
 * <p>
 * Created by szuev on 26.04.2017.
 */
public class JenaReasonerTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(JenaReasonerTest.class);

    /**
     * Illustrate different ways of finding a reasoner
     */
    @Test
    public void testFinding() {
        String NS = "urn:example/";

        // Build a trivial example data set
        OntModel example = OntModelFactory.createModel();
        OntDataProperty p = example.createDataProperty(NS + "p");
        OntDataProperty q = example.createDataProperty(NS + "q");
        p.addSuperProperty(q);
        example.createIndividual(NS + "a").addProperty(p, "foo");
        LOGGER.debug("Example model:");
        example.setNsPrefixes(OntModelFactory.STANDARD);
        ReadWriteUtils.print(example);

        Resource config = OntModelFactory.createDefaultModel()
                .createResource()
                .addProperty(ReasonerVocabulary.PROPsetRDFSLevel, "simple");
        Reasoner reasoner = RDFSRuleReasonerFactory.theInstance().create(config);

        InfModel inf = example.getInferenceModel(reasoner);
        LOGGER.debug("Inf model:");
        ReadWriteUtils.print(inf);

        Resource a = inf.getResource(NS + "a");
        Statement s = a.getProperty(q);
        LOGGER.debug("Statement: {}", s);
        Assertions.assertNotNull(s, "Null statement");
    }

    @Test
    public void testValidation1() {
        validationTest("ontapi/dttest1.nt", false);
    }

    @Test
    public void testValidation2() {
        validationTest("ontapi/dttest2.nt", false);
    }

    @Test
    public void testValidation3() {
        validationTest("ontapi/dttest3.nt", true);
    }

    /**
     * Illustrate validation
     */
    private void validationTest(String file, boolean result) {
        URI uri = ReadWriteUtils.getResourceURI(file);
        LOGGER.debug("Testing {}", uri);
        OntModel data = OntModelFactory.createModel(ReadWriteUtils.load(uri, OntFormat.NTRIPLES).getGraph());
        InfModel inf = data.getInferenceModel(ReasonerRegistry.getRDFSReasoner());
        ValidityReport validity = inf.validate();
        if (validity.isValid()) {
            LOGGER.debug("OK");
        } else {
            LOGGER.debug("Conflicts");
            for (Iterator<ValidityReport.Report> i = validity.getReports(); i.hasNext(); ) {
                ValidityReport.Report report = i.next();
                LOGGER.debug(" - {}", report);
            }
        }
        Assertions.assertEquals(result, validity.isValid());
    }

    /**
     * Illustrate generic rules and derivation tracing
     */
    @Test
    public void testDerivation() {
        // Test data
        String egNS = "urn:x-hp:eg/";
        OntModel rawData = OntModelFactory.createModel();
        Property p = rawData.createProperty(egNS, "p");
        Resource A = rawData.createResource(egNS + "A");
        Resource B = rawData.createResource(egNS + "B");
        Resource C = rawData.createResource(egNS + "C");
        Resource D = rawData.createResource(egNS + "D");
        A.addProperty(p, B);
        B.addProperty(p, C);
        C.addProperty(p, D);

        // Rule example
        String rules = "[rule1: (?a eg:p ?b) (?b eg:p ?c) -> (?a eg:p ?c)]";
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        reasoner.setDerivationLogging(true);
        InfModel inf = rawData.getInferenceModel(reasoner);

        List<Statement> statements = inf.listStatements(A, p, D).toList();
        LOGGER.debug("{}", statements);
        Assertions.assertEquals(1, statements.size());
        StringWriter res = new StringWriter();
        PrintWriter out = new PrintWriter(res, true);
        Iterator<Derivation> id = inf.getDerivation(statements.get(0));
        while (id.hasNext()) {
            id.next().printTrace(out, true);
        }
        LOGGER.debug("{}", res);
        String expected = "Rule rule1 concluded (eg:A eg:p eg:D) <-\n" +
                "    Rule rule1 concluded (eg:A eg:p eg:C) <-\n" +
                "        Fact (eg:A eg:p eg:B)\n" +
                "        Fact (eg:B eg:p eg:C)\n" +
                "    Fact (eg:C eg:p eg:D)\n";
        Assertions.assertEquals(expected, res.toString().replace("\r", ""));
    }

    /**
     * Another generic rules illustration
     */
    @Test
    public void testGenericRules() {
        // Test data
        String egNS = "urn:x-hp:eg/";
        OntModel rawData = OntModelFactory.createModel();
        Property first = rawData.createProperty(egNS, "concatFirst");
        Property second = rawData.createProperty(egNS, "concatSecond");
        Property p = rawData.createProperty(egNS, "p");
        Property q = rawData.createProperty(egNS, "q");
        Property r = rawData.createProperty(egNS, "r");
        Resource A = rawData.createResource(egNS + "A");
        Resource B = rawData.createResource(egNS + "B");
        Resource C = rawData.createResource(egNS + "C");
        A.addProperty(p, B);
        B.addProperty(q, C);
        r.addProperty(first, p);
        r.addProperty(second, q);

        String data = ReadWriteUtils.toString(rawData, OntFormat.TURTLE);

        // Rule example for
        String rules = "[r1: (?c eg:concatFirst ?p), (?c eg:concatSecond ?q) -> [r1b: (?x ?c ?y) <- (?x ?p ?z) (?z ?q ?y)]]";
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
        InfModel inf = rawData.getInferenceModel(reasoner);
        Iterator<Statement> list = inf.listStatements(A, null, (RDFNode) null);
        LOGGER.debug("A * * =>");
        while (list.hasNext()) {
            LOGGER.debug(" - {}", list.next());
        }
        Assertions.assertTrue(inf.contains(A, p, B));
        Assertions.assertTrue(inf.contains(A, r, C));

        Assertions.assertEquals(data, ReadWriteUtils.toString(rawData, OntFormat.TURTLE), "Data has been changed");
    }

}
