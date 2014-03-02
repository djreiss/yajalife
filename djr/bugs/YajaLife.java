package djr.bugs;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;
//import JSX.*;

/**
 * Class <code>YajaLife</code>
 *
 * @author <a href="mailto:astrodud@">astrodud</a>
 * @version 1.0
 */
public class YajaLife extends Panel implements Runnable, WindowListener {
   int width = 0, height = 0;
   Globals globals = null;
   int cellSize = 5, skipHowManyForDrawing = 1, pause = 0, keepMin = 0, saveGifStep = -1;
   int saveStep = -1, statsStep = 100;
   Properties props = null;

   boolean stopped = false, initialized = false, drawingResources = false;
   boolean keepLast = false;

   Graphics offscreen = null;
   Image offscreenImg = null;
   Thread thread = null;

   YajaLifeControlPanel ctrlPanel = null;
   Genome selectedBug = null;

   public YajaLife( String codebase ) {
      this.globals = new Globals();
      globals.codebase = codebase;
      readProperties( "YajaLife.properties" );

      setLayout( new BorderLayout() );
      
      setBackground( Color.black );
      initialized = false;
   }

   public void setProperties( Properties prop ) {
      if ( prop != null ) this.props = prop;
      cellSize = Integer.parseInt( this.getParameter( "CellSize" ) );
      width = this.size().width / cellSize;
      height = this.size().height / cellSize;
      pause = Integer.parseInt( this.getParameter( "PauseMillis" ) );
      keepMin = Integer.parseInt( this.getParameter( "MaintainMinimum" ) );
      keepLast = ( new Boolean( this.getParameter( "KeepLast" ) ) ).booleanValue();
      skipHowManyForDrawing = Integer.parseInt( this.getParameter( "SkipHowManyForDrawing" ) );
      saveGifStep = Integer.parseInt( this.getParameter( "ScreenShotStep" ) );
      saveStep = Integer.parseInt( this.getParameter( "SaveStep" ) );
      statsStep = Integer.parseInt( this.getParameter( "StatsStep" ) );

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

      Genome.setDictionaryName( globals, this.getParameter( "Dictionary" ) );
      globals.max_resources = Integer.parseInt( this.getParameter( "MaxResourcesPerCell" ) );
      globals.newLocalResPerTurn = Integer.parseInt( this.getParameter( "LocalResourcesPerStep" ) );
      globals.newGlobalResPerTurn = Integer.parseInt( this.getParameter( "GlobalResourcesPerStep" ) );
      Resource.initializeResources( globals, this.getParameter( "Resources" ) );
   }

   public void initialize() {
      if ( initialized ) return;
      setProperties( this.props );
      
      // Seed the resources grid
      globals.resGrid = new Grid( width, height, globals.max_resources * width * height + 1 );

      // Seed the grid with a set of initial replicators
      globals.bugGrid = new Grid( width, height, Integer.parseInt( this.getParameter( "MaxBugs" ) ) );
      int numBugs = Integer.parseInt( this.getParameter( "NumSeed" ) );
      for ( int i = 0; i < numBugs; i ++ ) new Genome( globals, this.getParameter( "BugSeed" ) );

      globals.bugGrid.cellSize = globals.resGrid.cellSize = cellSize;

      Button ctrlPanelBut = new Button( "Control Panel" );
      add( "South", ctrlPanelBut );
      ctrlPanelBut.setBackground( Color.lightGray );
      ctrlPanelBut.setBounds( size().width - 100, size().height - 25, 100, 25 );
      
      initialized = true;
      if ( thread == null ) thread = new Thread( this );
      thread.start();
   }

   public synchronized void run() {
      boolean done = false;
      while( ! done ) {
	 if ( ! stopped ) repaint();
	 if ( pause > 0 ) try { Thread.sleep( pause ); } catch( Exception e ) { };
	 try {
	    if ( thread != null ) thread.yield();
	    wait();
	 } catch( Exception e ) {
	    Thread.currentThread().interrupt();
	 }
      }
   }

   public boolean action( Event evt, Object what ) {
      if ( "Control Panel".equals( what ) ) {
	 if ( ctrlPanel == null ) ctrlPanel = new YajaLifeControlPanel( this );
	 if ( ! ctrlPanel.isVisible() ) ctrlPanel.show();
	 else ctrlPanel.hide();
      } else if ( "Close".equals( what ) ) {
	 ctrlPanel.hide();
      } else if ( "Pause".equals( what ) ) {
	 this.stop();
	 ( (Button) evt.target ).setLabel( "Resume" );
      } else if ( "Resume".equals( what ) ) {
	 this.start();
	 ( (Button) evt.target ).setLabel( "Pause" );
      } else if ( "Resources".equals( what ) ) {
	 drawingResources = true;
	 ( (Button) evt.target ).setLabel( "Bugs" );
      } else if ( "Bugs".equals( what ) ) {
	 drawingResources = false;
	 ( (Button) evt.target ).setLabel( "Resources" );
      } else if ( "Save".equals( what ) ) {
	 doSave();
      } else if ( "Read".equals( what ) ) {
	 doRead();
      } else if ( "Screen Shot".equals( what ) ) {
	 doScreenShot();
      } else if ( "Exit".equals( what ) ) {
	 this.hide(); System.exit( 0 );
      } else if ( "Stats".equals( what ) ) {
	 keyDown( null, 'r' );
	 keyDown( null, 'g' );
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
      } else {
	 System.err.println( what );
      }
      return true;
   }

   public synchronized void paint( Graphics g ) {
      if ( ! initialized ) initialize();

      if ( g == null ) return;
      if ( offscreen == null ) {
	 try {
	    offscreenImg = createImage( size().width, size().height );
	    offscreen = offscreenImg.getGraphics();
	 } catch( Exception e ) {
	    offscreen = null;
	 }
      } 
      Graphics gr = ( offscreen != null ) ? offscreen : g;
      Resource.addNewLocalResources( globals );
      Resource.addNewGlobalResources( globals );
      Grid bugs = globals.bugGrid;
      if ( bugs.num < keepMin ) {
	 //System.err.println( "Spawned new bug: " + bugs.steps );
	 if ( ! keepLast ) {
	    for ( int i = bugs.num; i < keepMin; i ++ ) 
	       new Genome( globals, this.getParameter( "BugSeed" ) );
	 } else {
	    Genome gg = (Genome) bugs.getAGriddable();
	    if ( gg == null || gg.generation < 10 ) {
	       new Genome( globals, this.getParameter( "BugSeed" ) );
	    } else { // Give the existing bug a new leaf on life!
	       gg.generation = 0;
	       if ( gg.cycles < globals.initial_cycles[ 0 ] ) 
		  gg.cycles = (int) globals.initial_cycles[ 0 ];
	       gg.removeFromGrid(); // Move it up on the list
	       gg.addToGrid();
	    }
	 }
      }
      boolean drawIt = bugs.step( drawingResources ? null : gr, skipHowManyForDrawing );
      if ( drawingResources ) globals.resGrid.paintHistogram( gr, globals.max_resources, 
							      skipHowManyForDrawing );

      if ( selectedBug != null ) {
	 if ( selectedBug.dead ) selectedBug = null;
	 else {
	    gr.setColor( Color.white );
	    gr.drawRect( selectedBug.x * cellSize, selectedBug.y * cellSize, 
			 cellSize, cellSize );
	 }
      }
      if ( offscreenImg != null ) {
	 if ( drawIt || drawingResources ) g.drawImage( offscreenImg, 0, 0, this );
	 if ( saveGifStep > 0 && bugs.steps % saveGifStep == 0 /*&& bugs.num > 1000*/ ) 
	    doScreenShot();
      }
      if ( saveStep > 0 && bugs.steps % saveStep == 0 ) doSave();
      if ( statsStep > 0 && bugs.steps % statsStep == 0 ) action( null, "Stats" );
      notifyAll();
   }

   public void doScreenShot() {
      FileOutputStream fos = null;
      try {
	 String file = this.getParameter( "ScreenShotPrefix" ) + globals.bugGrid.steps + ".gif";
	 File temp = new File( ( new File( file ) ).getParent() );
	 temp.mkdirs();
	 fos = new FileOutputStream( file );
	 Acme.JPM.Encoders.GifEncoder genc = 
	    new Acme.JPM.Encoders.GifEncoder( offscreenImg, fos );
	 genc.encode();
      } catch( Exception e ) {
	 e.printStackTrace();
      } finally {
	 try { if ( fos != null ) { fos.flush(); fos.close(); } } catch( IOException e ) { };
      }
   }

   /*public void doSaveXML() {
      try {
	 String file = this.getParameter( "SaveFilePrefix" ) + globals.bugGrid.steps + ".dat";
	 File temp = new File( ( new File( file ) ).getParent() );
	 temp.mkdirs();
	 FileOutputStream fos = new FileOutputStream( file );	 
	 ObjOut out = new ObjOut( new BufferedOutputStream( fos ) );
	 out.writeObject( props );
	 out.writeObject( globals );
	 fos.close();
      } catch( Exception e ) { e.printStackTrace(); }
      }*/

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
      stop();
      try {
	 DataInputStream dis = 
	    new DataInputStream( djr.util.MyUtils.OpenFile( "test.dat" ) );
	 java.util.zip.GZIPInputStream gis = new java.util.zip.GZIPInputStream( dis );
	 ObjectInputStream in = new ObjectInputStream( gis );
	 Properties newProps = (Properties) in.readObject();
	 if ( newProps != null ) setProperties( newProps );
	 Globals newGlob = (Globals) in.readObject();
	 if ( newGlob != null ) globals = newGlob;
      } catch( Exception e ) {
	 e.printStackTrace();
      }

      Graphics gr = ( offscreen != null ) ? offscreen : getGraphics();
      globals.bugGrid.step( gr, 1 );
      if ( offscreenImg != null ) getGraphics().drawImage( offscreenImg, 0, 0, this );
      if ( ! save ) start();

      System.err.println( "BUGS = " + globals.bugGrid.num +"; STEPS = " + 
			  globals.bugGrid.steps );
   }

   public void update( Graphics g ) {
      paint( g );
   }

   public synchronized void start() {
      stopped = false;
      notify();
   }

   public synchronized void stop() {
      stopped = true;
   }

   public void destroy() {
      stop();
      thread = null;
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

   public void windowOpened( WindowEvent e ) { this.show(); }
   public void windowClosing( WindowEvent e ) { this.hide(); System.exit( 0 ); }
   public void windowClosed( WindowEvent e ) { this.hide(); System.exit( 0 ); }
   public void windowIconified( WindowEvent e ) { }
   public void windowDeiconified( WindowEvent e ) { }
   public void windowActivated( WindowEvent e ) { }
   public void windowDeactivated( WindowEvent e ) { }   

   public boolean mouseDown( Event e, int x, int y ) {
      x /= cellSize;
      y /= cellSize;
      selectedBug = (Genome) globals.bugGrid.getGriddableAt( x, y );
      if ( selectedBug != null ) System.err.println( selectedBug.toString() );
      return true;
   }

   public boolean keyDown( Event e, int key ) {
      if ( key == 'r' ) System.err.println( Resource.printStats( globals ) );
      else if ( key == 'g' ) System.err.println( Genome.printStats( globals.bugGrid ) );
      return true;
   }

   public static void main( String args[] ) {
      int num = args.length > 0 ? Integer.parseInt( args[ 0 ] ) : 1;
      for ( int i = 0; i < num; i ++ ) {
	 Frame f = new Frame( "YajaLife" );
	 YajaLife world = new YajaLife( "." );
	 f.setLayout( new BorderLayout() );
	 int width = Integer.parseInt( world.getParameter( "Width" ) );
	 int height = Integer.parseInt( world.getParameter( "Height" ) );
	 f.setBounds( 0, 0, width, height );
	 f.add( "Center", world );
	 f.addWindowListener( world );
	 f.show();
      }
   }
}
