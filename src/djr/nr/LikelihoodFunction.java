package djr.nr;
import djr.util.*;
import djr.util.array.*;

/**
 * Class <code>LikelihoodFunction</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public abstract class LikelihoodFunction extends NDFunction {
   transient double data[];
   int ndata;

   public LikelihoodFunction() { super(); }

   public LikelihoodFunction( int ndims, double data[] ) { 
      super( ndims ); 
      this.data = data;
      ndata = data.length;
   }

   public abstract double evaluate( double x );

   public Function duplicate() {
      LikelihoodFunction out = (LikelihoodFunction) super.duplicate();
      out.data = this.data; out.ndata = this.ndata; return out;
   }   

   public double evaluate() {
      // This is the function we want to MAXIMIZE for pars
      double retVal = 0.0;
      for ( int i = 0; i < ndata; i++ )
	 retVal += DoubleUtils.Log10( evaluate( data[ i ] ) );
      return -retVal;
   }
   
   // As a test, try the sum of 2 gaussians!
   /*public static void main( String args[] ) {
      final int MEAN1 = 0, STDDEV1 = 1, MEAN2 = 2, STDDEV2 = 3;

      double data1[] = DoubleUtils.New( 500 );
      DoubleUtils.Normal( data1, 15, 30 ); // Make first data set w/ mean 15, stddev 30
      DoubleUtils.PrintStats( data1 ); System.out.println();

      double data2[] = DoubleUtils.New( 500 );
      DoubleUtils.Normal( data2, 70, 12 ); // Make 2nd data set w/ mean 70, stddev 12
      DoubleUtils.PrintStats( data2 ); System.out.println();

      double data[] = DoubleUtils.New( 1000 );
      for ( int i = 0; i < 500; i ++ ) { // join them together
	 data[ i ] = data1[ i ];
	 data[ i + 500 ] = data2[ i ];
      }

      DoubleUtils.Printf("IN  = %.3f   ", 15.0 );
      DoubleUtils.Printf("%.3f   ", 30.0 );
      DoubleUtils.Printf("%.3f   ", 70.0 );
      DoubleUtils.Printf("%.3f\n", 12.0 );

      class Test extends LikelihoodFunction {
	 public Test() { super(); }
	 public Test( int ndims, double data[] ) { super( ndims, data ); }
	 public double evaluate( double x ) {
	    return 1.0 / 2.0 *
	       ( Math.exp( -( x - params[ MEAN1 ] ) * ( x - params[ MEAN1 ] ) / 2 / 
			   params[ STDDEV1 ] / params[ STDDEV1 ] )
		 / params[ STDDEV1 ] / Math.sqrt( 2 * 3.141592654 ) +
		 Math.exp( -( x - params[ MEAN2 ] ) * ( x - params[ MEAN2 ] ) / 2 / 
			   params[ STDDEV2 ] / params[ STDDEV2 ] )
		 / params[ STDDEV2 ] / Math.sqrt( 2 * 3.141592654 ) );
	 } };

      LikelihoodFunction funk = new Test( 4, data );

      funk.params[ MEAN1 ] = 10;
      funk.params[ STDDEV1 ] = 40;
      funk.params[ MEAN2 ] = 100;
      funk.params[ STDDEV2 ] = 1;

      System.out.print( "START  = " );
      DoubleUtils.Printf("%.3f   ", funk.params );

      AmoebaSA amoeba = new AmoebaSA( funk );

      NDFunction out = null;
      try { out = amoeba.execute( 10, 1e-7, 1.0 ); }
      catch( Exception e ) { e.printStackTrace(); }

      System.out.print( "OUT = " );
      DoubleUtils.Printf( "%.3f   ", out.params );
   }
   */
}
