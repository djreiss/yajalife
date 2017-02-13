package djr.bugs;
import java.io.*;
import java.util.*;

/**
 * Class <code>Dictionary</code>
 *
 * @author <a href="mailto:astrodud@sourceforge.net">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Dictionary implements Serializable {
   Hashtable lut; // Maps string -> int
   String[] invLut; // Maps int -> string
   boolean[] isNOP; // Maps int -> boolean that tells if command is a NOP
   int isNOPlength;
   int length = 0; // Number of different "words" in dictionary

   public Dictionary( String codebase, String file ) {
      clearAll();
      readTable( codebase, file );
   }

   public String getName( int cmd ) {
      return ( cmd >= 0 && cmd < invLut.length ? invLut[ cmd ] : "" + cmd );
   }

   public int getCommand( String name ) {
      Integer ii = (Integer) lut.get( name );
      return ii != null ? ii.intValue() : Integer.MAX_VALUE;
   }

   public boolean isANOP( int cmd ) {
      return cmd >= isNOPlength || cmd < 0 ? false : isNOP[ cmd ];
   }
   
   public int getLength() {
      return this.length;
   }   

   public void clearAll() {
      this.lut = null;
      this.invLut = null;
      this.isNOP = null;
      this.length = 0;
   }

   public String toString() {
      return "Dictionary length=" + this.length + "; " + lut != null ? lut.toString() : null;
   }

   public void readTable( String codebase, String inFile ) {
      String str;
      if ( lut == null ) lut = new Hashtable();
      try {
	 DataInputStream dis = 
	    new DataInputStream( djr.util.MyUtils.OpenFile( inFile ) );
	 int newLength = 0;

	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) continue;
	    StringTokenizer t = new StringTokenizer( str );
	    String j = t.nextToken();
	    if ( "import".equals( j ) ) {
	       readTable( codebase, t.nextToken() );
	       continue;
	    }
	    newLength ++;
	 }
	 dis.close();

	 dis = new DataInputStream( djr.util.MyUtils.OpenFile( inFile ) );
	 int i = length, oldLength = length;
	 length += newLength;

	 if ( invLut == null ) invLut = new String[ length ];
	 else if ( invLut.length < length ) {
	    String[] temp = new String[ length ];
	    for ( int ii = 0; ii < invLut.length; ii ++ ) temp[ ii ] = invLut[ ii ];
	    invLut = temp;
	 }
	 if ( isNOP == null ) isNOP = new boolean[ length ];
	 else if ( isNOP.length < length ) {
	    boolean[] temp = new boolean[ length ];
	    for ( int ii = 0; ii < isNOP.length; ii ++ ) temp[ ii ] = isNOP[ ii ];
	    isNOP = temp;
	 }
	 isNOPlength = isNOP.length;
	 
	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) || str.startsWith( "import" ) ) continue;
	    StringTokenizer t = new StringTokenizer( str );
	    String j = t.nextToken();
	    invLut[ i ] = j;
	    if ( j.toLowerCase().indexOf( "nop" ) >= 0 ) isNOP[ i ] = true;
	    else isNOP[ i ] = false;
	    lut.put( j, new Integer( i ++ ) );
	    if ( i > length ) throw new Exception( "i = " + i + " is greater than length " + length );
	 }
	 dis.close();	 
      } catch( Exception e ) {
         System.err.println( "Exception " + e.toString() + "; could not load " + inFile );
      }
   }
}
