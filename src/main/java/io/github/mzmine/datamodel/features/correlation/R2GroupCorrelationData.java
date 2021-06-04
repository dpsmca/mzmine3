/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * correlation of one row to a group
 *
 * @author RibRob
 */
public class R2GroupCorrelationData {

  private final FeatureListRow row;
  // row index is xRow in corr data
  private List<R2RCorrelationData> corr;
  private final double maxHeight;
  // averages are calculated by dividing by the row count
  private double minHeightR, avgHeightR, maxHeightR;
  private double minShapeR, avgShapeR, maxShapeR, avgDPCount;

  // average cosine
  private double avgShapeCosineSim;
  private double avgCosineHeightCorr;

  // total Feature shape r
  private double avgTotalFeatureShapeR;

  public R2GroupCorrelationData(FeatureListRow row, List<R2RCorrelationData> corr,
      double maxHeight) {
    super();
    this.row = row;
    setCorr(corr);
    this.maxHeight = maxHeight;
  }

  /**
   * @param FeatureList
   * @return
   */
  public static Stream<R2GroupCorrelationData> streamFrom(FeatureList FeatureList) {
    if (FeatureList.getGroups() == null) {
      return Stream.empty();
    }
    return FeatureList.getGroups().stream().filter(g -> g instanceof CorrelationRowGroup)
        .map(g -> ((CorrelationRowGroup) g).getCorr()).flatMap(Arrays::stream);

  }


  public void setCorr(List<R2RCorrelationData> corr) {
    this.corr = corr;
    recalcCorr();
  }

  /**
   * Recalc correlation
   */
  public void recalcCorr() {
    // min max
    minHeightR = 1;
    maxHeightR = -1;
    avgHeightR = 0;
    minShapeR = 1;
    maxShapeR = -1;
    avgShapeR = 0;
    avgShapeCosineSim = 0;
    avgDPCount = 0;
    avgTotalFeatureShapeR = 0;
    avgCosineHeightCorr = 0;
    int cImax = 0;
    int cFeatureShape = 0;

    for (R2RCorrelationData r2r : corr) {
      if (r2r.hasHeightCorr()) {
        cImax++;
        avgCosineHeightCorr += r2r.getCosineHeightCorr();
        double iProfileR = r2r.getHeightCorrR();
        avgHeightR += iProfileR;
        if (iProfileR < minHeightR) {
          minHeightR = iProfileR;
        }
        if (iProfileR > maxHeightR) {
          maxHeightR = iProfileR;
        }
      }

      // Feature shape correlation
      if (r2r.hasFeatureShapeCorrelation()) {
        cFeatureShape++;
        avgTotalFeatureShapeR += r2r.getTotalR();
        avgShapeR += r2r.getAvgShapeR();
        avgShapeCosineSim += r2r.getAvgShapeCosineSim();
        avgDPCount += r2r.getAvgDPcount();
        if (r2r.getMinShapeR() < minShapeR) {
          minShapeR = r2r.getMinShapeR();
        }
        if (r2r.getMaxShapeR() > maxShapeR) {
          maxShapeR = r2r.getMaxShapeR();
        }
      }
    }
    avgTotalFeatureShapeR = avgTotalFeatureShapeR / cFeatureShape;
    avgHeightR = avgHeightR / cImax;
    avgCosineHeightCorr = avgCosineHeightCorr / cImax;
    avgDPCount = avgDPCount / cFeatureShape;
    avgShapeR = avgShapeR / cFeatureShape;
    avgShapeCosineSim = avgShapeCosineSim / cFeatureShape;
  }


  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  public double getAvgHeightSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getHeightSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  public double getAvgTotalSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getTotalSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   *
   * @param type
   * @return
   */
  public double getAvgFeatureShapeSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RCorrelationData r2r : corr) {
      double v = r2r.getAvgFeatureShapeSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  public double getAvgShapeCosineSim() {
    return avgShapeCosineSim;
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  public List<R2RCorrelationData> getCorr() {
    return corr;
  }

  public double getMinIProfileR() {
    return minHeightR;
  }

  public double getAvgIProfileR() {
    return avgHeightR;
  }

  public double getMaxIProfileR() {
    return maxHeightR;
  }

  public double getMinFeatureShapeR() {
    return minShapeR;
  }

  public double getAvgFeatureShapeR() {
    return avgShapeR;
  }

  public double getMaxFeatureShapeR() {
    return maxShapeR;
  }

  public double getAvgDPCount() {
    return avgDPCount;
  }

  public double getAvgTotalFeatureShapeR() {
    return avgTotalFeatureShapeR;
  }

  /**
   * Height correlation across samples
   *
   * @return
   */
  public double getAvgCosineHeightCorr() {
    return avgCosineHeightCorr;
  }


  public FeatureListRow getRow() {
    return row;
  }

  /**
   * @return the correlation data of this row to row[rowI]
   * @throws Exception
   */
  public R2RCorrelationData getCorrelationToRow(FeatureListRow row) {
    if (row != row) {
      return null;
    }
    for (R2RCorrelationData c : corr) {
      if (c.getRowA() == row || c.getRowB() == row) {
        return c;
      }
    }
    return null;
  }

}
