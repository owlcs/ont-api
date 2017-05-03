### ONT-API (ver. 17/04/2017)

## Summary
ONT-API is OWL-API over Apache Jena.
In other words it is an attempt to befriend these two semantic-web technologies.
 
## Dependencies
- OWL-API (https://github.com/owlcs/owlapi), current version: 5.0.5
- Apache Jena (https://github.com/apache/jena), current version: 3.0.1

## Goal
Pure Jena does _NOT_ support OWL-2 specification and pure OWL-API does _NOT_ provide an adequate opportunity to work with an ontological graph.
ONT-API is an implementation of the OWL-API interfaces on Jena, thus, in addition to the structural representation of ontology (axioms), 
it is possible also to work with the graph directly.
This can be useful for Jena-users, since it allows to use API that meets the specification along with other products 
written on Jena as well (such as topbraid-spin, d2rq, etc).
In additional, although currently the performance when working through structural view are worse then of OWL-API, 
and here of course could be specific bugs, the ONT-API may also be very useful for OWL-API users too, 
since the use of a single graph instead of disparate components allows solving a number of OWL-API(5.0.5) typical problems. 
Here is the short list of such well-known things:
- In case the ontological graph contains custom rdf:List (which does not belong to any axiom or part of axiom) the attempt to load and reload 
this ontology leads to its breakdown: the graph becomes not valid for loading (even through Jena) at all.
- At least for Turtle there is no supporting for the nested bulky annotations of ontology header (only plain annotation assertions are supported while read and write).
- Some of the axioms (at least DifferentIndividuals) can not be annotated at all. 
This means that after reloading ontology (at least as Turtle) there is no annotations for such axioms.
- In generally each OWL-parser and OWL-storer has its own implementation, 
so after reloading in different formats you can expect that list of axioms has been changed, and as mentioned above sometimes with corrupted information. 
Some of storers/parses can't work with annotated declarations, some of them ignore declaration-axioms fully or partially (e.g. for NamedIndividuals), etc.

## Concept
The principle of ONT-API is that all information is kept and remains in the graph (which are not necessary stored in memory), 
and if a set of triples from this graph corresponds to the axiom, then we can read them in the form of this axiom (i.e. in the structural view). 
Similarly, the writing of axioms goes in triplet form only: axioms are not stored in separately, if not to take into account the cache.
In the ONT-API reading and writing an ontology from a file or a stream happen through Jena,
however, the original OWL-API mechanisms to read/write also remain working, 
and even are used explicitly if the data format is not supported by Jena (e.g. Functional Syntax, Manchester Syntax, OWL/RDF, etc).
ONT-API supports all OWL-API features and options, but they are somewhat expanded. 
Instead of the original OWL-API interfaces in ONT-API there are overridden with several additional methods.
Also there are new configuration options and the policy with exceptions has been changed a little.
Nevertheless, it is always possible to use the original OWL-API or its parts in conjunction with ONT-API, 
for example, you can copy an ontology from the ONT-API manager to the OWL-API manager and vice versa.
The project contains tests from the OWL-API-contract, which show the working capacity of ONT-API. 
Also there are ONT-API specific tests.

## Structure (briefly)
* The root of project (package __ru.avicomp.ontapi__) contains the core classes. Some of them are:  
    * _OntManagers_  - the access point to the ONT-API and OWL-API
    * _OntologyManager_ is an extended __org.semanticweb.owlapi.model.OWLOntologyManager__ with access to overridden _OWLOntology_. 
    * _OntologyModel_ is an extended __org.semanticweb.owlapi.model.OWLOntology__ with methods to get Jena (Graph) Model shadow and method to manage axioms cache.
    * _OntFactoryImpl_ is an extended __org.semanticweb.owlapi.model.OWLOntologyFactory__, the point to create and load ontologies.
    * _OntFormat_ - the bridge between __org.apache.jena.riot.Lang__ and __org.semanticweb.owlapi.model.OWLDocumentFormat__.
* Package __ru.avicomp.ontapi.internal__ contains Axiom Translators (for reading and writing axioms to/from graph), technical interfaces and some helpers. Some of the important classes are:
    * _AxiomTranslator_ - it is the base for any axiom translator. There are 39 axiom translators, each of them corresponds to the particular axiom-type.
    * _InternalModel_  - it is the buffer between graph-triple and structural representation.
* Package __ru.avicomp.ontapi.config__ contains classes to work with project settings. Main classes:
    * _OntConfig_ it is overridden  __org.semanticweb.owlapi.model.OntologyConfigurator__, global manager config and builder 
for _OntLoaderConfiguration_ and _OntWriterConfiguration_.
    * _OntLoaderConfiguration_ - overridden __org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration__ with new ONT-API options to manage axiom reading.
    * _OntWriterConfiguration_ - overridden __org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration__ with new ONT-API options to manage axiom whiting.
* Package __ru.avicomp.ontapi.jena__ is a separated subsystem with _ru.avicomp.ontapi.jena.model.OntGraphModel_ inside which 
is analogue of __org.apache.jena.ontology.OntModel__, but for OWL-2. This subsystem is a core of ONT-API and can be used autonomously. 
Some of the basic components are:
    * _ru.avicomp.ontapi.jena.model.OntGraphModel_ is an extended __org.apache.jena.rdf.model.Model__, the facade and wrapper to the Graph.
    * _ru.avicomp.ontapi.jena.model.OntStatement_ is an extended __org.apache.jena.rdf.model.Statement__, which is linked to the ont-graph-model. 
    * _ru.avicomp.ontapi.jena.model.OntObject_. It is our analogue of __org.apache.jena.ontology.OntResource__, 
    the basis of any OWL (jena) objects, which are also contained in _model_ package.
    * _ru.avicomp.ontapi.jena.model.OntEntity_. It is an _OntObject_ for named resources (OWL-entities): class, datatype, 
    annotation property, data property, object property and named individual.
    * _ru.avicomp.ontapi.jena.impl.configuration.OntPersonality_ is an extended __org.apache.jena.enhanced.Personality__, 
    the interface/implementation mapping, which provides a kind of polymorphism on Jena resources. Using this mechanism we solve the problem of 'illegal punnings'.
    * _ru.avicomp.ontapi.jena.UnionGraph_ is our analogue of __org.apache.jena.graph.compose.MultiUnion__.
* Package __ru.avicomp.ontapi.transforms__ is a small separated subsystem also. 
It puts in order any graph (RDFS, OWL1) before using the main API. Here are two main classes:
    * _Transform_ - it is the abstract superclass for any graph-converter. 
    * _GraphTransformers.Store_ - it is the transforms storage and point to access to any of them.

## What's next (todo)
There are a lot of 'TODO's across the code, they must be fixed or removed.
Also it seems obvious that in the some next version of ONT-API the binding to the original OWL-API should be eliminated, 
perhaps the used code might be moved from dependencies as a submodule. Also the Jena version should be raised.

## Contacts