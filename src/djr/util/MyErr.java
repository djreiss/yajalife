package djr.util;

import java.io.*;
import corejava.Format;

/**
 * Class <code>MyErr</code>
 *
 * @author <a href="mailto:dreiss@systemsbiology.org">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class MyErr extends MyOut {
   public MyErr() {
      this( System.err );
   }

   public MyErr( PrintStream oldErr ) {
      super( oldErr );
   }
}
