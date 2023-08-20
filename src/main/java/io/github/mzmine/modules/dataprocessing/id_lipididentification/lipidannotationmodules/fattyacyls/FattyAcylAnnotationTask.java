/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.fattyacyls;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.FattyAcylFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation.ILipidFragmentFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.IMolecularSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.ISpeciesLevelMatchedLipidFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.customlipidclass.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidAnnotationResolver;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Task to search and annotate lipids in feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class FattyAcylAnnotationTask extends AbstractTask {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedSteps;
  private double totalSteps;
  private final FeatureList featureList;
  private final LipidClasses[] selectedLipids;
  private CustomLipidClass[] customLipidClasses;
  private final int minChainLength;
  private final int maxChainLength;
  private final int maxDoubleBonds;
  private final int minDoubleBonds;
  private final Boolean onlySearchForEvenChains;
  private final MZTolerance mzTolerance;
  private MZTolerance mzToleranceMS2;
  private final Boolean searchForMSMSFragments;
  private final Boolean keepUnconfirmedAnnotations;
  private double minMsMsScore;

  private final ParameterSet parameters;

  public FattyAcylAnnotationTask(ParameterSet parameters, FeatureList featureList,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;

    this.minChainLength = parameters.getParameter(
            FattyAcylAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationChainParameters.minChainLength).getValue();
    this.maxChainLength = parameters.getParameter(
            FattyAcylAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationChainParameters.maxChainLength).getValue();
    this.minDoubleBonds = parameters.getParameter(
            FattyAcylAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationChainParameters.minDBEs).getValue();
    this.maxDoubleBonds = parameters.getParameter(
            FattyAcylAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationChainParameters.maxDBEs).getValue();
    this.onlySearchForEvenChains = parameters.getParameter(
            FattyAcylAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationChainParameters.onlySearchForEvenChainLength).getValue();
    this.mzToleranceMS2 = parameters.getParameter(
            FattyAcylAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
        .getParameter(FattyAcylAnnotationMSMSParameters.mzToleranceMS2).getValue();
    this.mzTolerance = parameters.getParameter(FattyAcylAnnotationParameters.mzTolerance)
        .getValue();
    Object[] selectedObjects = parameters.getParameter(FattyAcylAnnotationParameters.lipidClasses)
        .getValue();
    this.searchForMSMSFragments = parameters.getParameter(
        FattyAcylAnnotationParameters.searchForMSMSFragments).getValue();
    if (searchForMSMSFragments.booleanValue()) {
      this.mzToleranceMS2 = parameters.getParameter(
              FattyAcylAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(FattyAcylAnnotationMSMSParameters.mzToleranceMS2).getValue();
      this.keepUnconfirmedAnnotations = parameters.getParameter(
              FattyAcylAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(FattyAcylAnnotationMSMSParameters.keepUnconfirmedAnnotations).getValue();
      this.minMsMsScore = parameters.getParameter(
              FattyAcylAnnotationParameters.searchForMSMSFragments).getEmbeddedParameters()
          .getParameter(FattyAcylAnnotationMSMSParameters.minimumMsMsScore).getValue();
    } else {
      this.keepUnconfirmedAnnotations = true;
    }
    Boolean searchForCustomLipidClasses = parameters.getParameter(
        FattyAcylAnnotationParameters.customLipidClasses).getValue();
    if (searchForCustomLipidClasses.booleanValue()) {
      this.customLipidClasses = FattyAcylAnnotationParameters.customLipidClasses.getEmbeddedParameter()
          .getChoices();
    }
    // Convert Objects to LipidClasses
    this.selectedLipids = Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
        .map(o -> (LipidClasses) o).toArray(LipidClasses[]::new);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0) {
      return 0;
    }
    return (finishedSteps) / totalSteps;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Find Fatty acyls in " + featureList;
  }

  /**
   * @see Runnable#run()
   */
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting Fatty acyls annotation in " + featureList);

    List<FeatureListRow> rows = featureList.getRows();
    if (featureList instanceof ModularFeatureList) {
      featureList.addRowType(new LipidMatchListType());
    }
    totalSteps = rows.size();

    // build lipid species database
    Set<ILipidAnnotation> lipidDatabase = buildLipidDatabase();

    // start lipid annotation
    rows.parallelStream().forEach(row -> {
      for (ILipidAnnotation lipidAnnotation : lipidDatabase) {
        findPossibleLipid(lipidAnnotation, row);
      }
      finishedSteps++;
    });

    // Add task description to featureList
    (featureList).addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Fatty acyl annotation", FattyAcylAnnotationModule.class,
            parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished Fatty acyl annotation task for " + featureList);
  }

  private Set<ILipidAnnotation> buildLipidDatabase() {

    Set<ILipidAnnotation> lipidDatabase = new LinkedHashSet<>();

    // add selected lipids
    buildLipidCombinations(lipidDatabase, selectedLipids);

    // add custom lipids
    if (customLipidClasses != null && customLipidClasses.length > 0) {
      buildLipidCombinations(lipidDatabase, customLipidClasses);
    }

    return lipidDatabase;
  }

  private void buildLipidCombinations(Set<ILipidAnnotation> lipidDatabase,
      ILipidClass[] lipidClasses) {
    // Try all combinations of fatty acid lengths and double bonds
    for (ILipidClass lipidClass : lipidClasses) {

      // TODO starting point to extend for better oxidized lipid support
      int numberOfAdditionalOxygens = 0;
      int minTotalChainLength = minChainLength * lipidClass.getChainTypes().length;
      int maxTotalChainLength = maxChainLength * lipidClass.getChainTypes().length;
      int minTotalDoubleBonds = minDoubleBonds * lipidClass.getChainTypes().length;
      int maxTotalDoubleBonds = maxDoubleBonds * lipidClass.getChainTypes().length;
      for (int chainLength = minTotalChainLength; chainLength <= maxTotalChainLength;
          chainLength++) {
        if (onlySearchForEvenChains && chainLength % 2 != 0) {
          continue;
        }
        for (int chainDoubleBonds = minTotalDoubleBonds; chainDoubleBonds <= maxTotalDoubleBonds;
            chainDoubleBonds++) {

          if (chainLength / 2 < chainDoubleBonds || chainLength == 0) {
            continue;
          }

          // Prepare a lipid instance
          ILipidAnnotation lipid = LIPID_FACTORY.buildSpeciesLevelLipid(lipidClass, chainLength,
              chainDoubleBonds, numberOfAdditionalOxygens);
          if (lipid != null) {
            lipidDatabase.add(lipid);
          }
        }
      }
    }
  }

  /**
   * Check if candidate peak may be a possible adduct of a given main peak
   */
  private void findPossibleLipid(ILipidAnnotation lipid, FeatureListRow row) {
    if (isCanceled()) {
      return;
    }
    Set<MatchedLipid> possibleRowAnnotations = new HashSet<>();
    Set<IonizationType> ionizationTypeList = new HashSet<>();
    LipidFragmentationRule[] fragmentationRules = lipid.getLipidClass().getFragmentationRules();
    for (LipidFragmentationRule fragmentationRule : fragmentationRules) {
      ionizationTypeList.add(fragmentationRule.getIonizationType());
    }
    for (IonizationType ionization : ionizationTypeList) {
      if (!Objects.requireNonNull(row.getBestFeature().getRepresentativeScan()).getPolarity()
          .equals(ionization.getPolarity())) {
        continue;
      }
      double lipidIonMass = MolecularFormulaManipulator.getMass(lipid.getMolecularFormula(),
          AtomContainerManipulator.MonoIsotopic) + ionization.getAddedMass();
      Range<Double> mzTolRange12C = mzTolerance.getToleranceRange(row.getAverageMZ());

      // MS1 check
      if (mzTolRange12C.contains(lipidIonMass)) {

        // If search for MSMS fragments is selected search for fragments
        if (searchForMSMSFragments.booleanValue()) {
          possibleRowAnnotations.addAll(searchMsmsFragments(row, ionization, lipid));
        } else {

          MatchedLipid matchedLipid = new MatchedLipid(lipid, row.getAverageMZ(), ionization,
              new HashSet<LipidFragment>(), 0.0);
          matchedLipid.setComment("Warning, this annotation is based on MS1 mass accuracy only!");
          // make MS1 annotation
          possibleRowAnnotations.add(matchedLipid);
        }
      }

    }
    if (!possibleRowAnnotations.isEmpty()) {
      addAnnotationsToFeatureList(row, possibleRowAnnotations);
    }
  }

  private void addAnnotationsToFeatureList(FeatureListRow row,
      Set<MatchedLipid> possibleRowAnnotations) {
    //consider previous annotations
    List<MatchedLipid> previousLipidMatches = row.getLipidMatches();
    if (!previousLipidMatches.isEmpty()) {
      row.set(LipidMatchListType.class, null);
      possibleRowAnnotations.addAll(previousLipidMatches);
    }
    LipidAnnotationResolver lipidAnnotationResolver = new LipidAnnotationResolver(true, true, true,
        mzToleranceMS2, minMsMsScore, keepUnconfirmedAnnotations, searchForMSMSFragments);
    List<MatchedLipid> finalResults = lipidAnnotationResolver.resolveFeatureListRowMatchedLipids(
        row, possibleRowAnnotations);
    for (MatchedLipid matchedLipid : finalResults) {
      if (matchedLipid != null) {
        row.addLipidAnnotation(matchedLipid);
      }
    }
  }

  /**
   * This method searches for MS/MS fragments. A mass list for MS2 scans will be used if present.
   */
  private Set<MatchedLipid> searchMsmsFragments(FeatureListRow row, IonizationType ionization,
      ILipidAnnotation lipid) {

    Set<MatchedLipid> matchedLipids = new HashSet<>();
    // Check if selected feature has MSMS spectra and LipidIdentity

    if (!row.getAllFragmentScans().isEmpty()) {
      List<Scan> msmsScans = row.getAllFragmentScans();
      for (Scan msmsScan : msmsScans) {
        Set<MatchedLipid> matchedLipidsInScan = new HashSet<>();
        if (msmsScan.getMassList() == null) {
          setErrorMessage("Mass List cannot be found.\nCheck if MS2 Scans have a Mass List");
          setStatus(TaskStatus.ERROR);
          return new HashSet<>();
        }
        DataPoint[] massList = null;
        massList = msmsScan.getMassList().getDataPoints();
        massList = MSMSLipidTools.deisotopeMassList(massList, mzToleranceMS2);
        LipidFragmentationRule[] rules = lipid.getLipidClass().getFragmentationRules();
        Set<LipidFragment> annotatedFragments = new HashSet<>();
        if (rules != null && rules.length > 0) {
          for (DataPoint dataPoint : massList) {
            Range<Double> mzTolRangeMSMS = mzToleranceMS2.getToleranceRange(dataPoint.getMZ());
            ILipidFragmentFactory glyceroAndGlyceroPhospholipidFragmentFactory = new FattyAcylFragmentFactory(
                mzTolRangeMSMS, lipid, ionization, rules,
                new SimpleDataPoint(dataPoint.getMZ(), dataPoint.getIntensity()), msmsScan,
                parameters.getParameter(FattyAcylAnnotationParameters.lipidChainParameters)
                    .getEmbeddedParameters());
            List<LipidFragment> annotatedFragmentsForDataPoint = glyceroAndGlyceroPhospholipidFragmentFactory.findLipidFragments();
            if (annotatedFragmentsForDataPoint != null
                && !annotatedFragmentsForDataPoint.isEmpty()) {
              annotatedFragments.addAll(annotatedFragmentsForDataPoint);
            }
          }
        }
        if (!annotatedFragments.isEmpty()) {
          ISpeciesLevelMatchedLipidFactory matchedLipidFactory = new GlyceroAndGlycerophosphoSpeciesLevelMatchedLipidFactory();
          MatchedLipid matchedSpeciesLevelLipid = matchedLipidFactory.validateSpeciesLevelAnnotation(
              row.getAverageMZ(), lipid, annotatedFragments, massList, minMsMsScore, mzToleranceMS2,
              ionization);
          matchedLipidsInScan.add(matchedSpeciesLevelLipid);

          IMolecularSpeciesLevelMatchedLipidFactory matchedMolecularSpeciesLipidFactory = new GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory();
          Set<MatchedLipid> molecularSpeciesLevelMatchedLipids = matchedMolecularSpeciesLipidFactory.predictMolecularSpeciesLevelMatches(
              annotatedFragments, lipid, row.getAverageMZ(), massList, minMsMsScore, mzToleranceMS2,
              ionization);
          if (molecularSpeciesLevelMatchedLipids != null
              && !molecularSpeciesLevelMatchedLipids.isEmpty()) {
            //Add species level fragments
            if (matchedSpeciesLevelLipid != null) {
              for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
                molecularSpeciesLevelMatchedLipid.getMatchedFragments()
                    .addAll(matchedSpeciesLevelLipid.getMatchedFragments());
              }
            }
            for (MatchedLipid molecularSpeciesLevelMatchedLipid : molecularSpeciesLevelMatchedLipids) {
              //check MSMS score
              molecularSpeciesLevelMatchedLipid = matchedMolecularSpeciesLipidFactory.validateMolecularSpeciesLevelAnnotation(
                  row.getAverageMZ(), molecularSpeciesLevelMatchedLipid.getLipidAnnotation(),
                  molecularSpeciesLevelMatchedLipid.getMatchedFragments(), massList, minMsMsScore,
                  mzToleranceMS2, ionization);
              if (molecularSpeciesLevelMatchedLipid != null) {
                matchedLipidsInScan.add(molecularSpeciesLevelMatchedLipid);
              }
            }
          }
        }
        matchedLipids.addAll(matchedLipidsInScan);
      }
      if (keepUnconfirmedAnnotations.booleanValue() && matchedLipids.isEmpty()) {
        MatchedLipid unconfirmedMatchedLipid = new MatchedLipid(lipid, row.getAverageMZ(),
            ionization, new HashSet<LipidFragment>(), 0.0);
        unconfirmedMatchedLipid.setComment(
            "Warning, this annotation is based on MS1 mass accuracy only!");
        matchedLipids.add(unconfirmedMatchedLipid);
      }
      if (!matchedLipids.isEmpty() && matchedLipids.size() > 1) {
        onlyKeepBestAnnotations(matchedLipids);
      }
    }

    return matchedLipids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private void onlyKeepBestAnnotations(Set<MatchedLipid> matchedLipids) {
    Map<String, List<MatchedLipid>> matchedLipidsByAnnotation = matchedLipids.stream()
        .filter(Objects::nonNull).collect(Collectors.groupingBy(
            matchedLipid -> matchedLipid.getLipidAnnotation().getAnnotation()));

    List<List<MatchedLipid>> duplicateMatchedLipids = matchedLipidsByAnnotation.values().stream()
        .filter(group -> group.size() > 1).toList();

    for (List<MatchedLipid> matchedLipidGroup : duplicateMatchedLipids) {
      if (matchedLipidGroup.size() > 1) {
        MatchedLipid bestMatch = matchedLipidGroup.stream()
            .max(Comparator.comparingDouble(MatchedLipid::getMsMsScore)).orElse(null);
        matchedLipidGroup.remove(bestMatch);

        for (MatchedLipid matchedLipidToRemove : matchedLipidGroup) {
          Iterator<MatchedLipid> iterator = matchedLipids.iterator();
          while (iterator.hasNext()) {
            MatchedLipid item = iterator.next();
            if (item != null && item.equals(matchedLipidToRemove)) {
              iterator.remove();
            }
          }
        }
      }
    }
  }

}
