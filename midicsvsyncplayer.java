/**
   Load a midicsv file -  http://www.fourmilab.ch/webtools/midicsv/.
   Output the midi notes (ready for noteout obj) by sending a float msg containing
   the score position, measured in quarter notes from the beginning of the score.
   The "reset" msg. resets the object to its initial state.
*/

import java.util.*;
import java.io.*;
import com.cycling74.max.*;

public class midicsvsyncplayer extends MaxObject {

    /** represent a note in/out information, with timing and channel*/
    private class NoteInOut implements Comparable<NoteInOut> {
        private int channel;      // starting from 1
        private int note;         // in midi notation
        private float timing;     // in quarter notes from the beginning
        private int velocity;     // midi velocity (0 for noteoff)
        public int compareTo(NoteInOut n) {
            if(timing < n.timing)
                return -1;
            if(velocity < n.velocity)
                return -1;
            if(note < n.note)
                return -1;
            if(channel < n.channel)
                return -1;
            return 1;
        }
        public String toString() {
            return String.format("[%7.2f: %3d (%s %3d %3d)]", timing, 
                                 note, velocity > 0 ? " on" : "off", velocity, channel);
        }
    }

    // private stuff
    private NoteInOut[] noteEvents = null;
    private int currentpointer     = 0;

    // parse file into the noteEvents array
    private void parseMidiCsv(String filepath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(filepath)));
        int quarternoteduration = -1;
        List<NoteInOut> events = new LinkedList<NoteInOut>();
        String line;
        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");
            if(tokens.length > 0) {
                String type = tokens[2].trim().toLowerCase();
                if(type.compareTo("header") == 0) {
                    quarternoteduration = Integer.parseInt(tokens[5].trim());
                } else if(type.compareTo("note_on_c") == 0 || type.compareTo("note_off_c") == 0) {
                    NoteInOut n = new NoteInOut();
                    n.channel  = Integer.parseInt(tokens[3].trim());
                    n.timing   = Float.parseFloat(tokens[1].trim()) / quarternoteduration;
                    n.note     = Integer.parseInt(tokens[4].trim());
                    n.velocity = Integer.parseInt(tokens[5].trim());
                    if(type.compareTo("note_off_c") == 0)
                        n.velocity = 0;
                    events.add(n);
                }
            }
        }
        noteEvents = new NoteInOut[events.size()];
        events.toArray(noteEvents);
        Arrays.sort(noteEvents);
        currentpointer = 0;
    }

    public midicsvsyncplayer() { 
        //        declareOutlets(new int[] { DataTypes.LIST });
    }

    /** float into an inlet means a position */
    public void inlet(float p) {
        int inlet_id = getInlet();
        if(inlet_id == 0) {
            int i = currentpointer;
            if(noteEvents != null) {
                while(i < noteEvents.length && i >= 0 && noteEvents[i].timing < p) {
                    int[] msg = new int[3];
                    msg[0] = noteEvents[i].note;
                    msg[1] = noteEvents[i].velocity;
                    msg[2] = noteEvents[i].channel;
                    outlet(0,msg);
                    i++;
                }
            }
            currentpointer = i;
        }
    }

    /** generic control msg */
    public void anything(String symbol, Atom[] args) {
        if(symbol.compareTo("load") == 0) {
            if(args.length != 1) {
                System.out.println("invalid argument list; must be \"load filename\"");
                return;
            }
            String filepath = args[0].getString();
            try {
                parseMidiCsv(filepath);
            } catch(IOException e) {
                System.out.println("could not parse input file");
            }
        } else if(symbol.compareTo("reset") == 0) {
            currentpointer = 0;
        }

    }

}
