package djr.bugs;
import java.awt.*;
import java.io.*;

/**
 * Class <code>Griddable</code>
 *
 * @author <a href="mailto:reiss@uw.edu">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Griddable implements Serializable {
   public Grid grid;
   public boolean inGrid, inList;
   public int index;
   public int x = -1, y = -1;
   public long id;
   Color color;

   static long count = 0;
   static java.util.Random rand = new java.util.Random( System.currentTimeMillis() );
   //static { djr.util.array.DoubleUtils.SetSeed(); }

   public Griddable() {
      this( null );
   }

   public Griddable( Grid g ) {
      this( g, g != null ? randomInt( 0, g.width ) : 0, g != null ? randomInt( 0, g.height ) : 0 );
   }

   public Griddable( Grid g, int x, int y ) {
      this.grid = g;
      setX( x );
      setY( y );
      this.id = count ++;
   }

   public boolean addToGrid() {
      return grid != null ? grid.addToGrid( this ) : false;
   }

   public boolean removeFromGrid() {
      return grid != null ? grid.removeFromGrid( this ) : false;
   }

   public boolean isValid() {
      return ( grid != null && x >= 0 && y >= 0 && inGrid && inList );
   }

   public void setX( int xx ) {
      if ( grid == null ) return;
      if ( xx > grid.width - 1 ) xx = xx % grid.width - 1;
      else if ( xx < 0 ) xx = xx % grid.width - 1 + grid.width;
      this.x = xx;
   }

   public void setY( int yy ) {
      if ( grid == null ) return;
      if ( yy > grid.height - 1 ) yy = yy % grid.height - 1;
      else if ( yy < 0 ) yy = yy % grid.height - 1 + grid.height;
      this.y = yy;
   }
   
   public static int randomInt( int min, int max ) {
      //if ( max <= min ) return max;
      return ( Math.abs( rand.nextInt() ) % ( max - min ) ) + min;
      //return djr.util.array.IntUtils.Random( min, max );
   }   

   public String toString() {
      StringBuffer out = new StringBuffer();
      out.append( "   " ).append( formatString( "ID = " + id, 18 ) );
      out.append( " " ).append( formatString( "INDEX = " + index, 18 ) ).append( "\n" );
      out.append( "   " ).append( formatString( "X = " + x, 18 ) );
      out.append( " " ).append( formatString( "Y = " + y, 18 ) ).append( "\n" );
      return out.toString();
   }

   public static String formatString( String s, int numSpaces ) {
      int l = s.length();
      if ( l > numSpaces ) {
	 s = s.substring( 0, numSpaces );
      } else {
	 String space = " ";
	 StringBuffer ss = new StringBuffer( s );
	 for ( int i = 0; i < numSpaces - l; i ++ ) ss.append( space );
	 s = ss.toString();
      }
      return s;
   }

   public void step() { }

   public void draw( Graphics g, int xx, int yy, int cs ) {
      if ( grid == null || g == null || color == null ) return;
      g.setColor( color );
      g.fillRect( xx, yy, cs, cs );
   }

   protected void move( int tox, int toy ) {
      if ( tox == x && toy == y ) return;
      if ( tox < 0 ) tox = grid.width - 1;
      if ( tox > grid.width - 1 ) tox = 0;
      if ( toy < 0 ) toy = grid.height - 1;
      if ( toy > grid.height - 1 ) toy = 0;
      grid.moveInGrid( this, tox, toy );
   }
}
