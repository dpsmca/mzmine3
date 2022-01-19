/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.filter_diams2;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.impl.SimpleMergedMsMsSpectrum;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.FinishedTask;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiaMs2CorrTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiaMs2CorrTask.class.getName());

  private final ModularFeatureList flist;
  private final ScanSelection ms2ScanSelection;
  private final double minMs1Intensity;
  private final double minMs2Intensity;
  private final int minCorrPoints;
  private final MZTolerance mzTolerance;
  private final double minPearson;

  private final ParameterSet parameters;
  private final ParameterSet adapParameters;
  //  private final ParameterSet smoothingParameters;
//  private final ParameterSet resolverParameters;
  private final int numRows;
  private final int numSubTasks = 2;
  private AbstractTask adapTask = null;
  private int currentTaksIndex = 1;
  private int currentRow = 0;

  private String description = "";

  protected DiaMs2CorrTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ModularFeatureList flist, ParameterSet parameters) {
    super(storage, moduleCallDate);

    this.flist = flist;
    this.parameters = parameters;
    ms2ScanSelection = parameters.getValue(DiaMs2CorrParameters.ms2ScanSelection);
    minMs1Intensity = parameters.getValue(DiaMs2CorrParameters.minMs1Intensity);
    minMs2Intensity = parameters.getValue(DiaMs2CorrParameters.minMs2Intensity);
    minCorrPoints = parameters.getValue(DiaMs2CorrParameters.numCorrPoints);
    mzTolerance = parameters.getValue(DiaMs2CorrParameters.ms2ScanToScanAccuracy);
    minPearson = parameters.getValue(DiaMs2CorrParameters.minPearson);
    numRows = flist.getNumberOfRows();

    adapParameters = MZmineCore.getConfiguration()
        .getModuleParameters(ModularADAPChromatogramBuilderModule.class).cloneParameterSet();
    final RawDataFilesSelection adapFiles = new RawDataFilesSelection(
        RawDataFilesSelectionType.SPECIFIC_FILES);
    adapFiles.setSpecificFiles(flist.getRawDataFiles().toArray(new RawDataFile[0]));
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.dataFiles, adapFiles);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.scanSelection, ms2ScanSelection);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, minCorrPoints);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolerance);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.suffix, "chroms");
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity,
        minMs2Intensity / 5);
    adapParameters.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minMs2Intensity);
  }

  @Override
  public String getTaskDescription() {
    return "DIA MS2 for feature list: " + flist.getName() + " " + (
        adapTask != null && !adapTask.isFinished() ? adapTask.getTaskDescription() : description);
  }

  @Override
  public double getFinishedPercentage() {
    return (adapTask != null ? adapTask.getFinishedPercentage() * 0.5 : 0)
        + (currentRow / (double) numRows) * 0.5d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (flist.getNumberOfRawDataFiles() != 1) {
      setErrorMessage("Cannot build DIA MS2 for feature lists with more than one raw data file.");
      setStatus(TaskStatus.ERROR);
    }

    final RawDataFile file = flist.getRawDataFile(0);
    final List<Scan> ms2Scans = List.of(ms2ScanSelection.getMatchingScans(file));
    final ScanDataAccess access = EfficientDataAccess.of(file, ScanDataType.CENTROID,
        ms2ScanSelection);

    // build chromatograms
    final MZmineProject dummyProject = new MZmineProjectImpl();
    var ms2Flist = runADAP(dummyProject, file);

    // store feature data in TreeRangeMap, to query by m/z in ms2 spectra
    final RangeMap<Double, IonTimeSeries<?>> ms2Eics = TreeRangeMap.create();
    ms2Flist.getRows().stream().map(row -> row.getFeature(file)).filter(Objects::nonNull).forEach(
        feature -> ms2Eics.put(SpectraMerging.createNewNonOverlappingRange(ms2Eics,
            mzTolerance.getToleranceRange(feature.getMZ())), feature.getFeatureData()));
    assert ms2Flist.getNumberOfRows() == ms2Eics.asMapOfRanges().size();

    // go through all features and find ms2s
    for (FeatureListRow row : flist.getRows()) {
      currentRow++;
      description = "Processing row " + currentRow + "/" + numRows;
      if (isCanceled()) {
        return;
      }

      final Feature feature = row.getFeature(file);
      if (feature == null || feature.getFeatureStatus() != FeatureStatus.DETECTED
          || feature.getHeight() < minMs1Intensity) {
        continue;
      }

      MergedMassSpectrum mergedMobilityScan = null; // for IMS
      final IonTimeSeries<? extends Scan> featureEIC = feature.getFeatureData();
      final double[][] shape = extractPointsAroundMaximum(minCorrPoints, featureEIC,
          feature.getRepresentativeScan());
      if (shape == null) {
        continue;
      }
      final double[] ms1Rts = shape[0];
      final double[] ms1Intensities = shape[1];

      // fwhm sometimes does funny stuff, so we restrict it to the overlap of fwhm + rt range
      final Range<Float> rtRange = Range.closed((float) ms1Rts[0], (float) ArrayUtils.back(ms1Rts));
      final List<Scan> ms2sInRtRange = ms2Scans.stream()
          .filter(scan -> rtRange.contains(scan.getRetentionTime())).toList();
      final Scan closestMs2 = getClosestMs2(feature.getRT(), ms2sInRtRange);
      if (closestMs2 == null || ms2sInRtRange.isEmpty() || ms2sInRtRange.size() < minCorrPoints) {
        logger.fine(() -> "Could not find enough ms2s in rtRange " + rtRange);
        continue;
      }

      // find m/zs in the closest ms2 scan and get their EICs
      if (!access.jumpToScan(closestMs2)) {
        continue;
      }

      final List<IonTimeSeries<?>> eligibleEICs = new ArrayList<>();
      for (int i = 0; i < access.getNumberOfDataPoints(); i++) {
        if (minMs2Intensity > access.getIntensityValue(i)) {
          continue;
        }

        final double mz = access.getMzValue(i);
        final IonTimeSeries<?> series = ms2Eics.get(mz);
        if (series != null) {
          eligibleEICs.add(series);
        }
      }

      if (eligibleEICs.isEmpty()) {
        continue;
      }

      // for ims data, later check if we can find the mz in the closest ms2 frame with the same mobility
      final MobilityScan bestMobilityScan = IonMobilityUtils.getBestMobilityScan(feature);
      if (bestMobilityScan != null && closestMs2 instanceof Frame) {
        final Range<Float> mobilityRange = IonMobilityUtils.getMobilityFWHM(
            ((IonMobilogramTimeSeries) featureEIC).getSummedMobilogram());
        final List<MobilityScan> mobilityScans = ms2Scans.stream()
            .filter(s -> rtRange.contains(s.getRetentionTime()))
            .flatMap(s -> ((Frame) s).getMobilityScans().stream())
            .filter(m -> mobilityRange.contains((float) m.getMobility())).toList();
        if (!mobilityScans.isEmpty()) {
          mergedMobilityScan = SpectraMerging.mergeSpectra(mobilityScans, mzTolerance, null);
        } else {
          continue; // if we have ims data, and there are no mobility scans to be merged, something is fishy.
        }
      }

      DoubleArrayList ms2Mzs = new DoubleArrayList();
      DoubleArrayList ms2Intensities = new DoubleArrayList();
      for (IonTimeSeries<?> eic : eligibleEICs) {
        final int num = eic.getNumberOfValues();
        final double[] intensities = new double[num];
        final double[] rts = new double[num];
        for (int i = 0; i < num; i++) {
          intensities[i] = eic.getIntensity(i);
          rts[i] = eic.getRetentionTime(i);
        }

        final CorrelationData correlationData = DIA.corrFeatureShape(ms1Rts, ms1Intensities, rts,
            intensities, minCorrPoints, 2, minMs2Intensity / 3);
        if (correlationData != null && correlationData.isValid()
            && Math.pow(correlationData.getPearsonR(), 2) > minPearson) {
          int startIndex = -1;
          int endIndex = -1;
          double maxIntensity = Double.NEGATIVE_INFINITY;

          final List<Scan> spectra = (List<Scan>) eic.getSpectra();
          for (int j = 0; j < spectra.size(); j++) {
            Scan spectrum = spectra.get(j);
            if (startIndex == -1 && rtRange.contains(spectrum.getRetentionTime())) {
              startIndex = j;
            }
            if (startIndex != -1 && eic.getIntensity(j) > maxIntensity) {
              maxIntensity = eic.getIntensity(j);
            }
            if (startIndex != -1 && !rtRange.contains(spectrum.getRetentionTime())) {
              endIndex = j - 1;
              break;
            }
          }
          // no value in ms1 feature rt range
          if (startIndex == -1) {
            continue;
          }
          // all values in ms1 feature rt range
          if (endIndex == -1) {
            endIndex = eic.getNumberOfValues() - 1;
          }

          final double mz = FeatureDataUtils.calculateMz(eic,
              FeatureDataUtils.DEFAULT_CENTER_FUNCTION, startIndex, endIndex);

          // for IMS measurements, the ion must be present in the MS2 mobility scans in the during
          // the feature's rt window and within the mobility scans of the feature's mobility window.
          // we could also look at mobility shape and correlate that, but it would probably take a
          // lot of optimisation and/or too long to compute
          if (mergedMobilityScan != null && mergedMobilityScan.getNumberOfDataPoints() > 1) {
            boolean mzFound = false;
            final double upper = mzTolerance.getToleranceRange(mz).upperEndpoint();
            for (int i = 0; i < mergedMobilityScan.getNumberOfDataPoints(); i++) {
              if (mzTolerance.checkWithinTolerance(mz, mergedMobilityScan.getMzValue(i))) {
                mzFound = true;
                break;
              } else if (mergedMobilityScan.getMzValue(i) > upper) {
                break;
              }
            }
            if (!mzFound) {
              continue; // dont add this mz
            }
          }
          ms2Mzs.add(mz);
          ms2Intensities.add(maxIntensity);
        }
      }

      if (ms2Mzs.isEmpty()) {
        continue;
      }

      MergedMsMsSpectrum ms2 = new SimpleMergedMsMsSpectrum(getMemoryMapStorage(),
          ms2Mzs.toDoubleArray(), ms2Intensities.toDoubleArray(), closestMs2.getMsMsInfo(),
          closestMs2.getMSLevel(),
          mergedMobilityScan != null ? mergedMobilityScan.getSourceSpectra() : ms2sInRtRange,
          MergingType.MAXIMUM, FeatureDataUtils.DEFAULT_CENTER_FUNCTION);
      feature.setFragmentScan(ms2);
      feature.setAllMS2FragmentScans(new ArrayList<>(List.of(ms2)));
    }

    flist.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(DiaMs2CorrModule.class, parameters,
            getModuleCallDate()));
    setStatus(TaskStatus.FINISHED);
  }

  private Scan getClosestMs2(float rt, List<Scan> ms2sInRtRange) {
    Scan closestMs2 = null;
    float diff = Float.POSITIVE_INFINITY;
    for (Scan s : ms2sInRtRange) {
      if (Math.abs(s.getRetentionTime() - rt) < diff) {
        closestMs2 = s;
      } else {
        break;
      }
    }
    return closestMs2;
  }

  private ModularFeatureList runADAP(MZmineProject dummyProject, RawDataFile file) {
    adapTask = new ModularADAPChromatogramBuilderTask(dummyProject, file, adapParameters,
        getMemoryMapStorage(), getModuleCallDate());
    adapTask.run();
    adapTask = new FinishedTask(adapTask);
    currentTaksIndex++;

    var ms2Flist = dummyProject.getCurrentFeatureLists().get(0);
    if (dummyProject.getCurrentFeatureLists().isEmpty()) {
      logger.warning("Cannot find ms2 feature list.");
      return null;
    }
//    final FeatureListAppliedMethod removed = file.getAppliedMethods()
//        .remove(file.getAppliedMethods().size() - 1);
//    assert removed.getModule().getClass().equals(ModularADAPChromatogramBuilderModule.class);
    return (ModularFeatureList) ms2Flist;
  }

  /**
   * Extracts a given number of data points around a maximum. The number of detected points is
   * automatically limited to the bounds of the chromatogram.
   *
   * @param numPoints    the number of points to extract.
   * @param chromatogram the chromatogram to extract the points from.
   * @param maximumScan  The maximum scan in the chromatogram.
   * @return a 2d array [0][] = rts, [1][] = intensities.
   */
  @Nullable
  private double[][] extractPointsAroundMaximum(final int numPoints,
      final IonTimeSeries<? extends Scan> chromatogram, @Nullable final Scan maximumScan) {
    if (maximumScan == null) {
      return null;
    }

    final List<? extends Scan> spectra = chromatogram.getSpectra();
    final int index = Math.abs(Collections.binarySearch(spectra, maximumScan));
    final int index2 = spectra.indexOf(maximumScan);

    // take one point more each, because MS1 and MS2 are acquired in alternating fashion, so we
    // need one more ms1 point on each side for the rt range, so we can fit the determined number
    // of ms2 points
    final int lower = Math.max(index - numPoints / 2 - 1, 0);
    final int upper = Math.min(index + numPoints / 2 + 1, chromatogram.getNumberOfValues() - 1);

    final double[] rts = new double[upper - lower];
    final double[] intensities = new double[upper - lower];
    for (int i = lower; i < upper; i++) {
      rts[i - lower] = chromatogram.getRetentionTime(i);
      intensities[i - lower] = chromatogram.getIntensity(i);
    }

    return new double[][]{rts, intensities};
  }

  @Override
  public void cancel() {
    super.cancel();
    if (adapTask != null) {
      adapTask.cancel();
    }
  }

  /*
  private ModularFeatureList runSmoothing(MZmineProject dummyProject, ModularFeatureList ms2Flist) {
    if (smoothingParameters != null) {
      final MZmineProcessingStep<SmoothingAlgorithm> smoother = smoothingParameters.getParameter(
          SmoothingParameters.smoothingAlgorithm).getValue();
      ParameterSet fullSmoothingParams = MZmineCore.getConfiguration()
          .getModuleParameters(SmoothingModule.class).cloneParameterSet();
      fullSmoothingParams.setParameter(SmoothingParameters.smoothingAlgorithm, smoother);
      fullSmoothingParams.setParameter(SmoothingParameters.suffix, "_sm");
      fullSmoothingParams.setParameter(SmoothingParameters.handleOriginal,
          OriginalFeatureListOption.REMOVE);

      currentTask = new SmoothingTask(dummyProject, (ModularFeatureList) ms2Flist,
          flist.getMemoryMapStorage(), fullSmoothingParams, getModuleCallDate());
      currentTask.run();
    }
    currentTask = null;
    currentTaksIndex++;
    return (ModularFeatureList) dummyProject.getCurrentFeatureLists().get(0);
  }

  private ModularFeatureList runResolving(MZmineProject dummyProject, ModularFeatureList ms2Flist) {
    ParameterSet fullResolverParameters = MZmineCore.getConfiguration()
        .getModuleParameters(MinimumSearchFeatureResolverModule.class);
    fullResolverParameters.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL,
        resolverParameters.getValue(
            MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_RATIO));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT));
    fullResolverParameters.setParameter(
        MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS,
        resolverParameters.getValue(
            MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        resolverParameters.getValue(MinimumSearchFeatureResolverParameters.PEAK_DURATION));
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, "_res");
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        OriginalFeatureListOption.REMOVE);
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    fullResolverParameters.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters,
        false);

    currentTask = new FeatureResolverTask(dummyProject, getMemoryMapStorage(), ms2Flist,
        fullResolverParameters, FeatureDataUtils.DEFAULT_CENTER_FUNCTION, getModuleCallDate());
    currentTask.run();
    currentTask = null;
    currentTaksIndex++;

    return (ModularFeatureList) dummyProject.getCurrentFeatureLists().get(0);
  }*/
}
