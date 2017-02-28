package ru.avicomp.ontapi.utils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;

/**
 * Utils to work with io.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SameParameterValue"})
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

    public static String toString(OWLOntology ontology, OntFormat format) {
        return toString(ontology, format == null ? new TurtleDocumentFormat() : format.createOwlFormat());
    }

    public static String toString(OWLOntology ontology, OWLDocumentFormat format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (OWLOntologyStorageException | IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String toString(Model model, OntFormat ext) {
        return toStringWriter(model, ext).toString();
    }

    public static StringWriter toStringWriter(Model model, OntFormat ext) {
        StringWriter sw = new StringWriter();
        model.write(sw, (ext == null ? OntFormat.TURTLE : ext).getID(), null);
        return sw;
    }

    public static InputStream toInputStream(String txt) {
        try {
            return IOUtils.toInputStream(txt, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static InputStream toInputStream(Model model, OntFormat ext) {
        return toInputStream(toString(model, ext));
    }

    public static InputStream toInputStream(OWLOntology model, OntFormat ext) {
        return toInputStream(toString(model, ext));
    }

    public static Model loadFromString(String input, OntFormat ext) {
        Model m = ModelFactory.createDefaultModel();
        m.read(toInputStream(input), null, ext.getID());
        return m;
    }

    public static Model loadResourceTTLFile(String file) {
        return load(getResourceURI(file), null);
    }

    public static Model loadOutTTLFile(String file) {
        return load(getOutURI(file), null);
    }

    public static Model load(URI file, OntFormat f) {
        String format = f == null ? "ttl" : f.getID();
        Model m = ModelFactory.createDefaultModel();
        LOGGER.debug("Load model from " + file);
        try (InputStream in = file.toURL().openStream()) {
            m.read(in, null, format);
            return m;
        } catch (IOException e) {
            LOGGER.fatal("Can't read model", e);
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        String dir;
        String name;
        if (file.contains("/")) {
            name = file.replaceAll(".+/([^/]+$)", "$1");
            dir = file.replace(name, "");
        } else {
            dir = "";
            name = file;
        }
        return getResourceURI(dir, name);
    }

    public static URI getOutURI(String file) {
        return new File(DESTINATION_DIR, file).toURI();
    }

    public static void save(Model model, String name, OntFormat type) {
        File dst = getFileToSave(name, type);
        LOGGER.debug("Save model to " + dst.toURI() + " (" + type.getID() + ")");
        try (FileWriter out = new FileWriter(dst)) {
            model.write(out, type.getID());
        } catch (IOException e) {
            LOGGER.fatal("Unable to save model " + name, e);
        }
    }

    public static void save(OWLOntology ontology, String name, OntFormat type) {
        File dst = getFileToSave(name, type);
        LOGGER.debug("Save owl-ontology to " + dst.toURI() + " (" + (type == null ? "TURTLE" : type.getID()) + ")");
        OWLDocumentFormat format = type == null ? new TurtleDocumentFormat() : type.createOwlFormat();
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

    public static OWLOntology loadOWLOntology(IRI fileIRI) {
        return loadOWLOntology(OntManagerFactory.createOWLManager(), fileIRI);
    }

    public static OntologyModel loadOntologyModel(IRI fileIRI) {
        return (OntologyModel) loadOWLOntology(OntManagerFactory.createONTManager(), fileIRI);
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model, OntFormat convertFormat) {
        String uri = TestUtils.getURI(model);
        LOGGER.info("Put ontology " + uri + "(" + convertFormat + ") to manager.");
        try (InputStream is = toInputStream(model, convertFormat == null ? OntFormat.TURTLE : convertFormat)) {
            return manager.loadOntologyFromOntologyDocument(is);
        } catch (IOException | OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntology convertJenaToOWL(Model model) {
        return convertJenaToOWL(null, model);
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model) {
        if (manager == null) manager = OntManagerFactory.createOWLManager();
        return convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }

    public static OntologyModel convertJenaToONT(Model model) {
        return convertJenaToONT(null, model);
    }

    public static OntologyModel convertJenaToONT(OntologyManager manager, Model model) {
        if (manager == null) manager = OntManagerFactory.createONTManager();
        return (OntologyModel) convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }
}
