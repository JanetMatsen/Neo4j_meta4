
import org.neo4j.*;
import com.besil.neo4jsna.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class AuthorDemo{
    public static void main(){
        String path = "data/tmp/cineasts";

        // Open a database instance
        GraphDatabaseService g = new GraphDatabaseFactory().newEmbeddedDatabase(path);

        // Declare the GraphAlgoEngine on the database instance
        GraphAlgoEngine engine = new GraphAlgoEngine(g);

        LabelPropagation lp = new LabelPropagation();
        // Starts the algorithm on the given graph g
        engine.execute(lp);
        Long2LongMap communityMap = lp.getResult();
        long totCommunities = new LongOpenHashSet( communityMap.values() ).size();
        System.out.println("There are "+totCommunities+" communities according to Label Propagation");

        //DirectedModularity modularity = new DirectedModularity(g);
        //engine.execute(modularity);
        //System.out.println("The directed modularity of this network is "+modularity.getResult());

        //UndirectedModularity umodularity = new UndirectedModularity(g);
        //engine.execute(umodularity);
        //System.out.println("The undirected modularity of this network is "+umodularity.getResult());

        //TriangleCount tc = new TriangleCount();
        //engine.execute(tc);
        //Long2LongMap triangleCount = tc.getResult();
        //Optional<Long> totalTriangles = triangleCount.values().stream().reduce( (x, y) -> x + y );
        //System.out.println("There are "+totalTriangles.get()+" triangles");

        //PageRank pr = new PageRank(g);
        //engine.execute(pr);
        //Long2DoubleMap ranks = pr.getResult();
        //Optional<Double> res = ranks.values().parallelStream().reduce( (x, y) -> x + y );
        //System.out.println("Check PageRank sum is 1.0: "+ res.get());

        ConnectedComponents cc = new ConnectedComponents();
        engine.execute(cc);
        Long2LongMap components = cc.getResult();
        int totalComponents = new LongOpenHashSet( components.values() ).size();
        System.out.println("There are "+ totalComponents+ " different connected components");

        StronglyConnectedComponents scc = new StronglyConnectedComponents();
        engine.execute(cc);
        components = scc.getResult();
        totalComponents = new LongOpenHashSet( components.values() ).size();
        System.out.println("There are "+ totalComponents+ " different strongly connected components");

        // Don't forget to shutdown the database
        g.shutdown();
    }
}