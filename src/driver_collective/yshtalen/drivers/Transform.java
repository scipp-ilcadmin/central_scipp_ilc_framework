/* Transform.java
 *
 * Created Apr 4, 2016, 10:11PM
 * @author Summer Zuber and Jane Shtalenkova
 */

package scipp_ilc.drivers;

import scipp_ilc.base.util.PolarCoords;
import scipp_ilc.base.util.LCIOFileManager;
import scipp_ilc.base.util.Jroot;
import scipp_ilc.base.util.ScippUtils;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;

import hep.physics.particle.properties.ParticleType;

import java.lang.String;
import java.util.Arrays;

import org.lcsim.util.Driver;

public class Transform extends Driver {

    public void setOutputfile(String s) {
        this.jrootFile = s;
    }

    public void startOfData() {
        eventNumber=0;
        try {
            root = new Jroot(jrootFile, "NEW");
            root.init("TH1D", "YSum", "YSum", "Y-Momentum Check Sum", 1000, -50, 50);
            root.init("TH1D", "XSum", "XSum", "X-Momentum Check Sum", 1000, -50, 50);
            root.init("TH1D", "YTSum", "YTSum", "Transformed Y-Momentum Check Sum", 1000, -50, 50);
            root.init("TH1D", "XTSum", "XTSum", "Transformed X-Momentum Check Sum", 1000, -50, 50);
        }    
        catch (java.io.IOException e){
            System.out.println(e);
            System.exit(1);
        }    
    }    


    public void endOfData(){
        try {
            root.end();
        }    
        catch (java.io.IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    
    public void process( EventHeader event){
        super.process(event);


        //declare variables
        double y_sum = 0;
        double x_sum = 0;
        double x_tsum = 0;
        double y_tsum = 0;

        int id, stat;
        for(MCParticle p : event.getMCParticles()){
            stat = p.getGeneratorStatus();
            id = p.getPDGID();
            if(stat == MCParticle.FINAL_STATE){
                x_sum += p.getPX();
                y_sum += p.getPY();
                double[] mom = ScippUtils.transform(p.getMomentum().v(), p.getEnergy());
                x_tsum += mom[0];
                y_tsum += mom[1];

            }
        }
        
        try{
            root.fill("XSum", x_sum);
            root.fill("YSum", y_sum);
            root.fill("XTSum", x_tsum);
            root.fill("YTSum", y_tsum);
        }    
        catch (java.io.IOException e){
            System.out.println(e);
            System.exit(1);
        }
        
        System.out.println(eventNumber);
        eventNumber++;
    }//End Process


    /*here all the classwide variables are declared*/
    private int eventNumber, totEvs;
    private int faceZ = 2950;
    private double inc_ang = 0.007;

    //xml derived variables
    private String jrootFile = "";

    //variables for jroot file construction and background/signal file reading
    private LCIOFileManager lcioFileMNGR = new LCIOFileManager();
    private Jroot root;


}    
