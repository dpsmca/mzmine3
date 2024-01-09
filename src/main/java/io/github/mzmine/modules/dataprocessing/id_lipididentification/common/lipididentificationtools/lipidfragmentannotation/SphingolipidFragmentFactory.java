/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidannotationmodules.LipidAnnotationChainParameters;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

;

public class SphingolipidFragmentFactory extends AbstractLipidFragmentFactory implements
    ILipidFragmentFactory {

  public SphingolipidFragmentFactory(MZTolerance mzToleranceMS2, ILipidAnnotation lipidAnnotation,
      IonizationType ionizationType, LipidFragmentationRule[] rules, Scan msMsScan,
      LipidAnnotationChainParameters chainParameters) {
    super(mzToleranceMS2, lipidAnnotation, ionizationType, rules, msMsScan, chainParameters);
  }

  @Override
  public List<LipidFragment> findLipidFragments() {
    List<LipidFragment> commonLipidFragments = findCommonLipidFragments();
    List<LipidFragment> lipidFragments = new ArrayList<>(commonLipidFragments);
    for (LipidFragmentationRule rule : rules) {
      if (!ionizationType.equals(rule.getIonizationType())
          || rule.getLipidFragmentationRuleType() == null) {
        continue;
      }
      List<LipidFragment> detectedFragments = checkForSphingolipidSpecificRuleTypes(rule);
      if (detectedFragments != null) {
        lipidFragments.addAll(detectedFragments);
      }
    }
    return lipidFragments;
  }

  private List<LipidFragment> checkForSphingolipidSpecificRuleTypes(LipidFragmentationRule rule) {
    LipidFragmentationRuleType ruleType = rule.getLipidFragmentationRuleType();
    switch (ruleType) {
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        checkForSphingolipidMonoHydroxyChainFragment(rule, lipidAnnotation, msMsScan);
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        return checkForSphingolipidDiHydroxyChainFragment(rule, lipidAnnotation, msMsScan);
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_FRAGMENT -> {
        return checkForSphingolipidTriHydroxyChainFragment(rule, lipidAnnotation, msMsScan);
      }
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidMonoHydroxyChainAndSubstructureNLFragment(rule, lipidAnnotation,
            msMsScan);
      }
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidDiHydroxyChainAndSubstructureNLFragment(rule, lipidAnnotation,
            msMsScan);
      }
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN_MINUS_FORMULA_FRAGMENT -> {
        return checkForSphingolipidTriHydroxyChainAndSubstructureNLFragment(rule, lipidAnnotation,
            msMsScan);
      }
      default -> {
        return List.of();
      }
    }

    return null;
  }

  private List<LipidFragment> checkForSphingolipidMonoHydroxyChainFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(),
            LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, msMsScan));
      }
    }
    return matchedFragments;
  }

  private List<LipidFragment> checkForSphingolipidDiHydroxyChainFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            msMsScan));
      }
    }
    return matchedFragments;
  }

  private List<LipidFragment> checkForSphingolipidTriHydroxyChainFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      rule.getIonizationType().ionizeFormula(sphingosineFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(sphingosineFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(sphingosineFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
            msMsScan));
      }
    }
    return matchedFragments;
  }

  private List<LipidFragment> checkForSphingolipidMonoHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      rule.getIonizationType().ionizeFormula(fragmentFormula);
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(),
            LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN, msMsScan));
      }
    }
    return matchedFragments;
  }


  private List<LipidFragment> checkForSphingolipidDiHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      ionizeFragmentBasedOnPolarity(fragmentFormula, rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
            msMsScan));
      }
    }
    return matchedFragments;
  }

  private List<LipidFragment> checkForSphingolipidTriHydroxyChainAndSubstructureNLFragment(
      LipidFragmentationRule rule, ILipidAnnotation lipidAnnotation, Scan msMsScan) {
    IMolecularFormula modificationFormula = FormulaUtils.createMajorIsotopeMolFormula(
        rule.getMolecularFormula());
    List<ILipidChain> sphingolipidBackboneChains = LIPID_CHAIN_FACTORY.buildLipidChainsInRange(
        LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN, minChainLength, maxChainLength,
        minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains);
    List<LipidFragment> matchedFragments = new ArrayList<>();
    MassList massList = msMsScan.getMassList();
    for (ILipidChain lipidChain : sphingolipidBackboneChains) {
      IMolecularFormula sphingosineFormula = lipidChain.getChainMolecularFormula();
      IMolecularFormula fragmentFormula = FormulaUtils.subtractFormula(sphingosineFormula,
          modificationFormula);
      ionizeFragmentBasedOnPolarity(fragmentFormula, rule.getPolarityType());
      Double mzExact = FormulaUtils.calculateMzRatio(fragmentFormula);
      Range<Double> toleranceRange = mzToleranceMS2.getToleranceRange(mzExact);
      int index = massList.binarySearch(toleranceRange.lowerEndpoint(), DefaultTo.GREATER_EQUALS);
      boolean fragmentMatched = false;
      BestDataPoint bestDataPoint = getBestDataPoint(mzExact, massList, index, fragmentMatched);
      if (bestDataPoint.fragmentMatched()) {
        int chainLength = lipidChain.getNumberOfCarbons();
        int numberOfDoubleBonds = lipidChain.getNumberOfDBEs();
        matchedFragments.add(new LipidFragment(rule.getLipidFragmentationRuleType(),
            rule.getLipidFragmentInformationLevelType(), rule.getLipidFragmentationRuleRating(),
            mzExact, MolecularFormulaManipulator.getString(fragmentFormula),
            new SimpleDataPoint(bestDataPoint.mzValue(), massList.getIntensityValue(index)),
            lipidAnnotation.getLipidClass(), chainLength, numberOfDoubleBonds,
            lipidChain.getNumberOfOxygens(), LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN,
            msMsScan));
      }
    }
    return matchedFragments;
  }

}
