import javax.swing.*;        


public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  private RouterPacket pkt;
  private int[] costs = new int[RouterSimulator.NUM_NODES];
  private int[] hops = new int[RouterSimulator.NUM_NODES];
  private int[][] distanceTable = new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];

  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] costs) {
    myID = ID;
    this.sim = sim;
    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");

    System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);

    //this.costs = costs;

    for(int i = 0; i < costs.length; i++) {
      //System.out.println(costs[i]);
      for(int j = 0; j < costs.length; j++) {
        //distanceTable[i][j] = RouterSimulator.INFINITY;
        distanceTable[i][j] = 1337;
      }
      System.arraycopy( costs, 0, distanceTable[myID], 0, costs.length );

      if (costs[i] == RouterSimulator.INFINITY) {
        hops[i] = RouterSimulator.INFINITY;
      }
      else {
        hops[i] = i;
      }

      if (costs[i] != 0) {
         //System.out.println("source: " + myID + " Destination: " + i);
         RouterPacket pkt = new RouterPacket(myID, i, costs);
         sendUpdate(pkt);
      }
    }

  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
 
  int[] recvCosts = pkt.mincost;
  boolean changed;
  int[] arr = new int[RouterSimulator.NUM_NODES];  
  
  for (int i = 0; i < costs.length; i++) {
    changed = false;
    if (costs[i] == 0) {
      //distanceTable[myID][myID] = 0;
      continue;
    }

    int alt = costs[pkt.sourceid] + recvCosts[i];
    if (alt < costs[i]) {
      changed = true;
      //updateLinkCost(i, alt);
      //hops[i] = pkt.sourceid;
      costs[i] = alt;
      System.arraycopy( costs, 0, distanceTable[pkt.sourceid], 0, costs.length );

      distanceTable[pkt.sourceid][i] = RouterSimulator.INFINITY;      
    }
  
    //if (hops[i])
    if (changed) {  
      for (int j = 0; j < costs.length; j++) {
        RouterPacket update = new RouterPacket(pkt.destid, j, costs);
        sendUpdate(update); 
      }
    } 
  }
}
  

  //--------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    sim.toLayer2(pkt);

  }
  

  //--------------------------------------------------
  public void printDistanceTable() {
    StringBuilder str = new StringBuilder("");
    String delim = "------------------------------------";
    String spaces = "   ";

	  myGUI.println("Current table for " + myID +
			"  at time " + sim.getClocktime());
    myGUI.println(delim);

    for (int a = 0; a < costs.length; a++) {
      if (myID == a) {
        continue;
      }
      str.append("Node #" + a + spaces);
      for (int b = 0; b < costs.length; b++) {
        str.append(distanceTable[a][b] + spaces);
      }
      str.append("\n");
    }
    myGUI.println(str.toString());

    myGUI.println(delim);
    for(int i = 0; i < costs.length; i++) {
      myGUI.println("cost: " + costs[i] + " via: " + hops[i]);
    }
    myGUI.println(delim);
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    //this.costs[dest] = newcost;
    System.out.println("Old cost: " + costs[dest] + " New cost: " + newcost + " For node: " + myID + " To node: " + dest);
    costs[dest] = newcost;
    
  }

}
