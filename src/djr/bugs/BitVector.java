package djr.bugs;

/**
 * Class <code>BitVector</code>
 *
 * @author <a href="mailto:astrodud@sourceforge.net">David Reiss</a>
 * @version 1.0
 */
public class BitVector implements java.io.Serializable {
   protected final int length;
   protected final int[] bits;

   public BitVector( int len ) {
      this.length = len;
      this.bits = new int[ (length + 31) / 32 ];
   }

   public final boolean setBit( final int x, final boolean flag ) {
      if ( x >= length || x < 0 ) return false;
      final int mask = ( 0x80000000 >>> (x & 0x1f) );
      if (flag) bits[ x / 32 ] |= mask;
      else bits[ x / 32 ] &= ~mask;
      return true;
   }

   public final boolean getBit( final int x ) {
      if ( x >= length || x < 0 ) return false;
      final int mask = ( 0x80000000 >>> (x & 0x1f) );
      return ( (bits[x / 32] & mask) != 0 );
   }

   public final int countSet() {
      return countSet( 0, length ); 
   }

   public final int countSet( int start, int end ) {
      int count = 0;
      for ( int i = start; i < end; i ++ ) if ( getBit( i ) ) count ++;
      return count;
   }

   public final void complement() {
      complement( 0, length ); 
   }

   public final void complement( int start, int end ) {
      for ( int i = start, xs = (end+31)/32; i < xs; i ++ ) bits[ i ] = ~bits[ i ];
   }

   public String toString() {
      StringBuffer buf = new StringBuffer( this.length );
      for ( int i = 0; i < this.length; i ++ ) buf.append( getBit( i ) ? '1' : '0' );
      return buf.toString();
   }

   /*public static void main( String args[] ) {
      BitVector bv = new BitVector( 10 );
      for ( int i = 0; i < 10; i += 2 ) bv.setBit( i, true );
      System.out.println( bv );
      bv.complement();
      System.out.println( bv );
      }*/
}
