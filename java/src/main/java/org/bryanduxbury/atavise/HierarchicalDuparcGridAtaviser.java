package org.bryanduxbury.atavise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchicalDuparcGridAtaviser implements GridAtaviser {

  private final RowAtaviser rowAtaviser;

  public HierarchicalDuparcGridAtaviser(RowAtaviser rowAtaviser) {
    this.rowAtaviser = rowAtaviser;
  }

  @Override public List<int[]> atavise(int cols, int rows, int[] grid) {
    List<int[]> results = internalAtavise(cols, rows, grid, 0, grid.length);
    return results;
  }

  private List<int[]> internalAtavise(int cols, int rows, int[] grid, int startRow, int endRow) {
    System.out.println("!");
    if (endRow - startRow == 1) {
      // cool, down to one row
      // row-atavise it
      List<int[]> rowPriors = rowAtaviser.atavise(cols + 2, grid[startRow] << 1);
      return rowPriors;
      //return index(rowPriors);
    }

    int mid = (endRow - startRow) / 2 + startRow;

    List<int[]> topPriors = internalAtavise(cols, rows, grid, startRow, mid);
    Map<TwoInts, Map<TwoInts, int[]>> topsByBottom =
        indexBy(topPriors, mid - startRow, mid - startRow + 1, 0, 1);
    List<int[]> bottomPriors = internalAtavise(cols, rows, grid, mid, endRow);
    Map<TwoInts, Map<TwoInts, int[]>> bottomsByTops =
        indexBy(bottomPriors, 0, 1, endRow - mid, endRow - mid + 1);

    List<int[]> newSolutions = new ArrayList<int[]>();

    for (Map.Entry<TwoInts, Map<TwoInts, int[]>> top : topsByBottom.entrySet()) {
      Map<TwoInts, int[]> matchingBottoms = bottomsByTops.get(top.getKey());
      if (matchingBottoms != null) {
        for (int[] left : top.getValue().values()) {
          for (int [] right : matchingBottoms.values()) {
            newSolutions.add(merge(left, right));
          }
        }
      }
    }

    if (newSolutions.size() > 100000) {
      newSolutions = newSolutions.subList(0, 100000);
    }

    //for (int[] top : topPriors) {
    //  for (int[] bottom : bottomPriors) {
    //    if (top[top.length-2] == bottom[0] && top[top.length-1] == bottom[1]) {
    //      newSolutions.add(merge(top, bottom));
    //    }
    //  }
    //}

    return newSolutions;
  }

  static Map<TwoInts, Map<TwoInts, int[]>> indexBy(List<int[]> solns, int a, int b, int c, int d) {
    Map<TwoInts, Map<TwoInts, int[]>> indexed = new HashMap<TwoInts, Map<TwoInts, int[]>>();

    for (int[] soln : solns) {
      TwoInts x = new TwoInts(soln[a], soln[b]);
      TwoInts y = new TwoInts(soln[c], soln[d]);
      Map<TwoInts, int[]> ys = indexed.get(x);
      if (ys == null) {
        ys = new HashMap<TwoInts, int[]>();
        indexed.put(x, ys);
      }
      ys.put(y, soln);
    }

    return indexed;
  }

  private static int[] merge(int[] top, int[] bottom) {
    int[] ret = new int[top.length + bottom.length - 2];
    System.arraycopy(top, 0, ret, 0, top.length);
    System.arraycopy(bottom, 2, ret, top.length, bottom.length - 2);
    return ret;
  }

  // benchmarking only!
  public static void main(String[] args) {
    HierarchicalDuparcGridAtaviser a =
        new HierarchicalDuparcGridAtaviser(new IntersectingRowAtaviser());
    long startTime = System.currentTimeMillis();
    //for (int trial = 0; trial < 10; trial++) {
      for (int i = 0; i < 1; i++) {
        a.atavise(5, 25, new int[]{0, 0, 31, 17, 31, 0, 0, 18, 31, 16, 0, 0, 10, 0, 0, 31, 17, 31, 0, 0, 18, 31, 16, 0, 0});
      }
    //}

    long endTime = System.currentTimeMillis();
    System.out.println(endTime-startTime);
  }
}