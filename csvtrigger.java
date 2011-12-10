import com.cycling74.max.*;
import java.util.*;
import java.io.*;

/**
   Triggers events based on float inlet value.
   Usage messages:
    - load <filename>
         <filename> points to a csv file, where the first value 
         is a float value (timing) and the second value is an integer (trigger id)
    - reset
 */
public class csvtrigger extends MaxObject {

    private class Event {
        public float  time;
        public int symbol;
    }

    private int           current   = -1;
    private float         lastvalue = -1;
    private Vector<Event> events    = null;

    public void inlet(float p) {
        int inlet_id = getInlet();
        if(events == null) {
            System.out.println("csvtrigger: you should load a trigger file first");
            return;
        }
        if(inlet_id == 0) {
            // System.out.println("float received: " + p);
            if(p > lastvalue)
                lastvalue = p;
            boolean triggered = false;
            while(current + 1 < events.size() && lastvalue >= events.elementAt(current+1).time) {
                current++;
                triggered = true;
            }
            if(triggered) {
                outlet(0, events.elementAt(current).symbol);
            }
        }
    }

    private void parseFile(String filepath) {
        
        events    = null;
        lastvalue = -1;
        current   = -1;

        try {
            BufferedReader in   = new BufferedReader(new FileReader(new File(filepath)));
            events              = new Vector<Event>();
            while(true) {
                String     line = in.readLine();
                if(line == null)
                    break;
                if(line.length() > 0) {
                    Event  e    = new Event();
                    e.time      = Float.parseFloat(line.split(",")[0]);
                    e.symbol    = Integer.parseInt(line.substring(line.indexOf(",")+1));
                    events.add(e);
                }
            }
        } catch(FileNotFoundException e) {
            System.out.println("csvtrigger: CANNOT FIND FILE " + filepath);
        }   catch(IOException e) {
            System.out.println("csvtrigger: AN ERROR OCCURRED WHILE PARSING FILE " + filepath);
            events = null;
        }

    }

    public void anything(String symbol, Atom[] args) {
        int inlet_id = getInlet();
        if(inlet_id == 0) {
            if(symbol.compareTo("reset") == 0) {
                lastvalue = -1;
                current   = -1;
            } else if(symbol.compareTo("load") == 0) {
                parseFile(args[0].getString());            
            }
        }
    }

}

 