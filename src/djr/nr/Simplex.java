package djr.nr;

import djr.util.array.*;

/**
 * Class <code>Simplex</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public abstract class Simplex {
   public int ndims, mdims;
   public NDFunction points[];
   public double yvals[];

   public Simplex() { };

   /* Create an N-dimensinal simplex (with n+1 N-dim points as vertices) */
   public Simplex( int ndims ) {
      initialize( ndims );
   }

   /* Create a new set of vertices. */
   protected void initialize( int ndims ) {
      this.ndims = ndims;
      mdims = ndims + 1;
      yvals = DoubleUtils.New( mdims );
      points = new NDFunction[ mdims ];
      for ( int i = 0; i < mdims; i ++ ) points[ i ] = createVertex( points[ i ] );
   }

   /* Create a vertex (not implemented) */
   public abstract NDFunction createVertex( NDFunction inout );

   /* Evaluate all vertex function into the yvals array. */
   public void evaluate() {
      for ( int i = 0; i < mdims; i ++ ) yvals[ i ] = evaluate( points[ i ] );
   }

   /* Evaluate a vertex. */
   public double evaluate( Function f ) { return f.evaluate(); }
}
