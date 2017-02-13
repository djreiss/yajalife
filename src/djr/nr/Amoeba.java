package djr.nr;
import djr.util.MyUtils;
import djr.util.array.*;

/**
 * Class <code>Amoeba</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Amoeba extends Simplex {
   public int ihi, ilo, inhi, NMAX = Integer.MAX_VALUE, niterWorse = 0;
   public double rtol, yb = Double.MAX_VALUE;
   public NDFunction startPt, psum, ptry, pb;
   public boolean initializing = false, finished = false;

   /* Must supply an NDFunction starting point for the search. */
   public Amoeba( NDFunction start ) {
      this.startPt = start;
      initialize( start.ndims );
      psum = createVertex( null );
      ptry = createVertex( null );      
      pb = createVertex( null );
   }

   /* Set the maximum number of allowable function (fitness measurement) calls. */
   public Amoeba setMaximumFunctionCalls( int max ) { NMAX = max; return this; }

   /* Override for custom evaluation of doneness. */
   public boolean finished() { return finished; }

   /* Stop the run. */
   public void stop() { finished = true; }

   /* Override for when we get a better set of parameters than what we've seen before. */
   public void gotABetterOne( Function f, double val ) { niterWorse = 0; }

   /* Override for when we evaluate a set of parameters that's not any better than one
      that we've seen before. */
   public void gotANotBetterOne( Function f, double val ) { niterWorse ++; }

   /* Create a new vertex (as a variation on the starting point supplied to the 
      constructor. inout may or may not be null. */
   public NDFunction createVertex( NDFunction inout ) {
      if ( inout == null ) inout = (NDFunction) startPt.duplicate();
      for ( int i = 0; i < ndims; i ++ )
	 inout.params[ i ] = startPt.params[ i ] + 
	    ( DoubleUtils.Random() - 0.5 ) * 2.0 * startPt.params[ i ] / 10.0;
      return inout;
   }

   public void evaluate() {
      initializing = true;
      super.evaluate();
      initializing = false;
   }

   public boolean isInitializing() {
      return initializing;
   }

   /* Evaluate a vertex. */
   public double evaluate( Function f ) { 
      double out = f.evaluate(); 
      if ( out < yb ) gotABetterOne( f, out );
      else gotANotBetterOne( f, out );
      return out;
   }

   /* Run the amoeba to a given tolerance, and repeat a number of times. */
   public NDFunction execute( int repeat, double tol ) throws NRException {
      evaluate(); // Initialize all the points
      niterWorse = 0;
      yb = Double.MAX_VALUE;
      for ( int i = 0; i < repeat; i ++ ) {
	 if ( i > 0 ) for ( int j = 0; j < mdims; j ++ ) 
	    points[ j ] = createVertex( points[ j ] );
	 startPt = (NDFunction) execute( tol ).duplicate();
      }
      return startPt;
   }
      
   /* Multidimensional minimization of the function funk(x) where
      x[0..ndim-1] is a vector in ndim dimensions, by the downhill
      simplex method of Nelder and Mead. The matrix p[0..ndim]
      [0..ndim-1] is input. Its ndim+1 rows are ndim-dimensional
      vectors which are the vertices of the starting simplex. Also
      input is the vector y[0..ndim], whose components must be pre-
      initialized to the values of funk evaluated at the ndim+1
      vertices (rows) of p; and tol the fractional convergence
      tolerance to be achieved in the function value (n.b.!). On
      output, p and y will have been reset to ndim+1 new points all
      within tol of a minimum function value, and returns the
      number of function evaluations taken. */
   /* Run the amoeba to a given tolerance and return. */
   public NDFunction execute( double tol ) throws NRException {
      int nfunk = 0;
      fillPSUM();

      while( true ) {
	 getExtremePoints();	 
	 rtol = getSize();

	 //System.err.println("HERE: "+rtol+" "+tol+" "+nfunk);
	 if ( finished() || rtol < tol ) 
	    return points[ ilo ]; // If finished, return best point. 
	 if ( nfunk >= NMAX ) throw new NRException( "NMAX exceeded" );
	 nfunk += 2;
	 /* Begin a new iteration. First extrapolate by a factor 1 through the face of the 
	    simplex across from the high point, i.e., re ect the simplex from the 
	    high point. */
	 double ytry = amotry( -1.0 );
	 /* Gives a result better than the best point, so try an additional
	    extrapolation by a factor 2. */ 
	 if ( ytry <= yvals[ ilo ] ) ytry = amotry( 2.0 );
	 /* The reflected point is worse than the second-highest, 
	    so look for an intermediate lower point, i.e., do a
	    one-dimensional contraction. */
	 else if ( ytry >= yvals[ inhi ] ) { 
	    double ysave = yvals[ ihi ];
	    ytry = amotry( 0.5 );
	    /* Can't seem to get rid of that high point. Better 
	       contract around the lowest (best) point. */
	    if ( ytry >= ysave ) {
	       for ( int i = 0; i < mdims; i ++ ) {
		  if ( i != ilo ) { 
		     for (int j = 0; j < ndims; j ++ ) 
			points[ i ].params[ j ] = //psum.params[ j ] =
			   0.5 * ( points[ i ].params[ j ] + points[ ilo ].params[ j ] );
		     yvals[ i ] = evaluate( points[ i ]/*psum*/ );
		  } 
	       } 
	       getExtremePoints();
	       nfunk ++; //= ndims; /* Keep track of function evaluations. */
	       fillPSUM(); /* Recompute psum. */
	    } 
	 } else -- nfunk; /* Correct the evaluation count. */
      } /* Go back for the test of doneness and the next iteration. */
   }

   /* Determine which point is the highest (worst), next-highest, and 
      lowest (best), by looping over the points in the simplex. */
   public void getExtremePoints() {
      ilo = 0;
      if ( yvals[ 0 ] > yvals[ 1 ] ) { inhi = 1; ihi = 0; }
      else { inhi = 0; ihi = 1; }
      for ( int i = 0; i < mdims; i ++ ) { 
	 if ( yvals[ i ] <= yvals[ ilo ] ) ilo = i;
	 if ( yvals[ i ] > yvals[ ihi ] ) { 
	    inhi = ihi;
	    ihi = i;
	 } else if ( yvals[ i ] > yvals[ inhi ] && i != ihi ) inhi=i;
      } 
   }

   final static double TINY = 1.0e-10; // A small number. 
   
   /* Compute the fractional range from highest to lowest. */
   public double getSize() {
      return 2.0 * Math.abs( yvals[ ihi ] - yvals[ ilo ] ) /
	 ( Math.abs( yvals[ ihi ] ) + Math.abs( yvals[ ilo ] ) + TINY );
   }

   protected void fillPSUM() {
      for ( int j = 0; j < ndims ; j ++ ) {
	 double sum = 0.0;
	 for ( int i = 0; i < mdims; i ++ ) sum += points[ i ].params[ j ];
	 psum.params[ j ] = sum;
      }
   }

   /* Extrapolate by a factor fac through the face of the simplex
      across from the high point, try it, and replace the high
      point if the new point is better. */
   protected double amotry( double fac ) throws NRException {
      double fac1 = ( 1.0 - fac ) / ndims;
      double fac2 = fac1 - fac;
      for ( int j = 0; j < ndims; j ++ ) 
	 ptry.params[ j ] = psum.params[ j ] * fac1 - points[ ihi ].params[ j ] * fac2;
      double ytry = evaluate( ptry ); /* Evaluate the function at the trial point. */ 
      if ( ytry <= yb ) {
	 for ( int j = 0; j < ndims; j ++ ) pb.params[ j ] = ptry.params[ j ];
	 yb = ytry;
      }
      if ( ytry < yvals[ ihi ] ) { /* If it's better than the highest, then */
	 yvals[ ihi ] = ytry; /* replace the highest. */
	 for (int j = 0; j < ndims; j ++ ) { 
	    psum.params[ j ] += ptry.params[ j ] - points[ ihi ].params[ j ];
	    points[ ihi ].params[ j ] = ptry.params[ j ];
	 } 
      } 
      return ytry;
   }

}
