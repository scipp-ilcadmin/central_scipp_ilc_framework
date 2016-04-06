/*
 * Lorentz.java
 *
 * Edited on January 9, 2014, 12:31 AM
 * @author Jane Shtalenkova on
 * April 5, 2016
 */
package scipp_ilc.drivers;

import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.Jroot;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import org.lcsim.util.Driver;

import java.lang.String;
import java.util.Arrays;

public class Lorentz extends Driver {
	

    //DEFINE XML FUNCTIONS
    //These functions are specially fomatted functions to pull variable data from the xml file
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

    //END DEFINE XML FUNCTIONS

	//This function is called when the program is first started
    //and initializes all persistant data
    
    public void startOfData() {
        eventNumber = 0;
    /*    try {

            //PLOTS
        }
        catch(java.io.IOException e){
            System.out.println(e);
            System.exit(1);        
        }*/
    }
    public void EndOfData(){

        /*try{

            //FINAL OUTPUT
            
        }
        catch(java.io.IOException e){
            System.out.println(e);
            System.exit(1);        
        }*/
    }


    //METHODS: TRANSFORM- IN: (px, py, pz), and E
    //                    OUT: (px', py', pz', E')  
    public double[] transform(double[] p, double E){
        double theta = inc_ang;
        double in_E = E_e;
        double beta = Math.sin(theta);
        double gamma = Math.pow((1-Math.pow(beta, 2)), -0.5);
        double[] cm_Vec = new double[4];

        /*
         *       |gamma         -gamma*beta| |p|                       |p'|
         *       |-gamma*beta         gamma|*|E| = TRANSOFORMED4vector |E'|
         *  
         *      */
    
        cm_Vec[0] = p[0]*gamma - gamma*beta*E;
        cm_Vec[1] = p[1];
        cm_Vec[2] = p[2];
        cm_Vec[3] = E*gamma - gamma*beta*p[0];
        return cm_Vec;
    }

    


    /*VARIABLES*/
    private int eventNumber, totEvs;
    private int faceZ = 2950;
    private double inc_ang = 0.007;
    private double E_e = 250.0;

    //XML*/
    private String jrootFile = "";

    //jroot file construction ; background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;


}      
