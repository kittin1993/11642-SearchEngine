/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The SYN operator for all retrieval models.
 */
public class QryIopNear extends QryIop {

  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   *  
   */
  private int distance = 0;
  public QryIopNear (int distance) {
      this.distance = distance;
  }
  protected void evaluate () throws IOException {

    //  Create an empty inverted list.  If there are no query arguments,
    //  that's the final result.
    
    this.invertedList = new InvList (this.getField());

    if (args.size () == 0) {
      return;
    }

    //  Each pass of the loop adds 1 document to result inverted list
    //  until all of the argument inverted lists are depleted.

    while (true) {

      //  Find the minimum next document id.  If there is none, we're done.

      int minDocid = Qry.INVALID_DOCID;
      if(!this.docIteratorHasMatchAll(null)) {
          return;
      }
      
      
      minDocid = this.docIteratorGetMatchCache(); 
      Qry q = this.args.get(0);
      List<Integer> positions = new ArrayList<Integer>();
      
      Vector<Integer> locations =
              ((QryIop) q).docIteratorGetMatchPosting().positions;
      positions.addAll(locations);
      
      for(int i = 1; i < this.args.size(); i++) {
          Qry q_i = this.args.get(i);
          positions = getPosition(positions, ((QryIop)q_i).docIteratorGetMatchPosting().positions);
          q_i.docIteratorAdvancePast(minDocid);
      }
      
      //positions' size is the final score
      if(positions.size() > 0) {
          this.invertedList.appendPosting(minDocid, positions);
      }

      this.args.get(0).docIteratorAdvancePast(minDocid);
    }
  }
  
  /**
   *  Helper functions help to get the list of positions that meet the near/n requirements
   *  between two arguments
   *  @param positions 
   *           the list stores positions(docid) of the first argument
   *  @param locations       
   *           the vector stores positions(docid) of the other argument 
   *  @return the list contains all positions(docid) that meet the distance limitation.
   *  
   */
  private List<Integer> getPosition(List<Integer> positions, Vector<Integer> locations) {
      List<Integer> result = new ArrayList<Integer>();
      for (int i = 0, j = 0; i < positions.size() && j < locations.size(); ) {
          if (positions.get(i) >= locations.get(j)) {
              j++;
          } else {
              if(locations.get(j) - positions.get(i) <= this.distance) {
                  result.add(locations.get(j));
                  i++;
                  j++;
              } else {
                  i++;
              }
          }
      }
      return result;
  }

}
