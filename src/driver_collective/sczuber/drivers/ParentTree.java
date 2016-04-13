/*
 * FinalStateAnalysis.java
 *
 * Created on July 11, 2013, 10:45 AM
 * Edited on January 9, 2014, 12:31 AM
 * @author Alex Bogert and Christopher Milke
 * Modified by Jane Shtalenkova on
 * April 22, 2015
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


public class ParentTree extends Driver {
    
    
    
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
    
    public void setInput1(String name) {
        this.lcioFileMNGR.setFilelist(name);
    }
    //END DEFINE XML FUNCTIONS





    //This function is called when the program is first started
    //and initializes all persistant data
    public void startOfData() {
        eventNumber = 0;

        try {
            root = new Jroot(jrootFile, "NEW");
	  
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
            
            System.out.print(tree);
            System.out.println(top);
            System.out.println("Total of " + g_Par + " events with gamma tops");
            root.end();
        }
        catch (java.io.IOException e) {
           System.out.println(e);
           System.exit(1);
        }
    }

	//Get electron signal event and ensure it actually hits the detector
	//public static EventHeader getEvent(LCIOFileManager mngr) {
        	//EventHeader event = mngr.nextEvent();

        	//if (event == null)
            		//throw new java.lang.RuntimeException("There are no signal events left. Process ending");

        	//MCParticle electron = ScippUtils.getElectron(event);

		//return event;

        //}
    
    public void printParents(MCParticle p, int n){
        //create appropriate indentation from child
        for(int i=0; i<n; i++){
            tree+="   ";
        }
        //update tree String
        String code = p.toString().substring(29);
        tree+= (code + "State: " + p.getGeneratorStatus() + " id: " + p.getPDGID() + "  Type: "+ p.getType() + " Energy: " +p.getEnergy() + "\n");
        
        //if parentless check for redundancy before incrementing g_Par
        if(p.getParents().size()==0){
            if(p.getPDGID()==22){
                boolean test = false;
                if(i>0){
                    for (int j = 0; j<=i;i++){
                        if(gs[j].equals(gs[i])==false){
                            j++;
                        }
                        else{
                           test=true; 
                        }
                    }
                }
                if(test=false){
                   code = gs[i];
                   i++; 
                   g_Par++;
                }
               
            }    
        }
        for (int k = 0; k <=i; k++){
            top+= (gs[k] + ", ");
        }
        for(MCParticle u : p.getParents()){
            printParents(u, n+1);
        }
    }
    public void printElectron(EventHeader event) {
        MCParticle mcp = null;
        System.out.println("\n\n\n\n\n\n**************NEW EVENT*******************\n\n\n");
                
        Arrays.fill(gs, null); 
        
        //number of events with gamma parents
        g_Par=0;
        i=0;
        
        top+= "\nEvent: " + eventNumber;
        
        for (MCParticle p : event.getMCParticles()) {
            
            int stat = p.getGeneratorStatus();
            //if(stat == MCParticle.FINAL_STATE){
                ParticleType type = p.getType();
		

                String name = type.getName();
                int id = p.getPDGID();
		
		
                //for every electron
                if(id==11 && p.getParents().size()==1){
                    tree+=( "Event: " + eventNumber + "\n");
                            
                    this.printParents(p, 0);
                    
                    tree+= "\n\n\n\n";
                }

                if(id==11 && p.getParents().size()==2){

                }

                if(id==-11 && p.getParents().size()==1){

                }

                if(id==-11 && p.getParents().size()==2) {

            //    }    
               		
		

            }//end final state
                   
            
           
        }//end event
       
        
        
    }//end printElectron
        
        
    //PROCESS FUNCTION
    //This is where the vast bulk of the program is run and controlled
    public void process( EventHeader event ) {
        super.process(event);

       
       
        
            this.printElectron(event);
            
            
            
        
        
     System.out.println("finished event "  + eventNumber++ + "\n\n");
     
    }//End Process
    
    
//xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;
 
    int eventNumber;
    String tree, top;
    int g_Par, i;
    String gs[]=new String[10];

    }
