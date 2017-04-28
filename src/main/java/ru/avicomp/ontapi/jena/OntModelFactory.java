package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.system.JenaSystem;

import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * Access point to {@link OntGraphModel}, common {@link Model} and {@link Graph}.
 * The our analogue of {@link org.apache.jena.rdf.model.ModelFactory}.
 * Please use it to avoid ExceptionInInitializerError during JenaSystem.init()
 * <p>
 * Created by szuev on 14.02.2017.
 */
public class OntModelFactory {

    static {
        // force init before any ont-model initializations here due to bug(?) in jena-arq-3.2.0 (upgrade 3.1.0 -> 3.2.0)
        // otherwise java.lang.ExceptionInInitializerError may occur.
        // to test (on 3.2.0) just run "new org.apache.jena.rdf.model.impl.ModelCom(null)" without (before) any JenaSystem.init();
        JenaSystem.init();
    }

    public static Graph createDefaultGraph() {
        return new GraphMem();
    }

    public static Model createDefaultModel() {
        return new ModelCom(createDefaultGraph());
    }

    public static OntGraphModel createModel() {
        return createModel(createDefaultGraph());
    }

    public static OntGraphModel createModel(Graph graph) {
        return createModel(graph, OntModelConfig.getPersonality());
    }

    public static OntGraphModel createModel(Graph graph, OntPersonality personality) {
        return new OntGraphModelImpl(graph, personality);
    }
}
