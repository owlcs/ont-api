package ru.avicomp.ontapi.utils;

import org.apache.jena.util.FileManager;
import org.semanticweb.owlapi.model.IRI;

import ru.avicomp.ontapi.OntologyManager;

/**
 * Collection of all spin models (located in resources)
 * <p>
 * Created by szuev on 21.04.2017.
 */
public enum SpinModels {
    SP("/spin/sp.ttl", "http://spinrdf.org/sp"),
    SPIN("/spin/spin.ttl", "http://spinrdf.org/spin"),
    SPL("/spin/spl.spin.ttl", "http://spinrdf.org/spl"),
    SPIF("/spin/spif.ttl", "http://spinrdf.org/spif"),
    SPINMAP("/spin/spinmap.spin.ttl", "http://spinrdf.org/spinmap"),
    SMF("/spin/functions-smf.ttl", "http://topbraid.org/functions-smf"),
    FN("/spin/functions-fn.ttl", "http://topbraid.org/functions-fn"),
    AFN("/spin/functions-afn.ttl", "http://topbraid.org/functions-afn"),
    SMF_BASE("/spin/sparqlmotionfunctions.ttl", "http://topbraid.org/sparqlmotionfunctions"),
    SPINMAPL("/spin/spinmapl.spin.ttl", "http://topbraid.org/spin/spinmapl");

    private final String file, uri;

    SpinModels(String file, String uri) {
        this.file = file;
        this.uri = uri;
    }

    public static void addMappings(OntologyManager m) {
        for (SpinModels spin : values()) {
            m.getIRIMappers().add(FileMap.create(spin.getIRI(), spin.getFile()));
        }
    }

    public static void addMappings(FileManager fileManager) {
        for (SpinModels spin : values()) {
            fileManager.getLocationMapper().addAltEntry(spin.getIRI().getIRIString(), spin.getFile().toURI().toString());
        }
    }

    public IRI getIRI() {
        return IRI.create(uri);
    }

    public IRI getFile() {
        return IRI.create(ReadWriteUtils.getResourceURI(file));
    }
}
