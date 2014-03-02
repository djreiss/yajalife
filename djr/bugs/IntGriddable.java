package djr.bugs;

/**
 * Class <code>IntGriddable</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class IntGriddable extends Griddable implements java.io.Serializable {
   public final int value;

   public IntGriddable( int value ) {
      this( null, value );
   }

   public IntGriddable( Grid g, int value ) {
      this( g, g != null ? randomInt( 0, g.width ) : 0, g != null ? randomInt( 0, g.height ) : 0, value );
   }

   public IntGriddable( Grid g, int x, int y, int value ) {
      super( g, x, y );
      this.value = value;
   }
}
