
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class ConnectedComponentsFinder {
    public static void main(String[] args) throws IOException{
        String dbpath = args[0] ;
        //String dbpath = "./databases/db_binary_0.010000" ;
        System.out.println("Finding connected components for " + dbpath);
        
        // Parse out the cutoff from the dbpath:
        Pattern p = Pattern.compile(".*db_binary_(.*)");
        Matcher m = p.matcher(dbpath);
        m.find();
        String cutoff = m.group(1);
        System.out.println("Cutoff for this database query: " + cutoff);
        
        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(dbpath);
        ExecutionEngine execEngine = new ExecutionEngine(g, StringLogger.SYSTEM);

        // Declare the GraphAlgoEngine on the database instance
        GraphAlgoEngine engine = new GraphAlgoEngine(g);

        // Find connected components
        ConnectedComponents cc = new ConnectedComponents();
        try{
            float startTimeCC = System.currentTimeMillis();
            engine.execute(cc);
            float estimatedTimeCC = (System.currentTimeMillis() - startTimeCC)/1000;
            Long2LongMap components = cc.getResult();
        
            int totalComponents = new LongOpenHashSet( components.values() ).size();
            System.out.println(String.format("Connected Components time (seconds): %f.  For cutoff = %s", estimatedTimeCC, cutoff));
            System.out.println("There are "+ totalComponents+ " different connected components for cutoff " + cutoff );
        }
        catch (Exception e) {
            System.out.println("Exception caught");
            System.out.println("Shutting down database");
            g.shutdown();
            throw e;
        }

        // Don't forget to shutdown the database
        System.out.println("Shutting down database");
        g.shutdown();
    }
}