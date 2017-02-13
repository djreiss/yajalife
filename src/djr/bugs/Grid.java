package djr.bugs;
import java.awt.*;
import java.io.*;
import djr.util.array.ObjVector;

/**
 * Class <code>Grid</code>
 *
 * @author <a href="mailto:astrodud@sourceforge.net">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Grid implements Serializable {
   static Color histogramColors[];

   int width, height, cellSize = 5, whenGC = 100;
   int maxCount = 100000;
   Griddable list[];
   ObjVector grid[][];
   int maxInd, num, lastRemoveInd;
   long steps = 0L, startingTime = -1L;
   Globals globals;
   
   public Grid( Globals glob, int width, int height, int max ) {
      this.width = width;
      this.height = height;
      this.maxCount = max;
      this.globals = glob;
      grid = new ObjVector[ width ][ height ];
      for ( int x = 0; x < width; x ++ ) for ( int y = 0; y < height; y ++ ) 
	 grid[ x ][ y ] = new ObjVector( 5 );
      list = new Griddable[ maxCount ];
   }

   public void resize( int newW, int newH ) {
      if ( newW < width || newH < height ) {
	 for ( int i = maxInd - 1; i >= 0; i -- ) {
	    Griddable g = list[ i ];
	    if ( g != null && ( g.x >= newW || g.y >= newH ) ) g.removeFromGrid();
	 }
      }
      ObjVector gg[][] = new ObjVector[ newW ][ newH ];
      int maxH = Math.min( newH, height );
      for ( int x = 0; x < newW; x ++ ) {
	 if ( x < width ) {
	    System.arraycopy( grid[ x ], 0, gg[ x ], 0, maxH );
	    if ( newH > height ) for ( int y = height; y < newH; y ++ ) 
	       gg[ x ][ y ] = new ObjVector( 5 );
	 } else for ( int y = 0; y < newH; y ++ ) gg[ x ][ y ] = new ObjVector( 5 );
      }
      this.width = newW;
      this.height = newH;
      this.grid = gg;
   }

   public boolean canAddToGrid() {
      return this.num < this.maxCount;
   }

   public boolean addToGrid( Griddable g ) {
      if ( ! addToList( g ) ) return false;
      g.inGrid = true;
      if ( ! g.isValid() ) return false;
      grid[ g.x ][ g.y ].add( g );
      num ++;
      return true;
   }

   public boolean removeFromGrid( Griddable g ) {
      if ( ! removeFromList( g ) ) return false;
      if ( ! g.inGrid ) return false;
      grid[ g.x ][ g.y ].deleteFast( g );
      g.inGrid = false;
      num --;
      return true;
   }

   public ObjVector getGriddablesAt( int x, int y ) {
      return grid[ x ][ y ];
   }

   public Griddable getGriddableAt( int x, int y ) {
      return (Griddable) grid[ x ][ y ].get( 0 );
   }

   public void moveInGrid( Griddable g, int tox, int toy ) {
      if ( ! g.inGrid ) return;
      if ( ! g.isValid() ) return;
      grid[ g.x ][ g.y ].deleteFast( g );
      g.x = tox; g.y = toy;
      grid[ g.x ][ g.y ].add( g );
   }

   public int getCountAt( int x, int y ) {
      return grid[ x ][ y ].size();
   }
   
   protected boolean addToList( Griddable g ) {
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

      Griddable[] l = list;
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

   protected boolean removeFromList( Griddable g ) {
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

   public void gcEvery( int whenGC ) {
      this.whenGC = whenGC;
   }	

   public void step( Graphics g ) {
      steps ++;
      if ( steps == 1 ) startingTime = System.currentTimeMillis();
      if ( this.maxInd <= 0 ) try { Thread.sleep( 1000 ); } catch( Exception e ) { };
      if ( g != null ) {
	 g.setColor( Color.black );
	 g.fillRect( 0, 0, width * cellSize + cellSize, height * cellSize + cellSize );
      }
      Griddable[] l = list;
      int start = Griddable.randomInt( 0, maxInd );
      boolean backwards = ( Griddable.randomInt( 0, 2 ) == 0 );
      int i = start;
      do {
	 Griddable b = l[ i ];
	 if ( b != null ) {
	    b.step();
	    if ( g != null && grid[ b.x ][ b.y ].get( 0 ) == b )
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
      if ( steps % whenGC == 0 ) {
	 //System.err.println("STEPS = "+steps+" "+num+" "+maxInd);
	 System.gc();
      }
   }

   public Griddable getAGriddable() {
      for ( int i = 0; i <= maxInd; i ++ ) {
	 Griddable b = list[ i ];
	 if ( b != null ) return b;
      }
      return null;
   }

   public boolean allAreDead() {
      return this.maxInd <= 0;
   }

   public void paintHistogram( Graphics g, int maxVal ) {
      if ( g == null ) return;
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

   public void paint( Graphics g ) {
      if ( g == null ) return;
      g.clearRect( 0, 0, width * cellSize + cellSize, height * cellSize + cellSize );
      for ( int x = 0; x < width; x ++ ) for ( int y = 0; y < height; y ++ ) {
	 ObjVector vec = grid[ x ][ y ];
	 if ( vec.size() > 0 ) paintCell( g, x, y, false );
      }
   }

   public void paintCell( Graphics g, int x, int y, boolean clearFirst ) {
      if ( g == null ) return;
      ObjVector vec = grid[ x ][ y ];
      int xx = x * cellSize, yy = y * cellSize;
      if ( clearFirst ) g.clearRect( xx, yy, cellSize, cellSize );
      if ( globals.drawOne ) ( (Griddable) vec.get( 0 ) ).draw( g, xx, yy, cellSize );
      else for ( int i = 0, size = vec.size(); i < size; i ++ ) 
	 ( (Griddable) vec.get( i ) ).draw( g, xx, yy, cellSize );
   }
}
