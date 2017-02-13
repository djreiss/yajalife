package djr.util.array;

/**
 * Template class <code>FloatVector</code>
 * See djr.util.Utils.java for a description of what goes on here.
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class FloatVector extends MyVector {
   protected float[] data;

   public FloatVector() {
      this( 10 );
   }

   public FloatVector( int initialSize ) {
      this( initialSize, -1 );
   }

   public FloatVector( int initialSize, int growth ) {
      data = new float[ initialSize ];
      this.inuse = 0;
      this.growth = growth;
      this.initialSize = initialSize;
   }

   public FloatVector( float arr[] ) {
      this( arr.length );
      System.arraycopy( arr, 0, data, 0, arr.length );
      this.inuse = arr.length;
      this.initialSize = arr.length;
   }

   public void add( float value ) {
      if ( size() >= data.length ) {
	 int leng = data.length;
	 float temp[] = new float[ growth <= 0 && leng > 0 ? leng * 2 : leng + growth ];
	 System.arraycopy( data, 0, temp, 0, size() );
	 data = temp;
      }
      data[ inuse ++ ] = value;
   }

   public void addElement( float value ) {
      add( value );
   }

   public float get( int which ) {
      if ( which < 0 || which > data.length ) return 0;
      return data[ which ];
   }

   public float elementAt( int which ) /*throws ArrayIndexOutOfBoundsException*/ {
      //if ( which < 0 || which >= size() ) 
      //	 throw new ArrayIndexOutOfBoundsException( which );
      return get( which );
   }

   public float set( int which, float value ) {
      if ( which < 0 || which > data.length ) return 0;
      data[ which ] = value;
      return value;
   }

   public float setElementAt( int which, float value ) /*throws ArrayIndexOutOfBoundsException*/ {
      //if ( which < 0 || which >= size() ) 
      // throw new ArrayIndexOutOfBoundsException( which );
      return set( which, value );
   }

   public float remove( int which ) {
      if ( which < 0 || which > data.length ) return 0;
      int j = size() - which - 1;
      float out = data[ which ];
      if ( j > 0 ) System.arraycopy( data, which + 1, data, which, j);
      data[ -- inuse ] = 0;
      return out;
   }

   public float removeFast( int which ) {
      if ( which < 0 || which > data.length ) return 0;
      float out = data[ which ];
      data[ which ] = data[ inuse - 1 ];
      data[ -- inuse ] = 0;
      return out;
   }

   public float removeElementAt( int which ) /*throws ArrayIndexOutOfBoundsException*/ {
      //if ( which < 0 || which >= size() ) 
      //   throw new ArrayIndexOutOfBoundsException( which );
      return remove( which );
   }

   public boolean removeAll() {
      if ( size() <= 0 ) return false;
      inuse = 0;
      //java.util.Arrays.fill( data, (float) 0 );
      data = new float[ initialSize ];
      return true;
   }

   public boolean clear() { return removeAll(); }
   public boolean erase() { return removeAll(); }

   public int indexOf( float val ) {
      for ( int i = 0, s = size(); i < s; i ++ ) if ( val == data[ i ] ) return i;
      //int out = java.util.Arrays.binarySearch( (float[]) data, val );
      //if ( out >= 0 && out < size() ) return out;
      return -1;
   }

   public boolean contains( float val ) {
      return indexOf( val ) >= 0;
   }

   public boolean delete( float val ) {
      return removeElement( val );
   }

   public boolean deleteFast( float val ) {
      int ind = indexOf( val );
      if ( ind >= 0 ) { removeFast( ind ); return true; }
      return false;
   }

   public boolean removeElement( float val ) {
      int ind = indexOf( val );
      if ( ind >= 0 ) { remove( ind ); return true; }
      return false;
   }

   public int capacity() { return data.length; }

   /*public int search( float o ) {
      int i = FloatUtils.LastIndexOf( data, o );
      if ( i >= 0 ) return size() - i;
      return -1;
      }*/

   public float[] toArray() { return data(); }

   public float[] rawData() { return data; }

   public float[] data() {
      if ( capacity() == size() ) return data; 
      float out[] = new float[ size() ];
      System.arraycopy( data, 0, out, 0, size() );
      return out;
   }

   public float[] data( int start, int end ) {
      int len = end - start + 1;
      if ( len > size() ) return data();
      if ( end >= size() ) { end = size() - 1; len = end - start + 1; }
      float out[] = new float[ len ];
      System.arraycopy( data, start, out, 0, len );
      return out;
   }

   public float[] data( float out[] ) { 
      if ( out == null || out.length < size() ) return data();
      System.arraycopy( data, 0, out, 0, size() );
      return out;
   }

   public void removeEveryOther( int steps ) {
      for ( int i = size()-1; i > 0; i -= steps ) removeElementAt( i );
   }

   public String toString( int ind ) {
      return Float.toString( get( ind ) );
   }

   public java.util.Enumeration elements() {
      return new java.util.Enumeration() { int cur = 0;
	    public boolean hasMoreElements() { return cur < size(); }
	    public Object nextElement() { return new Float( get( cur ++ ) ); }
	    public float nextElement2() { return get( cur ++ ); }
	 };
   }
}
