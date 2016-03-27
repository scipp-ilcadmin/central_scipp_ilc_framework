/*
 * ElectronTagger.java
 *
 * Created on March 26, 2016
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.beamcal.geometry.SimpleListGeometry;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;

import java.util.List;


public class ElectronTagger extends Driver {
    
    //BEGIN DEFINE XML FUNCTIONS
    public void setOutputfile(String s) {
        this.jrootFile = s;
    }

    public void setGeomfile(String f) {
        this.Geometry_xml_file = f;
    }
    //END DEFINE XML FUNCTIONS


    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        geom = new SimpleListGeometry(Geometry_xml_file);

        eventNumber = 0;
        String root_mode = "NEW";
        
        try {
            root = new Jroot(jrootFile,root_mode);
            
            root.init("TH2D","scatter1","posxy","X Y Hit Occupancy Over All Layers", 350, -175, 175, 350, -175, 175);        
            
        } catch(java.io.IOException e) {
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
        //List<SimCalorimeterHit> hits = event.get(SimCalorimeterHit.class, "BeamCalHits");

    }//End Process


    /*here all the classwide variables are declared*/
    private int eventNumber;

    //xml derived variables
    private String jrootFile = "";
    private String Geometry_xml_file = "";

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
    private SimpleListGeometry geom;
}
