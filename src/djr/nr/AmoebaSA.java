package djr.nr;
import djr.util.MyUtils;
import djr.util.array.*;

/**
 * Class <code>AmoebaSA</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class AmoebaSA extends Amoeba {
   protected double temp, yhi, ylo, ynhi, tempTol = 0.1;
   protected int iter, niter = 100, nfunk = 0;
   protected boolean reallyStop = false, perturbed = false;

   /* Must supply an NDFunction starting point for the search. */
   public AmoebaSA( NDFunction start ) {
      super( start );
   }

   /* Update the temperature based on its old value and the # of iterations run so far */
   public double updateTemp( double oldTemp, int oldIters ) { return oldTemp * 0.9; }

   /* A new repetition is starting. Update the new starting temperature based on its old 
      value and the # of iterations run so far */
   public double updateTemp2( double oldTemp, int repeatIter ) { return oldTemp * 0.8; }

   /* iters = number of iterations at a given temperature */
   public AmoebaSA setNumItersPerRun( int iter ) { niter = iter; return this; }

   /* temp_tol = once temp falls to this fraction of the original, then an iteration
      is complete (and if repeat>0 it will repeat). */
   public AmoebaSA setTemperatureTol( double ttol ) { this.tempTol = ttol; return this; }

   /* Heat up the annealer. */
   public void perturb() { System.err.println("HERE: PERTURB"); finished = perturbed = true; }

   /* Really stop the damn thing! */
   public void stop() { System.err.println("HERE: STOP"); finished = reallyStop = true; }

   /* Run the SA to a given tolerance (starting at a given temperature), and repeat
      a number of times. */
   public NDFunction execute( int repeat, double tol, double temptr ) 
      throws NRException {
      yb = Double.MAX_VALUE;
      for ( int i = 0; i < repeat; i ++ ) {
	 startPt = execute( tol, temptr );
	 if ( reallyStop ) break;
	 if ( perturbed ) { finished = perturbed = false; }
	 if ( i < repeat-1 ) {
	    for ( int j = 0; j < mdims; j ++ ) points[ j ] = createVertex( points[ j ] );
	    temptr = updateTemp2( temptr, i ); // Restart SA with a lower starting temp
	 }
      }
      return startPt;
   }

   /* Run the SA to a given tolerance (starting at a given temperature), and return. */
   public NDFunction execute( double tol, double temptr ) throws NRException {
      evaluate(); // Initialize all the points 
      iter = -1;
      nfunk = 0;
      niterWorse = 0;
      finished = false;
      double limit = temptr * tempTol; // Stop this if temp drops by factor of 10
      while( ! finished() && iter < 0 && temptr >= limit ) {
	 startPt = (NDFunction) executeOne( tol, temptr ).duplicate();
	 if ( reallyStop || perturbed ) break;
	 temptr = updateTemp( temptr, iter );
      }
      return startPt;
   }
      
   /* Multidimensional minimization of the function funk(x) where
    * x[0..ndim-1] is a vector in ndim dimensions, by simulated
    * annealing combined with the downhill simplex method of Nelder
    * and Mead. The input matrix p[0..ndim][0..ndim-1] has ndim+1
    * rows, each an ndim-dimensional vector which is a vertex of
    * the starting simplex. Also input are the following: the
    * vector y[0..ndim], whose components must be pre-initialized
    * to the values of funk evaluated at the ndim+1 vertices
    * (rows) of p; ftol, the fractional convergence tolerance to be
    * achieved in the function value for an early return; iter,
    * and temptr. The routine makes iter function evaluations at an
    * annealing temperature temptr, then returns. You should then
    * decrease temptr according to your annealing schedule, reset
    * iter, and call the routine again (leaving other arguments
    * unaltered between calls). If iter is returned with a positive
    * value, then early convergence and return occurred. If you
    * initialize yb to a very large value on the rst call, then yb
    * and pb[1..ndim] will subsequently return the best function
    * value and point ever encountered (even if it is no longer a
    * point in the simplex). */
   /* Execute one run (iteration) of the SA. */
   public NDFunction executeOne( double tol, double startTemp ) 
      throws NRException {
      temp = startTemp;
      fillPSUM();
      iter = niter;

      while( true ) { 
	 getExtremePoints();	 
	 rtol = getSize();

	 //System.err.println( "HERE: "+iter+" "+
	 //   DoubleUtils.SPrintf("%.4f ",new double[]{temp,rtol,tol}) +
	 //   nfunk+" "+(finished()||rtol<tol||iter<0));
	 if ( finished() || rtol < tol || iter < 0 ) 
	    return points[ ilo ]; // If finished, return best point. 
	 if ( nfunk >= NMAX ) throw new NRException( "NMAX exceeded" );
	 nfunk += 2;
	 iter -= 2;
	 /* Begin a new iteration. First extrapolate by a factor 1 through the face of the 
	    simplex across from the high point, i.e., re ect the simplex from the 
	    high point. */
	 double ytry = amotsa( -1.0 );
	 /* Gives a result better than the best point, so try an additional
	    extrapolation by a factor 2. */ 
	 if ( ytry <= yvals[ ilo ] ) {
	    ytry = amotsa( 2.0 ); 
	 /* The reflected point is worse than the second-highest, 
	    so look for an intermediate lower point, i.e., do a
	    one-dimensional contraction. */
	 } else if ( ytry >= ynhi ) { 
	    double ysave = yhi;
	    ytry = amotsa( 0.5 );
	    /* Can't seem to get rid of that high point. Better 
	       contract around the lowest (best) point. */
	    if ( ytry >= ysave ) {
	       for ( int i = 0; i < mdims; i ++ ) {
		  if ( i != ilo ) { 
		     for (int j = 0; j < ndims; j ++ ) {
			//psum.params[ j ] =
			points[ i ].params[ j ] = 
			   0.5 * ( points[ i ].params[ j ] + points[ ilo ].params[ j ] );
			//points[ i ].params[ j ] = psum.params[ j ];
		     }
		     yvals[ i ] = evaluate( points[ i ]/*psum*/ );
		  } 
	       } 
	       //getExtremePoints();
	       nfunk ++; //= ndims; /* Keep track of function evaluations. */
	       //iter -= ndims;
	       fillPSUM(); /* Recompute psum. */
	    }
	 } else { -- nfunk; ++ iter; } /* Correct the evaluation count. */
      } /* Go back for the test of doneness and the next iteration. */
   }

   /* Determine which point is the highest (worst), next-highest, and 
      lowest (best), by looping over the points in the simplex. */
   public void getExtremePoints() {
      ilo = 0;
      ihi = 1;
      ynhi = ylo = yvals[ 0 ] - temp * Math.log( DoubleUtils.Random() );
      yhi = yvals[ 1 ] - temp * Math.log( DoubleUtils.Random() );
      if ( ylo > yhi ) {
	 ihi = 0;
	 ilo = 1;
	 ynhi = yhi;
	 yhi = ylo;
	 ylo = ynhi;
      }
      for ( int i = 2; i < mdims; i ++ ) {
	 double yt = yvals[ i ] - temp * Math.log( DoubleUtils.Random() );
	 if ( yt <= ylo ) {
	    ilo = i;
	    ylo = yt;
	 }
	 if ( yt > yhi ) {
	    ynhi = yhi;
	    ihi = i;
	    yhi = yt;
	 } else if ( yt > ynhi ) {
	    ynhi = yt;
	 }
      }
   }

   /* Compute the fractional range from highest to lowest. */
   public double getSize() {
      return 2.0 * Math.abs( yhi - ylo ) / ( temp + Math.abs( yhi ) + Math.abs( ylo ) );
   }

   /* Extrapolate by a factor fac through the face of the simplex
      across from the high point, try it, and replace the high
      point if the new point is better. */
   double amotsa( double fac ) throws NRException {
      double fac1 = ( 1.0 - fac ) / (double) ndims;
      double fac2 = fac1 - fac;
      for ( int j = 0; j < ndims; j ++ ) 
	 ptry.params[ j ] = psum.params[ j ] * fac1 - points[ ihi ].params[ j ] * fac2;
      double ytry = evaluate( ptry );
      if ( ytry <= yb ) {
	 for ( int j = 0; j < ndims; j ++ ) pb.params[ j ] = ptry.params[ j ];
	 yb = ytry;
      }
      double yflu = ytry + temp * Math.log( DoubleUtils.Random() );
      if ( yflu < yhi ) {
	 yvals[ ihi ] = ytry;
	 yhi = yflu;
	 for ( int j = 0; j < ndims; j ++ ) {
	    psum.params[ j ] += ptry.params[ j ] - points[ ihi ].params[ j ];
	    points[ ihi ].params[ j ] = ptry.params[ j ];
	 }
      }	
      return yflu;
   }

}
