package djr.bugs;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.util.Properties;
import djr.util.ArgProcessorInterface;
import djr.util.ArgProcessor;
import djr.util.gui.ArgProcessorGUI;
import djr.util.array.ObjVector;
import djr.util.MyUtils;
//import JSX.*;

/**
 * Class <code>YajaLife</code>
 *
 * @author <a href="mailto:astrodud@sourceforge.net">David Reiss</a>
 * @version 1.1 (Sat Nov 26 21:49:21 PST 2005)
 */
public class YajaLife extends JPanel implements ArgProcessorInterface, Runnable, 
						WindowListener, ActionListener,
						MouseListener, KeyListener {
   protected static int frameNumber = 1;
   protected static ObjVector worlds = new ObjVector();

   public transient JFrame frame = null;
   protected String args[] = null;
   int frameNo = 0, width = 0, height = 0;
   Globals globals = null;
   int cellSize = 5, skipHowManyForDrawing = 1, pause = 0, keepMin = 0, saveGifStep = -1;
   int saveStep = -1, statsStep = 100;
   int maxBugs, numSeed;
   String bugSeedFileName;
   Properties props = null;
   long startingTime = -1;

   boolean done = false, stopped = false, initialized = false, drawingResources = false;
   boolean keepLast = false;

   transient Graphics offscreen = null;
   transient Image offscreenImg = null;
   transient Thread thread = null;

   transient YajaLifeControlPanel ctrlPanel = null;
   Genome selectedBug = null;

   public YajaLife( String codebase ) {
      this( codebase, null );
   }

   public YajaLife( String codebase, String args[] ) {
      this.args = args;
      readProperties( "YajaLife.properties" );
      worlds.add( this );

      this.globals = new Globals();
      globals.codebase = codebase;

      initialized = false;
      initialize();
      initializeFrame();

      frame.show();
   }

   public void setProperties( Properties prop ) {
      if ( prop != null ) this.props = prop;
      cellSize = Integer.parseInt( this.getParameter( "CellSize" ) );
      width = Integer.parseInt( getParameter( "Width" ) );
      height = Integer.parseInt( getParameter( "Height" ) );
      if ( frame != null ) frame.setBounds( 10, 30, ( width + 1 ) * cellSize, ( height + 1 ) * cellSize + 40 );

      pause = Integer.parseInt( this.getParameter( "PauseMillis" ) );
      keepMin = Integer.parseInt( this.getParameter( "MaintainMinimum" ) );
      keepLast = ( new Boolean( this.getParameter( "KeepLast" ) ) ).booleanValue();
      skipHowManyForDrawing = Integer.parseInt( this.getParameter( "SkipHowManyForDrawing" ) );
      saveGifStep = Integer.parseInt( this.getParameter( "ScreenShotStep" ) );
      saveStep = Integer.parseInt( this.getParameter( "SaveStep" ) );
      statsStep = Integer.parseInt( this.getParameter( "StatsStep" ) );

      numSeed = Integer.parseInt( this.getParameter( "NumSeed" ) );
      bugSeedFileName = this.getParameter( "BugSeed" );
      maxBugs = Integer.parseInt( this.getParameter( "MaxBugs" ) );

      globals.stepByOne = ( new Boolean( this.getParameter( "StepByOne" ) ) ).booleanValue();
      globals.stepByLots = ( new Boolean( this.getParameter( "StepByLots" ) ) ).booleanValue();
      globals.setInitialCycles( this.getParameter( "InitialCycles" ) );
      globals.move_penalty = Integer.parseInt( this.getParameter( "MovePenalty" ) );
      globals.max_cmut = Integer.parseInt( this.getParameter( "MaxCMut" ) );
      globals.max_dd = Integer.parseInt( this.getParameter( "MaxDD" ) );
      globals.point_mut = Integer.parseInt( this.getParameter( "PointMut" ) );
      globals.copy_mut_default = Integer.parseInt( this.getParameter( "CopyMut" ) );
      globals.divide_mut_default = Integer.parseInt( this.getParameter( "DivideMut" ) );
      globals.divide_ins_default = Integer.parseInt( this.getParameter( "InsertMut" ) );
      globals.max_age = Integer.parseInt( this.getParameter( "MaxAge" ) );
      globals.max_cycles = Integer.parseInt( this.getParameter( "MaxCycles" ) );
      globals.drawOne = ( new Boolean( this.getParameter( "DrawOne" ) ) ).booleanValue();

      Genome.setDictionaryName( globals, this.getParameter( "Dictionary" ) );
      globals.max_resources = Integer.parseInt( this.getParameter( "MaxResourcesPerCell" ) );
      globals.newLocalResPerTurn = Integer.parseInt( this.getParameter( "LocalResourcesPerStep" ) );
      globals.newGlobalResPerTurn = Integer.parseInt( this.getParameter( "GlobalResourcesPerStep" ) );
      Resource.initializeResources( globals, this.getParameter( "Resources" ) );
   }

   public void initializeFrame() {
      this.frameNo = frameNumber;
      frame = new JFrame( "YajaLife " + ( frameNumber ++ ) );
      frame.getContentPane().setLayout( new BorderLayout() );
      frame.setBounds( 10, 30, ( width + 1 ) * cellSize, ( height + 1 ) * cellSize + 40 );
      frame.getContentPane().add( "Center", this );
      frame.addWindowListener( this );
      addMouseListener( this );
      addKeyListener( this );
      this.setLayout( new BorderLayout() );
      this.setFocusable( true );

      if ( args != null ) {
	 djr.util.array.ObjVector argv = new djr.util.array.ObjVector( args );
	 boolean gui = argv.contains( "-guiopt" );
	 ArgProcessor argProc = gui ? new ArgProcessorGUI() : new ArgProcessor();
	 if ( ! argProc.ProcessArgs( this, args ) ) System.exit( 0 );
      }

      setLayout( new BorderLayout() );
      setDoubleBuffered( false );
      
      this.setBackground( Color.black );
      frame.getContentPane().setBackground( Color.black );
      frame.setBackground( Color.black );

      JButton ctrlPanelBut = new JButton( "Control Panel" );
      frame.getContentPane().add( "South", ctrlPanelBut );
      ctrlPanelBut.addActionListener( this );
      frame.doLayout();      
   }

   public void initialize() {
      if ( initialized ) return;

      cellSize = Integer.parseInt( getParameter( "CellSize" ) );
      width = Integer.parseInt( getParameter( "Width" ) );
      height = Integer.parseInt( getParameter( "Height" ) );

      setProperties( this.props );

      // Seed the resources grid
      globals.resGrid = new Grid( globals, width, height, 
				  globals.max_resources * width * height + 1 );

      // Seed the grid with a set of initial replicators
      globals.bugGrid = new Grid( globals, width, height, maxBugs );
      globals.bugGrid.gcEvery( 250 );
      for ( int i = 0; i < numSeed; i ++ ) new Genome( globals, bugSeedFileName );

      globals.bugGrid.cellSize = globals.resGrid.cellSize = cellSize;

      initialized = true;
      if ( thread == null ) thread = new Thread( this );
      thread.start();
   }

   public void run() {
      while( ! done ) {
	 if ( ! stopped ) {
	    try { paint( getGraphics() ); } catch( Exception e ) { 
	       System.err.println( "An exception " + e + " was caught. Ignoring." );
	       e.printStackTrace();
	    }
	 }
	 if ( pause > 0 ) try { Thread.sleep( pause ); } catch( Exception e ) { };
	 try {
	    if ( thread != null ) thread.currentThread().yield();
	 } catch( Exception e ) {
	    Thread.currentThread().interrupt();
	 }
      }

      thread = null;
      ( new Thread() { public void run() {
	 if ( ctrlPanel != null ) {
	    ctrlPanel.hide();
	    ctrlPanel.dispose();
	 }
	 if ( offscreenImg != null ) offscreenImg.flush();
	 offscreen = null;
	 offscreenImg = null;
	 frame.hide(); 
	 frame.dispose();
	 thread.currentThread().yield();
	 System.gc();
	 worlds.delete( YajaLife.this );
	 if ( worlds.size() <= 0 ) System.exit( 0 );
      } } ).start();
   }

   protected Graphics getMyGraphics( Graphics g ) {
      if ( offscreen == null ) {
         try {
            offscreenImg = createImage( size().width, size().height );
            offscreen = offscreenImg.getGraphics();
         } catch( Exception e ) {
            offscreen = null;
         }
      } 
      Graphics gr = ( offscreen != null ) ? offscreen : g;
      return gr;
   }

   public synchronized void paint( Graphics g ) {
      if ( g == null ) return;

      Resource.addNewLocalResources( globals );
      Resource.addNewGlobalResources( globals );
      Grid bugs = globals.bugGrid;
      if ( bugs.num < keepMin ) {
	 //System.err.println( "Spawned new bug: " + bugs.steps );
	 if ( ! keepLast ) {
	    for ( int i = bugs.num; i < keepMin; i ++ ) 
	       new Genome( globals, bugSeedFileName );
	 } else {
	    Genome gg = (Genome) bugs.getAGriddable();
	    if ( gg == null || gg.generation < 10 ) {
	       new Genome( globals, bugSeedFileName );
	    } else { // Give the existing bug a new leaf on life!
	       gg.generation = 0;
	       if ( gg.cycles < globals.initial_cycles[ 0 ] ) 
		  gg.cycles = (int) globals.initial_cycles[ 0 ];
	       gg.removeFromGrid(); // Move it up on the list
	       gg.addToGrid();
	    }
	 }
      }

      Graphics gr = getMyGraphics( g );      
      boolean drawIt = ( bugs.steps + 1 ) % skipHowManyForDrawing == 0;
      if ( drawingResources ) {
	 if ( drawIt ) globals.resGrid.paintHistogram( gr, globals.max_resources );
	 bugs.step( null );
      } else {
	 if ( drawIt ) bugs.step( gr );
	 else bugs.step( null );
      }

      if ( drawIt && selectedBug != null ) {
	 if ( selectedBug.dead ) selectedBug = null;
	 else {
	    gr.setColor( Color.white );
	    gr.drawRect( selectedBug.x * cellSize, selectedBug.y * cellSize, 
			cellSize, cellSize );
	 }
      }
      
      if ( drawIt && offscreenImg != null ) g.drawImage( offscreenImg, 0, 0, this );

      if ( saveGifStep > 0 && bugs.steps % saveGifStep == 0 ) doScreenShot( true );
      if ( saveStep > 0 && bugs.steps % saveStep == 0 ) doSave();
      if ( statsStep > 0 && bugs.steps % statsStep == 0 ) doAction( null, "Stats" );
   }

   public void doScreenShot( boolean auto ) {
      FileOutputStream fos = null;
      try {
	 String file = null;
	 if ( auto ) {
	    file = getParameter( "ScreenShotPrefix" ) + globals.bugGrid.steps + ".gif";
	 } else { 
	    JFileChooser fc = new JFileChooser();
	    int returnVal = fc.showSaveDialog( this.ctrlPanel );
	    if ( returnVal == JFileChooser.APPROVE_OPTION ) 
	       file = fc.getSelectedFile().getAbsolutePath();
	 }
	 if ( file == null ) return;
	 File temp = new File( ( new File( file ) ).getParent() );
	 temp.mkdirs();
	 fos = new FileOutputStream( file );
	 if ( offscreenImg != null ) { 
	    Acme.JPM.Encoders.GifEncoder genc = 
	       new Acme.JPM.Encoders.GifEncoder( offscreenImg, fos );
	    genc.encode();
	 }
      } catch( Exception e ) {
	 e.printStackTrace();
      } finally {
	 try { if ( fos != null ) { fos.flush(); fos.close(); } } catch( IOException e ) { };
      }
   }

   public void doSave() {
      try {
	 String file = this.getParameter( "SaveFilePrefix" ) + globals.bugGrid.steps + ".dat";
	 File temp = new File( ( new File( file ) ).getParent() );
	 temp.mkdirs();
	 FileOutputStream fos = new FileOutputStream( file );
	 java.util.zip.GZIPOutputStream gos = 
	    new java.util.zip.GZIPOutputStream( new BufferedOutputStream( fos ) );
	 ObjectOutputStream out = new ObjectOutputStream( gos );
	 out.writeObject( props );
	 out.writeObject( globals );
	 out.flush();
	 gos.flush();
	 gos.close();
	 fos.close();
      } catch( Exception e ) {
	 e.printStackTrace();
      }
   }

   public void doRead() {
      boolean save = stopped;
      this.stop();
      try {
	 JFileChooser fc = new JFileChooser();
	 int returnVal = fc.showOpenDialog( this.ctrlPanel );

	 String fname = null;
	 if ( returnVal == JFileChooser.APPROVE_OPTION )
	    fname = fc.getSelectedFile().getAbsolutePath();

	 if ( fname == null ) return;
	 DataInputStream dis = 
	    new DataInputStream( djr.util.MyUtils.OpenFile( fname ) );
	 java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream( dis );
	 ObjectInputStream in = new ObjectInputStream( gis );
	 Properties newProps = (Properties) in.readObject();
	 if ( newProps != null ) setProperties( newProps );
	 Globals newGlob = (Globals) in.readObject();
	 if ( newGlob != null ) globals = newGlob;

      } catch( Exception e ) {
	 e.printStackTrace();
      }

      globals.bugGrid.paint( getMyGraphics( getGraphics() ) );
      if ( offscreenImg != null ) getGraphics().drawImage( offscreenImg, 0, 0, this );
      if ( ! save ) this.start();

      System.err.println( "BUGS = " + globals.bugGrid.num + "; STEPS = " + 
			  globals.bugGrid.steps );
   }

   public void actionPerformed( ActionEvent evt ) {
      this.doAction( evt, evt.getActionCommand() );
   }

   public void doAction( ActionEvent evt, Object what ) {
      if ( "Control Panel".equals( what ) ) {
	 if ( ctrlPanel == null ) ctrlPanel = new YajaLifeControlPanel( this, "YajaLife " + frameNo );
	 if ( ! ctrlPanel.isVisible() ) {
	    Rectangle bnds = frame.getBounds();
	    ctrlPanel.setLocation( bnds.x, bnds.y + bnds.height + 30 );
	    ctrlPanel.show();
	 }
	 else ctrlPanel.hide();
      } else if ( "Stats".equals( what ) ) {
	 keyDown( 'r' );
	 keyDown( 'g' );
      } else if ( "Close".equals( what ) ) {
	 this.destroy();
      } else if ( "Pause".equals( what ) ) {
	 this.stop();
	 ( (javax.swing.JButton) evt.getSource() ).setLabel( "Resume" );
      } else if ( "Resume".equals( what ) ) {
	 this.start();
	 ( (javax.swing.JButton) evt.getSource() ).setLabel( "Pause" );
      } else if ( "Resources".equals( what ) ) {
	 drawingResources = true;
	 ( (javax.swing.JButton) evt.getSource() ).setLabel( "Bugs" );
	 ctrlPanel.setText( "Info: All resources are being drawn." );
      } else if ( "Bugs".equals( what ) ) {
	 drawingResources = false;
	 ( (javax.swing.JButton) evt.getSource() ).setLabel( "Resources" );
	 ctrlPanel.setText( "Info: All bugs are being drawn." );
      } else if ( "Save".equals( what ) ) {
	 doSave();
      } else if ( "Read".equals( what ) ) {
	 doRead();
      } else if ( "Screen Shot".equals( what ) ) {
	 doScreenShot( false );
      } else if ( "Add".equals( what ) ) {
	 YajaLife world = new YajaLife( ".", args );
      } else if ( "Clone".equals( what ) ) {
	 YajaLife world = cloneWorld( this, args );
      } else if ( "Exit".equals( what ) ) {
	 this.hide(); System.exit( 0 );
      } else if ( "Step".equals( what ) ) {
	 boolean save1 = globals.stepByLots;
	 boolean save2 = globals.stepByOne;
	 globals.stepByLots = false;
	 globals.stepByOne = true;
	 int save3 = skipHowManyForDrawing;
	 skipHowManyForDrawing = 1;
	 this.paint( this.getGraphics() );
	 if ( selectedBug != null ) System.err.println( selectedBug.toString() );
	 globals.stepByLots = save1;
	 globals.stepByOne = save2;
	 skipHowManyForDrawing = save3;
      } else if ( "Configure".equals( what ) ) {
	 ArgProcessor argProc = new ArgProcessorGUI();
	 argProc.ProcessArgs( this, args );
      } else if ( "Stats".equals( what ) ) {
	 keyDown( 'r' );
	 keyDown( 'g' );
      } else {
	 System.err.println( what );
      }
   }

   public YajaLife cloneWorld( YajaLife world, String args[] ) {
      YajaLife newWorld = (YajaLife) MyUtils.DeepCopy( world );
      
      newWorld.args = args;
      newWorld.readProperties( "YajaLife.properties" );
      worlds.add( newWorld );

      newWorld.initializeFrame();

      newWorld.initialized = true;
      newWorld.thread = new Thread( newWorld );
      newWorld.thread.start();
      newWorld.frame.show();

      return( newWorld );
   }

   public void mousePressed( MouseEvent evt ) {
      this.mouseDown( evt.getX(), evt.getY() );
   }

   public boolean mouseDown( int x, int y ) {
      x /= cellSize;
      y /= cellSize;
      selectedBug = (Genome) globals.bugGrid.getGriddableAt( x, y );
      if ( selectedBug != null ) {
	 System.err.println( selectedBug.toString() );
	 //getGraphics().setColor( Color.white );
	 //getGraphics().drawRect( selectedBug.x * cellSize, selectedBug.y * cellSize, 
	 //			 cellSize, cellSize );
	 this.paint( this.getGraphics() );
      }
      this.requestFocus();
      return true;
   }

   public void keyPressed( KeyEvent evt ) {
      this.keyDown( evt.getKeyChar() );
   }

   public boolean keyDown( char key ) {
      if ( key == 'r' || key == 'g' ) System.err.println( "YajaLife World # " + frameNo );
      if ( key == 'r' ) System.err.println( Resource.printStats( globals ) );
      else if ( key == 'g' ) System.err.println( Genome.printStats( globals.bugGrid ) );
      return true;
   }

   public void update( Graphics g ) {
      paint( g );
   }

   public void start() {
      stopped = false;
   }

   public void stop() {
      stopped = true;
   }

   public void destroy() {
      done = true;
   }

   protected void readProperties( String properties ) {
      if ( properties == null ) return;
      try {
	 InputStream dis = djr.util.MyUtils.OpenFile( properties );
	 if ( dis == null || dis.available() <= 0 ) throw new Exception( "" );
	 if ( props == null ) props = new Properties();
	 props.load( dis );
      } catch( Exception e ) {
	 System.err.println( e + ": Could not load properties file " + properties );
      }
   }

   protected String getParameter( String param ) {
      return props != null ? props.getProperty( param ) : null;
   }

   public void windowOpened( WindowEvent e ) { frame.show(); }
   public void windowClosing( WindowEvent e ) { this.destroy(); } //System.exit( 0 ); }
   public void windowClosed( WindowEvent e ) { this.destroy(); } //System.exit( 0 ); }
   public void windowIconified( WindowEvent e ) { }
   public void windowDeiconified( WindowEvent e ) { }
   public void windowActivated( WindowEvent e ) { }
   public void windowDeactivated( WindowEvent e ) { }   

   public void mouseClicked( MouseEvent e ) { }
   public void mouseEntered( MouseEvent e ) { }
   public void mouseExited( MouseEvent e ) { }
   public void mouseReleased( MouseEvent e ) { }

   public void keyReleased( KeyEvent e ) { }
   public void keyTyped( KeyEvent e ) { }

   public void SetupArgs( ArgProcessor argProc ) { 
      argProc.AddArg( "World Parameters" );
      argProc.AddArg( "N", "<int>", "" + 1, "Number of worlds to run simultaneously" );
      argProc.AddArg( "W", "<int>", "" + width, "Grid width (cells)" );
      argProc.AddArg( "H", "<int>", "" + height, "Grid height (cells)" );
      argProc.AddArg( "cs", "<int>", "" + cellSize, "Cell size (pixels)" );
      argProc.AddArg( "pause", "<int>", "" + pause, "Pause this many milliseconds between steps" );
      argProc.AddArg( "mmin", "<int>", "" + keepMin, "Maintain at least this many bugs" );
      argProc.AddArg( "keep", null, "" + keepLast, "Clone remaining bug(s) if number falls below minimum" );
      argProc.AddArg( "one", null, "" + globals.drawOne, "Draw only one bug per cell (faster)" );
      argProc.AddArg( "skip", "<int>", "" + skipHowManyForDrawing, "Skip this many steps before a re-draw" );
      argProc.AddArg( "shot", "<int>", "" + saveGifStep, "Save a screen shot every N steps" );
      argProc.AddArg( "shotprefix", "<string>", this.getParameter( "ScreenShotPrefix" ), "Screen shot filename prefix" );
      argProc.AddArg( "save", "<int>", "" + saveStep, "Save the current world every N steps" );
      argProc.AddArg( "saveprefix", "<string>", this.getParameter( "SaveFilePrefix" ), "Saved world filename prefix" );
      argProc.AddArg( "stats", "<int>", "" + statsStep, "Print world statistics every N steps" );

      argProc.AddArg( "Initialization parameters" );
      argProc.AddArg( "nseed", "<int>", "" + numSeed, "Number of initial seed bugs to plant" );
      argProc.AddArg( "bseed", "<fname>", bugSeedFileName, "Program to use as initial seed" );

      argProc.AddArg( "Bug parameters" );
      argProc.AddArg( "bmax", "<int>", "" + maxBugs, "Maximum number of bugs allowed" );
      argProc.AddArg( "sbo", null, "" + globals.stepByOne, "Perform only one instruction per iteration" );
      argProc.AddArg( "sbl", null, "" + globals.stepByLots, "Perform many instruction per iteration" );
      argProc.AddArg( "cyc", "<string>", "" + this.getParameter( "InitialCycles" ), "Initial cycles parameters" );
      argProc.AddArg( "mp", "<int>", "" + globals.move_penalty, "Move penalty" );
      argProc.AddArg( "agemax", "<int>", "" + globals.max_age, "Maximum bug age" );
      argProc.AddArg( "cycmax", "<int>", "" + globals.max_cycles, "Maximum bug cycles" );
      argProc.AddArg( "ddmax", "<int>", "" + globals.max_dd, "Maximum drop distance" );
      argProc.AddArg( "cmut", "<int>", "" + globals.copy_mut_default, "Default copy mutation rate" );
      argProc.AddArg( "cmax", "<int>", "" + globals.max_cmut, "Maximum copy mutation rate" );
      argProc.AddArg( "pmut", "<int>", "" + globals.point_mut, "Point mutation rate" );
      argProc.AddArg( "dmut", "<int>", "" + globals.divide_mut_default, "Default divide mutation rate" );
      argProc.AddArg( "imut", "<int>", "" + globals.divide_ins_default, "Default divide insertion mutation rate" );

      argProc.AddArg( "Dictionary parameters" );
      argProc.AddArg( "dict", "<fname>", this.getParameter( "Dictionary" ), "Dictionary file name" );

      argProc.AddArg( "Resources parameters" );
      argProc.AddArg( "res", "<fname>", this.getParameter( "Resources" ), "Resources file name" );
      argProc.AddArg( "rmax", "<int>", "" + globals.max_resources, "Maximum number of resources per cell" );
      argProc.AddArg( "gres", "<int>", "" + globals.newGlobalResPerTurn, "Number of global resources added per iteration" );
      argProc.AddArg( "lres", "<int>", "" + globals.newLocalResPerTurn, "Number of local resources added per iteration" );

      argProc.AddArg( "Misc. parameters" );
      argProc.AddArg( "run", null, "true", "Just start running on startup" );
      argProc.AddArg( "guiopt", null, "false", "Use GUI to set options on startup" );
      argProc.AddArg( "help", null, "false", "display this help" );
      argProc.AddArg( "h", null, "false", "display this help" );
      argProc.AddArg( "?", null, "false", "display this help" );
   }

   public void SetArgs( ArgProcessor proc ) {
      boolean changeGridSize = false, changeFrameSize = false;

      cellSize = proc.getIntArg( "cs" );
      if ( globals.bugGrid != null ) globals.bugGrid.cellSize = cellSize;
      if ( globals.resGrid != null ) globals.resGrid.cellSize = cellSize;
      if ( ! props.getProperty( "CellSize" ).equals( "" + cellSize ) ) changeFrameSize = true;
      props.setProperty( "CellSize", "" + cellSize );

      width = proc.getIntArg( "W" );
      height = proc.getIntArg( "H" );
      if ( ! props.getProperty( "Width" ).equals( "" + width ) ) changeGridSize = true;
      if ( ! props.getProperty( "Height" ).equals( "" + height ) ) changeGridSize = true;
      props.setProperty( "Width", "" + width );
      props.setProperty( "Height", "" + height );

      if ( changeGridSize ) {
	 if ( globals.bugGrid != null ) globals.bugGrid.resize( width, height );
	 if ( globals.resGrid != null ) globals.resGrid.resize( width, height );
      }
      if ( changeGridSize || changeFrameSize ) {
	 frame.setSize( ( width + 1 ) * cellSize, ( height + 1 ) * cellSize + 40 );
	 frame.doLayout();
	 if ( frame.isVisible() ) {
	    offscreen = null;
	    if ( offscreenImg != null ) offscreenImg.flush();
	    offscreenImg = null;
	    Rectangle bnds = getBounds();
	    getGraphics().clearRect( bnds.x, bnds.y, bnds.width, bnds.height );
	 }
      }

      pause = proc.getIntArg( "pause" );
      keepMin = proc.getIntArg( "mmin" );
      keepLast = proc.getBooleanArg( "keep" );
      skipHowManyForDrawing = proc.getIntArg( "skip" );
      saveGifStep = proc.getIntArg( "shot" );
      props.setProperty( "ScreenShotPrefix", proc.getArg( "shotprefix" ) );
      saveStep = proc.getIntArg( "save" );
      props.setProperty( "SaveFilePrefix", proc.getArg( "saveprefix" ) );
      statsStep = proc.getIntArg( "stats" );

      bugSeedFileName = proc.getArg( "bseed" );
      numSeed = proc.getIntArg( "nseed" );

      maxBugs = proc.getIntArg( "bmax" );
      globals.drawOne = proc.getBooleanArg( "one" );
      globals.stepByOne = proc.getBooleanArg( "sbo" );
      globals.stepByLots = proc.getBooleanArg( "sbl" );

      props.setProperty( "InitialCycles", proc.getArg( "cyc" ) );
      globals.setInitialCycles( proc.getArg( "cyc" ) );
      globals.move_penalty = proc.getIntArg( "mp" );
      globals.max_cmut = proc.getIntArg( "cmax" );
      globals.max_dd = proc.getIntArg( "ddmax" );
      globals.point_mut = proc.getIntArg( "pmut" );
      globals.copy_mut_default = proc.getIntArg( "cmut" );
      globals.divide_mut_default = proc.getIntArg( "dmut" );
      globals.divide_ins_default = proc.getIntArg( "imut" );
      globals.max_age = proc.getIntArg( "agemax" );
      globals.max_cycles = proc.getIntArg( "cycmax" );

      String newDict = proc.getArg( "dict" );
      if ( ! props.getProperty( "Dictionary" ).equals( newDict ) ) {
	 props.setProperty( "Dictionary", newDict );
	 Genome.setDictionaryName( globals, newDict );
      }

      String newRes = proc.getArg( "res" );
      if ( ! props.getProperty( "Resources" ).equals( newRes ) ) {
	 props.setProperty( "Resources", newRes );
	 Resource.initializeResources( globals, newRes );
      }

      globals.max_resources = proc.getIntArg( "rmax" );
      globals.newLocalResPerTurn = proc.getIntArg( "lres" );
      globals.newGlobalResPerTurn = proc.getIntArg( "gres" );
   }   

   public static void main( String args[] ) {
      int num = 1;
      if ( args.length > 1 ) for ( int i = 0; i < args.length - 1; i ++ )
	 if ( args[ i ].equals( "-N" ) ) num = Integer.parseInt( args[ i + 1 ] );
      for ( int i = 0; i < num; i ++ ) {
	 YajaLife world = new YajaLife( ".", args );
      }
   }
}
