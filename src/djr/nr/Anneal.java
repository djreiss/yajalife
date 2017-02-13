package djr.nr;
import djr.util.MyUtils;
import djr.util.array.*;

/**
 * Class <code>Anneal</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Anneal {
   protected int niter = 10, restarts = 2, nfunk, nworse = 0;
   protected int NMAX = Integer.MAX_VALUE, NBAD = Integer.MAX_VALUE;
   protected double tempTol = -1, bestScore = -Double.MAX_VALUE;
   protected boolean stopped = false, perturbed = false, initializing = false;
   protected MutableFunction bestPars = null;

   public Anneal() { /*super();*/ }

   /* Set the maximum number of allowable function (fitness measurement) calls. */
   public void setMaximumFunctionCalls( int max ) { NMAX = max; }

   /* Stop the run after this many "generations" are run without increasing the 
      annealer's fitness value. */
   public void stopAfterThisManyBad( int bad ) { NBAD = bad; }

   /* Update the temperature based on its old value and the # of unsuccessful 
      iterations run so far */
   public double updateTemp( double oldTemp, int nsucc ) { return oldTemp * 0.9; }

   /* Update the temperature based on its old value and the # of iterations run so far */
   public void setTemperatureTol( double tol ) { tempTol = tol; }

   /* iters = number of iterations at a given temperature */
   public void setNumItersPerRun( int iter ) { niter = iter; }

   /* Set the number of restarts (# of runs of niter iterations) */
   public void setNumRestarts( int nr ) { restarts = nr; }

   /* Not really running, just doing multiple evaluations to figure out e.g. good
      starting temp, etc. */
   public boolean isInitializing() { return initializing; }

   /* Stop the annealer for good. */
   public void stop() { stopped = true; }

   /* Heat up the annealer. */
   public void perturb() { perturbed = true; }

   /* Evaluate the fitness of the object */
   public double evaluate( Function f ) { return f.evaluate(); }

   /* Override for custom evaluation of doneness. */
   public boolean finished() { return stopped || nworse > NBAD; }

   /* Modify an object and place it into a new object (which may or may not be null). */
   public Function mutate( Function params, Function newParams ) { 
      if ( newParams == null ) 
	 newParams = (MutableFunction) params.duplicate();
      return ( (MutableFunction) params ).mutate( (MutableFunction) newParams ); }

   /* Run the annealer until finished() returns true. */
   public Function anneal( MutableFunction startingParams, double startingTemp ) 
      throws NRException {
      return anneal( startingParams, tempTol, startingTemp );
   }

   /* Run the annealer. */
   public Function anneal( MutableFunction startingParams, double ttol, 
			   double startingTemp ) throws NRException {
      int nfunk = 0;

      MutableFunction params = startingParams;
      MutableFunction newParams = (MutableFunction) params.duplicate();
      bestPars = params;
      double score = evaluate( params );
      bestScore = score;

      for ( int xx = 1; xx <= restarts + 1; xx ++ ) {
	 double temptr = startingTemp / (double) xx;
	 int nsucc = niter;
	 try { 
	    while( ! finished() && temptr / startingTemp > ttol && nfunk < NMAX ) {
	       newParams = (MutableFunction) mutate( params, newParams );
	       nfunk ++;
	       double newScore = evaluate( newParams );
	       if ( metrop( newScore - score, temptr ) ) {
		  MutableFunction tempPar = params; params = newParams; 
		  newParams = tempPar;
		  score = newScore;
		  temptr = updateTemp( temptr, nsucc );
		  nsucc ++;
	       } else nsucc --;
	       if ( nsucc <= 0 ) {
		  temptr = updateTemp( temptr, nsucc );
		  nsucc = niter;
	       }
	       if ( score < bestScore ) {
		  bestScore = score;
		  bestPars = (MutableFunction) params.duplicate();
		  //System.err.println(bestScore+" "+temptr+" "+nworse);
		  nworse = 0;
	       } else nworse ++;
	       if ( perturbed ) { perturbed = false; break; }
	    }
	 } catch( Exception e ) { e.printStackTrace(); }
	 if ( nfunk >= NMAX ) throw new NRException( "Function calls exceeded NMAX" );
      }
      return bestPars;
   }   

   protected boolean metrop( double de, double t ) {
      if ( de < 0.0 ) return true;
      return DoubleUtils.Random() < Math.exp( -de / t );
   }

   public double estimateStartingTemp( MutableFunction startingParams )
      throws NRException {
      return estimateStartingTemp( startingParams, 0.8, 1000 );
   }

   public double estimateStartingTemp( MutableFunction startingParams, 
				       double targetAcceptanceProb, 
				       int niters ) throws NRException {
      initializing = true;
      MutableFunction params = startingParams;
      MutableFunction newParams = (MutableFunction) params.duplicate();
      double score = evaluate( params );
      double diffScores[] = DoubleUtils.New( niters );

      int i = 0;
      while( i < niters ) {
	 try { 
	    newParams = (MutableFunction) mutate( params, newParams );
	    double newScore = evaluate( newParams );
	    MutableFunction tempPar = params; params = newParams; 
	    newParams = tempPar;
	    if ( newScore > score ) diffScores[ i ++ ] = newScore - score;
	    score = newScore;
	 } catch( Exception e ) { e.printStackTrace(); }
      }
      double out = -DoubleUtils.Mean( diffScores ) / Math.log( targetAcceptanceProb );
      initializing = false;
      return out;
   }   

   public double estimateScatter( MutableFunction startingParams, 
				  int niters ) throws NRException {
      initializing = true;
      MutableFunction params = startingParams;
      MutableFunction newParams = null;
      double scores[] = DoubleUtils.New( niters );

      int i = 0;
      while( i < niters ) {
	 try { 
	    newParams = (MutableFunction) mutate( params, newParams );
	    double newScore = evaluate( newParams );
	    scores[ i ++ ] = newScore;
	 } catch( Exception e ) { e.printStackTrace(); }
      }
      double out = DoubleUtils.Stddev( scores );
      initializing = false;
      return out;
   }   

   /*public static void main( String args[] ) {
      class Test extends NDFunction implements MutableFunction {
	 public Test( int size ) { super( size ); }
	 // Simple test -- make each value as close to neighbor as possible 
	 public double evaluate() { double sum = 0.0;
	 for ( int i = 1, s = ndims; i < s; i ++ ) 
	    sum += Math.abs( params[ i ] - params[ i - 1 ] );
	 return sum; }
	 public MutableFunction mutate( MutableFunction newParams ) {
	    Test np = (Test) newParams;
	    if ( np == null ) np = (Test) this.duplicate();
	    else DoubleUtils.Copy( np.params, this.params );
	    int ind = IntUtils.Random( np.ndims );
	    np.params[ ind ] = DoubleUtils.Random() * 10.0;
	    return np; }
      };

      Test funk = new Test( 100 );
      DoubleUtils.Random( funk.params );
      DoubleUtils.Mult( funk.params, 10.0 );

      Anneal annealer = new Anneal();
      annealer.stopAfterThisManyBad( 500 );
      //annealer.setMaximumFunctionCalls( 10000 );

      Test best = null;
      try { 
	 DoubleUtils.Printf( "Estimated score scatter: %.3f\n",
			     annealer.estimateScatter( funk, 1000 ) );
	 DoubleUtils.Printf( "Estimated starting temp: %.3f\n",
			     annealer.estimateStartingTemp( funk ) );
	 best = (Test) annealer.anneal( funk, 10.0 );
	 System.out.println( "BEST: " + best.evaluate() );
	 DoubleUtils.Printf( "%.3f ", best.params );
      } catch( Exception e ) { e.printStackTrace(); }
   }
   */
}
