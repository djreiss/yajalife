package djr.bugs;

/**
 * Interface <code>Constants</code>
 *
 * @author <a href="mailto:reiss@uw.edu">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public interface Constants {
   static final int NORTH = 1, EAST = 2, SOUTH = 3, WEST = 4;

   static final int UNDEFINED = -Integer.MAX_VALUE; // Genes with this value are undefined (since zero has a definition)
   static final int reg_A = 0, reg_B = 1, reg_C = 2; // Enumeration for setting register to A, B, or C
   public static final int RED = 0, GREEN = 1, BLUE = 2;

}
