
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

public class ConstructBinaryNetwork {
    // Note: don't have to run the data-building query each time. 
    public static void main(String[] args) throws IOException{
        // can't run for eclipse if cutoff is specified by args[0]
        double cutoff = Double.parseDouble(args[0]);
        //double cutoff = 0.02;  //testing in Eclipse only

        String cutoff_string = String.valueOf(cutoff);
        System.out.println("Cutoff as string: " + cutoff_string);

        String dbpath = String.format("../data_mining_Neo4j_v2_3_2/databases/db_binary_" + cutoff_string);
        System.out.println("dbpath: " + dbpath);

        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(dbpath);
        ExecutionEngine execEngine = new ExecutionEngine(g, StringLogger.SYSTEM);

        // First delete whatever is there before
        String delete_query = "MATCH (n) \n OPTIONAL MATCH (n)-[r]-() \n DELETE n,r";
        ExecutionResult execResultDelete = execEngine.execute(delete_query);

        String querypath = "../data_mining_Neo4j_v2_3_2/queries/load_2_organism_network--specify_cutoff.txt";
        String querystr = PrepBuildQuery(cutoff, querypath, dbpath);

        int n_nodes_before = count_nodes(g);
        int n_relationships_before = count_relationships(g);
        String message_before = String.format("Number of nodes, relationships (edges) before network construction: %d, %d", 
                n_nodes_before, n_relationships_before);
        System.out.println(message_before);

        long startTime = System.nanoTime();
        ExecutionResult execResult = execEngine.execute(querystr);
        double seconds = (System.nanoTime() - startTime)/1e9;
        System.out.println(String.format("Network construction time (seconds): %f", seconds));


        String results = execResult.dumpToString();

        int n_nodes_after = count_nodes(g);
        int n_relationships_after = count_relationships(g);
        String message_after = String.format("Number of nodes, relationships (edges) after network construction: %d, %d", 
                n_nodes_after, n_relationships_after);
        System.out.println(message_after);
        int n_nodes_added = n_nodes_after - n_nodes_before;
        System.out.println(String.format("Added %d nodes.", n_nodes_added));
        Double graph_density = 1.*n_relationships_after/n_nodes_after;
        System.out.println(String.format("Graph density: %f", graph_density));

        System.out.println("Shutting down database");
        g.shutdown();
        System.out.println("script over");
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
    
    public static int count_relationships(GraphDatabaseService g){
        // try looping through all the nodes
        // https://neo4j.com/docs/java-reference/current/javadocs/org/neo4j/graphdb/Transaction.html
        try ( Transaction tx = g.beginTx() )
        {
            int relationship_count = 0;
            for (Relationship relationship : GlobalGraphOperations.at(g).getAllRelationships()) {
                //System.out.println(node.getPropertyKeys());
                relationship_count += 1;
            }
            tx.success();
            return relationship_count;
        }

    }

    public static String PrepBuildQuery(double cutoff, String querypath, String dbpath) throws IOException{
        // Saving at: /Users/janet/Neo4j_meta4/data_mining_Neo4j_v2_3_2/databases
        System.out.println("Saving database to " + dbpath);

        String querystr_raw = new String(Files.readAllBytes(Paths.get(querypath)));
        System.out.println(querystr_raw);

        String querystr = String.format(querystr_raw, cutoff);

        System.out.println(querystr);
        return querystr;
    }
}