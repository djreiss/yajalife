package djr.util.array;

import java.util.*;
import corejava.*;
import cern.jet.stat.*;
import cern.jet.random.Normal;
//import cern.jet.random.engine.MersenneTwister; //DRand;
import edu.cornell.lassp.houle.RngPack.Ranecu;
import djr.util.MyUtils;

/**
 * Template class <code>FloatUtils</code>.
 *
 * This is a "template" class and should not be directly compiled.
 * Commands to create real file before compiling (e.g. for ints) are:
 * cp Utils.java IntUtils.java
 * rpl Float Integer IntUtils.java ; rpl Float Int IntUtils.java ; rpl float int IntUtils.java
 * javac IntUtils.java
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class FloatUtils {
   public static double version = 1.9978;
   //public static MersenneTwister rand = null;
   public static Ranecu rand = null;
   public static Normal nrand = null;
   public static final double LOG2 = Math.log( 2.0 );
   public static final double LOG10 = Math.log( 10.0 );

   static { SetSeed(); }

   // All the typeUtils classes share the rand in DoubleUtils.
   public static final void SetSeed() { 
      if ( DoubleUtils.rand == null ) SetSeed( System.currentTimeMillis() );
      else rand = DoubleUtils.rand; }
   public static final void SetSeed( long seed ) { 
      DoubleUtils.rand = rand = new Ranecu( seed );
      Random(); } // The first number is always 0.9997something, for some reason
   // NO BOOL
   public static final float Random() {
      return (float) rand.raw(); }
   public static final long GetSeed() {
      return DoubleUtils.rand.getSeed(); }
   public static final void Random( double arr[] ) { 
      DoubleUtils.rand.raw( arr, arr.length ); }
   public static final float Random( int max ) { 
      return (float) ( DoubleUtils.Random() * max ); }
   public static final float Random( double min, double max ) { 
      return (float) ( min + ( max - min ) * DoubleUtils.Random() ); }
   public static final float Random( int min, int max ) { 
      return Random( (double) min, (double) max ); }
   public static final float RandChoose( int lo, int hi ) {
      return (float) ( (long) lo + (long) ( ( 1L + (long) hi - (long) lo ) * 
					   DoubleUtils.Random() ) ); }
   public static final float RandChoose( int hi ) {
      return RandChoose( 0, hi ); }
   // END NO BOOL
   public static final float RandChoose( float[] in ) {
      return in[ IntUtils.RandChoose( 0, in.length - 1 ) ]; }

   /* BOOL ONLY
   public static final boolean Random() {
      return rand.raw() > 0.5 ? false : true; }
   // END BOOL ONLY */

   // NO BOOL
   public static final float Normal( double mean, double stddev ) { 
      return (float) Normal.staticNextDouble( mean, stddev ); }
   public static final float Normal() { 
      return Normal( 0.0, 1.0 ); }

   public static final double Log( float in ) { return in <= 0 ? -999 : Math.log( in ); }
   public static final double Log2( float in ) { return DoubleUtils.Log( in ) / LOG2; }
   public static final double Log10( float in ) { return DoubleUtils.Log( in ) / LOG10; }
   public static final double Sign( float in ) { return in < 0 ? -1.0 : 1.0; }
   // END NO BOOL

   public static final float[] New( int nx ) { 
      return new float[ nx ]; }
   public static final float[][] New( int nx, int ny ) { 
      return new float[ nx ][ ny ]; }
   public static final float[][][] New( int nx, int ny, int nz ) { 
      return new float[ nx ][ ny ][ nz ]; }
   public static final float[][][][] New( int nx, int ny, int nz, int na ) { 
      return new float[ nx ][ ny ][ nz ][ na ]; }

   public static final float[] Resize( float[] in, int newx ) {
      float[] out = New( newx ); Copy( out, in ); return out; }
   public static final float[][] Resize( float[][] in, int newx, int newy ) {
      float[][] out = New( newx, newy ); Copy( out, in ); return out; }
   public static final float[][][] Resize( float[][][] in, int newx, int newy, int newz ) {
      float[][][] out = New( newx, newy, newz ); Copy( out, in ); return out; }
   public static final float[][][][] Resize( float[][][][] in, int newx, int newy, int newz, int newa ) {
      float[][][][] out = New( newx, newy, newz, newa ); Copy( out, in ); return out; }

   public static final float[] New( float[] in ) {
      if ( in == null ) return null;
      float[] out = New( in.length ); Copy( out, in ); return out; }
   public static final float[][] New( float[][] in ) {
      float[][] out = new float[ in.length ][];
      for ( int i = 0, s = in.length; i < s; i ++ ) out[ i ] = New( in[ i ] ); return out; }
   public static final float[][][] New( float[][][] in ) {
      float[][][] out = new float[ in.length ][][];
      for ( int i = 0, s = in.length; i < s; i ++ ) out[ i ] = New( in[ i ] ); return out; }

   public static final Object NewObj( Object obj ) {
      if ( obj instanceof float[] ) return New( (float[]) obj );
      else if ( obj instanceof float[][] ) return New( (float[][]) obj );
      else if ( obj instanceof float[][][] ) return New( (float[][][]) obj );
      return null; }

   public static final float[] SubArr( float[] in, int start, int end ) {
      float[] out = New( end - start + 1 );
      System.arraycopy( in, start, out, 0, end - start + 1 );
      return out; }

   // NO BOOL
   public static final float[] Sequence( int length ) {
      float[] out = New( length ); for ( int i = 0; i < length; i ++ ) out[ i ] = (float) i;
      return out; }
   public static final double[] Convert( Object in ) {
      if ( in instanceof double[] ) return (double[]) in;
      float iin[] = (float[]) in;
      double out[] = new double[ iin.length ];
      for ( int i = 0, size = iin.length; i < size; i ++ ) out[ i ] = (double) iin[ i ];
      return out; }
   // END NO BOOL

   public static final void Print( Object in ) {
      if ( in instanceof float[] ) Print( (float[]) in, 0, ( (float[]) in ).length ); 
      else for ( int i = 0, size = ( (Object[]) in ).length; i < size; i ++ ) 
	 Print( ( (Object[]) in )[ i ] ); }
   public static final void Print( String start, Object in ) {
      System.out.print( start ); Print( in ); }

   public static final void Print( float[] in, int start, int end ) {
      if ( in == null ) System.out.println( "NULL" );
      for ( int i = start; i < end; i ++ ) System.out.print( in[ i ] + " " );
      System.out.println(); }

   // NO BOOL
   public static final void Printf( String fmt, float in ) {
      if ( Double.isInfinite( (double) in ) ) in = - Float.MAX_VALUE;
      Format.print( System.out, fmt, in ); };
   public static final String SPrintf( String fmt, float in ) {
      if ( Double.isInfinite( (double) in ) ) in = - Float.MAX_VALUE;
      return ( new Format( fmt ) ).form( in ); }

   public static final void PrintCols( String fmt, float in1[], float in2[] ) {
      PrintCols( fmt, new float[][]{ in1, in2 } ); }
   public static final void PrintCols( String fmt, float in[][], boolean lines ) {
      for ( int i = 0, size = in[ 0 ].length; i < size; i ++ ) {
	 if ( lines ) System.out.print( i + " " );
	 for ( int j = 0, ss = in.length; j < ss; j ++ ) 
	    System.out.print( SPrintf( fmt, in[ j ][ i ] ) );
	 System.out.println(); } }
   public static final void PrintCols( String fmt, float in[][] ) {
      PrintCols( fmt, in, false ); }

   public static final void Printf( String fmt, Object in ) {
      if ( in == null ) System.out.println( "NULL" );
      if ( in instanceof float[] ) { for ( int i = 0, size = ( (float[]) in ).length; i < size; i ++ )
	 Printf( fmt, ( (float[]) in )[ i ] ); }
      else { for ( int i = 0, size = ( (Object[]) in ).length; i < size; i ++ )
	 Printf( fmt, ( (Object[]) in )[ i ] ); }
      System.out.println(); }
   public static final void Printf( String start, String fmt, Object in ) {
      System.out.print( start ); Printf( fmt, in ); }

   public static final String SPrintf( String fmt, Object in ) {
      if ( in == null ) return "NULL";
      String out = "";
      if ( in instanceof float[] ) { for ( int i = 0, size = ( (float[]) in ).length; i < size; i ++ )
	 out += SPrintf( fmt, ( (float[]) in )[ i ] ); }
      else { for ( int i = 0, size = ( (Object[]) in ).length; i < size; i ++ )
	 out += SPrintf( fmt, ( (Object[]) in )[ i ] ); }
      return out; }

   public static final void PrintT( String fmt, float[][] in ) {
      for ( int y = 0, size = in[0].length; y < size; y ++ ) {
	 for ( int x = 0, s = in.length; x < s; x ++ ) Format.print( System.out, fmt, in[ x ][ y ] );
	 System.out.println(); } }
   public static final void PrintT( float[][] in ) {
      for ( int y = 0, size = in[0].length; y < size; y ++ ) {
	 for ( int x = 0, s = in.length; x < s; x ++ ) System.out.print( in[ x ][ y ] + " " );
	 System.out.println(); } }
   // END NO BOOL

   public static final float[][] Transpose( float[][] in ) {
      float out[][] = New( in[ 0 ].length, in.length );
      for ( int i = 0; i < in[ 0 ].length; i ++ ) for ( int j = 0; j < in.length; j ++ ) 
	 out[ i ][ j ] = in[ j ][ i ]; return out; }

   public static final void Copy( Object dest, Object src ) {
      if ( src == null || dest == null ) return;
      if ( src instanceof float[] ) System.arraycopy( (float[]) src, 0, (float[]) dest, 0, 
			IntUtils.Min( ( (float[]) src ).length, ( (float[]) dest ).length ) );
      else for ( int i = 0, s = IntUtils.Min( ( (Object[]) src ).length, 
	     ( (Object[]) dest ).length ); i < s; i ++ ) 
	 Copy( ( (Object[]) dest )[ i ], ( (Object[]) src )[ i ] );
   }

   public static final float[] Flatten( float[][] in, float[] out ) {
      if ( out == null || out.length < NElem( in ) ) out = new float[ NElem( in ) ]; 
      int ind = 0; for ( int i = 0, s = in.length; i < s; i ++ ) {
	 System.arraycopy( in[ i ], 0, out, ind, in[ i ].length ); ind += in[ i ].length; }
      return out; }
   public static final float[] Flatten( float[][] in ) {
      return Flatten( in, null ); }

   public static final float[] Flatten( float[][][] in, float[] out ) {
      if ( out == null || out.length < NElem( in ) ) out = new float[ NElem( in ) ]; 
      int ind = 0; for ( int i = 0, s = in.length; i < s; i ++ ) {
	 float temp[] = Flatten( in[ i ] );
	 System.arraycopy( temp, 0, out, ind, temp.length ); ind += temp.length; }
      return out; }
   public static final float[] Flatten( float[][][] in ) {
      return Flatten( in, null ); }

   private static float[][] UnflattenPriv( float in[], int start, int nx, int ny, float out[][] ) {
      if ( out == null || out.length < nx ) out = new float[ nx ][]; int ind = start;
      for ( int i = 0; i < nx; i ++ ) {
	 if ( out[ i ] == null || out[ i ].length < ny ) out[ i ] = new float[ ny ];
	 System.arraycopy( in, ind, out[ i ], 0, ny ); ind += ny;
      } return out; }
   public static float[][] Unflatten( float in[], int nx, int ny, float out[][] ) {
      return UnflattenPriv( in, 0, nx, ny, out ); }
   public static float[][] Unflatten( float in[], int nx, int ny ) {
      return UnflattenPriv( in, 0, nx, ny, null ); }

   public static float[][][] Unflatten( float in[], int nx, int ny, int nz, float out[][][] ) {
      if ( out == null || out.length < nx ) out = new float[ nx ][][]; int ind = 0, step = ny *nz;
      for ( int i = 0; i < nx; i ++ ) { out[ i ] = UnflattenPriv( in, ind, ny, nz, out[ i ] );
      ind += step; } return out; }
   public static float[][][] Unflatten( float in[], int nx, int ny, int nz ) {
      return Unflatten( in, nx, ny, nz, null ); }

   public static final void Zero( Object in, int start, int end ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) Arrays.fill( (float[]) in, start, end, (float) 0 );
      else for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Zero( ( (Object[]) in )[ i ], start, end ); }
   public static final void Zero( Object in ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) Arrays.fill( (float[]) in, (float) 0 );
      else for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Zero( ( (Object[]) in )[ i ] ); }
   public static final void Zero( float in[], int inds[] ) {
      if ( in == null ) return;
      else for ( int i = 0, s = inds.length; i < s; i ++ ) in[ inds[ i ] ] = 0;
   }

   public static final void Set( Object in, int from, int to, float val ) {
      if ( in instanceof float[] ) Arrays.fill( (float[]) in, from, to, val );
      else for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Set( ( (Object[]) in )[ i ], from, to, val ); }
   public static final void Set( Object in, float val ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) Arrays.fill( (float[]) in, val );
      else for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Set( ( (Object[]) in )[ i ], val ); }
   public static final void Set( float in[], int inds[], float val ) {
      if ( in == null ) return;
      else for ( int i = 0, s = inds.length; i < s; i ++ ) in[ inds[ i ] ] = val;
   }

   // NO BOOL
   public static final float Sum( Object in, int min, int max ) {
      if ( in == null ) return 0;
   // END NO BOOL
   /* BOOL ONLY
   public static final int Sum( Object in, int min, int max ) {
   // END BOOL ONLY */
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
	 if ( max >= inn.length ) max = inn.length - 1;
   // NO BOOL
	 float out = 0; for ( int i = min; i <= max; i ++ ) out += inn[ i ]; return out; 
   // END NO BOOL
   /* BOOL ONLY
         int out = 0; for ( int i = min; i <= max; i ++ ) out += inn[ i ] ? 1 : 0; 
	 return out; 
   // END BOOL ONLY */
      } else {
   // NO BOOL
	 float out = 0; 
   // END NO BOOL
   /* BOOL ONLY
         int out = 0; 
   // END BOOL ONLY */
	 for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	    out += Sum( ( (Object[]) in )[ i ], min, max ); return out;
      } }

   // NO BOOL
   public static final float Sum( Object in ) {
   // END NO BOOL
   /* BOOL ONLY
   public static final int Sum( Object in ) {
   // END BOOL ONLY */
      if ( in instanceof float[] ) return Sum( (float[]) in, 0, ( (float[]) in ).length ); 
      else {
   // NO BOOL
	 float out = 0; 
   // END NO BOOL
   /* BOOL ONLY
         int out = 0; 
   // END BOOL ONLY */
	 for ( int i = 0, s = ( (Object[]) in).length; i < s; i ++ ) 
	    out += Sum( ( (Object[]) in )[ i ] ); return out; }
   }

   // NO BOOL
   public static final float SumSquared( Object in ) {
      if ( in == null ) return 0;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; float out = 0; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) out += inn[ i ] * inn[ i ]; return out;
      } else {
	 float out = 0; for ( int i = 0, s = ( (Object[]) in).length; i < s; i ++ ) 
	    out += SumSquared( ( (Object[]) in )[ i ] ); return out; }
   }

   public static final float SumSquared2( Object in ) {
      if ( in == null ) return 0;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; float out = 0; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) out += Sign( inn[ i ] ) * inn[ i ] * inn[ i ]; return out;
      } else {
	 float out = 0; for ( int i = 0, s = ( (Object[]) in).length; i < s; i ++ ) 
	    out += SumSquared2( ( (Object[]) in )[ i ] ); return out; }
   }

   public static final float Prod( Object in ) {
      if ( in == null ) return 0;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; float out = inn[ 0 ]; for ( int i = 1, s = inn.length; i < s; i ++ ) out *= inn[ i ]; return out;
      } else {
	 float out = Prod( ( (Object[]) in )[ 0 ] ); for ( int i = 1, s = ( (Object[]) in).length; i < s; i ++ ) 
	    out *= Prod( ( (Object[]) in )[ i ] ); return out; }
   }

   public static final float Max( Object in ) { float out = - Float.MAX_VALUE;
      if ( in == null ) return 0;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) out = Max( out, inn[ i ] ); return out; 
      } else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 out = Max( out, Max( ( (Object[]) in )[ i ] ) ); return out; } }
   
   public static final float Min( Object in ) { float out = Float.MAX_VALUE;
      if ( in == null ) return 0;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) out = Min( out, inn[ i ] ); return out; 
      } else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 out = Min( out, Min( ( (Object[]) in )[ i ] ) ); return out; } }

   public static final float[] MinMax( Object in, float inout[] ) { 
      if ( in == null ) return null;
      if ( inout == null ) { inout = New( 2 ); 
      inout[ 0 ] = Float.MAX_VALUE; inout[ 1 ] = - Float.MAX_VALUE; }
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) {
	 inout[ 0 ] = Min( inout[ 0 ], inn[ i ] ); 
	 inout[ 1 ] = Max( inout[ 1 ], inn[ i ] ); } return inout;
      } else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 inout = MinMax( ( (Object[]) in )[ i ], inout ); return inout; } }
   public static final float[] MinMax( Object in ) { 
      return MinMax( in, null ); }

   public static final void Round( Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) (int) inn[ i ]; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Round( ( (Object[]) in )[ i ] ); }
   }

   public static final void Floor( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) if ( inn[ i ] < val ) inn[ i ] = val; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Floor( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Ceil( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) if ( inn[ i ] > val ) inn[ i ] = val; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Ceil( ( (Object[]) in )[ i ], val ); }
   }
   
   public static final void Mult( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] *= val; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Mult( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Mult( Object in, Object in2 ) { 
      if ( in == null || in2 == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] *= ( (float[]) in2 )[ i ]; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Mult( ( (Object[]) in )[ i ], ( (Object[]) in2 )[ i ] ); }
   }

   public static final void Divide( Object in, float val ) { 
      if ( in == null ) return;
      if ( val == 0 ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] /= val; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Divide( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Divide( float val, Object in ) { 
      if ( in == null ) return;
      if ( val == 0 ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) ( val / inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Divide( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Divide( Object in, Object in2 ) { 
      if ( in == null || in2 == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] /= ( (float[]) in2 )[ i ]; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Divide( ( (Object[]) in )[ i ], ( (Object[]) in2 )[ i ] ); }
   }

   public static final void Add( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] += val; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Add( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Add( Object in, Object in2 ) { 
      if ( in == null || in2 == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] += ( (float[]) in2 )[ i ]; }
      else { for ( int i = 0, s = IntUtils.Min( ( (Object[]) in ).length, 
						( (Object[]) in2 ).length ); i < s; i ++ ) 
	 Add( ( (Object[]) in )[ i ], ( (Object[]) in2 )[ i ] ); }
   }

   public static final void Sub( Object in, float val ) { 
      Add( in, (float) -val ); }

   public static final void Sub( Object in, Object in2 ) { 
      if ( in == null || in2 == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] -= ( (float[]) in2 )[ i ]; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Sub( ( (Object[]) in )[ i ], ( (Object[]) in2 )[ i ] ); }
   }

   public static final void Invert( Object in ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) 
	 inn[ i ] = (float) ( 1.0 / (double) inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Invert( ( (Object[]) in )[ i ] ); }
   }

   // END NO BOOL

   public static final void Random( Object in ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = Random(); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Random( ( (Object[]) in )[ i ] ); }
   }

   // NO BOOL
   public static final void Normal( Object in, double mean, double stddev ) {
      if ( in == null ) return;
      if ( nrand == null ) nrand = new Normal( mean, stddev, DoubleUtils.rand );
      else nrand.setState( mean, stddev );
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) nrand.nextDouble(); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Normal( ( (Object[]) in )[ i ], mean, stddev ); }
   }

   public static final void NormalAdd( Object in, double mean, double stddev ) {
      if ( in == null ) return;
      if ( nrand == null ) nrand = new Normal( mean, stddev, DoubleUtils.rand );
      else nrand.setState( mean, stddev );
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] += (float) nrand.nextDouble(); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 NormalAdd( ( (Object[]) in )[ i ], mean, stddev ); }
   }

   public static final void Log10( Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) Log10( inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Log10( ( (Object[]) in )[ i ] ); }
   }

   public static final void Log2( Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) Log2( inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Log2( ( (Object[]) in )[ i ] ); }
   }

   public static final void Log( Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) inn[ i ] = (float) Log( inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Log( ( (Object[]) in )[ i ] ); }
   }

   public static final void Pow( float val, Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) 
	 inn[ i ] = (float) Math.pow( val, (double) inn[ i ] ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Pow( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Pow( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) 
	 inn[ i ] = (float) Math.pow( (double) inn[ i ], val ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Pow( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Pow2( Object in, float val ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) 
	 inn[ i ] = (float) ( Sign( inn[ i ] ) * Math.pow( (double) inn[ i ], val ) ); }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Pow2( ( (Object[]) in )[ i ], val ); }
   }

   public static final void Abs( Object in ) { 
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) {
	 float x = inn[ i ]; inn[ i ] = (float) ( x > 0 ? x : -x ); } }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Abs( ( (Object[]) in )[ i ] ); }
   }

   public static final void Replace( Object in, float from, float to ) {
      if ( in == null ) return;
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) 
	 if ( inn[ i ] == from ) inn[ i ] = to; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 Replace( ( (Object[]) in )[ i ], from, to ); }      
   }

   public static final int WhereMax( float[] in, int maxind ) { 
      if ( in == null ) return -1;
      float out = - Float.MAX_VALUE ; int ind = -1;
      maxind = Math.min( in.length, maxind+1 );
      for ( int i = 0; i < maxind; i ++ ) 
	 if ( out < in[ i ] ) { out = in[ i ]; ind = i; } return ind; }
   public static final int WhereMax( float[] in ) {
      return WhereMax( in, in.length ); }

   public static final int WhereMin( float[] in, int maxind ) { 
      float out = Float.MAX_VALUE ; int ind = -1;
      maxind = Math.min( in.length, maxind+1 );
      for ( int i = 0; i < maxind; i ++ ) if ( out > in[ i ] ) { out = in[ i ]; ind = i; } 
      return ind; }
   public static final int WhereMin( float[] in ) { 
      return WhereMin( in, in.length ); }

   public static final void MaxNorm( Object in ) { 
      float max = Max( in ); Divide( in, max ); }
   public static final void UnitNorm( Object in ) {
      float sum = Sum( in ); Divide( in, sum ); }

   public static final double Integral( float[] inx, float[] iny ) { // trapezoid rule
      double sum = 0.0;
      for ( int i = 1, s = inx.length; i < s; i ++ )
	 sum += ( inx[ i ] + inx[ i - 1 ] ) / 2.0 * ( iny[ i ] - iny[ i - 1 ] ) ;
      return sum;
   }

   public static final float[] Integrate( float[] in, float[] out ) {
      if ( out == null || out.length < in.length ) out = New( in.length );
      float sum = 0;
      for ( int i = 0, s = in.length; i < s; i ++ ) { sum += in[ i ]; out[ i ] = sum; }
      return out;
   }
   // END NO BOOL

   public static final boolean Equals( Object in1, Object in2 ) {
      if ( in1 instanceof float[] ) return Arrays.equals( (float[]) in1, (float[]) in2 );
      else {
	 if ( ( (Object[]) in1 ).length != ( (Object[]) in2 ).length ) return false;
	 for ( int i = 0, s = ( (Object[]) in1 ).length; i < s; i ++ ) 
	    if ( ! Equals( ( (Object[]) in1 )[ i ], ( (Object[]) in2 )[ i ] ) ) return false; 
	 return true; } }

   public static int NEqualTo( Object in, float val ) { 
      int out = 0; 
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) if ( inn[ i ] == val ) out ++; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 out += NEqualTo( ( (Object[]) in )[ i ], val ); }
      return out; }

   public static int NNotEqualTo( Object in, float val ) { 
      return NElem( in ) - NEqualTo( in, val ); }

   public static boolean AnyEqualTo( Object in, float val ) { 
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) if ( inn[ i ] == val ) return true; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 if ( AnyEqualTo( ( (Object[]) in )[ i ], val ) ) return true; }
      return false; }   

   public static boolean AnyNotEqualTo( Object in, float val ) { 
      if ( in instanceof float[] ) { float[] inn = (float[]) in; 
      for ( int i = 0, s = inn.length; i < s; i ++ ) if ( inn[ i ] != val ) return true; }
      else { for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 if ( AnyNotEqualTo( ( (Object[]) in )[ i ], val ) ) return true; }
      return false; }   

   public static final void Shift( float[] in, int amt, float spare ) {
      if ( amt > 0 ) { System.arraycopy( in, 0, in, amt, in.length - amt ); Arrays.fill( in, 0, amt, spare ); }
      else if ( amt < 0 ) { System.arraycopy( in, -amt, in, 0, in.length + amt ); Arrays.fill( in, in.length+amt, in.length, spare ); } }
   public static final void Shift( float[][] in, int amt, float spare ) {
      for ( int i = 0, s = in.length; i < s; i ++ ) Shift( in[ i ], amt, spare ); }

   public static final void Mask( float[] in, boolean[] flags, boolean val ) {
      for ( int i = 0, s = in.length; i < s; i ++ ) if ( flags[ i ] == val ) in[ i ] = 0; }
   public static final int IndexOf( float[] in, float val ) {
      for ( int i = 0, s = in.length; i < s; i ++ ) if ( in[ i ] == val ) return i; return -1; }
   public static final boolean Contains( float[] in, float val ) {
      return ( IndexOf( in, val ) != -1 ); }
   public static final int LastIndexOf( float[] in, float val ) {
      for ( int i = in.length - 1; i >= 0; i -- ) if ( in[ i ] == val ) return i; return -1; }
   public static final float[] Index( float in[], int index[] ) {
      float out[] = New( in.length ); 
      for ( int i = 0, s = in.length; i < s; i ++ ) out[ i ] = in[ index[ i ] ]; return out; }

   public static final float[] Reverse( float[] in ) {
      for ( int i = 0, s = in.length; i < s/2; i ++ ) {
	 float temp = in[ i ]; in[ i ] = in[ s-i-1 ]; in[ s-i-1 ] = temp; }
      return in; }

   // NO BOOL
   public static final void Sort( float[] in ) { Arrays.sort( in ); }
   public static final int[] Indexx( float[] in, int[] out ) {
      double darr[] = Convert( in );
      try { return djr.nr.Indexx.indexx( darr, out ); } catch( djr.nr.NRException e ) { }; 
      return null; }
   public static final int[] IRank( float[] in, int[] out ) {
      double darr[] = Convert( in );
      try { return djr.nr.Indexx.irank( darr, out ); } catch( djr.nr.NRException e ) { }; 
      return null; }

   public static final int Search( float[] in, float val, boolean sort ) {
      if ( sort ) Sort( in ); return Arrays.binarySearch( in, val ); }
   public static final int Search( float[] in, float val ) {
      return Search( in, val, true ); }

   public static final float[] Uniq( float[] in ) {
      FloatVector v = new FloatVector();
      for ( int i = 0, s = in.length; i < s; i ++ ) 
	 if ( ! v.contains( in[ i ] ) ) v.addElement( (float) i );
      return v.data();
   }
   // END NO BOOL

   public static final int NElem( Object in ) { 
      if ( in instanceof float[] ) return ( (float[]) in ).length; 
      else { int sum = 0; for ( int i = 0, s = ( (Object[]) in ).length; i < s; i ++ ) 
	 sum += NElem( ( (Object[]) in )[ i ] ); return sum; } }
      
   // NO BOOL
   public static final double Mean( Object in ) {
      return ( (double) Sum( in ) / (double) NElem( in ) ); }

   public static final double Mean( Object in, int i1, int i2 ) {
      return ( (double) Sum( in, i1, i2 ) / (double) ( i2 - i1 + 1 ) ); }

   public static final double Median( float[] in ) {
      final int nx = in.length; float temp[] = New( nx );
      Copy( temp, in ); Sort( temp ); double out = (double) temp[ (nx+1)/2 ];
      if ( nx % 2 != 1 ) out = ( (double) temp[ nx/2 ] + (double) temp[ nx/2+1 ] ) / 2.0;
      return out; }

   public static final double Variance( float[] in, double mean ) {
      double sum = 0.0; int nelem = 0;
      for ( int i = 0, s = in.length; i < s; i ++ ) 
	 sum += ( in[ i ] - mean ) * ( in[ i ] - mean );
      return sum / ( (double) in.length - 1.0 ); }

   public static final double Stddev( float[] in, double mean ) {
      return Math.sqrt( Variance( in, mean ) ); }

   public static final double Stddev( float[] in ) {
      return Stddev( in, Mean( in ) ); }

   public static final void Stats( float[] in, double[] out ) {
      out[ 0 ] = Mean( in ); out[ 1 ] = Median( in ); out[ 2 ] = Stddev( in ); }
   public static final double[] Stats( float[] in ) {
      double out[] = new double[ 3 ]; Stats( in, out ); return out; }

   public static void PrintStats( float[] data ) {
      System.out.println      ( "NELEM   = " +      NElem(data) );
      Format.print( System.out, "MEAN    = %.3f\n", Mean(data) );
      Format.print( System.out, "MEDIAN  = %.3f\n", Median(data) );
      Format.print( System.out, "MODE    = %.3f\n", Mode(data) );
      Format.print( System.out, "STDDEV  = %.3f\n", Stddev(data) );
      double stddevs = (double) (Max(data) - Mode(data)) / (double) Stddev(data);
      Format.print( System.out, "STDDEVS = %.3f\n", stddevs );
      Format.print( System.out, "ERF     = %.5f\n", Probability.errorFunction( stddevs ) );
      Format.print( System.out, "NORMAL  = %.5f\n", Probability.normal( stddevs ) );
   }

   public static final double[][] Histogram( float[] in, int nbins ) {
      double min = (double) Min( in ), max = (double) Max( in );
      double binsize = ( max - min ) / ( (double) nbins ), bs2 = binsize / 2.0;
      double[][] out = DoubleUtils.New( 2, nbins + 1 ); int ind = 0;
      for ( double i = min; i <= max; i += binsize ) {
	 out[ 0 ][ ind ] = i;
	 for ( int j = 0, size = in.length; j < size; j ++ )
	    if ( in[ j ] != 0 && in[ j ] >= i - bs2 && in[ j ] < i + bs2 ) out[ 1 ][ ind ] ++;
	 if ( ind ++ >= nbins ) break; }
      return out; }

   public static final double[] Histogram( float[] in, float[] bins ) {
      double out[] = DoubleUtils.New( bins.length );
      for ( int i = 0, s = bins.length; i < s; i ++ ) {
	 double bin = (double) bins[ i ];
	 double lower = (double) ( i == 0 ? -Double.MAX_VALUE : ( bins[ i ] + bins[ i-1 ] ) / 2 );
	 double upper = (double) ( i == s-1 ? Double.MAX_VALUE : ( bins[ i ] + bins[ i+1 ] ) / 2 );
	 for ( int j = 0, size = in.length; j < size; j ++ )
	    if ( in[ j ] != 0 && in[ j ] >= lower && in[ j ] < upper ) out[ i ] ++;
      }
      return out;
   }

   public static final double Mode( float[] in ) {
      int nbins = Math.min( (int) ( in.length / 20.0 ), 20 );
      double[][] hist = Histogram( in, nbins );
      return hist[ 0 ][ DoubleUtils.WhereMax( hist[ 1 ] ) ]; }

   public static final float Max( float x1, float x2 ) {
      return ( x1 > x2 ? x1 : x2 ); }
   public static final float Max( float x1, float x2, float x3 ) {
      return Max( x1, Max( x2, x3 ) ); }
   public static final float Max( float x1, float x2, float x3, float x4 ) {
      return Max( Max( x1, x2 ), Max( x3, x4 ) ); }
   public static final float Min( float x1, float x2 ) {
      return ( x1 < x2 ? x1 : x2 ); }
   public static final boolean Approx( float x1, float x2 ) {
      return ( Math.abs( x1 - x2 ) < 1e-8 ); }

   public static final int Sample( float[] x ) {
      return Sample( x, x.length, 500 ); }
   public static final int Sample( float[] x, int len ) {
      return Sample( x, len, 500 ); }

   // Assumes x[] is normalized so its max. value is 1.
   public static final int Sample( float[] x, int len, int maxCount ) {
      int r1 = -1, count = 0; double r2, xx; len --;
      while( count ++ < maxCount ) {
	 r1 = IntUtils.RandChoose( 0, len );
	 xx = x[ r1 ];
	 if ( xx <= 0.0 ) { r1 = -1; continue; }
	 r2 = DoubleUtils.Random(); if ( r2 <= xx ) break;
	 r1 = -1;
      }
      return r1; }

   public static final double[] Convolve( float x[], float kern[], double out[] ) {
      if ( out == null || out.length < x.length ) out = DoubleUtils.New( x.length );
      else DoubleUtils.Zero( out );
      UnitNorm( kern );
      int ksize = kern.length, k2 = ksize / 2;
      for ( int i = 0, s = x.length, min = i-k2, max = i+k2; 
	    i < s; i ++, min ++, max ++ ) {
	 for ( int j = min, k = 0; j <= max; j ++, k ++ ) {
	    if ( j < 0 ) continue; else if ( j >= s ) break;
	    out[ i ] += (double) ( x[ j ] * kern[ k ] );
	 }
      }
      return out;
   }

   public static final double logGamma( float xx ) {
      return cern.jet.stat.Gamma.logGamma( (double) xx );
   }
   // END NO BOOL

   public static final float[] FTokenize( String str, final String tok ) { 
      return FTokenize( MyUtils.Tokenize( str, tok ) ); }

   public static final float[] FTokenize( String str[] ) { 
      return FTokenize( str, 0, str.length ); }

   public static final float[] FTokenize( String str[], int start, int end ) { 
      float[] out = New( end - start );
      for ( int i = start; i < end; i ++ ) {
   // NO BOOL
	 try { out[ i - start ] = (float) Float.parseFloat( str[ i ] ) ; } 
   // END NO BOOL
   /* BOOL ONLY
	 try { out[ i - start ] = Float.valueOf( str[ i ] ).FloatValue() ; } 
   // END BOOL ONLY */
	 catch( Exception e ) { out[ i ] = 0; } } return out; }

   public static final float[] FromVector( Vector v ) {
      if ( v == null || v.size() <= 0 ) return null;
      float out[] = New( v.size() );
      try {
	 for ( int i = 0, s = v.size(); i < s; i ++ )
	    out[ i ] = ( (Float) v.elementAt( i ) ).floatValue();
      } catch( Exception e ) { };
      return out;
   }

   public static final Vector ToVector( float[] in ) {
      if ( in == null ) return null;
      Vector v = new Vector();
      for ( int i = 0, s = in.length; i < s; i ++ ) v.addElement( new Float ( in[ i ] ) );
      return v;
   }
}
