package djr.bugs;
import java.awt.*;
import java.io.*;
import djr.util.array.ObjVector;

/**
 * Class <code>Grid</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class Grid implements Serializable {
   static Color histogramColors[];

   int width, height, cellSize = 5;
   int maxCount = 100000;
   Griddable list[];
   ObjVector grid[][];
   int maxInd, num, lastRemoveInd;
   long steps;
   
   public Grid( int width, int height, int max ) {
      this.width = width;
      this.height = height;
      this.maxCount = max;
      grid = new ObjVector[ width ][ height ];
      for ( int x = 0; x < width; x ++ ) for ( int y = 0; y < height; y ++ ) 
	 grid[ x ][ y ] = new ObjVector( 5 );
      list = new Griddable[ maxCount ];
   }

   public final boolean canAddToGrid() {
      if ( this.num >= this.maxCount ) return false;
      return true;
   }

   public final boolean addToGrid( final Griddable g ) {
      if ( ! addToList( g ) ) return false;
      g.inGrid = true;
      if ( ! g.isValid() ) return false;
      ObjVector vec = grid[ g.x ][ g.y ];
      vec.addElement( g );
      num ++;
      return true;
   }

   public final boolean removeFromGrid( final Griddable g ) {
      if ( ! removeFromList( g ) ) return false;
      if ( ! g.inGrid ) return false;
      grid[ g.x ][ g.y ].removeElement( g );
      g.inGrid = false;
      num --;
      return true;
   }

   public final ObjVector getGriddablesAt( int x, int y ) {
      return grid[ x ][ y ];
   }

   public final Griddable getGriddableAt( int x, int y ) {
      return (Griddable) grid[ x ][ y ].elementAt( 0 );
   }

   public final void moveInGrid( final Griddable g, final int tox, final int toy ) {
      if ( ! g.inGrid ) return;
      if ( ! g.isValid() ) return;
      grid[ g.x ][ g.y ].removeElement( g );
      g.x = tox; g.y = toy;
      grid[ g.x ][ g.y ].addElement( g );
   }

   public final int getCountAt( int x, int y ) {
      return grid[ x ][ y ].size();
   }
   
   protected final boolean addToList( final Griddable g ) {
      if ( g.inList ) return false;
      if ( lastRemoveInd >= 0 && list[ lastRemoveInd ] == null ) {
	 list[ lastRemoveInd ] = g;
	 if ( lastRemoveInd > maxInd ) maxInd = lastRemoveInd;
	 g.index = lastRemoveInd;
	 g.inList = true;
	 lastRemoveInd = -1;
	 return true;
      }
      /*if ( maxInd < maxCount-1 && list[ maxInd + 1 ] == null ) {
	 maxInd ++;
	 list[ maxInd ] = g;
	 if ( lastRemoveInd == maxInd ) lastRemoveInd = -1;
	 g.index = maxInd;
	 return true;
	 }*/

      final Griddable[] l = list;
      for ( int i = 0; i < maxCount; i ++ ) {
         if ( l[ i ] == null ) {
            l[ i ] = g;
            if ( i > maxInd ) maxInd = i;
	    g.index = i;
	    g.inList = true;
            return true;
         }
      }
      return false;
   }

   protected final boolean removeFromList( final Griddable g ) {
      if ( ! g.inList ) return false;
      if ( list[ g.index ] == g ) {
         list[ g.index ] = null;
	 lastRemoveInd = g.index;
	 g.index = -1;
	 g.inList = false;
         return true;
      }
      return false;
   }

   public final boolean step( Graphics g, int skipHowManyForDrawing ) {
      steps ++;
      if ( this.maxInd <= 0 ) try { Thread.sleep( 1000 ); } catch( Exception e ) { };
      final boolean drawIt = ( g != null && steps % skipHowManyForDrawing == 0 );
      if ( drawIt ) g.clearRect( 0, 0, width * cellSize + cellSize, height * cellSize + cellSize );
      final Griddable[] l = list;
      int start = Griddable.randomInt( 0, maxInd );
      boolean backwards = ( Griddable.randomInt( 0, 2 ) == 0 );
      int i = start;
      do {
	 Griddable b = l[ i ];
	 if ( b != null ) {
	    b.step();
	    if ( drawIt && grid[ b.x ][ b.y ].elementAt( 0 ) == b )
	       paintCell( g, b.x, b.y, false );
	 }
	 if ( ! backwards ) {
	    i ++;
	    if ( i > maxInd || i >= l.length ) i = 0;
	 } else {
	    i --;
	    if ( i < 0 ) i = maxInd - 1;
	 }
      } while( i != start && i >= 0 );
      while ( l[ maxInd ] == null && maxInd > 0 ) maxInd --;
      if ( steps % 100 == 0 ) {
	 //System.err.println("STEPS = "+steps+" "+num+" "+maxInd);
	 System.gc();
      }
      return drawIt;
   }

   public final Griddable getAGriddable() {
      for ( int i = 0; i <= maxInd; i ++ ) {
	 Griddable b = list[ i ];
	 if ( b != null ) return b;
      }
      return null;
   }

   public final boolean allAreDead() {
      return this.maxInd <= 0;
   }

   public final void paintHistogram( Graphics g, int maxVal, int skipHowManyForDrawing ) {
      final boolean drawIt = ( steps % skipHowManyForDrawing == 0 );
      if ( g == null || ! drawIt ) return;
      if ( histogramColors == null || histogramColors.length < maxVal ) {
	 histogramColors = new Color[ maxVal + 1 ];
	 for ( int i = 255/maxVal, j = 0; i <= 255; i += 255/maxVal, j ++ ) 
	    histogramColors[ j ] = new Color( i, i, i );
      }
      for ( int xx = 0; xx < width; xx ++ ) {
	 for ( int yy = 0; yy < height; yy ++ ) {
	    int count = getCountAt( xx, yy );
	    if ( count > 0 ) g.setColor( histogramColors[ Math.min( count - 1, maxVal ) ] );
	    else g.setColor( Color.black );
	    g.fillRect( xx * cellSize, yy * cellSize, cellSize, cellSize );
	 }
      }
   }

   public final void paint( Graphics g ) {
      if ( g == null ) return;
      g.clearRect( 0, 0, width * cellSize + cellSize, height * cellSize + cellSize );
      for ( int x = 0; x < width; x ++ ) for ( int y = 0; y < height; y ++ ) {
	 ObjVector vec = grid[ x ][ y ];
	 if ( vec.size() > 0 ) paintCell( g, x, y, false );
      }
   }

   public final void paintCell( Graphics g, final int x, final int y, boolean clearFirst ) {
      if ( g == null ) return;
      ObjVector vec = grid[ x ][ y ];
      int xx = x * cellSize, yy = y * cellSize;
      if ( clearFirst ) g.clearRect( xx, yy, cellSize, cellSize );
      //for ( int i = 0, size = vec.size(); i < size; i ++ ) 
      // ( (Griddable) vec.elementAt( i ) ).draw( g, xx, yy, cellSize );
      ( (Griddable) vec.elementAt( 0 ) ).draw( g, xx, yy, cellSize );
   }
}
