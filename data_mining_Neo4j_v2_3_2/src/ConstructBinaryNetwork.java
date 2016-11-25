
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

public class ConstructBinaryNetwork {
    // Note: don't have to run the data-building query each time. 
    public static void main(String[] args) throws IOException{
        // can't run for eclipse if cutoff is specified by args[0]
        //double cutoff = Double.parseDouble(args[0]);
        double cutoff = 0.04;  //testing in Eclipse only

        // Saving at: /Users/janet/Neo4j_meta4/data_mining_Neo4j_v2_3_2/databases
        String dbpath = String.format("./databases/db_binary_%f", cutoff);
        System.out.println("Saving database to " + dbpath);

        String querypath = "/Users/janet/Neo4j_meta4/data_mining_Neo4j_v2_3_2/queries/load_2_organism_network--specify_cutoff.txt";
        String querystr_raw = new String(Files.readAllBytes(Paths.get(querypath)));
        System.out.println(querystr_raw);

        String querystr = String.format(querystr_raw, cutoff);

        System.out.println(querystr);

        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(dbpath);
        ExecutionEngine execEngine = new ExecutionEngine(g, StringLogger.SYSTEM);

        long startTime = System.currentTimeMillis();
        ExecutionResult execResult = execEngine.execute(querystr);
        long estimatedTime = System.currentTimeMillis() - startTime;
        float seconds = (float) estimatedTime/1000;
        System.out.println(String.format("Network construction time (seconds): %f", seconds));


        String results = execResult.dumpToString();

        // Declare the GraphAlgoEngine on the database instance
        GraphAlgoEngine engine = new GraphAlgoEngine(g);

        int n_nodes = count_nodes(g);
        String message = String.format("Number of nodes: %d", n_nodes);
        System.out.println(message);

        System.out.println("Shutting down database");
        g.shutdown();
    }

    public static int count_nodes(GraphDatabaseService g){
        // try looping through all the nodes
        // https://neo4j.com/docs/java-reference/current/javadocs/org/neo4j/graphdb/Transaction.html
        try ( Transaction tx = g.beginTx() )
        {
            int node_count = 0;
            for (Node node : GlobalGraphOperations.at(g).getAllNodes()) {
                //System.out.println(node.getPropertyKeys());
                node_count += 1;
            }
            tx.success();
            return node_count;
        }

    }
}