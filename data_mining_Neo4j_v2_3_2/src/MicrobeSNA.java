
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.neo4j.*;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

import com.besil.neo4jsna.*;
import com.besil.neo4jsna.algorithms.ConnectedComponents;
import com.besil.neo4jsna.algorithms.LabelPropagation;
import com.besil.neo4jsna.algorithms.PageRank;
import com.besil.neo4jsna.algorithms.StronglyConnectedComponents;
import com.besil.neo4jsna.algorithms.TriangleCount;
import com.besil.neo4jsna.engine.GraphAlgoEngine;
import com.besil.neo4jsna.measures.DirectedModularity;
import com.besil.neo4jsna.measures.UndirectedModularity;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class MicrobeSNA {
    public static void main(String[] args) throws IOException{
        String dbpath = "neo4j_db";
        // Note: don't have to run the data-building query each time. 
        String querypath = "/Users/janet/Neo4j_meta4/data_mining_Neo4j_v2_3_2/queries/load_2_organism_network--specify_cutoff.txt";
        String querystr_raw = new String(Files.readAllBytes(Paths.get(querypath)));
        System.out.println(querystr_raw);
        double cutoff = 0.05;
        String querystr = String.format(querystr_raw, cutoff);

        System.out.println(querystr);

        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(dbpath);
        ExecutionEngine execEngine = new ExecutionEngine(g, StringLogger.SYSTEM);

        long startTime = System.currentTimeMillis();
        ExecutionResult execResult = execEngine.execute(querystr);
        long estimatedTime = System.currentTimeMillis() - startTime;
        float seconds = (float) estimatedTime/1000;
        System.out.println(String.format("Seconds: %f", seconds));


        String results = execResult.dumpToString();

        // Declare the GraphAlgoEngine on the database instance
        GraphAlgoEngine engine = new GraphAlgoEngine(g);

        // try looping through all the nodes
        // https://neo4j.com/docs/java-reference/current/javadocs/org/neo4j/graphdb/Transaction.html
        try ( Transaction tx = g.beginTx() )
        {
            for (Node node : GlobalGraphOperations.at(g).getAllNodes()) {
                //System.out.println(node.getPropertyKeys());
                System.out.println("nodesey");
            }
            tx.success();
        }


        /*
        LabelPropagation lp = new LabelPropagation();
        // Starts the algorithm on the given graph g
        engine.execute(lp);
        Long2LongMap communityMap = lp.getResult();
        long totCommunities = new LongOpenHashSet( communityMap.values() ).size();
        System.out.println("There are "+totCommunities+" communities according to Label Propagation");
         */
        /*  Couldn't get these Modularity things to work, but perhaps not a problem!
        DirectedModularity modularity = new DirectedModularity(g);
        engine.execute(modularity);
        System.out.println("The directed modularity of this network is "+modularity.getResult());

        UndirectedModularity umodularity = new UndirectedModularity(g);
        engine.execute(umodularity);
        System.out.println("The undirected modularity of this network is "+umodularity.getResult());
         */

        /*
        TriangleCount tc = new TriangleCount();
        engine.execute(tc);
        Long2LongMap triangleCount = tc.getResult();
        Optional<Long> totalTriangles = triangleCount.values().stream().reduce( (x, y) -> x + y );
        System.out.println("There are "+totalTriangles.get()+" triangles");
         */

        /*
        PageRank pr = new PageRank(g);
        engine.execute(pr);
        Long2DoubleMap ranks = pr.getResult();
        Optional<Double> res = ranks.values().parallelStream().reduce( (x, y) -> x + y );
        System.out.println("Check PageRank sum is 1.0: "+ res.get());
         */

        
        ConnectedComponents cc = new ConnectedComponents();
        float startTimeCC = System.currentTimeMillis();
        engine.execute(cc);
        float estimatedTimeCC = (System.currentTimeMillis() - startTime)/1000;
        Long2LongMap components = cc.getResult();
        int totalComponents = new LongOpenHashSet( components.values() ).size();
        System.out.println(String.format("Connected Components time (seconds): %f.  For cutoff = %f", estimatedTimeCC, cutoff));
        System.out.println("There are "+ totalComponents+ " different connected components for cutoff " + cutoff );

        /*
        StronglyConnectedComponents scc = new StronglyConnectedComponents();
        engine.execute(cc);
        components = scc.getResult();
        totalComponents = new LongOpenHashSet( components.values() ).size();
        System.out.println("There are "+ totalComponents+ " different strongly connected components");
         */

        // Don't forget to shutdown the database
        System.out.println("Shutting down database");
        g.shutdown();
    }
}