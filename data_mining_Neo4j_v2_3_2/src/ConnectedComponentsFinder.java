
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

import com.besil.neo4jsna.algorithms.ConnectedComponents;
import com.besil.neo4jsna.engine.GraphAlgoEngine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class ConnectedComponentsFinder {
    public static void main(String[] args) throws IOException{
        //String dbpath = args[0] ;
        String dbpath = "./databases/db_binary_0.030000" ;
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
            System.out.println("Exception caught during connected components assessment.");
            System.out.println("Shutting down database");
            g.shutdown();
            throw e;
        }

        // Dump to CSV by iterating over nodes
        try{
            try ( Transaction tx = g.beginTx() )
            {
                int loop_no = 0;


                for (Node node : GlobalGraphOperations.at(g).getAllNodes()) {

                    loop_no += 1;
                    if(loop_no == 1){
                        // Doesn't seem to be printing headers.
                        for (String key : node.getPropertyKeys()) {
                            System.out.print("\"" + node.getProperty(key) + "\"");
                            System.out.print(", ");
                        }
                    }

                    //System.out.println(node.getPropertyKeys());
                    //System.out.println("nodesey");
                    //System.out.println(node.getLabels());  // [Gene]
                    //System.out.println(node.getPropertyKeys() );  // [locus_tag, organism, gene, gene_product, ConnectedComponents]

                    // Iterate over [locus_tag, organism, gene, gene_product, ConnectedComponents]
                    // Print the field if it's filled, or some type of null if empty.
                    //System.out.println(node.getPropertyKeys().getClass().getName());
                    //String[] fields = new String["a"];
                    Iterable<String> fields = node.getPropertyKeys();
                    for (String field : fields) {
                        System.out.print("\"" + node.getProperty(field) + "\"");
                        System.out.print(", ");
                    }
                    System.out.print("\n");
                }
                tx.success();
            }
        }
        catch (Exception e) {
            System.out.println("Exception caught during CSV preparation.");
            System.out.println("Shutting down database");
            g.shutdown();
        }

        // Don't forget to shutdown the database
        System.out.println("Shutting down database");
        g.shutdown();
    }
}