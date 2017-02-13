package djr.nr;

import djr.util.array.*;

/**
 * Class <code>NonlinearFit</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public abstract class NonlinearFit {
   public abstract double funcs( double x, double a[], double dyda[], int ma);

   public double mrqmin(double x[], double y[], double sig[], int ndata, double a[], int ia[], 
			int ma, double covar[][], double alpha[][],
			double alamda) throws NRException {
      /* Levenberg-Marquardt method, attempting to reduce the
	 value chisq of a fit between a set of data points x[1..ndata],
	 y[1..ndata] with individual standard deviations sig[1..ndata], and a
	 nonlinear function dependent on ma coeffcients a[1..ma]. The input
	 array ia[1..ma] indicates by nonzero entries those components of a
	 that should be fitted for, and by zero entries those components that
	 should be held fixed at their input values. The program returns
	 current best-fit values for the parameters a[1..ma], and
	 chisq. The arrays covar[1..ma][1..ma], alpha[1..ma][1..ma] are used
	 as working space during most iterations. Supply a routine
	 funcs(x,a,yfit,dyda,ma) that evaluates the fitting function yfit, and
	 its derivatives dyda[1..ma] with respect to the fitting parameters a at
	 x. On the first call provide an initial guess for the parameters a,
	 and set alamda<0 for initialization (which then sets alamda=.001). If a
	 step succeeds chisq becomes smaller and alamda decreases by a
	 factor of 10. If a step fails alamda grows by a factor of 10. You
	 must call this routine repeatedly until convergence is
	 achieved. Then, make one final call with alamda=0, so that
	 covar[1..ma][1..ma] returns the covariance matrix, and alpha the
	 curvature matrix. (Parameters held fixed will return zero covariances.) */

      int j,k,l, mfit = 0;
      double chisq = 0,ochisq = 0,atry[] = null,beta[] = null,da[] = null,oneda[][] = null;
      if (alamda < 0.0) { /* Initialization. */
	 atry=DoubleUtils.New(ma);
	 beta=DoubleUtils.New(ma);
	 da=DoubleUtils.New(ma);
	 for (mfit=0,j=1;j<=ma;j++) if (ia[j] != 0) mfit++;
	 oneda=DoubleUtils.New(mfit,1);
	 alamda=0.001;
	 chisq = mrqcof(x,y,sig,ndata,a,ia,ma,alpha,beta,chisq);
	 ochisq=chisq;
	 for (j=1;j<=ma;j++) atry[j]=a[j];
      } for (j=1;j<=mfit;j++) { /* Alter linearized fitting matrix, by augmenting 
				   diagonal elements. */
	 for (k=1;k<=mfit;k++) covar[j][k]=alpha[j][k];
	 covar[j][j]=alpha[j][j]*(1.0+(alamda));
	 oneda[j][1]=beta[j];
      } 
      gaussj(covar,mfit,oneda,1); /* Matrix solution. */ 
      for (j=1;j<=mfit;j++) da[j]=oneda[j][1];
      if (alamda == 0.0) { /* Once converged, evaluate covariance matrix. */
	 covsrt(covar,ma,ia,mfit);
	 covsrt(alpha,ma,ia,mfit); /* Spread out alpha to its full size too. */
	 return chisq;
      } 
      for (j=0,l=1;l<=ma;l++) /* Did the trial succeed? */
	 if (ia[l] != 0) atry[l]=a[l]+da[++j];
      chisq = mrqcof(x,y,sig,ndata,atry,ia,ma,covar,da,chisq);
      if (chisq < ochisq) { /* Success, accept the new solution. */
	 alamda *= 0.1;
	 ochisq=(chisq);
	 for (j=1;j<=mfit;j++) { 
	    for (k=1;k<=mfit;k++) alpha[j][k]=covar[j][k];
	    beta[j]=da[j];
	 }
	 for (l=1;l<=ma;l++) a[l]=atry[l];
      } else { /* Failure, increase alamda and return. */
	 alamda *= 10.0;
	 chisq=ochisq;
      } 
      return chisq;
   }

   protected double mrqcof(double x[], double y[], double sig[], int ndata, double a[], 
			   int ia[], int ma, double alpha[][], double beta[], double chisq ) {
      /* Used by mrqmin to evaluate the linearized fitting matrix alpha, and vector beta
	 as in (15.5.8), and calculate chi2sq. */
      int i,j,k,l,m,mfit=0;
      double ymod,wt,sig2i,dy,dyda[];
      dyda=DoubleUtils.New(ma);
      for (j=1;j<=ma;j++) if (ia[j] != 0) mfit++;
      for (j=1;j<=mfit;j++) { /* Initialize (symmetric) alpha, beta. */
	 for (k=1;k<=j;k++) alpha[j][k]=0.0;
	 beta[j]=0.0;
      } 
      chisq=0.0;
      for (i=1;i<=ndata;i++) { /* Summation loop over all data. */
	 ymod = funcs(x[i],a,dyda,ma);
	 sig2i=1.0/(sig[i]*sig[i]);
	 dy=y[i]-ymod;
	 for (j=0,l=1;l<=ma;l++) { 
	    if (ia[l] != 0) { 
	       wt=dyda[l]*sig2i;
	       for (j++,k=0,m=1;m<=l;m++) if (ia[m] != 0) alpha[j][++k] += wt*dyda[m];
	       beta[j] += dy*wt;
	    } 
	 } 
	 chisq += dy*dy*sig2i; /*And find chisq. */
      } 
      for (j=2;j<=mfit;j++) /* Fill in the symmetric side. */
	 for (k=1;k<j;k++) alpha[k][j]=alpha[j][k];
      return chisq;
   }

   final static void SWAP(double a,double b) { double swap=a;a=b;b=swap; }
   
   protected void covsrt(double covar[][], int ma, int ia[], int mfit) {
      /* Expand in storage the covariance matrix covar, so as to take into account 
	 parameters that are being held fixed. (For the latter, return zero covariances.) */
      int i,j,k;
      double swap;
      for (i=mfit+1;i<=ma;i++) 
	 for (j=1;j<=i;j++) covar[i][j]=covar[j][i]=0.0;
      k=mfit;
      for (j=ma;j>=1;j--) { 
	 if (ia[j] != 0) { 
	    for (i=1;i<=ma;i++) SWAP(covar[i][k],covar[i][j]);
	    for (i=1;i<=ma;i++) SWAP(covar[k][i],covar[j][i]);
	    k--;
	 } 
      } 
   }

   protected void gaussj(double a[][], int n, double b[][], int m) throws NRException {
      /* Linear equation solution by Gauss-Jordan elimination,
         equation (2.1.1) above. a[1..n][1..n] is the input
         matrix. b[1..n][1..m] is input containing the m right-hand
         side vectors. On output, a is replaced by its matrix inverse,
         and b is replaced by the corresponding set of solution
         vectors. */
      int indxc[],indxr[],ipiv[];
      int i,icol = 0,irow = 0,j,k,l,ll;
      double big,dum,pivinv,temp;
      indxc=IntUtils.New(n); 
      /* The integer arrays ipiv, indxr, and indxc are used for bookkeeping on the pivoting. */
      indxr=IntUtils.New(n);
      ipiv=IntUtils.New(n);
      for (j=1;j<=n;j++) ipiv[j]=0;
      for (i=1;i<=n;i++) { /* This is the main loop over the columns to be reduced. */
	 big=0.0;
	 for (j=1;j<=n;j++) /* This is the outer loop of the search for a pivot element. */
	    if (ipiv[j] != 1) for (k=1;k<=n;k++) { 
	       if (ipiv[k] == 0) { 
		  if (Math.abs(a[j][k]) >= big) { 
		     big=Math.abs(a[j][k]);
		     irow=j;
		     icol=k;
		  } 
	       } else if (ipiv[k] > 1) throw new NRException("gaussj: Singular Matrix-1");
	    } 
	 ++ ipiv[icol];
	 /* We now have the pivot element, so we interchange rows, if
            needed, to put the pivot element on the diagonal. The
            columns are not physically interchanged, only relabeled:
            indxc[i], the column of the ith pivot element, is the ith
            column that is reduced, while indxr[i] is the row in which
            that pivot element was originally located. If indxr[i] 6 =
            indxc[i] there is an implied column interchange. With this
            form of bookkeeping, the solution b's will end up in the
            correct order, and the inverse matrix will be scrambled by
            columns. */
	 if (irow != icol) { 
	    for (l=1;l<=n;l++) SWAP(a[irow][l],a[icol][l]);
	    for (l=1;l<=m;l++) SWAP(b[irow][l],b[icol][l]);
	 } 
	 indxr[i]=irow;
	 /* We are now ready to divide the pivot row by the pivot element, 
	    located at irow and icol. */
	 indxc[i]=icol;
	 if (a[icol][icol] == 0.0) throw new NRException("gaussj: Singular Matrix-2");
	 pivinv=1.0/a[icol][icol];
	 a[icol][icol]=1.0;
	 for (l=1;l<=n;l++) a[icol][l] *= pivinv;
	 for (l=1;l<=m;l++) b[icol][l] *= pivinv;
	 for (ll=1;ll<=n;ll++) /* Next, we reduce the rows... */
	    if (ll != icol) { /* ...except for the pivot one, of course. */
	       dum=a[ll][icol];
	       a[ll][icol]=0.0;
	       for (l=1;l<=n;l++) a[ll][l] -= a[icol][l]*dum;
	       for (l=1;l<=m;l++) b[ll][l] -= b[icol][l]*dum;
	    }
      } 
      /* This is the end of the main loop over columns of the
         reduction. It only remains to unscram- ble the solution in
         view of the column interchanges. We do this by interchanging
         pairs of columns in the reverse order that the permutation
         was built up. */
      for (l=n;l>=1;l--) { 
	 if (indxr[l] != indxc[l]) for (k=1;k<=n;k++) SWAP(a[k][indxr[l]],a[k][indxc[l]]);
      } /* And we are done. */
   }
}
