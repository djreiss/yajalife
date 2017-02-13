package djr.nr;
import djr.util.array.*;

/**
 * Class <code>NDFunction</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public abstract class NDFunction implements Function {
   public int ndims;
   public double[] params;

   public NDFunction() { super(); }

   public NDFunction( int ndims ) { 
      this.ndims = ndims;
      params = new double[ ndims ]; 
   }

   public abstract double evaluate();

   public Function duplicate() {
      NDFunction funk = null;
      try { 
	 funk = (NDFunction) getClass().newInstance();
	 funk.ndims = this.ndims;
	 funk.params = DoubleUtils.New( this.params );
	 //System.err.println("HERE1");
	 return funk;
      } catch( Exception e ) {
	 funk = (NDFunction) djr.util.MyUtils.DeepCopy( this ); 
	 //System.err.println("HERE2"); e.printStackTrace();
      }
      return funk;
   }

   // As a test, try a simple parabola
   /*public static void main( String args[] ) {
      double pars[] = new double[] { 2.678, 4.123, 1.987 };
      System.out.print( "PARS = " );
      DoubleUtils.Printf( "%.3f   ", pars );

      class Test extends NDFunction {
	 double inpars[];
	 public Test() { super(); }
	 public Test( int ndims, double inpars[] ) { 
	    super( ndims ); this.inpars = inpars; }
	 public Function duplicate() { 
	    Test out = (Test) super.duplicate(); out.inpars = inpars; return out; }
	 public double evaluate() {
	    return inpars[ 0 ] + inpars[ 1 ] *
	       ( params[ 0 ] - inpars[ 2 ] ) * ( params[ 0 ] - inpars[ 2 ] ); 
	 } };

      NDFunction funk = new Test( 1, pars );
      funk.params[ 0 ] = 100;
      System.out.print( "START = " );
      DoubleUtils.Printf( "%.3f   ", funk.params );

      AmoebaSA amoeba = new AmoebaSA( funk );

      NDFunction out = null;
      try { out = amoeba.execute( 1e-9, 1.0 ); }
      catch( Exception e ) { e.printStackTrace(); }

      System.out.print( "OUT = " );
      DoubleUtils.Printf( "%.3f   ", out.params );
   }
   */
}
