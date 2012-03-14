package org.broadinstitute.sting.gatk.walkers.haplotypecaller;

import org.broadinstitute.sting.utils.Haplotype;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ebanks
 * Date: Mar 14, 2011
 */
public class SimpleDeBruijnAssembler extends LocalAssemblyEngine {

    // the additional size of a valid chunk of sequence, used to string together k-mers
    private static final int KMER_OVERLAP = 6;
    private static final int PRUNE_FACTOR = 3;
    private static final int NUM_BEST_PATHS_PER_KMER = 12;
    
    private final boolean DEBUG;

    // the deBruijn graph object
    private final ArrayList<DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge>> graphs = new ArrayList<DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge>>();

    public SimpleDeBruijnAssembler( final boolean debug ) {
        super();
        DEBUG = debug;
    }

    public ArrayList<Haplotype> runLocalAssembly(final ArrayList<GATKSAMRecord> reads, final Haplotype refHaplotype) {
        // create the graphs
        createDeBruijnGraphs(reads);

        // prune singleton paths off the graph
        pruneGraphs();

        // find the best paths in the graphs
        return findBestPaths( refHaplotype );
    }

    private void createDeBruijnGraphs(final ArrayList<GATKSAMRecord> reads) {
        graphs.clear();
        // create the graph
        for( int kmer = 7; kmer <= 101; kmer += 8 ) {
            final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph = new DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge>(DeBruijnEdge.class);
            createGraphFromSequences( graph, reads, kmer );
            graphs.add(graph);
        }
    }

    private void pruneGraphs() {
        for( final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph : graphs ) {
            for( final DeBruijnVertex v : graph.vertexSet() ) {
                // If node has more than one outgoing path and some of those paths have very low weight then prune the low weight path
                if( graph.outDegreeOf(v) > 1 ) {
                    ArrayList<DeBruijnEdge> edgesToRemove = new ArrayList<DeBruijnEdge>();
                    for( final DeBruijnEdge e : graph.outgoingEdgesOf(v) ) {
                        if( e.getMultiplicity() < PRUNE_FACTOR ) {
                            edgesToRemove.add(e);
                        }
                    }
                    if( edgesToRemove.size() == graph.outDegreeOf(v) ) { // don't want to remove all the edges if they all have a score of 1
                        edgesToRemove.remove(0);
                    }
                    graph.removeAllEdges( edgesToRemove );
                }
            }
            if( !graph.vertexSet().isEmpty() ) {
                ArrayList<DeBruijnVertex> verticesToRemove = new ArrayList<DeBruijnVertex>();
                verticesToRemove.add(graph.vertexSet().iterator().next());
                while( !verticesToRemove.isEmpty() ) {
                    verticesToRemove.clear();
                    // We might have created new low weight root nodes in the graph, so need to prune those as well, iteratively
                    for( final DeBruijnVertex v : graph.vertexSet() ) {
                        if( graph.outDegreeOf(v) == 1 && graph.outgoingEdgesOf(v).iterator().next().getMultiplicity() < PRUNE_FACTOR ) {
                            graph.removeEdge(graph.outgoingEdgesOf(v).iterator().next());
                            verticesToRemove.add(v);
                        }
                    }
                    graph.removeAllVertices( verticesToRemove );
                }
            }
        }
    }

    private static void createGraphFromSequences( final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph, final ArrayList<GATKSAMRecord> reads, final int KMER_LENGTH ) {
        for ( final GATKSAMRecord read : reads ) {
            final byte[] sequence = read.getReadBases();
            if( sequence.length > KMER_LENGTH + KMER_OVERLAP ) {
                final int kmersInSequence = sequence.length - KMER_LENGTH + 1;
                for (int i = 0; i < kmersInSequence - 1; i++) {
                    // get the kmers
                    final byte[] kmer1 = new byte[KMER_LENGTH];
                    System.arraycopy(sequence, i, kmer1, 0, KMER_LENGTH);
                    final byte[] kmer2 = new byte[KMER_LENGTH];
                    System.arraycopy(sequence, i+1, kmer2, 0, KMER_LENGTH);

                    addEdgeToGraph( graph, kmer1, kmer2 );

                    // TODO -- eventually, we'll need to deal with reverse complementing the sequences (???)
                }
            }
        }
    }

    private static void addEdgeToGraph( final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph, final byte[] kmer1, final byte[] kmer2 ) {

        final DeBruijnVertex v1 = addToGraphIfNew( graph, kmer1 );
        final DeBruijnVertex v2 = addToGraphIfNew( graph, kmer2 );

        final Set<DeBruijnEdge> edges = graph.outgoingEdgesOf(v1);
        DeBruijnEdge targetEdge = null;
        for ( DeBruijnEdge edge : edges ) {
            if ( graph.getEdgeTarget(edge).equals(v2) ) {
                targetEdge = edge;
                break;
            }
        }

        if ( targetEdge == null )
            graph.addEdge(v1, v2, new DeBruijnEdge());
        else
            targetEdge.setMultiplicity(targetEdge.getMultiplicity() + 1);
    }

    private static DeBruijnVertex addToGraphIfNew( final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph, final byte[] kmer ) {

        // the graph.containsVertex() method is busted, so here's a hack around it
        final DeBruijnVertex newV = new DeBruijnVertex(kmer);
        for ( final DeBruijnVertex v : graph.vertexSet() ) {
            if ( v.equals(newV) )
                return v;
        }

        graph.addVertex(newV);
        return newV;
    }

    private ArrayList<Haplotype> findBestPaths( final Haplotype refHaplotype ) {
        final ArrayList<Haplotype> returnHaplotypes = new ArrayList<Haplotype>();
        int numHaplotypes = 0;
        returnHaplotypes.add( refHaplotype );

        for( final DefaultDirectedGraph<DeBruijnVertex, DeBruijnEdge> graph : graphs ) {
            final ArrayList<KBestPaths.Path> bestPaths = KBestPaths.getKBestPaths(graph, NUM_BEST_PATHS_PER_KMER);
            for ( final KBestPaths.Path path : bestPaths ) {
                final Haplotype h = new Haplotype( path.getBases( graph ), path.getScore() );
                if( h.getBases() != null ) {
                    if( h.getBases().length >= refHaplotype.getBases().length ) { // the haplotype needs to be at least as big as the active region
                        if( !returnHaplotypes.contains(h) ) { // no reason to add a new haplotype if the bases are the same as one already present
                            returnHaplotypes.add( h );
                        }
                    }
                }
            }
        }

        if( DEBUG ) { 
            System.out.println("Found " + returnHaplotypes.size() + " candidate haplotypes to evaluate:");
            for( final Haplotype h : returnHaplotypes ) {
                System.out.println( new String(h.getBases()) );
            }
        }

        return returnHaplotypes;
    }

}
