//testing rsync 07
/*
 * EventAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on August 26, 2015, 02:21 AM
 * @author Christopher Milke
 *
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.ScippUtils;
import scipp_ilc.base.util.LCIOFileManager;


import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

import org.lcsim.util.Driver;

import java.util.List;


public class GammaGamma extends Driver {
    
    
    //DEFINE XML FUNCTIONS
    //These functions are specially formatted functions to pull variable data from the xml file
    /*****************************************************************************************
        XML FUNCTION FORMAT

    public void //setVariablename(variable type) { //the first letter after "set" must be uppercase
                                                  //but can (must?) be lowercase in xml file
        set variable here; 
    }
    *******************************************************************************************/

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }


    public void setBeamOutAligned(boolean algn) {
        aligned = algn;
    }
    
    
    //END DEFINE XML FUNCTIONS




    //This function is called when the program is first started
    //and initializes all persistent data
    public void startOfData() {
        eventNumber = 0;
        String root_mode = "NEW";
        
        try {
            root = new Jroot(jrootFile,root_mode);
            
            /* PLOTS */ 
            root.init("TH1D","hist1","posz","z position",2000,2000,4000);
            root.init("TH2D","posxy_e","posxy_e","X Y Hit Occupancy Over All Layers", 350, -175, 175, 350, -175, 175);        
            root.init("TH2D","posxy_p","posxy_p","X Y Hit Occupancy Over All Layers", 350, -175, 175, 350, -175, 175);        
            root.init("TH1D","energy_e","energy_e","Final State Electron Energy", 1000, 0.0, 250.0);
            root.init("TH1D","energy_p","energy_p","Final State Positron Energy", 1000, 0.0, 250.0);
           
            root.init("TH2D", "EvT_e", "EvT_e", "Energy v Theta of FS Electron", 1000, -0.02, 0.02, 1000, 0.0, 250.0); 
            root.init("TH2D", "EvT_p", "EvT_p", "Energy v Theta of FS Positron", 1000, -0.02, 0.02, 1000, 0.0, 250.0); 
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

            /* FINAL OUTPUT */
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process( event );

        double pos[] = new double[3];
        double mom[] = new double[3];
        double tmom[] = new double[4];

        int stat, id;
        
        for(MCParticle p: event.getMCParticles()){
            stat = p.getGeneratorStatus();
            if(stat == MCParticle.FINAL_STATE){
                try{
                    //get endpint 3-vector
                    pos = p.getEndPoint().v();
                }
                catch (RuntimeException ex){
                    System.out.println(ex);
                }
                try{
                    //get momentum 3-vector        
                    pos = p.getEndPoint().v();
                    mom = p.getMomentum().v();
                                                                    
                    //get transformed momentum 4-vector: <p_x, p_y, p_z>
                    tmom = ScippUtils.transform(mom, p.getEnergy());
                
                    //get PDG id number
                    id = p.getPDGID();
                   
                    //calculate theta
                    double angles[] = PolarCoords.CtoP(tmom[0], tmom[1]);
                    double rad = angles[0];
                    double theta = Math.atan(rad/Math.abs(tmom[2]));
                    
                     
                    //if electron
                    if(id==11){
                        root.fill("posxy_e", pos[0], pos[1]);
                        root.fill("energy_e", tmom[3]);
                        root.fill("EvT_e", theta, tmom[3]);
                    }
                    //if positron
                    else if(id==-11){
                        root.fill("posxy_p", pos[0], pos[1]);
                        root.fill("energy_p", tmom[3]);
                        root.fill("EvT_p", theta, tmom[3]);
                    }
                }
                catch(java.io.IOException e) {
                    System.out.println(e);
                    System.exit(1);
                }
                
            }//end FINAL_STATE
        }//end events

        System.out.println("finished event "+ ++eventNumber);        

    }//End Process


    /*here all the classwide variables are declared*/
    private int eventNumber;
    double inc_ang = 0.007;

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private Jroot root;
    private boolean aligned;
}
