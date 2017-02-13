package djr.nr;
import djr.util.*;
import djr.util.array.*;

/**
 * Class <code>ChisqFunction</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public abstract class ChisqFunction extends NDFunction {
   transient double xaxis[], yaxis[], sig[];
   int ndata;

   ChisqFunction() { 
      super();
   }

   ChisqFunction( int ndims, double xaxis[], double yaxis[], double sig[] ) { 
      super( ndims ); 
      this.xaxis = xaxis;
      this.yaxis = yaxis;
      this.sig = sig;
      ndata = xaxis.length;
   }

   public abstract double evaluate( double x );

   public Function duplicate() {
      ChisqFunction out = (ChisqFunction) super.duplicate();
      out.ndata = ndata;
      out.xaxis = this.xaxis; out.yaxis = this.yaxis; out.sig = this.sig;
      return out;
   }   

   public double evaluate() {
      // This is the function we want to MINIMIZE for params
      double retVal = 0.0;
      for ( int i = 0; i < ndata; i ++ ) {
	 double val = yaxis[ i ] - evaluate( xaxis[ i ] );
	 val *= val;
	 retVal += sig[ i ] != 1.0 && sig[ i ] != 0.0 ? 
	    val / ( sig[ i ] * sig[ i ] ) : val;
      }
      return retVal;
   }
   
   // As a test, try the sum of 2 gaussians!
   /*public static void main( String args[] ) {
      final int MEAN1 = 0, STDDEV1 = 1, MEAN2 = 2, STDDEV2 = 3;

      double xaxis[] = DoubleUtils.Sequence( 300 );
      double yaxis[] = DoubleUtils.New( 300 );
      double sig[] = DoubleUtils.New( 300 );

      class Test extends ChisqFunction {
	 public Test( int ndims, double xaxis[], double yaxis[], double sig[] ) { 
	    super( ndims, xaxis, yaxis, sig ); }
	 public Test() { super(); }
	 public double evaluate( double x ) {
	    return 1.0 / 2.0 *
	       ( Math.exp( -( x - params[ MEAN1 ] ) * ( x - params[ MEAN1 ] ) / 2 / 
			   params[ STDDEV1 ] / params[ STDDEV1 ] )
		 / params[ STDDEV1 ] / Math.sqrt( 2 * 3.141592654 ) +
		 Math.exp( -( x - params[ MEAN2 ] ) * ( x - params[ MEAN2 ] ) / 2 / 
			   params[ STDDEV2 ] / params[ STDDEV2 ] )
		 / params[ STDDEV2 ] / Math.sqrt( 2 * 3.141592654 ) );
	 } };

      ChisqFunction funk = new Test( 4, xaxis, yaxis, sig );

      funk.params[ MEAN1 ] = 10;
      funk.params[ STDDEV1 ] = 40;
      funk.params[ MEAN2 ] = 100;
      funk.params[ STDDEV2 ] = 20;

      DoubleUtils.Sub( xaxis, 100 );
      DoubleUtils.Set( sig, 1.0 );

      System.out.print( "IN  = " );
      DoubleUtils.Printf("%.3f   ", funk.params );

      for ( int i = 0; i < 300; i ++ ) yaxis[ i ] = funk.evaluate( xaxis[ i ] );

      for ( int i = 0; i < 4; i ++ ) 
	 funk.params[ i ] += ( DoubleUtils.Random() - 0.5 ) * funk.params[ i ];
      System.out.print( "START  = " );
      DoubleUtils.Printf("%.3f   ", funk.params );

      AmoebaSA amoeba = new AmoebaSA( funk );

      NDFunction out = null;
      try { out = amoeba.execute( 1e-9, 1e-5 ); }
      catch( Exception e ) { e.printStackTrace(); }

      System.out.print( "OUT = " );
      DoubleUtils.Printf( "%.3f   ", out.params );
   }
   */
}
