package djr.bugs;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.lang.reflect.*;
import djr.util.array.*;

/**
 * Class <code>Genome</code>
 *
 * @author <a href="mailto:reiss@uw.edu">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */

/* Things do to:
   1. if a bug does a jump_p into a neighbor, then the neighbor moves away, the bug's pointer
      has to revert back into it's own program
   3. if no room for a new bug on grid, replace oldest bug with new bug (right now, we just don't add the new bug)
*/

public class Genome extends Griddable implements Constants, Serializable {
   public static int CMD_NOP, CMD_NOP_A, CMD_NOP_B, CMD_NOP_C, CMD_NEG, CMD_IF_N_EQ, 
      CMD_IF_N_0, CMD_IF_BIT_1, CMD_IF_GREATER, CMD_JUMP_B, CMD_JUMP_F, CMD_CALL, 
      CMD_RETURN, CMD_RAND, CMD_SHIFT_R, CMD_SHIFT_L, CMD_BIT_1, CMD_INC, CMD_DEC, 
      CMD_ZERO, CMD_PUSH, CMD_POP, CMD_SET_NUM, CMD_ADD, CMD_SUB, CMD_NAND, CMD_NOR, 
      CMD_XOR, CMD_NOT, CMD_ORDER, CMD_DUP, CMD_ALLOCATE, CMD_COPY, CMD_READ, CMD_WRITE,
      CMD_IF_N_CPY, CMD_SWITCH_REG, CMD_SWITCH_STACK, CMD_MOD_CMUT, CMD_MOD_DD, CMD_DIVIDE, 
      CMD_SEARCH_F, CMD_SEARCH_B, CMD_INJECT, CMD_JUMP_P, CMD_GET, CMD_PUT, CMD_TURN_R, 
      CMD_TURN_L, CMD_MOVE_F, CMD_MOVE_B, CMD_GESTATE, CMD_MOD_COLOR, CMD_GROW, CMD_ARMOR,
      CMD_SENSE_R, CMD_SENSE_G, CMD_SENSE_B;

   private static int px[] = { 0, 0, 0 }, py[] = { 0, 0, 0 };

   static int undef[]; // A temp array to zero the genotype
   static int zeros[]; // A temp array to zero other arrays
   static Grid resGrid; // Grid of Resource objects
   static Dictionary dictionary; // The genome's dictionary
   static char[] tempChars; // Used for converting genome to string

   Globals globals;

   Genome owner; // Points to genome whose program we are running (usually equals this except e.g. if a jump_p was called)
   int genes[]; // holds the program
   int origLength; // holds the original length of the program
   BitVector executed; // tells what instructions in the program were/were not executed
   long cycles, age; // Keep track of cycles remaining, and total # of cycles run
   long off_cycles; // Cycles to be given to offspring on a divide
   long gestation_time; // Count cycles between allocation and division
   int direction; // Direction it's pointing
   int cur; // Index of current instruction
   int max_cmd; // Total number of different available commands to execute (from the Dictionary)
   int dd; // Drop distance (for offspring)
   int cmut, dmut, dins; // Mutation rates: copy mutation rate, divide mutation rate, and divide insert/remove mutation rate
   int reg[], Rcur; // Array of memory registers
   int stack[][], Scur, sPtr; // Array of stacks
   int in[], out[]; // Input/output buffers used to interact w/ environment
   int labels[], nLabels, compLabels[]; // Store labels for current command (and temp ones for finding complements)
   boolean negated, dead; // Is the next cmd to be negated? Is the creature dead?
   boolean hasAllocated, hasDivided; // Keep track of whether an allocation and/or divide has happened
   long mother, father, nChildren, generation; // Keep track of mother, father, and how many offspring, parents
   int tempRewards[]; // Keep track of # of resources awarded to this genome per gestation period
   int rewards[]; // Keep track of # of resources awarded over its entire life.
   String stringRep; // Keep a string representation for comparisons and hashing
   double fitness; // Keep a running tally of rewards achieved between each offspring

   int size; // Size tells how well attack/defense capabilities are used and energy needed to move
   int armor; // Tells how much attack/defense capabilities creature has

   public Genome( Globals g, String fileName ) {
      super( g.bugGrid );
      initPrivate( g, fileName );
   }

   public Genome( Genome g, int[] newGenes, int whereX, int whereY ) {
      super( g.grid );
      this.owner = this;
      setX( whereX );
      setY( whereY );
      this.genes = newGenes;
      initPrivate( g.globals, null );
      this.color = computeColor( g );
      if ( ! this.dead ) {
	 this.generation = g.generation + 1;
	 long which = g.off_cycles;
	 which *= ( 1.0 + globals.initial_cycles[ 1 ] / Math.pow( (double) generation, globals.initial_cycles[ 2 ] ) );
	 this.cycles = Math.min( globals.max_cycles, which );
	 mother = g.id;
      } else {
	 g.cycles += g.off_cycles;
	 g.cycles = Math.min( globals.max_cycles, g.cycles );
      }
      g.off_cycles = 0;
   }

   private void initPrivate( Globals g, String fileName ) {
      this.owner = this;
      this.dead = false;
      this.globals = g;
      if ( dictionary == null ) {
	 globals.commands = new Dictionary( globals.codebase, globals.dictionaryName );
	 initializeCommandVariables( getClass().getName(), globals.commands );
	 dictionary = globals.commands;
      }
      direction = 0;
      negated = hasAllocated = hasDivided = false;
      cur = 0;
      age = generation = 0;
      //cmut = dmut = dins = 1;
      dd = g.DROP_DISTANCE_DEFAULT;
      cycles = (int) g.initial_cycles[ 0 ];
      off_cycles = gestation_time = 0;
      cmut = g.copy_mut_default;
      dmut = g.divide_mut_default;
      dins = g.divide_ins_default;
      fitness = 1.0;

      if ( grid != null && grid.canAddToGrid() ) {
	 setX( x ); 
	 setY( y ); // If can't find a spot on the grid, we die. We should make it so this replaces the oldest bug instead.
	 if ( ! addToGrid() ) die();
	 if ( this.dead ) return;
      }

      if ( zeros == null ) zeros = new int[ 1024 ];
      if ( undef == null ) {
	 undef = new int[ 1024 ];
	 for ( int i = 0; i < undef.length; i ++ ) undef[ i ] = UNDEFINED;
      }

      max_cmd = dictionary.getLength();
      reg = new int[ g.NUM_REGISTERS_DEFAULT ]; Rcur = g.REGISTER_DEFAULT;
      System.arraycopy( undef, 0, reg, 0, g.NUM_REGISTERS_DEFAULT );
      stack = new int[ g.NUM_STACKS_DEFAULT ][ g.STACK_SIZE_DEFAULT ]; 
      Scur = g.STACK_DEFAULT; sPtr = 0;
      for ( int i = g.NUM_STACKS_DEFAULT - 1; i >= 0; i -- ) System.arraycopy( undef, 0, stack[ i ], 0, g.STACK_SIZE_DEFAULT );
      in = new int[ g.BUFFER_SIZE_DEFAULT ]; 
      System.arraycopy( undef, 0, in, 0, g.BUFFER_SIZE_DEFAULT );
      out = new int[ g.BUFFER_SIZE_DEFAULT ]; 
      System.arraycopy( undef, 0, out, 0, g.BUFFER_SIZE_DEFAULT );
      labels = new int[ g.MAX_LABELS ]; nLabels = 0; compLabels = null;
      mother = father = nChildren = 0;
      tempRewards = new int[ Resource.getNumResources( g ) ];
      System.arraycopy( zeros, 0, tempRewards, 0, tempRewards.length );
      rewards = new int[ Resource.getNumResources( g ) ];
      System.arraycopy( zeros, 0, rewards, 0, rewards.length );
      stringRep = null;

      // Was here
      if ( fileName != null ) readFromFile( g, fileName );

      origLength = 0;
      if ( this.genes == null ) return;
      origLength = this.genes.length;
      executed = new BitVector( origLength );
      color = computeColor( null );
   }

   private Color computeColor( Genome mom ) {
      String temp1 = this.toString2();
      Color out = null;
      if ( mom == null ) {
	 out = ( new Color( temp1.hashCode() | 0xff000000 ) ).brighter();
      } else {
	 int minColor = 150;
	 String temp2 = mom.toString2();
	 int diff = temp1.compareTo( temp2 ) / 3;
	 if ( diff == 0 ) return mom.color;
	 int which = randomInt( 0, 3 );
	 
	 int red = mom.color.getRed();
	 int green = mom.color.getGreen();
	 int blue = mom.color.getBlue();
	 if ( which == 0 ) {
	    red += diff;
	    if ( red > 255 ) red = 255 - 2 * diff;
	    if ( red < minColor ) red = minColor + 2 * diff; 
	 } else if ( which == 1 ) {
	    green += diff;
	    if ( green > 255 ) green = 255 - 2 * diff;
	    if ( green < minColor ) green = minColor + 2 * diff;
	 } else if ( which == 2 ) {
	    blue += diff;
	    if ( blue > 255 ) blue = 250 - 2 * diff;
	    if ( blue < minColor ) blue = minColor + 2 * diff;
	 }
	 out = new Color( red&0xff, green&0xff, blue&0xff );
      }
      return out;
   }

   protected void clearMemory() {
      reg = null; Rcur = globals.REGISTER_DEFAULT;
      stack = null; Scur = globals.STACK_DEFAULT;
      in = out = null;
      genes = null;
      negated = false;
      dead = true;
      //cmut = dmut = dins = 1;
      dd = globals.DROP_DISTANCE_DEFAULT;
      hasAllocated = hasDivided = false;
      cur = max_cmd = sPtr = 0;
      mother = father = nChildren = nLabels = 0;
      labels = compLabels = null;
      cycles = off_cycles = age = gestation_time = 0;
      fitness = 1.0;
      color = null;
      owner = null;
      tempRewards = rewards = null;
      stringRep = null;
      globals = null;
   }

   protected void finalize() throws Throwable {
      super.finalize();
      clearMemory();
   }

   public static void setDictionaryName( Globals g, String name ) {
      g.dictionaryName = name;
   }

   protected Dictionary getDictionary() {
      return dictionary;
   }

   public void draw( Graphics g, int xx, int yy, int cs ) {
      if ( this.dead || g == null || color == null ) return;
      if ( this.direction <= 0 || this.direction > 4 ) { super.draw( g, xx, yy, cs ); return; }
      if ( this.owner != this ) g.setColor( Color.yellow );
      else g.setColor( color );
      int cs2 = cs / 2;
      switch( direction ) {
      case SOUTH:
	 px[2] = xx; px[0] = xx+cs; px[1] = xx+cs2; 
	 py[0] = py[2] = yy; py[1] = yy+cs; break;
      case EAST:
	 px[0] = px[2] = xx; px[1] = xx+cs;
	 py[0] = yy; py[1] = yy+cs2; py[2] = yy+cs; break;
      case NORTH: 
	 px[0] = xx; px[1] = xx+cs2; px[2] = xx+cs;
	 py[1] = yy; py[0] = py[2] = yy+cs; break;
      case WEST:
	 px[1] = xx; px[0] = px[2] = xx+cs;
	 py[2] = yy; py[1] = yy+cs2; py[0] = yy+cs; break;
      }
      g.fillPolygon( px, py, 3 );
   }
   
   // Default is to step by the # of locations in the genome
   public void step() {
      if ( owner == null || owner.dead || owner.genes == null ) owner = this;
      if ( owner.genes == null ) owner.die();
      if ( owner == null || owner.dead ) return;
      try {
	 if ( globals.stepByLots ) this.step( owner.genes.length );
	 else if ( globals.stepByOne ) this.step( 1 );
	 else {
	    long steps = this.cycles / 1000;
	    if ( steps < 1 ) steps = 1;
	    this.step( steps ); // Bugs w/ more cycles get more steps
	 }
      } catch( Exception e ) {
	 e.printStackTrace();
	 if ( owner.genes == null ) owner.die();
	 else if ( this.genes == null ) die();
      }
   }

   // Do the given number of commands from the genome
   public void step( long numSteps ) {
      if ( numSteps <= 0 ) return;
      if ( owner == null || owner.dead ) owner = this;
      if ( owner != null && ! owner.dead && owner.genes == null ) owner.die();
      if ( cycles <= 0 && off_cycles > 0 ) { cycles ++; off_cycles --; }
      if ( ( cycles + off_cycles <= 0 ) || this.genes == null ) die();
      if ( this.dead || owner.dead ) return;
      for ( long i = 0; i < numSteps; i ++ ) {
	 doPointMutate();
	 cur = wrapPtr( cur );
	 int gen[] = owner.genes;
	 int cmd = gen[ cur ]; // Get the command
	 cur = wrapPtr( ++ cur ); // Increment the pointer and wrap if necessary
	 if ( cmd != UNDEFINED ) {
	    // Give executed status only if it's not a NOP command
	    if ( ! isANOP( cmd ) && executed != null && ! executed.getBit( wrapPtr( cur - 1 ) ) )
	       executed.setBit( wrapPtr( cur - 1 ), true );
	    // Get all NOPs at the current location
	    nLabels = isANOP( gen[ cur ] ) ? getLabelAt( cur, labels ) : 0; 
	    if ( nLabels > 0 ) { // Only give executed status to first NOP in the label
	       if ( executed != null && ! executed.getBit( wrapPtr( cur ) ) )
		  executed.setBit( wrapPtr( cur ), true );
	       // Increment the pointer by the size of the sequence of NOPs
	       cur = wrapPtr( cur + nLabels ); 
	       express( cmd, labels, nLabels );
	    } else express( cmd, null, -1 );
	    if ( hasAllocated && ! hasDivided ) gestation_time ++;
	    else if ( hasDivided ) { gestation_time = 0; fitness = 1.0; }
	 }
	 nLabels = 0;
	 if ( cycles > 0 ) cycles --;
	 else if ( off_cycles > 0 ) off_cycles --;
	 else { die(); return; }
	 age ++;
	 if ( age > globals.max_age ) { die(); return; }
	 if ( cycles <= 0 && off_cycles > 0 ) { cycles ++; off_cycles --; }
      }
   }

   // Make sure the instruction pointer doesn't go past the end of the program (wrap it to the beginning)
   // or past the beginning (wrap it to the end)
   protected int wrapPtr( int ptr ) {
      int len = owner.genes.length;
      if ( ptr >= len ) ptr %= len; 
      else if ( ptr < 0 ) { ptr %= len; ptr += len; }
      return ptr;
   }
   
   // Get a random command
   protected int randomCmd() {
      int out = randomInt( 0, max_cmd );
      return out;
   }

   // Is the given command a NOP?
   protected boolean isANOP( int cmd ) {
      return dictionary.isANOP( cmd );
   }

   // Get the register index for a given NOP (e.g. if cmd == CMD_NOP_A, return reg_A)
   protected int getRegIndexForNOP( int cmd ) {
      if ( cmd == CMD_NOP_C ) return reg_C;
      else if ( cmd == CMD_NOP_A ) return reg_A;
      return globals.reg_B;
   }

   // Get the complement command for the given register NOP command 
   protected int getComplementCommand( int cmd ) {
      if ( cmd == CMD_NOP_C ) return CMD_NOP_A;
      else if ( cmd == CMD_NOP_A ) return CMD_NOP_B;
      return CMD_NOP_C;
   }      

   // Get the index of the complement of the given register index
   protected int getComplementIndex( int r ) { 
      int x = r + 1;
      if ( x >= reg.length ) x = 0;
      return x;
   }

   // At a given location, get any label (sequence of only NOPs) from the genome, put it into ls[], and return its size
   // E.g. a sequence "NOP_B NOP_A" will set ls[0]=NOP_B, ls[1]=NOP_A, and return 2.
   protected int getLabelAt( int where, int[] ls ) {
      int i = 0, gen[] = owner.genes, max = globals.MAX_LABELS;
      while( i < max && isANOP( gen[ where ] ) ) {
	 ls[ i ] = gen[ where ];
	 where = wrapPtr( ++ where );
	 i ++;
      }
      return i;
   }

   // Get an instruction size (including any preceding NEGs or following NEGs and NOPs) at a given location
   protected int getInstructionSize( int where ) {
      int i = 1, gen[] = owner.genes;
      while ( i <= 10 && gen[ where ] == CMD_NEG ) {
	 where = wrapPtr( ++ where );
	 i ++;
      }
      where = wrapPtr( ++ where );
      int j = 1;
      while( j <= 10 && isANOP( gen[ where ] ) ) {
	 where = wrapPtr( ++ where );
	 i ++; j ++;
      }
      return i;
   }

   // Get an instruction size (including any preceding NEGs or following NEGs and NOPs) for the command preceding the given location
   protected int getPrecedingInstructionSize( int where ) {
      int i = 1, gen[] = owner.genes;
      where = wrapPtr( -- where );
      while( i <= 10 && ( isANOP( gen[ where ] ) || gen[ where ] == CMD_NEG ) ) {
	 where = wrapPtr( -- where );
	 i ++;
      }
      int j = 1;
      where = wrapPtr( -- where );
      while ( j <= 10 && gen[ where ] == CMD_NEG ) {
	 where = wrapPtr( -- where );
	 i ++; j ++;
      }
      return i;
   }   

   // Skip ahead or back the given number of instructions
   protected void skipCmd( int numCmds ) {
      int gen[] = owner.genes;
      if ( numCmds > gen.length || numCmds < -genes.length ) numCmds %= gen.length;
      if ( numCmds > 0 ) for ( int i = 0; i < numCmds; i ++ ) cur = wrapPtr( cur + getInstructionSize( cur ) );
      else if ( numCmds < 0 ) for ( int i = numCmds+1; i <= 0; i ++ ) cur = wrapPtr( cur - getPrecedingInstructionSize( cur ) );
   }

   // Search forward or back for NOP labels that are complementary to the given ones; return the location of those labels
   protected int searchForComplementLabels( boolean forward, int[] labs,
						  int nlabs ) {
      if ( nlabs <= 0 ) return 0;
      int ptr = cur, count = 0;
      boolean found = false;
      int gen[] = owner.genes, len = gen.length;
      if ( compLabels == null ) compLabels = new int[ globals.MAX_LABELS ];
      int[] comps = compLabels;
      for ( int i = 0; i < nlabs; i ++ ) comps[ i ] = getComplementCommand( labs[ i ] );
      while( ! found && count < len ) {
	 count ++;
	 found = true;
	 if ( ! isANOP( gen[ ptr ] ) ) {
	    found = false;
	 } else {
	    for ( int i = 0; i < nlabs; i ++ ) {
	       if ( gen[ wrapPtr( ptr + i ) ] != comps[ i ] ) {
		  found = false;
		  break;
	       }
	    }
	 }
	 if ( ! found ) {
	    if ( forward ) ptr = wrapPtr( ++ ptr );
	    else ptr = wrapPtr( -- ptr );
	 }
	 if ( ptr == cur ) break;
      }
      return ptr;
   }

   // If ! z, skip the next instruction. That's all.
   protected void doIf( boolean z ) {
      if ( ! z ) cur = wrapPtr( cur + getInstructionSize( cur ) );
   }

   // Push a value onto the current stack
   protected void pushValue( int value ) {
      if ( sPtr < stack[ Scur ].length - 1 ) sPtr ++;
      stack[ Scur ][ sPtr ] = value;
   }

   // Pop a value from the current stack and return it
   protected int popValue() {
      int out = UNDEFINED;
      if ( sPtr >= 0 ){
	 out = stack[ Scur ][ sPtr ];
	 stack[ Scur ][ sPtr ] = UNDEFINED;
	 sPtr --;
      }
      return out == UNDEFINED ? 0 : out;
   }

   // Express the command (execute it). Also passed are any labels (NOPs) following the command in the program, 
   // and the number of labels. lsize is <= 0 and labs is null, if no labels (NOPs) follow the command.
   protected boolean express( int cmd, int[] labs, int lsize ) {
      if ( cmd == UNDEFINED ) return true;
      boolean out = false, dontChangeNegated = false;
      int nop1 = ( lsize > 0 ) ? getRegIndexForNOP( labs[ 0 ] ) : Rcur; // Get index of current register (if no label, default Rcur)
      //int nop2 = ( lsize > 0 ) ? getComplementIndex( getRegIndexForNOP( labs[ 0 ] ) ) : getComplementIndex( Rcur ); // Get its complement

      if ( isANOP( cmd ) ) { out = true; }
      else if ( cmd == CMD_ADD ) { doAdd( true, nop1 ); out = true; }
      else if ( cmd == CMD_INC ) { doInc( true, nop1 ); out = true; }
      else if ( cmd == CMD_ALLOCATE ) { doAllocate( nop1 ); out = true; }
      else if ( cmd == CMD_PUSH ) { doPush( nop1 ); out = true; }
      else if ( cmd == CMD_POP ) { doPop( nop1 ); out = true; }
      else if ( cmd == CMD_COPY ) { doCopy(); out = true; }
      else if ( cmd == CMD_IF_N_EQ ) { doIf( isNotEqual( nop1 ) ); out = true; }
      else if ( cmd == CMD_JUMP_B ) { doJump( false ); out = true; }
      else if ( cmd == CMD_DIVIDE ) { doDivide( labs, lsize ); out = true; }
      else if ( cmd == CMD_GET ) { doGet( labs, lsize ); out = true; }
      else if ( cmd == CMD_PUT ) { doPut( nop1 ); out = true; }
      else if ( cmd == CMD_SEARCH_F ) { doSearch( true, labs, lsize ); out = true; }
      else if ( cmd == CMD_SEARCH_B ) { doSearch( false, labs, lsize ); out = true; }
      else if ( cmd == CMD_GESTATE ) { doGestate( nop1, labs, lsize ); out = true; }
      else if ( cmd == CMD_TURN_R ) { doTurn( true ); out = true; }
      else if ( cmd == CMD_TURN_L ) { doTurn( false ); out = true; }
      else if ( cmd == CMD_MOVE_F ) { doMove( true ); out = true; }
      else if ( cmd == CMD_MOVE_B ) { doMove( false ); out = true; }
      else if ( cmd == CMD_NEG ) { negated = true; dontChangeNegated = true; out = true; }
      else if ( cmd == CMD_READ ) { doRead(); out = true; }
      else if ( cmd == CMD_WRITE ) { doWrite(); out = true; }
      else if ( cmd == CMD_SUB ) { doAdd( false, nop1 ); out = true; }
      else if ( cmd == CMD_NAND ) { doNand( true, nop1 ); out = true; }
      else if ( cmd == CMD_NOR ) { doNor( true, nop1 ); out = true; }
      else if ( cmd == CMD_XOR ) { doXor( true, nop1 ); out = true; }
      else if ( cmd == CMD_NOT ) { doNot( nop1  ); out = true; }
      else if ( cmd == CMD_ORDER ) { doOrder( nop1 ); out = true; }
      else if ( cmd == CMD_DUP ) { doDup( nop1 ); out = true; }
      else if ( cmd == CMD_IF_N_0 ) { doIf( isNotZero( nop1 ) ); out = true; }
      else if ( cmd == CMD_IF_BIT_1 ) { doIf( isBitOne( nop1 ) ); out = true; }
      else if ( cmd == CMD_IF_GREATER ) { doIf( isGreater( nop1 ) ); out = true; }
      else if ( cmd == CMD_JUMP_F ) { doJump( true ); out = true; }
      else if ( cmd == CMD_CALL ) { doCall(); out = true; }
      else if ( cmd == CMD_RETURN ) { doReturn(); out = true; }
      else if ( cmd == CMD_RAND ) { doRand( nop1 ); out = true; }
      else if ( cmd == CMD_SHIFT_R ) { doShift( true, nop1 ); out = true; }
      else if ( cmd == CMD_SHIFT_L ) { doShift( false, nop1 ); out = true; }
      else if ( cmd == CMD_BIT_1 ) { doBitOne( nop1 ); out = true; }
      else if ( cmd == CMD_DEC ) { doInc( false, nop1 ); out = true; }
      else if ( cmd == CMD_ZERO ) { doZero( nop1 ); out = true; }
      else if ( cmd == CMD_SET_NUM ) { doSetNum( labs, lsize ); out = true; }
      else if ( cmd == CMD_IF_N_CPY ) { doIf( isNCopy() ); out = true; }
      else if ( cmd == CMD_SWITCH_REG ) { switchReg(); out = true; }
      else if ( cmd == CMD_SWITCH_STACK ) { switchStack(); out = true; }
      else if ( cmd == CMD_INJECT ) { doInject( nop1, labs, lsize ); out = true; }
      else if ( cmd == CMD_JUMP_P ) { doJumpP( nop1, labs, lsize ); out = true; }
      else if ( cmd == CMD_MOD_CMUT ) { modCMut( nop1 ); out = true; }
      else if ( cmd == CMD_MOD_DD ) { doModDD(); out = true; }
      //else if ( cmd == CMD_GROW ) { doGrow( nop1, labs, lsize ); out = true; }
      //else if ( cmd == CMD_ARMOR ) { doArmor( nop1, labs, lsize ); out = true; }
      //else if ( cmd == CMD_SENSE_R ) { doSense( RED, labs, lsize ); out = true; }
      //else if ( cmd == CMD_SENSE_G ) { doSense( GREEN, labs, lsize ); out = true; }
      //else if ( cmd == CMD_SENSE_B ) { doSense( BLUE, labs, lsize ); out = true; }
      else if ( cmd == CMD_MOD_COLOR ) { doModColor( nop1 ); out = true; }

      if ( ! dontChangeNegated ) negated = false;
      return out;
   }

   // Switch the current (default) register up or down
   protected void switchReg() {
      if ( ! negated ) if ( ++ Rcur >= reg.length ) Rcur = 0;
      else if ( -- Rcur < 0 ) Rcur = reg.length - 1;
   }

   // Switch the currently used stack
   protected void switchStack() {
      if ( ! negated ) if ( ++ Scur >= stack.length ) Scur = 0;
      else if ( -- Scur < 0 ) Scur = stack.length - 1;
   }

   // Increment/decrement the current register 
   protected void doInc( boolean inc, int Rind ) {
      if ( negated ) inc = ! inc;
      if ( inc ) reg[ Rind ] ++;
      else reg[ Rind ] --;
   }

   // Shift the current register
   protected void doShift( boolean right, int Rind ) {
      if ( negated ) right = ! right;
      if ( right ) reg[ Rind ] >>= 1;
      else reg[ Rind ] <<= 1;
   }

   // Set the current register to a random command
   protected void doRand( int Rind ) {
      reg[ Rind ] = randomCmd();
   }

   // Zero the current register
   protected void doZero( int Rind ) {
      if ( ! negated ) reg[ Rind ] = 0;
      else reg[ Rind ] = Integer.MAX_VALUE;
   }

   // Set the first bit of the current register to 1
   protected void doBitOne( int Rind ) {
      if ( ! negated ) reg[ Rind ] |= ( 1 << 31 ); // Set last bit to 1 (is this right?)
      else reg[ Rind ] |= 1; // Set first bit to 1 (is this right?)
   }

   // And-not the current register w/ its complement
   protected void doNand( boolean nand, int Rind ) {
      if ( negated ) nand = ! nand;
      if ( nand ) reg[ Rind ] &= ~reg[ getComplementIndex( Rind ) ];
      else reg[ Rind ] &= reg[ getComplementIndex( Rind ) ];
   }

   // Or-not the current register w/ its complement
   protected void doNor( boolean nor, int Rind ) {
      if ( negated ) nor = ! nor;
      if ( nor ) reg[ Rind ] |= ~reg[ getComplementIndex( Rind ) ];
      else reg[ Rind ] |= reg[ getComplementIndex( Rind ) ];
   }

   // Xor-not the current register w/ its complement
   protected void doXor( boolean xor, int Rind ) {
      if ( negated ) xor = ! xor;
      if ( xor ) reg[ Rind ] ^= ~reg[ getComplementIndex( Rind ) ];
      else reg[ Rind ] ^= reg[ getComplementIndex( Rind ) ];
   }

   // Not the current register
   protected void doNot( int Rind ) {
      if ( ! negated ) reg[ Rind ] = ~reg[ Rind ];
      else reg[ Rind ] = ~reg[ getComplementIndex( Rind ) ];
   }

   // Increment the current register by the value in its complement
   protected void doAdd( boolean add, int Rind ) {
      if ( negated ) add = ! add;
      if ( add ) reg[ Rind ] += reg[ getComplementIndex( Rind ) ];
      else reg[ Rind ] -= reg[ getComplementIndex( Rind ) ];
   }

   // Set the current register to equal its complement
   protected void doDup( int Rind ) {
      if ( ! negated ) reg[ Rind ] = reg[ getComplementIndex( Rind ) ];
      else reg[ Rind ] = ~reg[ getComplementIndex( Rind ) ];
   }

   // Switch the current register with its complement to make sure it is less than its complement
   protected void doOrder( int Rind ) {
      int comp = reg[ getComplementIndex( Rind ) ];
      if ( ( ! negated && comp < reg[ Rind ] ) || ( negated && comp > reg[ Rind ] ) ) {
	 reg[ getComplementIndex( Rind ) ] = reg[ Rind ];
	 reg[ Rind ] = comp;
      }
   }

   // Is the current register not zero?
   protected boolean isNotZero( int Rind ) {
      if ( ! negated ) return reg[ Rind ] != 0;
      return reg[ Rind ] == 0;
   }

   // Is the current register greater than its complement?
   protected boolean isGreater( int Rind ) {
      if ( ! negated ) return reg[ Rind ] > reg[ getComplementIndex( Rind ) ];
      return reg[ Rind ] < reg[ getComplementIndex( Rind ) ];
   }

   // Is the current register not equal to its complement?
   protected boolean isNotEqual( int Rind ) {
      if ( ! negated ) return reg[ Rind ] != reg[ getComplementIndex( Rind ) ];
      return reg[ Rind ] == reg[ getComplementIndex( Rind ) ];
   }

   // Is the current register's lowest bit 1?
   protected boolean isBitOne( int Rind ) {
      if ( ! negated ) return ( reg[ Rind ] & 0x00000001 ) == 1;
      return ( reg[ Rind ] & 0x00000001 ) == 0;
   }

   // Jump forward/backward -- if there is a current label, skip to the complement of the label; else
   // skip the number of commands in the current register
   protected void doJump( boolean forward ) {
      if ( negated ) forward = ! forward;
      if ( nLabels > 0 ) {
	 cur = searchForComplementLabels( forward, labels, nLabels );
	 cur = wrapPtr( cur + getInstructionSize( cur ) );
      } else skipCmd( forward ? reg[ Rcur ] : - reg[ Rcur ] );
   }

   // Push current register onto current stack
   protected void doPush( int Rind ) {
      pushValue( reg[ Rind ] );
   }

   // Pop value from current stack into current register
   protected void doPop( int Rind ) {
      reg[ Rind ] = popValue();
   }

   // Push the current instruction location onto the stack and do a jump_f
   protected void doCall() {
      pushValue( cur );
      doJump( true );
   }

   // Pop an instruction location from the stack and set the current instruction pointer to that value
   // If we're in another genome's program, return to this gene's program.
   protected void doReturn() {
      if ( this.owner != this ) this.owner = this;
      int val = popValue();
      if ( val < owner.genes.length && val >= 0 && val != UNDEFINED ) cur = val;
   }

   // Return the ternary equivalent of the given label. Take nop_A = 0, nop_B = 1, nop_C = 2.
   // So nop_C nop_A nop_B = 2 0 1 in ternary; 19 in decimal. If there is no label, set BX to 0.
   // 2 * (3^2) + 0 * (3^1) + 1 * (3^0). Double the output value if we are negated (via a "neg")
   protected int getTernary( int[] labs, int lsize ) {
      int out = 0;
      if ( lsize <= 0 ) return out;
      for ( int i = 0, j = 0; i < lsize; i ++, j ++ ) {
	 int x = labs[ i ];
	 int val = 0;
	 if ( x == CMD_NOP_A ) val = 0;
	 else if ( x == CMD_NOP_B ) val = 1;
	 else if ( x == CMD_NOP_C ) val = 2;
	 int tot = 1;
	 if ( j > 0 ) for ( int q = 0; q < j; q ++ ) tot *= globals.NUM_REGISTERS_DEFAULT;
	 out += val * tot;
      }
      return negated ? out * 2 : out;
   }

   // Set BX to ternary equivalent of the given label
   protected void doSetNum( int[] labs, int lsize ) {
      reg[ Rcur ] = getTernary( labs, lsize );
   }

   // Search forward for the complement label and return its distance in the BX register, and the size of the label that 
   // followed in CX. If the complement label is not found, the distance is 0.
   protected void doSearch( boolean forward, int[] labs, int nlabs ) {
      if ( negated ) forward = ! forward;
      int pos = searchForComplementLabels( forward, labs, nlabs );
      if ( pos == cur ) { reg[ reg_B ] = reg[ reg_C ] = 0; return; }
      if ( forward ) {
	 if ( pos < cur ) reg[ reg_B ] = owner.genes.length - cur + pos;
	 else reg[ reg_B ] = pos - cur;
      } else {
	 if ( pos > cur ) reg[ reg_B ] = cur + owner.genes.length - pos;
	 else reg[ reg_B ] = cur - pos;
      }
      reg[ reg_C ] = getInstructionSize( pos ) + nlabs;
   }

   // Copy the command at the location pointed to by BX into the location pointed to by BX + AX
   // First make sure the pointers are valid (i.e. within one wrapping of the genome)
   protected void doCopy() {
      if ( reg[ reg_B ] == UNDEFINED || reg[ reg_A ] == UNDEFINED ) return;
      int from = wrapPtr( reg[ reg_B ] );
      int to = wrapPtr( reg[ reg_B ] + reg[ reg_A ] );
      int len = this.genes.length;
      if ( from >= len || from < 0 || to >= len || to < 0 ) return;
      if ( cmut > 0 ) {
	 int rand = randomInt( 0, 1001 );
	 if ( rand < cmut ) this.genes[ to ] = randomCmd();
	 else this.genes[ to ] = this.genes[ from ];
      } else this.genes[ to ] = this.genes[ from ];
   }

   // Copy the command at the location BX from the program into CX
   // First make sure the pointer is valid (i.e. within one wrapping of the genome)   
   protected void doRead() {
      if ( reg[ reg_B ] == UNDEFINED ) return;
      int from = wrapPtr( reg[ reg_B ] );
      if ( from >= this.genes.length || from < 0 ) return;
      if ( cmut > 0 ) {
	 int rand = randomInt( 0, 1001 );
	 if ( rand < cmut ) reg[ reg_C ] = randomCmd();
	 else reg[ reg_C ] = this.genes[ from ];
      } else reg[ reg_C ] = this.genes[ from ];
   }

   // Copy the command from CX into the location BX + AX
   // First make sure the pointers are valid (i.e. within one wrapping of the genome)
   // If the copied value is not a valid command, insert a random valid command instead.
   protected void doWrite() {
      if ( reg[ reg_C ] == UNDEFINED || reg[ reg_B ] == UNDEFINED || reg[ reg_A ] == UNDEFINED ) return;
      int to = wrapPtr( reg[ reg_B ] + reg[ reg_A ] );
      if ( to >= this.genes.length || to < 0 ) return;
      if ( reg[ reg_C ] < 0 || reg[ reg_C ] >= max_cmd ) { this.genes[ to ] = randomCmd(); return; }
      if ( cmut > 0 ) {
	 int rand = randomInt( 0, 1001 );
	 if ( rand < cmut ) this.genes[ to ] = randomCmd();
	 else this.genes[ to ] = reg[ reg_C ];
      } else this.genes[ to ] = reg[ reg_C ];
   }

   // Are the commands at locations BX and BX + AX are identical?
   protected boolean isNCopy() {
      if ( reg[ reg_A ] == UNDEFINED || reg[ reg_B ] == UNDEFINED ) return false;
      int from = wrapPtr( reg[ reg_B ] );
      int to = wrapPtr( reg[ reg_B ] + reg[ reg_A ] );
      int len = this.genes.length;
      if ( from >= len || from < 0 || to > len || to < 0 ) return false;
      return from == to;
   }

   // Turn (left or right)
   protected void doTurn( boolean right ) {
      if ( direction == 0 ) direction = randomInt( NORTH, WEST + 1 );
      else {
	 if ( negated ) right = ! right;
	 if ( right ) direction ++;
	 else direction --;
	 if ( direction > WEST ) direction = NORTH;
	 else if ( direction < NORTH ) direction = WEST;
      }
   }

   // Move in the grid (forward or backwards)
   protected void doMove( boolean forward ) {
      if ( direction == 0 ) return;
      if ( this.cycles < globals.move_penalty ) return;
      int newx = x, newy = y;
      if ( negated ) forward = ! forward;
      switch ( direction ) {
      case NORTH: newy += ( forward ? -1 : 1 ); break;
      case SOUTH: newy += ( forward ? 1 : -1 ); break;
      case EAST: newx += ( forward ? 1 : -1 ); break;
      case WEST: newx += ( forward ? -1 : 1 ); break;
      }
      move( newx, newy );
      this.cycles -= globals.move_penalty;
   }
   
   // Point mutation -- do it on *this* genome's program
   protected void doPointMutate() {
      if ( globals.point_mut <= 0 ) return;
      int rand = randomInt( 0, 1000000 );
      if ( rand < globals.point_mut ) { // Too bad -- gotta do a point mutation
	 rand = randomInt( 0, this.genes.length ); // Which addres to mutate?
	 this.genes[ rand ] = randomCmd();
      }
   }

   // Add (subtract) ?BX? cycles to off_cycles. If no label, then just add
   // (subtract) one.
   protected void doGestate( int Rind, int[] labs, int lsize ) {
      long amt = (long) ( lsize <= 0 ? 1 : Math.abs( reg[ Rind ] ) );
      amt = ( negated ? -amt : amt );
      if ( amt > 0 ) {
	 if ( cycles <= 0 ) return;
	 else if ( cycles < amt ) amt = cycles;
	 if ( globals.max_cycles - off_cycles < amt ) amt = globals.max_cycles - off_cycles;
      } else {
	 if ( off_cycles <= 0 ) return;
	 else if ( off_cycles < -amt ) amt = -off_cycles;
	 if ( globals.max_cycles - cycles < -amt ) amt = - ( globals.max_cycles - cycles );
      }
      cycles = Math.min( cycles - amt, globals.max_cycles );
      off_cycles = Math.min( off_cycles + amt, globals.max_cycles );
      //System.err.println("GESTATED: "+amt+" "+cycles+" "+off_cycles);
   }

   // Allocate ?BX? instructions of memory for the new genome, and return the start location of this memory in AX.
   // Only one allocate can happen between successful divides (or else failure). Also, the allocation amount must be limited to
   // between 1/2 and 2* the genome's length (or else failure).
   protected void doAllocate( int Rind ) {
      if ( hasAllocated ) return;
      int amt = reg[ Rind ];
      int[] temp = this.genes;
      if ( amt < temp.length/globals.ACCEPTABLE_DIVIDE_RANGE || amt > globals.ACCEPTABLE_DIVIDE_RANGE*temp.length ) return;
      try {
	 int len = temp.length + amt;
	 this.genes = new int[ len ];
	 for ( int i = 0; i < len; i += 1024 ) 
	    System.arraycopy( undef, 0, this.genes, i, Math.min( len - i, undef.length ) );
	 System.arraycopy( temp, 0, this.genes, 0, Math.min( len, temp.length ) );
	 reg[ reg_A ] = temp.length;
	 hasAllocated = true;
	 hasDivided = false;
	 gestation_time = 0;
	 fitness = 1.0;
      } catch( OutOfMemoryError e ) { System.err.println( "OUT OF MEMORY IN ALLOCATE" ); }
   }

   // Split the memory in this genome at ?AX?, placing the instructions beyond the dividing point into a new program.
   // This will fail (i.e. return null) if:
   // 1. Either the mother or daughter will have <10 instructions (DONE)
   // 2. No allocation has occurred since the last divide (DONE)
   // 3. Less than 70% of the mother was executed (DONE)
   // 4. Less than 70% of the daughter's memory was copied into (DONE)
   // 5. The daughter will be <1/2 or >2* the mother's size (DONE)
   protected int[] doSplitMemory( int[] labs, int nlabs ) throws OutOfMemoryError {
      // Check that case (2) above is okay
      if ( hasDivided || ! hasAllocated ) return null;
      // Check that case (3) above is okay
      if ( getExecutedCount() * 100 / origLength < globals.MIN_PERCENT ) return null;
      int ind = nlabs > 0 ? getRegIndexForNOP( labs[ 0 ] ) : reg_A;
      int where = reg[ ind ];
      if ( where == UNDEFINED || where >= this.genes.length || where < 0 ) return null;
      where = wrapPtr( where );
      int dlength = this.genes.length - where, mlength = where;
      // Check that case(1) above is okay
      if ( dlength < globals.MIN_LENGTH || mlength < globals.MIN_LENGTH ) return null;
      // Check that case (5) above is okay
      if ( dlength < mlength / globals.ACCEPTABLE_DIVIDE_RANGE || dlength > mlength * globals.ACCEPTABLE_DIVIDE_RANGE ) return null;
      int[] temp = this.genes;

      this.genes = new int[ mlength ]; 
      System.arraycopy( temp, 0, this.genes, 0, mlength );
      int[] daughter = new int[ dlength ]; 
      System.arraycopy( temp, mlength, daughter, 0, dlength );
      if ( dmut > 0 ) { // Divide mutation
	 int rand = randomInt( 0, 101 );
	 if ( rand < dmut ) {
	    rand = randomInt( 0, daughter.length );
	    daughter[ rand ] = randomCmd();
	 }
      }
      if ( dins > 0 ) {
	 int rand = randomInt( 0, 101 );
	 if ( rand < dins ) { // An insertion/deletion mutation?
	    temp = insDel( daughter );
	    if ( temp != null ) daughter = temp;
	 }
      }
      int notundefined = 0; // Check that case (4) above is okay
      for ( int i = 0, sz = daughter.length; i < sz; i ++ ) {
	 if ( daughter[ i ] != UNDEFINED ) notundefined ++;
	 if ( notundefined * 100 / daughter.length >= globals.MIN_PERCENT ) break;
      }
      if ( notundefined * 100 / daughter.length < globals.MIN_PERCENT ) return null;
      if ( daughter != null ) {
	 hasAllocated = false;
	 hasDivided = true;
	 for ( int i = 0, sz = tempRewards.length; i < sz; i ++ ) rewards[ i ] += tempRewards[ i ];
	 System.arraycopy( zeros, 0, tempRewards, 0, tempRewards.length );
	 fitness = 1.0;
      }
      return daughter;
   }

   // Randomly insert or delete a command from the gene string
   protected int[] insDel( int[] daughter ) {
      int rand = randomInt( 0, daughter.length ); // Where to insert/delete?
      int insdel = randomInt( 0, 2 ); // Insert or delete?
      int[] temp = null;
      if ( insdel == 0 ) { // insert
	 temp = new int[ daughter.length + 1 ];
	 System.arraycopy( daughter, 0, temp, 0, rand );
	 temp[ rand ] = randomCmd();
	 System.arraycopy( daughter, rand, temp, rand + 1, daughter.length - rand );
      } else { // delete
	 temp = new int[ daughter.length - 1 ];
	 if ( rand > 0 ) System.arraycopy( daughter, 0, temp, 0, rand - 1 );
	 System.arraycopy( daughter, rand, temp, rand, daughter.length - rand - 1 );
      }
      return temp;
   }

   // Split the memory in this genome at ?AX? and place the new instruction set into a new bug
   protected void doDivide( int[] labs, int nlabs ) {
      try {
	 int[] daughter = doSplitMemory( labs, nlabs );
	 if ( daughter != null ) placeDaughter( daughter );
      } catch( OutOfMemoryError e ) { System.err.println( "OUT OF MEMORY IN DIVIDE" ); }
   }

   // Actually create the new daughter
   // Wont place it if grid is already full (need to find a way to 
   // fix that!)
   protected void placeDaughter( int[] newGenes ) {
      if ( newGenes == null || newGenes.length <= 0 ) return;
      if ( ! this.grid.canAddToGrid() ) return;
      int xoff = randomInt( -dd, dd + 1 ), yoff = randomInt( -dd, dd + 1 );
      if ( direction >= 1 && direction <= 4 ) {
	 switch( direction ) {
	 case NORTH: xoff = 0; yoff = randomInt( -dd, 1 ); break;
	 case SOUTH: xoff = 0; yoff = randomInt( 0, dd + 1 ); break;
	 case EAST: yoff = 0; xoff = randomInt( 0, dd + 1 ); break;
	 case WEST: yoff = 0; xoff = randomInt( -dd, 1 ); break;
	 }
      }
      new Genome( this, newGenes, this.x + xoff, this.y + yoff );
      nChildren ++;
      gestation_time = 0;
      fitness = 1.0;
   }

   // Split the memory in this genome at ?AX? and inject the new instruction set into another bug's genome
   // The location in the other genome is chosen by complement labels.
   protected void doInject( int Rcur, int[] labs, int nlabs ) {
      if ( labs == null || nlabs <= 0 ) return;
      Genome host = getNeighborGenome();
      if ( host == null || host.dead || host.genes == null ) return;
      Genome saveOwner = this.owner;
      int saveptr = cur;
      this.cur = 0;
      this.owner = host;
      int pos = searchForComplementLabels( true, labs, nlabs );
      this.owner = saveOwner;
      this.cur = saveptr;
      if ( pos == cur || pos < 0 || pos >= owner.genes.length ) return;

      try {
	 int[] daughter = doSplitMemory( labs, nlabs );
	 if ( daughter != null ) injectCode( daughter, host, pos );
      } catch( OutOfMemoryError e ) { System.err.println( "OUT OF MEMORY IN INJECT" ); }
   }

   // Actually inject the code into the given host, at the given location in the host's program
   protected void injectCode( int[] code, Genome host, int where ) {
      if ( code == null || code.length <= 0 ) return;
      int[] old = host.genes;
      int[] newcode = new int[ code.length + old.length ];
      if ( where > 0 ) System.arraycopy( old, 0, newcode, 0, where );
      System.arraycopy( code, 0, newcode, where, code.length );
      System.arraycopy( old, where, newcode, where + code.length, old.length - where );
      host.genes = newcode;
      gestation_time = 0;
      fitness = 1.0;
   }

   // Read a food value from the "environment" into ?CX?. Store a copy in the input buffer.
   // Test the inputs/outputs (because we reward for just a "get")
   protected void doGet( int[] labs, int nlabs ) {
      int food = randomCmd(); // Just get a random command value, for now
      int ind = nlabs > 0 ? getRegIndexForNOP( labs[ 0 ] ) : reg_C; // Put it in the register
      reg[ ind ] = food;
      System.arraycopy( in, 0, in, 1, globals.BUFFER_SIZE_DEFAULT - 2 ); // Shift the buffer values up
      in[ 0 ] = food;
      testOutputs();
   }

   // Write the value in ?BX? to the output buffer, and zero out ?BX?.
   // Finally test the output against the input using the available task resources.
   protected void doPut( int Rcur ) {
      if ( reg[ Rcur ] == UNDEFINED ) return;
      System.arraycopy( out, 0, out, 1, globals.BUFFER_SIZE_DEFAULT - 2 ); // Shift the buffer values up
      out[ 0 ] = reg[ Rcur ];
      reg[ Rcur ] = 0;
      testOutputs();
   }

   // Test whether the output buffer has outputs that correspond to the inputs via the available
   // task resources. If so, get the reward from the task, and modify the cycles accordingly.
   protected void testOutputs() {
      // Test against global resources first
      double reward = Resource.testGlobalOutputs( globals, in, out, tempRewards );

      // Let factor for global resources be larger for very early generations
      double factor = 1.0;
      if ( reward != 1.0 ) 
	 factor = ( 1.0 + globals.initial_cycles[ 1 ] / 
		Math.pow( (double) ( generation + 1 ), globals.initial_cycles[ 2 ] ) );

      if ( factor != 1.0 || factor * reward * cycles < globals.max_cycles ) 
	 reward *= factor;

      // Test against local resources if cycles aren't too high
      if ( reward == 1.0 || reward * cycles < globals.max_cycles )
	 reward *= Resource.testLocalOutputs( globals, in, out, tempRewards, this.x, this.y );

      if ( reward != 1.0 ) {
	 System.arraycopy( undef, 0, in, 0, in.length );
	 System.arraycopy( undef, 0, out, 0, out.length );
	 this.cycles = (int) ( this.cycles * reward );
	 this.cycles = Math.min( this.cycles, globals.max_cycles );
	 this.fitness *= reward;
      }
   }

   // Jump into another genome at the first occurence of the complement label. If no comp. label exists, jump to line BX.
   // First, push the current pointer location onto the stack so we can return to this location on a "return" call.
   protected void doJumpP( int Rcur, int[] labs, int nlabs ) {
      Genome host = getNeighborGenome();
      if ( host == null || host.dead || host.genes == null ) return;
      int pos = reg[ Rcur ];
      if ( labs != null && nlabs > 0 ) {
	 int[] save = owner.genes;
	 int saveptr = this.cur;
	 this.cur = 0;
	 Genome saveOwner = this.owner;
	 this.owner = host;
	 int temppos = searchForComplementLabels( true, labs, nlabs );
	 if ( temppos != cur && temppos >= 0 && temppos < owner.genes.length ) pos = temppos;
	 this.owner = saveOwner;
	 this.cur = saveptr;
      }
      if ( pos < 0 || pos > host.genes.length ) return;
      pushValue( cur );
      this.owner = host;
      this.cur = wrapPtr( pos );
   }

   // Get neighboring genome that is not this. First check for genome in this grid space.
   // If none, check for genome in facing grid space (if this genome has a direction).
   protected Genome getNeighborGenome() {
      if ( grid == null ) return null;
      if ( direction == 0 ) return null;
      ObjVector vec = grid.getGriddablesAt( x, y );
      int size = vec.size();
      if ( size <= 1 ) return null;
      for ( int i = 0; i < size; i ++ ) {
	 Genome g = (Genome) vec.elementAt( i );
	 if ( g != this ) return g;
      }
      
      int newx = x, newy = y;
      boolean forward = negated ? false : true;
      switch ( direction ) {
      case NORTH: newy += ( forward ? -1 : 1 ); break;
      case SOUTH: newy += ( forward ? 1 : -1 ); break;
      case EAST: newx += ( forward ? 1 : -1 ); break;
      case WEST: newx += ( forward ? -1 : 1 ); break;
      }

      vec = grid.getGriddablesAt( newx, newy );
      size = vec.size();
      if ( size <= 0 ) return null;
      for ( int i = 0; i < size; i ++ ) {
	 Genome g = (Genome) vec.elementAt( i );
	 if ( g != this ) return g;
      }
      return null;
   }

   // Get the average color presented in front of (behind) the bug.
   // Place it in ?AX?.
   protected void doSense( int color, int labs[], int nlabs ) {
      Grid gg = globals.resGrid;
      int newx = x, newy = x;
      boolean forward = negated ? false : true;
      switch ( direction ) {
      case NORTH: newy += ( forward ? -1 : 1 ); break;
      case SOUTH: newy += ( forward ? 1 : -1 ); break;
      case EAST: newx += ( forward ? 1 : -1 ); break;
      case WEST: newx += ( forward ? -1 : 1 ); break;
      }
      int value = UNDEFINED;
      IntGriddable ig = (IntGriddable) gg.getGriddableAt( newx, newy );
      // Now compute the average color of the requested color component over all
      // resources and bugs at the location newx,newy

      int ind = nlabs > 0 ? getRegIndexForNOP( labs[ 0 ] ) : reg_A; // Put it in the register
      reg[ ind ] = value;
   }

   // Adjust the copy mutation rate
   protected void modCMut( int Rind ) {
      if ( reg[ Rind ] != UNDEFINED ) cmut += negated ? -reg[ Rind ] : reg[ Rind ];
      if ( cmut > globals.max_cmut ) cmut = globals.max_cmut;
      else if ( cmut < globals.min_cmut ) cmut = globals.min_cmut;
   }

   // Increment/decrement the offspring "drop distance"
   protected void doModDD() {
      if ( ! negated ) dd ++;
      else dd --;
      if ( dd < 0 ) dd = 0;
      else if ( dd > globals.max_dd ) dd = globals.max_dd;
   }

   // Increment (decrement) the color of this bug based on current register index (default 
   // is to modify blue color;
   protected void doModColor( int Rind ) {
      int ad = negated ? -globals.color_inc : globals.color_inc;
      int r = this.color.getRed(), g = this.color.getGreen(), b = this.color.getBlue();
      switch ( Rind ) {
         case reg_A: r += ad; if ( r < 0 ) r = 0; else if ( r > 255 ) r = 255; break;
         case reg_C: g += ad; if ( g < 0 ) g = 0; else if ( g > 255 ) g = 255; break;
         case reg_B: default: b += ad; if ( b < 0 ) b = 0; else if ( b > 255 ) b = 255; break;
      }
      this.color = new Color( r, g, b );
   }
   
   // This organism has died -- remove it from the grid and make sure its stuff is all garbage-collected
   protected void die() {
      if ( this.dead ) return;
      if ( grid != null ) removeFromGrid();
      this.dead = true;
      clearMemory();
   }

   public int getExecutedCount() {
      return ( executed != null ? executed.countSet() : 0 );
   }

   // Convert genome into a string (e.g. for comparisons and hashing)
   public String toString2() {
      if ( stringRep == null ) {
	 int glen = origLength;
	 int len = 0;
	 for ( int i = 0; i < glen; i ++ ) 
	    if ( genes[ i ] != UNDEFINED && ! isANOP( genes[ i ] ) ) len ++;
	 if ( tempChars == null || tempChars.length < len ) tempChars = new char[ len ];
	 len = 0;
	 for ( int i = 0; i < glen; i ++ ) 
	    if ( genes[ i ] != UNDEFINED && ! isANOP( genes[ i ] ) ) tempChars[ len ++ ] = (char) genes[ i ];
	 stringRep = String.valueOf( tempChars );
      } 
      return stringRep;
   }

   public String toString() {
      StringBuffer out = new StringBuffer( super.toString() );
      String cr = "\n", cr2 = "\n  ", cr3 = "\n   ", sp = " ";
      out.append( "   " ).append( formatString( "CYCLES = " + cycles, 18 ) );
      out.append( " " ).append( formatString( "AGE = " + age, 18 ) );
      out.append( " " ).append( formatString( "GEN = " + generation, 18 ) ).append( cr2 );
      out.append( " " ).append( formatString( "ORIGLEN = " + origLength, 18 ) );
      out.append( " " ).append( formatString( "LEN = " + owner.genes.length, 18 ) );
      out.append( " " ).append( formatString( "EXECUTED = " + getExecutedCount(), 18 ) ).append( cr2 );
      out.append( " " ).append( formatString( "GEST = " + gestation_time, 18 ) );
      out.append( " " ).append( formatString( "OCYCLES = " + off_cycles, 18 ) ).append( cr2 );
      out.append( " " ).append( formatString( ( Rcur == reg_A ? ">A = " : "A = " ) + ( reg[ reg_A ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_A ] ), 18 ) );
      out.append( " " ).append( formatString( ( Rcur == reg_B ? ">B = " : "B = " ) + ( reg[ reg_B ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_B ] ), 18 ) );
      out.append( " " ).append( formatString( ( Rcur == reg_C ? ">C = " : "C = " ) + ( reg[ reg_C ] == UNDEFINED ? "UNDEF" : "" + reg[ reg_C ] ), 18 ) ).append( cr2 );
      out.append( " " ).append( formatString( "NEG = " + negated, 18 ) );
      out.append( " " ).append( formatString( "INSTSIZE = " + getInstructionSize( cur ) + "," +
					      getPrecedingInstructionSize( cur ), 18 ) );
      out.append( " " ).append( formatString( "DIRECTION = " + 
				  ( direction == NORTH ? "N" : 
				    ( direction == SOUTH ? "S" : direction == EAST ? "E" :
				     ( direction == WEST ? "W" : "" ) ) ), 18 ) ).append( cr2 );
      double factor = 1.0 + globals.initial_cycles[ 1 ] / Math.pow( (double) ( generation + 1 ), globals.initial_cycles[ 2 ] );
      out.append( " " ).append( formatString( "NCHILD = " + nChildren, 18 ) );
      out.append( " " ).append( formatString( "FITNESS = " + fitness, 18 ) );
      out.append( " " ).append( formatString( "FACTOR = " + factor, 18 ) ).append( cr2 );
      out.append( " " ).append( formatString( "DD = " + dd, 15 ) );
      out.append( " " ).append( formatString( "CMUT = " + cmut, 15 ) );
      out.append( " " ).append( formatString( "DMUT = " + dmut, 15 ) );
      out.append( " " ).append( formatString( "DINS = " + dins, 15 ) ).append( cr );

      out.append( "\n   REWARDS:\n   " );
      for ( int i = 0, sz = rewards.length; i < sz; i ++ ) {
	 boolean global = globals.resources[ i ].global;
	 String name = globals.resources[ i ].name + ( global ? "(g)" : "" );
	 out.append( formatString( name + ": " + rewards[ i ], 18 ) ).append( "\t" );
	 if ( i % 3 == 2 ) out.append( cr3 );
      }

      out.append( cr ).append( printStacksAndBuffers() ).append( cr3 );
      String arrow = ">", exec = "*";
      int gen[] = owner.genes;
      for ( int i = 0, sz = gen.length; i < sz; i ++ ) {
	 StringBuffer temp = new StringBuffer();
	 if ( i >= origLength ) break;
	 if ( i == cur ) temp.append( arrow );
	 if ( executed != null && executed.getBit( i ) ) temp.append( exec );
	 temp = new StringBuffer( formatString( temp.toString(), 2 ) );
	 temp.append( gen[ i ] != UNDEFINED ? dictionary.getName( gen[ i ] ) : "UNDEF" );
	 out.append( formatString( temp.toString(), 15 ) );
	 if ( gen[ i ] == CMD_NEG && i + 1 < gen.length && i + 1 < origLength ) continue;
	 if ( i + 1 < gen.length && i + 1 < origLength && isANOP( gen[ i + 1 ] ) ) continue;
	 out.append( cr3 );
      }
      out.append( cr );
      return out.toString();
   }

   public String printStacksAndBuffers() {
      StringBuffer outs = new StringBuffer( "   " );
      //outs += "STACKS: ";
      for ( int i = 0, sz = stack.length; i < sz; i ++ ) {
	 StringBuffer sstack = new StringBuffer();
	 for ( int j = 0, ssz = stack[ i ].length; j < ssz; j ++ )
	    sstack.append( formatString( ( stack[ i ][ j ] != UNDEFINED ? "" + stack[ i ][ j ] : "X" ), 4 ) );
	 outs.append( formatString( "STACK" + (i+1) + ": ", 10 ) );
	 outs.append( formatString( sstack.toString(), 25 ) );
      }
      outs.append( "\n   " );
      outs.append( formatString( "IN: ", 10 ) );
      StringBuffer ins = new StringBuffer();
      for ( int i = 0, sz = in.length; i < sz; i ++ ) 
	 ins.append( formatString( ( in[ i ] != UNDEFINED ? "" + in[ i ] : "X" ), 4 ) );
      outs.append( formatString( ins.toString(), 25 ) );
      outs.append( formatString( "OUT: ", 10 ) );
      ins = new StringBuffer();
      for ( int i = 0, sz = out.length; i < sz; i ++ ) 
	 ins.append( formatString( ( out[ i ] != UNDEFINED ? "" + out[ i ] : "X" ), 4 ) );
      outs.append( formatString( ins.toString(), 25 ) ).append( "\n" );
      return outs.toString();
   }

   public static String printStats( Grid genGrid ) {
      long cycAvg = 0, cycMax = -1;
      long ageAvg = 0, ageMax = -1, ageMin = 99999;
      long gtAvg = 0, gtMax = -1;
      long ocAvg = 0, ocMax = -1;
      long ddAvg = 0, cmutAvg = 0, dmutAvg = 0, dinsAvg = 0;
      long ncAvg = 0, ncMax = -1;
      long genAvg = 0, genMin = 99999, genMax = -1;
      double fitAvg = 0, fitMax = -1;
      int lenAvg = 0, lenMin = 99999, lenMax = -1;
      int count = 0;

      for ( int i = 0, sz = genGrid.maxInd; i < sz; i ++ ) {
	 Genome g = (Genome) genGrid.list[ i ];
	 if ( g == null ) continue;
	 cycAvg += g.cycles;
	 cycMax = Math.max( cycMax, g.cycles );
	 ageMin = Math.min( ageMin, g.age );
	 ageAvg += g.age;
	 ageMax = Math.max( ageMax, g.age );
	 gtAvg += g.gestation_time;
	 gtMax = Math.max( gtMax, g.gestation_time );
	 ocAvg += g.off_cycles;
	 ocMax = Math.max( ocMax, g.off_cycles );
	 ddAvg += g.dd;
	 cmutAvg += g.cmut;
	 dmutAvg += g.dmut;
	 dinsAvg += g.dins;
	 ncAvg += g.nChildren;
	 ncMax = Math.max( g.nChildren, ncMax );
	 genMin = Math.min( g.generation, genMin );
	 genAvg += g.generation;
	 genMax = Math.max( g.generation, genMax );
	 fitAvg += g.fitness;
	 fitMax = Math.max( fitMax, g.fitness );
	 lenAvg += g.origLength;
	 lenMin = Math.min( lenMin, g.origLength );
	 lenMax = Math.max( lenMax, g.origLength );
	 count ++;
      }
      cycAvg /= count;
      ageAvg /= count;
      gtAvg /= count;
      ocAvg /= count;
      ddAvg /= count;
      cmutAvg /= count;
      dmutAvg /= count;
      dinsAvg /= count;
      ncAvg /= count;
      genAvg /= count;
      fitAvg /= (double) count;
      lenAvg /= count;

      StringBuffer out = new StringBuffer();
      out.append( formatString( "STEPS = " + genGrid.steps, 20 ) );
      out.append( formatString( "COUNT = " + count, 20 ) );
      out.append( formatString( "MAXIND = " + genGrid.maxInd, 20 ) ).append( "\n" );
      out.append( formatString( "CYCLES:", 20 ) );
      out.append( formatString( "AVG = " + cycAvg, 20 ) );
      out.append( formatString( "MAX = " + cycMax, 20 ) ).append( "\n" );
      out.append( formatString( "AGE:", 20 ) );
      out.append( formatString( "AVG = " + ageAvg , 20 ) );
      out.append( formatString( "MAX = " + ageMax, 20 ) );
      out.append( formatString( "MIN = " + ageMin, 15 ) ).append( "\n" );
      out.append( formatString( "GESTATION TIME:", 20 ) );
      out.append( formatString( "AVG = " + gtAvg, 20 ) );
      out.append( formatString( "MAX = " + gtMax, 20 ) ).append( "\n" );
      out.append( formatString( "OFFSPRING CYCLES:", 20 ) );
      out.append( formatString( "AVG = " + ocAvg, 20 ) );
      out.append( formatString( "MAX = " + ocMax, 20 ) ).append( "\n" );
      out.append( formatString( "GENERATION:", 20 ) );
      out.append( formatString( "AVG = " + genAvg, 20 ) );
      out.append( formatString( "MAX = " + genMax, 20 ) );
      out.append( formatString( "MIN = " + genMin, 15 ) ).append( "\n" );
      out.append( formatString( "LENGTH:", 20 ) );
      out.append( formatString( "AVG = " + lenAvg, 20 ) );
      out.append( formatString( "MAX = " + lenMax, 20 ) );
      out.append( formatString( "MIN = " + lenMin, 15 ) ).append( "\n" );
      out.append( formatString( "NCHILDREN:", 20 ) );
      out.append( formatString( "AVG = " + ncAvg, 20 ) );
      out.append( formatString( "MAX = " + ncMax, 20 ) ).append( "\n" );
      out.append( formatString( "DROP DISTANCE:", 20 ) );
      out.append( formatString( "AVG = " + ddAvg, 20 ) ).append( "\n" );
      out.append( formatString( "COPY MUTATION:", 20 ) );
      out.append( formatString( "AVG = " + cmutAvg, 20 ) ).append( "\n" );
      out.append( formatString( "DIVIDE MUTATION:", 20 ) );
      out.append( formatString( "AVG = " + dmutAvg, 20 ) ).append( "\n" );
      out.append( formatString( "INSERT MUTATION:", 20 ) );
      out.append( formatString( "AVG = " + dinsAvg, 20 ) ).append( "\n" );
      out.append( formatString( "FITNESS:", 20 ) );
      out.append( formatString( "AVG = " + formatString( "" + fitAvg, 10 ), 20 ) );
      out.append( formatString( "MAX = " + formatString( "" + fitMax, 10 ), 20 ) ).append( "\n" );

      long currTime = System.currentTimeMillis();
      double stepDuration = ( (double) ( currTime - genGrid.startingTime ) ) / ( (double) genGrid.steps );
      out.append( formatString( "STEP DURATION:", 20 ) );
      out.append( formatString( formatString( "" + stepDuration, 10 ), 20 ) ).append( "\n" );
      return out.toString();
   }

   // Initialize the static CMD_ variables for a given class using the commands in the given dictionary
   public static void initializeCommandVariables( String cName, Dictionary cmds ) {
      if ( cmds == null ) return;
      for ( int i = 0, sz = cmds.getLength(); i < sz; i ++ ) {
	 initializeCommandVariable( cName, cmds.getName( i ), i );
      }
   }   

   // Initialize the given static CMD_ variable to the given value for the given class
   public static void initializeCommandVariable( String cName, String name, int cmd ) {
      try {
	 Class c = Class.forName( cName );
	 name = name.toUpperCase();
	 Field f = c.getField( "CMD_" + name );
	 if ( f == null ) {
	    System.err.println( "Could not find field " + name );
	    return;
	 }
	 f.set( null, new Integer( cmd ) );
	 //System.err.println("SET " + "CMD_" + name + " to " + cmd + " for class " + cName );
      } catch( Exception e ) {
	 System.err.println( "Exception setting variable CMD_" + name + " to " + cmd + ": " + e );
      }
   }

   // Initialize the genome from a file
   protected void readFromFile( Globals gl, String fileName ) {
      // If cached, use the cached value.
      if ( gl.fileGenes != null && gl.fileGenes.get( fileName ) != null ) {
	 Genome g = (Genome) gl.fileGenes.get( fileName );
	 if ( g.genes != null ) {
	    //owner.genes = new int[ g.genes.length ];
	    //System.arraycopy( g.genes, 0, owner.genes, 0, owner.genes.length );
	    //if ( g.dmut > 0 ) {
	    // int rand = randomInt( 0, 101 );
	    // if ( rand < g.dmut ) {
	    //int rand = randomInt( 0, owner.genes.length );
	    //owner.genes[ rand ] = randomCmd();
	    owner.genes = insDel( g.genes );
	    //}
	    //}
	    return;
	 }
      }
      String str = "";
      try {
	 DataInputStream dis = new DataInputStream( djr.util.MyUtils.OpenFile( fileName ) );
	 int length = 0;
	 // Count the length of the program, skip comments and empty lines
	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) continue;
	    length ++;
	 }
	 dis.close();
	 if ( length <= 0 ) throw new Exception( "length = " + length );
	 dis = new DataInputStream( djr.util.MyUtils.OpenFile( fileName ) );
	 owner.genes = new int[ length ];
	 System.arraycopy( undef, 0, owner.genes, 0, owner.genes.length );
	 int end = 0;
	 // Read in the program, skip comments and empty lines
	 while( ( str = dis.readLine() ) != null ) {
	    if ( str == null || str.startsWith( "#" ) || str.startsWith( "//" ) || str.equals( "" ) ) continue;	    
	    boolean found = false;
	    // Use the dictionary to get the int value for the given string read in
	    int ii = dictionary.getCommand( str );
	    if ( ii != UNDEFINED ) {
	       owner.genes[ end ++ ] = ii; // And set it in the genome
	       found = true;
	    }
	    if ( ! found ) System.err.println( "Could not find translation for " + str );
	 }
	 dis.close();
	 // Store the genotype so we don't read in the same file multiple times for many instances.
	 if ( gl.fileGenes == null ) gl.fileGenes = new Hashtable();
	 gl.fileGenes.put( fileName, this );
      } catch( Exception e ) {
         System.err.println( "Could not load bug file " + fileName );
	 e.printStackTrace();
      }
   }

   private void writeObject( ObjectOutputStream out ) throws IOException {
      out.defaultWriteObject();
   }
      
   private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
   }
}
