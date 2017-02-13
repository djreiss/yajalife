package djr.nr;

import djr.util.array.*;

/**
 * Class <code>KSTest</code>
 * Use the kstwo method from Numerical Recipes
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class KSTest {
   double d, prob;

   public KSTest( double data1[], double data2[] ) {
      int j1=0,j2=0;
      double d1,d2,dt,en1,en2,en,fn1=0.0,fn2=0.0;
      DoubleUtils.Sort( data1 );
      DoubleUtils.Sort( data2 );
      int n1 = data1.length, n2 = data2.length;
      en1=n1;
      en2=n2;
      d=0.0;
      while (j1 < n1 && j2 < n2) {
	 if ((d1=data1[j1]) <= (d2=data2[j2])) fn1=j1++/en1;
	 if (d2 <= d1) fn2=j2++/en2;
	 if ((dt=Math.abs(fn2-fn1)) > d) d=dt;
      }
      en=Math.sqrt(en1*en2/(en1+en2));
      prob=probks((en+0.12+0.11/en)*(d));
   }

   public double getProb() { return prob; }

   final double EPS1 = 0.001;
   final double EPS2 = 1.0e-8;

   double probks( double alam ) {
      int j;
      double a2,fac=2.0,sum=0.0,term,termbf=0.0;

      a2 = -2.0*alam*alam;
      for (j=1;j<=100;j++) {
	 term=fac*Math.exp(a2*j*j);
	 sum += term;
	 if (Math.abs(term) <= EPS1*termbf || Math.abs(term) <= EPS2*sum) return sum;
	 fac = -fac;
	 termbf=Math.abs(term);
      }	
      return 1.0;
   }
}
