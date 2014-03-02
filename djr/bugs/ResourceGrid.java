package djr.bugs;

import java.util.*;
import java.io.*;

/**
 * Class <code>ResourceGrid</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class ResourceGrid extends Grid {
   Resource[] resources = null;
   int max_resources = 5; // Max. resources per cell (average)
   int num_resources = -1; // Number of different resources available
   String resFile = "resources.dat"; // Name of resources.dat file
   long[] resCount = null; // Keep track of # of global resources available
   long[] awarded = null;

   public ResourceGrid( int width, int height, int max ) {
      super( width, height, max );
   }
}
