package djr.bugs;
import java.net.*;
import java.io.*;
import java.util.*;
import djr.util.array.ObjVector;

/**
 * Class <code>Resource</code>
 *
 * @author <a href="mailto:reiss@uw.edu">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class Resource extends Genome implements Serializable {
   static int[] counts = new int[ 2 ];

   int resIndex, insRequired, outsRequired, instrCount;
   long max = Long.MAX_VALUE;
   double reward[] = null;
   String name = null;
   boolean valuesDontMatter, global, test;

   public Resource( Globals g, int resIndex, String name, double rewards[], 
		    int insReq, int outsReq, boolean glob, long max, int count ) {
      super( g, null );
      this.owner = this;
      this.resIndex = resIndex;
      this.name = name;
      this.reward = rewards;
      this.insRequired = insReq;
      this.outsRequired = outsReq;
      this.global = glob;
      this.max = max;
      this.instrCount = count;
      this.valuesDontMatter = false;

      // Reclaim memory for things not needed
      this.color = null;
      this.executed = null;
      this.in = this.out = null;
      this.rewards = null;
      this.stringRep = null;
   }

   public double getRewardForIO( int[] in, int[] out, 
				       int[] alreadyRewarded ) {
      if ( alreadyRewarded[ resIndex ] >= reward.length ) { 
	 alreadyRewarded[ resIndex ] ++; 
	 return 1.0;
      }

      if ( global && globals.resCount[ resIndex ] < 1 ) return 1.0;
      double outReward = reward[ alreadyRewarded[ resIndex ] ];
      cycles = globals.max_cycles;
      age = 0;
      dead = false;
      cur = 0;
      Rcur = reg_A;
      cmut = dmut = dins = 0;
      if ( insRequired == 0 && out[ 0 ] != UNDEFINED ) return outReward; // For "get" reward
      if ( outsRequired == 0 && in[ 0 ] != UNDEFINED ) return outReward; // For "put" reward
      if ( valuesDontMatter && in[ 0 ] != UNDEFINED && out[ 0 ] != UNDEFINED ) return outReward; // For "getput" reward
      for ( int i = 0; i < outsRequired; i ++ )
	 if ( out[ i ] == UNDEFINED || out[ i ] == 0 ) return 1.0;
      for ( int i = 0; i < insRequired; i ++ )
	 if ( in[ i ] == UNDEFINED || in[ i ] == 0 ) return 1.0;
      System.arraycopy( undef, 0, reg, 0, reg.length );
      System.arraycopy( in, 0, reg, 0, Math.min( reg.length, Math.min( in.length, insRequired ) ) );
      //int saveA = reg[ Rcur ], saveB = reg[ getComplementIndex( Rcur ) ];
      this.step( instrCount );
      boolean okay = true;
      for ( int i = 0; i < outsRequired; i ++ ) {
	 int rcur = Rcur + i;
	 if ( rcur >= reg.length ) rcur = 0;
	 if ( out[ i ] != reg[ rcur ] ) { okay = false; break; }
      }
      if ( okay ) {
	 if ( test ) printTestOutput( in, out );
	 return outReward;
      }
      return 1.0;
   }

   private void printTestOutput( int[] in, int[] out ) {
      System.err.println(name+":    "+instrCount+" "+insRequired+" "+outsRequired+
			 "    "+reg[0]+" "+reg[1]+
			 "    "+in[0]+" "+in[1]+
			 "    "+out[0]+" "+out[1]);
   }

   protected void die() { }

   public static double testGlobalOutputs( Globals globals, int[] in, 
					   int[] out, int[] alreadyRewarded ) {
      int save = globals.point_mut;
      globals.point_mut = 0;
      double outd = -99999;
      int saveIndex = -1;
      Resource res[] = globals.resources;
      for ( int i = 0, size = globals.resources.length; i < size; i ++ ) {
	 Resource r = res[ i ];
	 if ( r != null && r.global ) {
	    double rreward = r.getRewardForIO( in, out, alreadyRewarded );
	    if ( rreward > outd && globals.resCount[ r.resIndex ] > 0 ) {
	       outd = rreward;
	       saveIndex = r.resIndex;
	    }
	 }	
      }
      if ( outd != -99999 && outd != 1.0 ) {
	 alreadyRewarded[ saveIndex ] ++;
	 globals.resCount[ saveIndex ] --;
	 globals.awarded[ saveIndex ] ++;
      } else {
	 outd = 1.0;
      }
      globals.point_mut = save;
      return outd;
   }

   public static double testLocalOutputs( Globals globals, int[] in, 
					  int[] out, int[] alreadyRewarded, 
					  int x, int y ) {
      int save = globals.point_mut;
      globals.point_mut = 0;
      double outd = -99999;
      ObjVector vec = globals.resGrid.getGriddablesAt( x, y );
      int saveIndex = -1, size = vec.size();
      IntGriddable saveIG = null, ig = null;
      Resource res[] = globals.resources;
      if ( size <= 0 ) return 1.0;
      for ( int i = 0; i < size; i ++ ) {
	 ig = (IntGriddable) vec.elementAt( i );
	 if ( ig == null ) break;
	 Resource rr = res[ ig.value ];
	 if ( rr == null || rr.dead ) continue; 
	 double rreward = rr.getRewardForIO( in, out, alreadyRewarded );
	 if ( rreward > outd && rreward != 1.0 ) {
	    outd = rreward;
	    saveIndex = rr.resIndex;
	    saveIG = ig;
	 }
      }
      
      if ( outd != -99999 && saveIG != null && outd != 1.0 ) {
	 alreadyRewarded[ saveIndex ] ++;
	 saveIG.removeFromGrid();
	 globals.awarded[ saveIndex ] ++;
      } else {
	 outd = 1.0;
      }
      globals.point_mut = save;
      return outd;      
   }

   public static boolean addNewGlobalResources( Globals globals ) {
      for ( int j = 0, len = globals.newGlobalResPerTurn; j < len; j ++ ) {      
	 Resource res = null;
	 int i = 0, reslen1 = globals.resCount.length, reslen2 = globals.resources.length;
	 long rc[] = globals.resCount;
	 while( res == null || ! res.global || res.resIndex >= reslen1 || 
		rc[ res.resIndex ] >= res.max ) {
	    int rand = randomInt( 0, reslen2 );
	    if ( rand >= reslen2 ) break;
	    res = globals.resources[ rand ];
	    if ( i ++ > reslen2 ) break;
	 }
	 if ( res == null || ! res.global || res.resIndex >= reslen1 || 
	      rc[ res.resIndex ] >= res.max ) continue;
	 rc[ res.resIndex ] ++;
      }
      return true;
   }

   public static boolean addNewLocalResources( Globals globals ) {
      int rlen = globals.resources.length;
      Grid resGrid = globals.resGrid;
      for ( int j = 0, len = globals.newLocalResPerTurn; j < len; j ++ ) {
	 if ( ! resGrid.canAddToGrid() ) return false;
	 Resource res = null;
	 int i = 0;
	 while( res == null || res.global ) {
	    int rand = randomInt( 0, rlen );
	    res = globals.resources[ rand ];
	    if ( i ++ > rlen ) break;
	 }
	 if ( res == null || res.global ) continue;
	 int x = Griddable.randomInt( 0, resGrid.width );
	 int y = Griddable.randomInt( 0, resGrid.height );
	 countResourcesAtLocation( globals, res.resIndex, x, y, counts );
	 if ( counts[ 0 ] >= globals.max_resources || counts[ 1 ] >= res.max ) continue;
	 IntGriddable ig = new IntGriddable( resGrid, x, y, res.resIndex );
	 ig.addToGrid();
      }
      return true;
   }

   /** Count instances of a given resource at the given location in the grid. */
   private static void countResourcesAtLocation( Globals globals, int resIndex, 
						int x, int y, int[] counts ) {
      ObjVector vec = globals.resGrid.getGriddablesAt( x, y );
      int size = vec.size();
      counts[ 0 ] = size;
      counts[ 1 ] = 0;
      for ( int i = 0; i < size; i ++ )
	 if ( ( (IntGriddable) vec.elementAt( i ) ).value == resIndex ) counts[ 1 ] ++;
   }

   public String toString() {
      Dictionary dict = getDictionary();
      String out = "   RESOURCE:     NAME = " + name + " INDEX = " + resIndex + " REWARD = " + reward[ 0 ] + "\n";
      out += ( Rcur == reg_A ? "   >A = " : "   A = " ) + ( reg[ reg_A ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_A ] ) +
	 ( Rcur == reg_B ? " >B = " : "  B = " ) + ( reg[ reg_B ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_B ] ) +
	 ( Rcur == reg_C ? " >C = " : "  C = " ) + ( reg[ reg_C ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_C ] ) + "\n";
      out += "   LEN = " + owner.genes.length + " NEG = " + negated;
      out += "\n\n";
      String pref = "   ", arrow = " > ";
      for ( int i = 0; i < owner.genes.length; i ++ ) {
	 if ( i != cur ) out += pref;
	 else out += arrow;
	 out += owner.genes[ i ] != UNDEFINED ? dict.getName( owner.genes[ i ] ) : "UNDEF";
	 if ( owner.genes[ i ] == CMD_NEG && i + 1 < owner.genes.length ) continue;
	 if ( i + 1 < owner.genes.length && isANOP( owner.genes[ i + 1 ] ) ) continue;
	 out += "\n";
      }
      return out + "\n";
   }

   public static String printStats( Globals globals ) {
      StringBuffer out = new StringBuffer();
      Resource res[] = globals.resources;
      Grid resGrid = globals.resGrid;
      for ( int i = 0, len = globals.resources.length; i < len; i ++ ) {
	 if ( res[ i ].global ) {
	    out.append( formatString( res[ i ].name + "(g):", 15 ) );
	    out.append( "AVAIL = " ).append( formatString( "" + globals.resCount[ i ], 10 ) ) 
	       .append( " AWARDED = " ).append( formatString( "" + globals.awarded[ i ], 10 ) )
	       .append( "\n" );
	 } else {
	    int avail = 0;
	    for ( int j = 0, len2 = resGrid.maxInd; j < len2; j ++ ) { 
	       IntGriddable ig = (IntGriddable) resGrid.list[ j ];
	       if ( ig != null && ig.value == i ) avail ++;
	    }
	    out.append( formatString( res[ i ].name + ":", 15 ) );
	    out.append( "AVAIL = " ).append( formatString( "" + avail, 10 ) )
	       .append( " AWARDED = " ).append( formatString( "" + globals.awarded[ i ], 10 ) )
	       .append( "\n" );
	 }
      }
      return out.toString();
   }

   public static int getNumResources( Globals globals ) {
      if ( globals.num_resources != -1 ) return globals.num_resources;
      String str = "";
      try {
	 DataInputStream dis = 
	    new DataInputStream( djr.util.MyUtils.OpenFile( globals.resFile ) );
	 int index = 0;
	 // Count the number of different resources, skip comments and empty lines
	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) continue;
	    index ++;
	 }
	 dis.close();
	 if ( index <= 0 ) throw new Exception( "resources count = " + index );
	 globals.num_resources = index;
      } catch( Exception e ) {
         System.err.println( "Could not load resources file " + globals.resFile );
	 e.printStackTrace();
      }
      return globals.num_resources;
   }

   static void initializeResources( Globals globals, String resourcesFile ) {
      globals.resFile = resourcesFile;
      String str = "";
      try {
	 int index = getNumResources( globals );
	 DataInputStream dis = new DataInputStream( 
		      djr.util.MyUtils.OpenFile( globals.resFile ) );
	 if ( undef == null ) {
	    undef = new int[ 1024 ];
	    for ( int i = 0; i < undef.length; i ++ ) undef[ i ] = UNDEFINED;
	 }	 
	 // Read in the resources, skip comments and empty lines
	 globals.resources = new Resource[ index ];
	 globals.resCount = new long[ index ];
	 globals.awarded = new long[ index ];
	 index = 0;
	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) continue;
	    // Parse the line -- first value is the name of the resource
	    StringTokenizer t = new StringTokenizer( str );
	    String name = t.nextToken();
	    // If name ends with "_test" print out test output for this resource
	    boolean test = name.endsWith( "_test" );
	    if ( test ) name = name.substring( 0, name.length() - "_test".length() );
	    // Second value is the reward (comma-separated gives reward per # of times executed per gestation period)
	    StringTokenizer tt = new StringTokenizer( t.nextToken(), "," );
	    double rewards[] = new double[ tt.countTokens() ];
	    int iii = 0;
	    while( tt.hasMoreTokens() ) {
	       double reward = Double.valueOf( tt.nextToken() ).doubleValue();
	       rewards[ iii ++ ] = reward;
	    }
	    // 3rd value is the max # allowed (either globally or per cell)
	    int max = Integer.parseInt( t.nextToken() );
	    // 3rd value is the # of valid inputs required to allow the reward to be valid
	    int insReq = Integer.parseInt( t.nextToken() );
	    // 4th value is the # of valid outputs required to allow the reward to be valid
	    int outsReq = Integer.parseInt( t.nextToken() );
	    // 5th value is whether the resource is global (always available) or not (1 or 0)
	    int global = Integer.parseInt( t.nextToken() );
	    // 6th value is the # of instructions to be run when checking bug output
	    int instCount = Integer.parseInt( t.nextToken() );
	    // Rest of the line is the program -- read it in
	    int genes[] = new int[ t.countTokens() ];
	    System.arraycopy( undef, 0, genes, 0, genes.length );
	    int end = 0;
	    // Create the resource
	    Resource r = new Resource( globals, index, name, rewards, insReq, outsReq, (global==1?true:false), max, instCount );
	    while( t.hasMoreTokens() ) {
	       str = t.nextToken();
	       if ( str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) break;
	       boolean found = false;
	       // Use the dictionary to get the int value for the given string read in
	       int ii = r.getDictionary().getCommand( str );
	       if ( ii != UNDEFINED ) {
		  genes[ end ++ ] = ii; // And set it in the genome
		  found = true;
	       }
	       if ( ! found ) {
		  //System.err.println( "Could not find translation for " + str );
		  genes[ end ++ ] = r.getDictionary().getCommand( "nop" );
		  r.valuesDontMatter = true;
	       }
	    }
	    // Set the resource's program
	    r.genes = genes;
	    r.test = test;
	    // Add to the global table
	    globals.resources[ index ] = r;
	    if ( r.global ) globals.resCount[ index ] = max;
	    index ++;
	 }
	 dis.close();
      } catch( Exception e ) {
         System.err.println( "Could not load resources file " + globals.resFile );
	 e.printStackTrace();
      }      
   }
}
