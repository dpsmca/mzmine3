/*
 * Copyright (c) 2004-2023 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.batchwizard.builders;

import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imagebuilder.ImageBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderModule;
import io.github.mzmine.modules.dataprocessing.featdet_imsexpander.ImsExpanderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.SelectedScanTypes;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.group_imagecorrelate.ImageCorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.group_imagecorrelate.ImageCorrelateGroupingParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceImagingWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowImagingWizardParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.maths.similarity.SimilarityMeasure;
import java.util.Optional;

public class WizardBatchBuilderImagingDda extends BaseWizardBatchBuilder {

  private final Integer minNumberOfPixels;
  private final Boolean enableDeisotoping;

  private final Boolean applyImageCorrelataion;
  private final Boolean applyMedianFilter;
  private final Boolean applyQuantileFilter;
  private final Boolean applyHotspotRemoval;

  public WizardBatchBuilderImagingDda(final WizardSequence steps) {
    // extract default parameters that are used for all workflows
    super(steps);

    Optional<? extends WizardStepParameters> params = steps.get(WizardPart.ION_INTERFACE);
    // special workflow parameter are extracted here
    minNumberOfPixels = getValue(params, IonInterfaceImagingWizardParameters.minNumberOfDataPoints);
    enableDeisotoping = getValue(params, IonInterfaceImagingWizardParameters.enableDeisotoping);

    // Imaging workflow parameters
    params = steps.get(WizardPart.WORKFLOW);
    applyImageCorrelataion = getValue(params, WorkflowImagingWizardParameters.CORRELATE_IMAGES);
    applyMedianFilter = true;
    applyQuantileFilter = true;
    applyHotspotRemoval = true;
  }

  @Override
  public BatchQueue createQueue() {
    final BatchQueue q = new BatchQueue();
    makeAndAddImportTask(q);
    makeAndAddMassDetectorSteps(q);
    makeAndImageBuilderStep(q);

    if (isImsActive) {
      makeAndAddImsExpanderStep(q);
      makeAndAddSmoothingStep(q, false, minImsDataPoints, imsSmoothing);
      makeAndAddMobilityResolvingStep(q, null);
      makeAndAddSmoothingStep(q, false, minImsDataPoints, imsSmoothing);
    }

    if (enableDeisotoping) {
      makeAndAddDeisotopingStep(q, null);
    }

    makeAndAddIsotopeFinderStep(q);

    makeAndAddAlignmentStep(q);
    makeAndAddRowFilterStep(q);
    if (applyImageCorrelataion) {
      makeAndAddImageCorrelationSteps(q, minNumberOfPixels, applyMedianFilter, applyQuantileFilter,
          applyHotspotRemoval);
    }

    // annotation
    makeAndAddLibrarySearchStep(q, false);
    return q;
  }

  private void makeAndAddImageCorrelationSteps(BatchQueue q, Integer minNumberOfPixels,
      Boolean applyMedianFilter, Boolean applyQuantileFilter, Boolean applyHotspotRemoval) {

    ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ImageCorrelateGroupingModule.class).cloneParameterSet();

    param.setParameter(ImageCorrelateGroupingParameters.FEATURE_LISTS,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.getParameter(ImageCorrelateGroupingParameters.NOISE_LEVEL).setValue(1E2);
    param.setParameter(ImageCorrelateGroupingParameters.MIN_NUMBER_OF_PIXELS, minNumberOfPixels);
    param.setParameter(ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW, applyMedianFilter);
    param.getParameter(ImageCorrelateGroupingParameters.MEDIAN_FILTER_WINDOW).getEmbeddedParameter()
        .setValue(3);
    param.getParameter(ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD)
        .setValue(applyQuantileFilter);
    param.getParameter(ImageCorrelateGroupingParameters.QUANTILE_THRESHOLD).getEmbeddedParameter()
        .setValue(0.5);
    param.getParameter(ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL)
        .setValue(applyHotspotRemoval);
    param.getParameter(ImageCorrelateGroupingParameters.HOTSPOT_REMOVAL).getEmbeddedParameter()
        .setValue(0.99);
    param.getParameter(ImageCorrelateGroupingParameters.MEASURE)
        .setValue(SimilarityMeasure.PEARSON);
    param.getParameter(ImageCorrelateGroupingParameters.MIN_R).setValue(0.85);

    q.add(new MZmineProcessingStepImpl<>(
        MZmineCore.getModuleInstance(ImageCorrelateGroupingModule.class), param));
  }

  @Override
  protected void makeAndAddImportTask(final BatchQueue q) {
    // todo make auto mass detector work, so we can use it here.

    if (isImsActive && imsInstrumentType == MobilityType.TIMS) {
      final ParameterSet param = MZmineCore.getConfiguration()
          .getModuleParameters(AllSpectralDataImportModule.class).cloneParameterSet();
      final AdvancedSpectraImportParameters advancedParam = (AdvancedSpectraImportParameters) new AdvancedSpectraImportParameters().cloneParameterSet();

      final CentroidMassDetector massDetector = MassDetectionParameters.centroid;
      final ParameterSet massDetectorParam = MZmineCore.getConfiguration()
          .getModuleParameters(CentroidMassDetector.class).cloneParameterSet();
      massDetectorParam.setParameter(CentroidMassDetectorParameters.noiseLevel,
          massDetectorOption.getMs1NoiseLevel());
      massDetectorParam.setParameter(CentroidMassDetectorParameters.detectIsotopes, false);
      MZmineProcessingStep<MassDetector> massDetectorStep = new MZmineProcessingStepImpl<>(
          massDetector, massDetectorParam);
      advancedParam.setParameter(AdvancedSpectraImportParameters.msMassDetection, true,
          massDetectorStep);

      param.getParameter(AllSpectralDataImportParameters.advancedImport).setValue(true);
      param.getParameter(AllSpectralDataImportParameters.advancedImport)
          .setEmbeddedParameters(advancedParam);
      param.getParameter(AllSpectralDataImportParameters.fileNames).setValue(dataFiles);
      param.getParameter(SpectralLibraryImportParameters.dataBaseFiles).setValue(libraries);

      q.add(new MZmineProcessingStepImpl<>(
          MZmineCore.getModuleInstance(AllSpectralDataImportModule.class), param));
    } else {
      super.makeAndAddImportTask(q);
    }
  }

  @Override
  protected void makeAndAddImsExpanderStep(final BatchQueue q) {
    ParameterSet param = MZmineCore.getConfiguration().getModuleParameters(ImsExpanderModule.class)
        .cloneParameterSet();

    param.setParameter(ImsExpanderParameters.handleOriginal, handleOriginalFeatureLists);
    param.setParameter(ImsExpanderParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(ImsExpanderParameters.useRawData, false);
    param.getParameter(ImsExpanderParameters.useRawData).getEmbeddedParameter()
        .setValue(massDetectorOption.getMs1NoiseLevel());
    param.setParameter(ImsExpanderParameters.mzTolerance, true);
    param.getParameter(ImsExpanderParameters.mzTolerance).getEmbeddedParameter()
        .setValue(mzTolScans);
    param.setParameter(ImsExpanderParameters.mobilogramBinWidth, false);
    param.setParameter(ImsExpanderParameters.maxNumTraces, true, 5);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImsExpanderModule.class),
        param));
  }


  protected void makeAndImageBuilderStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(ImageBuilderModule.class).cloneParameterSet();
    param.setParameter(ADAPChromatogramBuilderParameters.dataFiles,
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES));
    // crop rt range
    param.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(1));
    param.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, mzTolScans);
    param.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, minFeatureHeight);
    param.setParameter(ImageBuilderParameters.minimumConsecutiveScans, 5);
    param.setParameter(ImageBuilderParameters.minTotalSignals, minNumberOfPixels);
    param.setParameter(ImageBuilderParameters.suffix, "images");

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(ImageBuilderModule.class),
        param));
  }


  protected void makeAndAddAlignmentStep(final BatchQueue q) {
    final ParameterSet param = MZmineCore.getConfiguration()
        .getModuleParameters(JoinAlignerModule.class).cloneParameterSet();
    param.setParameter(JoinAlignerParameters.peakLists,
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
    param.setParameter(JoinAlignerParameters.peakListName, "Aligned feature list");
    param.setParameter(JoinAlignerParameters.MZTolerance, mzTolInterSample);
    param.setParameter(JoinAlignerParameters.MZWeight, 3d);
    param.setParameter(JoinAlignerParameters.RTTolerance, new RTTolerance(100000, Unit.MINUTES));
    param.setParameter(JoinAlignerParameters.RTWeight, 0d);
    param.setParameter(JoinAlignerParameters.mobilityTolerance, isImsActive);
    param.getParameter(JoinAlignerParameters.mobilityTolerance).getEmbeddedParameter().setValue(
        imsInstrumentType == MobilityType.TIMS ? new MobilityTolerance(0.01f)
            : new MobilityTolerance(1f));
    param.setParameter(JoinAlignerParameters.SameChargeRequired, false);
    param.setParameter(JoinAlignerParameters.SameIDRequired, false);
    param.setParameter(JoinAlignerParameters.compareIsotopePattern, false);
    param.setParameter(JoinAlignerParameters.compareSpectraSimilarity, false);
    param.setParameter(JoinAlignerParameters.handleOriginal, handleOriginalFeatureLists);

    q.add(new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(JoinAlignerModule.class),
        param));
  }

  protected void makeAndAddMassDetectorSteps(final BatchQueue q) {
    if (isImsActive && imsInstrumentType == MobilityType.TIMS) {
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.FRAMES);
      makeAndAddMassDetectionStep(q, 2, SelectedScanTypes.MOBLITY_SCANS);
    } else {
      makeAndAddMassDetectionStep(q, 1, SelectedScanTypes.SCANS);
    }
  }

}
