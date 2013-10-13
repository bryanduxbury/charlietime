package org.bryanduxbury.atavise;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bryanduxbury.atavise.solution_filter.TubularRowFilter;
import org.bryanduxbury.atavise.solution_limiter.Aggressive;
import org.bryanduxbury.atavise.solution_limiter.SolutionLimiter;

public class HierarchicalDuparcGridAtaviser implements GridAtaviser {

  private final RowAtaviser rowAtaviser;
  private final SolutionLimiter.Factory solnLimiterFactory;

  public HierarchicalDuparcGridAtaviser(RowAtaviser rowAtaviser, SolutionLimiter.Factory solnLimiterFactory) {
    this.rowAtaviser = rowAtaviser;
    this.solnLimiterFactory = solnLimiterFactory;
  }

  @Override public Collection<int[]> atavise(Grid grid) {
    Collection<int[]> results = internalAtavise(grid, 0, grid.getHeight(), null);

    return results;
  }

  private Collection<int[]> internalAtavise(Grid grid, int startRow, int endRow,
      Set<TwoInts> spoilers) {
    if (endRow - startRow == 1) {
      // cool, down to one row
      // row-atavise it
      List<int[]> rowPriors = rowAtaviser.atavise(grid.getWidth() + 2, grid.getCells()[startRow] << 1);
      if (spoilers != null) {
        List<int[]> filtered = new ArrayList<int[]>();
        for (int[] prior : rowPriors) {
          if (spoilers.contains(new TwoInts(prior[0], prior[1]))) {
            filtered.add(prior);
          }
        }
        rowPriors = filtered;
      }
      return rowPriors;
    }

    // compute the midpoint for recursing
    int mid = (endRow - startRow) / 2 + startRow;

    // compute the top half
    Collection<int[]> topPriors = internalAtavise(grid, startRow, mid, null);
    // index the results by the bottom-most rows (while uniqueing by the topmost rows)
    Map<TwoInts, Map<TwoInts, int[]>> topsByBottom =
        indexBy(topPriors, mid - startRow, mid - startRow + 1, 0, 1);
    topPriors = null;

    // compute the results for the bottom half
    Collection<int[]> bottomPriors = internalAtavise(grid, mid, endRow, /*topsByBottom.keySet()*/ null);
    // index the results by the bottom-most rows (while uniqueing by the topmost rows)
    Map<TwoInts, Map<TwoInts, int[]>> bottomsByTops =
        indexBy(bottomPriors, 0, 1, endRow - mid, endRow - mid + 1);
    bottomPriors = null;

    // compute the intersection of all tops and bottoms
    //List<int[]> newSolutions = new ArrayList<int[]>();
    //Map<FourInts, int[]> newSolutions2 = new HashMap<FourInts, int[]>();
    SolutionLimiter sl = solnLimiterFactory.getSolutionLimiter();

    // for each unique bottom in the top set...
    OUTER:
    for (Map.Entry<TwoInts, Map<TwoInts, int[]>> top : topsByBottom.entrySet()) {
      // ... get the set of matching solutions in the bottom set ...
      Map<TwoInts, int[]> matchingBottoms = bottomsByTops.get(top.getKey());
      // ... if there are any matches ...
      if (matchingBottoms != null) {
        // ... then for each unique top in the tops with matching bottoms ...
        for (int[] left : top.getValue().values()) {
          // ... and for each unique bottom in bottoms with matching tops ...
          for (int [] right : matchingBottoms.values()) {
            // ... add a new solution to the result set
            int[] merged = merge(left, right);
            sl.add(merged);

            if (sl.isFull()) {
              break OUTER;
            }
          }
        }
      }
    }

    return sl.getSolutions();
  }

  static Map<TwoInts, Map<TwoInts, int[]>> indexBy(Collection<int[]> solns, int a, int b, int c, int d) {
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
    HierarchicalDuparcGridAtaviser a = new HierarchicalDuparcGridAtaviser(
        new CachingRowAtaviser(new IntersectingRowAtaviser(new TubularRowFilter())),
        new Aggressive.Factory(100000));
    long startTime = System.currentTimeMillis();
    //for (int trial = 0; trial < 10; trial++) {
      for (int i = 0; i < 1; i++) {
        int[] s0101 =
            {0, 0, 31, 17, 31, 0, 0, 18, 31, 16, 0, 0, 10, 0, 0, 31, 17, 31, 0, 0, 18, 31, 16, 0, 0};
        System.out.print(a.atavise(new Grid(s0101, 5)).size());
        System.out.println(" priors!");
      }
    //}

    long endTime = System.currentTimeMillis();
    System.out.println(endTime-startTime);
  }
}
