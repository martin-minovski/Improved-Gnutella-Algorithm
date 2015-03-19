package frames;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author Martin
 */
public class MainFrame extends JFrame {

    final mxGraph graph;
    Object parent;
    
    ArrayList<Object> nodes = new ArrayList<>();
    ArrayList<String> nodeNames = new ArrayList<>();
    Random rand = new Random();
    
    int circleCounter = 0;

    public void insertNode(String name) {

        int x = 60 + rand.nextInt(1024 - 120);
        int y = 60 + rand.nextInt(800 - 120);
        
        if (circleCounter < 8) {
            switch (circleCounter) {
                case 0: 
                    x = 500;
                    y = 100;
                    break;
                    
                case 1: 
                    x = 750;
                    y = 250;
                    break;
                    
                case 2: 
                    x = 900;
                    y = 500;
                    break;
                    
                case 3: 
                    x = 750;
                    y = 750;
                    break;
                    
                case 4: 
                    x = 500;
                    y = 900;
                    break;
                    
                case 5: 
                    x = 250;
                    y = 750;
                    break;
                    
                case 6: 
                    x = 100;
                    y = 500;
                    break;
                    
                case 7: 
                    x = 250;
                    y = 250;
                    break;
                    
                default:
                    break;
            }
            
            circleCounter++;
        }
        
        nodes.add(graph.insertVertex(parent, null, name, x, y-80, 60, 20));
        nodeNames.add(name);
    }

    public void addVertex(String node1, String node2, String vertexName) {
        Object n1 = null;
        Object n2 = null;

        for (int i = 0; i < nodes.size(); i++) {
            if (nodeNames.get(i).equals(node1)) {
                n1 = nodes.get(i);
            }
            if (nodeNames.get(i).equals(node2)) {
                n2 = nodes.get(i);
            }
        }

        Object e = graph.insertEdge(parent, null, vertexName, n1, n2);
        
    }

    public MainFrame() {
        super("Peer Map");
        
        graph = new mxGraph();
        graph.getModel().beginUpdate();
        
        parent = graph.getDefaultParent();


    }
    
    public void render() {
        graph.getModel().endUpdate();
        final mxGraphComponent graphComponent = new mxGraphComponent(graph);
        getContentPane().add(graphComponent);
    }
    
    
    
}
