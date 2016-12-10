
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.cypher.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.tooling.GlobalGraphOperations;

public class ConstructNetwork50M {
    // Note: don't have to run the data-building query each time. 
    public static void main(String[] args) throws IOException{
        // can't run for eclipse if cutoff is specified by args[0]
        //double cutoff = Double.parseDouble(args[0]);
        double cutoff = 0.06;  //testing in Eclipse only
        String dbpath = String.format("../data_mining_Neo4j_v2_3_2/databases/db_50M_%f", cutoff);

        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(dbpath);
        ExecutionEngine execEngine = new ExecutionEngine(g, StringLogger.SYSTEM);

        // First delete whatever is there before
        String delete_query = "MATCH (n) \n OPTIONAL MATCH (n)-[r]-() \n DELETE n, r";
        ExecutionResult execResultDelete = execEngine.execute(delete_query);

        // Prep Neo4j database building query. 
        String querypath = "../data_mining_Neo4j_v2_3_2/queries/load_network--specify_cutoff.txt";
        String db_tsv_path = new java.io.File( "../data/50M_network/50M_network.tsv" ).getCanonicalPath();
        System.out.println("Path to network tsv: " + db_tsv_path);
        String querystr = PrepBuildQuery(db_tsv_path, cutoff, querypath, dbpath);

        // Count nodes, edges for reporting.  Should be 0 b/c of deletion. 
        int n_nodes_before = count_nodes(g);
        int n_relationships_before = count_relationships(g);
        String message_before = 
                String.format("Number of nodes, relationships (edges) before network construction: %d, %d",
                        n_nodes_before, n_relationships_before);
        System.out.println(message_before);

        // Track time for building the database. 
        long startTime = System.currentTimeMillis();
        ExecutionResult execResult = execEngine.execute(querystr);
        long estimatedTime = System.currentTimeMillis() - startTime;
        float seconds = (float) estimatedTime/1000;
        System.out.println(String.format("Network construction time (seconds): %f", seconds));

        // Print Neo4j results to console. 
        String results = execResult.dumpToString();

        // Count nodes, edges after db construction. 
        int n_nodes_after = count_nodes(g);
        int n_relationships_after = count_relationships(g);
        String message_after = 
                String.format("Number of nodes, relationships (edges) after network construction: %d, %d",
                        n_nodes_after, n_relationships_after);
        System.out.println(message_after);
        int n_nodes_added = n_nodes_after - n_nodes_before;
        System.out.println(String.format("Added %d nodes.", n_nodes_added));
        Double graph_density = 1.*n_relationships_after/n_nodes_after;
        System.out.println(String.format("Graph density: %f", graph_density));

        System.out.println("Shutting down database");
        g.shutdown();
    }

    public static int count_nodes(GraphDatabaseService g){
        // looping through all the nodes to count them.
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
        // loop through all the edges and count them.
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

    public static String PrepBuildQuery(String tsv_path, double cutoff, String querypath, String dbpath) throws IOException{
        // Saving at: /Users/janet/Neo4j_meta4/data_mining_Neo4j_v2_3_2/databases
        System.out.println("Saving database to " + dbpath);

        String querystr_raw = new String(Files.readAllBytes(Paths.get(querypath)));
        System.out.println(querystr_raw);

        String querystr = String.format(querystr_raw, tsv_path, cutoff);

        System.out.println(querystr);
        return querystr;
    }
}