package djr.bugs;

import java.util.*;
import java.io.*;

/**
 * Class <code>Globals</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class Globals implements Constants, Serializable {

   // YajaLife globals:

   Grid bugGrid = null, resGrid = null;
   int newLocalResPerTurn = 1, newGlobalResPerTurn = 1;

   // Genome globals:

   boolean stepByOne = false, stepByLots = false;
   int NUM_REGISTERS_DEFAULT = 3; // Default number of memory registers
   int REGISTER_DEFAULT = reg_B; // Default current register
   int NUM_STACKS_DEFAULT = 2; // Default number of stacks
   int STACK_SIZE_DEFAULT = 4; // Default size of each stack
   int BUFFER_SIZE_DEFAULT = 4; // Default size of i/o buffers
   int DROP_DISTANCE_DEFAULT = 1; // Default distance that offspring get dropped (dd)
   int STACK_DEFAULT = 0; // Default current stack
   int MAX_LABELS = 3; // Maximum # of NOP_A/B/C labels in a row
   int MIN_PERCENT = 70; // Min percentages (executed and/or copied) allowed for acceptable divide
   int MIN_LENGTH = 10; // Min genome length (in instructions) allowed for acceptable divide
   int ACCEPTABLE_DIVIDE_RANGE = 2; // Daughter must be >=1/2 and <=2 times the length of parent
   
   double[] initial_cycles = { 500, 1.5, 0.5 };
   int point_mut = 10; // Point mutation rate (units of 10^-6) -- rate is per site per step
   int copy_mut_default = 10; // Default copy mutation rate (in units of 10^-4)
   int divide_mut_default = 10; // Default divide mutation rate (in units of 10^-2)
   int divide_ins_default = 10; // Default divide insert/remove mutation rate (in units of 10^-2)

   int max_cmut = 10; // Maximum copy mutation
   int min_cmut = 1; // Minimum copy mutation
   int max_dd = 5; // Max distance that offspring can get dropped (dd)
   int move_penalty = 2; // Cycles required to move one cell
   int max_cycles = 10000; // Max. allowed value for cycles (used if a bug is very good at productive I/O)
   int max_age = 10000; // Maximum allowed age (age === total instructions run)
   int color_inc = 16; // Amount allowed to increment/decrement a color component
   int max_size = 1000; // Maximum size allowed
   int max_armor = 1000; // Maximum armor/weaponry allowed

   Dictionary commands = null;
   String dictionaryName = "genome.dat", codebase = "";
   Hashtable fileGenes = null; // Store genomes read in from a file (in case multiple ones come from the same file)

   // Resource globals:

   Resource[] resources = null;
   int max_resources = 5; // Max. resources per cell (average)
   int num_resources = -1; // Number of different resources available
   String resFile = "resources.dat"; // Name of resources.dat file
   long[] resCount = null; // Keep track of # of global resources available
   long[] awarded = null;
 
   public void setInitialCycles( String input ) {
      StringTokenizer t = new StringTokenizer( input, " ,\t" );
      int i = 0;
      while( t.hasMoreTokens() ) initial_cycles[ i ++ ] = Double.valueOf( t.nextToken() ).doubleValue();
   }
}
