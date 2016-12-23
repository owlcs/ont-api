package ru.avicomp.ontapi.utils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.*;

/**
 * Utils to work with io.
 *
 * Created by @szuev on 27.09.2016.
 */
public class ReadWriteUtils {
    private static final Logger LOGGER = Logger.getLogger(ReadWriteUtils.class);

    private static final String DESTINATION_DIR = "out";

    public static void print(OWLOntology ontology) {
        print(ontology, null);
    }

    public static void print(Model model) {
        print(model, null);
    }

    public static void print(OWLOntology ontology, OntFormat ext) {
        LOGGER.debug("\n" + toString(ontology, ext));
    }

    public static void print(Model model, OntFormat ext) {
        LOGGER.debug("\n" + toString(model, ext));
    }

    private static String toString(OWLOntology ontology, OntFormat type) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            OWLDocumentFormat format = type == null ? new TurtleDocumentFormat() : type.getOwlFormat();
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (OWLOntologyStorageException | IOException e) {
            throw new OntApiException(e);
        }
    }

    private static String toString(Model model, OntFormat ext) {
        return toStringWriter(model, ext).toString();
    }

    public static StringWriter toStringWriter(Model model, OntFormat ext) {
        StringWriter sw = new StringWriter();
        model.write(sw, (ext == null ? OntFormat.TTL_RDF : ext).getType(), null);
        return sw;
    }

    public static InputStream toInputStream(Model model, OntFormat ext) {
        try {
            return IOUtils.toInputStream(toString(model, ext), StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new OntApiException(e);
        }
    }

    public static Model loadFromTTL(String file) {
        return load(getResourceURI(file), null);
    }

    public static Model load(URI file, OntFormat f) {
        String format = f == null ? "ttl" : f.getType();
        Model m = ModelFactory.createDefaultModel();
        LOGGER.debug("Load model from " + file);
        try (InputStream in = file.toURL().openStream()) {
            m.read(in, null, format);
            return m;
        } catch (IOException e) {
            LOGGER.fatal("Can't read model", e);
            throw new OntApiException(e);
        }
    }

    public static OntModel loadJenaOntModel(OntModelSpec spec, File file, OntFormat f) {
        return ModelFactory.createOntologyModel(spec, load(file.toURI(), f));
    }

    private static File getFileToSave(String name, OntFormat type) {
        File dir = new File(DESTINATION_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return new File(dir, name + (type != null ? "." + type.getExt() : ""));
    }

    public static File getResourceFile(String projectDirName, String fileName) throws URISyntaxException, FileNotFoundException {
        URL url = ReadWriteUtils.class.getResource(projectDirName.startsWith("/") ? projectDirName : "/" + projectDirName);
        if (url == null)
            throw new IllegalArgumentException("Can't find project " + projectDirName + ".");
        File dir = new File(url.toURI());
        LOGGER.debug("Directory: " + dir);
        File res = new File(dir, fileName);
        if (!res.exists()) throw new FileNotFoundException(fileName);
        return res;
    }

    public static File getResourceFile(String fileName) {
        try {
            return getResourceFile("", fileName);
        } catch (URISyntaxException | FileNotFoundException e) {
            LOGGER.fatal(e);
        }
        return null;
    }

    public static URI getResourceURI(String dir, String file) {
        try {
            return getResourceFile(dir, file).toURI();
        } catch (URISyntaxException | FileNotFoundException e) {
            LOGGER.fatal(e);
        }
        return null;
    }

    public static URI getResourceURI(String file) {
        return getResourceURI("", file);
    }

    public static URI getOutURI(String file) {
        return new File(DESTINATION_DIR, file).toURI();
    }

    public static void save(Model model, String name, OntFormat type) {
        File dst = getFileToSave(name, type);
        LOGGER.debug("Save model to " + dst.toURI() + " (" + type.getType() + ")");
        try (FileWriter out = new FileWriter(dst)) {
            model.write(out, type.getType());
        } catch (IOException e) {
            LOGGER.fatal("Unable to save model " + name, e);
        }
    }

    public static void save(OWLOntology ontology, String name, OntFormat type) {
        File dst = getFileToSave(name, type);
        LOGGER.debug("Save owl-ontology to " + dst.toURI() + " (" + (type == null ? "TURTLE" : type.getType()) + ")");
        OWLDocumentFormat format = type == null ? new TurtleDocumentFormat() : type.getOwlFormat();
        try (FileOutputStream out = new FileOutputStream(dst)) {
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
        } catch (OWLOntologyStorageException | IOException e) {
            LOGGER.fatal("Unable to print owl-ontology " + ontology, e);
        }
    }

    public static OWLOntology loadOWLOntology(OWLOntologyManager manager, IRI fileIRI) {
        LOGGER.info("Load ontology model from " + fileIRI + ".");
        OWLOntology owl = null;
        try {
            owl = manager.loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            Assert.fail(e.getMessage());
        }
        return owl;
    }

    public static OntologyModel loadOntologyFromIOStream(OntologyManager manager, Model model, OntFormat convertFormat) {
        if (manager == null) manager = OntManagerFactory.createONTManager();
        return (OntologyModel) loadOWLOntologyFromIOStream(manager, model, convertFormat);
    }

    public static OWLOntology loadOWLOntologyFromIOStream(OWLOntologyManager manager, Model model, OntFormat convertFormat) {
        String uri = TestUtils.getURI(model);
        LOGGER.info("Put ontology " + uri + "(" + convertFormat + ") to manager.");
        try (InputStream is = toInputStream(model, convertFormat == null ? OntFormat.TTL_RDF : convertFormat)) {
            manager.loadOntologyFromOntologyDocument(is);
        } catch (IOException | OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
        OWLOntology res = manager.getOntology(IRI.create(uri));
        Assert.assertNotNull("Can't find ontology " + uri, res);
        return res;
    }

    public static OntologyModel loadOntologyFromIOStream(Model model) {
        return loadOntologyFromIOStream(null, model, null);
    }
}
