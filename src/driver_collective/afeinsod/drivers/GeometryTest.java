/*
 * GeometryTest.java
 *
 * Created on Mar 27, 2014, 10:05 PM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.beamcal.geometry.*;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;
import org.lcsim.util.Driver.NextEventException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collections;

import java.lang.String;

public class GeometryTest extends Driver {
    public GeometryTest() {} //null constructor

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }
    
    
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        geom = new SimpleListGeometry("geom.xml");
	try { 
		root =new Jroot(jrootFile, "New");
		root.init("TH2D","tilepic", "tilepic","Tile Picture", 3000, -10F, 10F, 3000, -10F, 10F);
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }


    //This function is called after all file runs have finished,
    // and closes any necessary data
    public void endOfData(){
        try {
            root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        float tileID = 0;
        
        try {
                for( float x = -6; x < 6 ; x +=0.5 ) {
                    for ( float y = -6 ; y < 6 ; y +=0.5 ) {
                        System.out.println("x="+x+", y="+y);
                        

                        float[] coords = new float[2];
                        coords[0] = x;
                        coords[1] = y;

                        int testID = geom.getID(x,y);

                        if (tileID != testID)
                            root.fill("tilepic",x,y);
                        
                        tileID = testID;
                    }
                }

                for( float y = -6; y < 6 ; y +=0.5 ) {
                    for ( float x = -6 ; x < 6 ; x +=0.5 ) {
                        System.out.println("x="+x+", y="+y);

                        float[] coords = new float[2];
                        coords[0] = x;
                        coords[1] = y;

                        int testID = geom.getID(x,y);

                        if (tileID != testID)
                            root.fill("tilepic",x,y);

                        tileID = testID;
                    }
                }

 
        }catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }

    }//End Process
    

    /*here all the classwide variables are declared*/

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private Jroot root;

    //miscellenous variables
    private SimpleListGeometry geom;
}
