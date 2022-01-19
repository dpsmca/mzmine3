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

package util;

import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.correlation.FeatureCorrelationUtil.DIA;
import io.github.mzmine.util.ArrayUtils;
import java.util.Arrays;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DIACorrelationTest {

  private static final Logger logger = Logger.getLogger(DIACorrelationTest.class.getName());

  @Test
  void testDIACorrelation() {

    // values from gaussian function

    // main values
    double[] mainX = {-2, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1, -1, -0.9, -0.8,
        -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1, 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1,
        1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2};

    double[] mainY = new double[mainX.length];

    for (int i = 0; i < mainX.length; i++) {
      mainY[i] = gauss(mainX[i], 0.25, 0);
    }

    // shift x values so we have to interpolate
    double[] xShiftedBy0_05 = {-1.95, -1.85, -1.75, -1.65, -1.55, -1.45, -1.35, -1.25, -1.15, -1.05,
        -0.95, -0.85, -0.75, -0.65, -0.55, -0.45, -0.35, -0.25, -0.15, -0.05, 0.05, 0.15, 0.25,
        0.35, 0.45, 0.55, 0.65, 0.75, 0.85, 0.95, 1.05, 1.15, 1.25, 1.35, 1.45, 1.55, 1.65, 1.75,
        1.85, 1.95, 2.05};
    double[] y_shifted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_shifted[i] = gauss(xShiftedBy0_05[i], 0.25, 0);
    }
    final CorrelationData correlationData = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_shifted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted " + correlationData.getPearsonR());
    logger.info(() -> "Cosine shifted " + correlationData.getCosineSimilarity());

    // distort shape by making it wider
    double[] y_distorted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_distorted[i] = gauss(xShiftedBy0_05[i], 0.4, 0);
    }
    final CorrelationData correlationData2 = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_distorted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted " + correlationData2.getPearsonR());
    logger.info(() -> "Cosine shifted " + correlationData2.getCosineSimilarity());
    Assertions.assertTrue(correlationData.getPearsonR() > correlationData2.getPearsonR());
    Assertions.assertTrue(
        correlationData.getCosineSimilarity() > correlationData2.getCosineSimilarity());

    // shift and distort
    double[] y_shifted_distorted = new double[xShiftedBy0_05.length];
    for (int i = 0; i < mainX.length; i++) {
      y_shifted_distorted[i] = gauss(xShiftedBy0_05[i], 0.4, 0.25);
    }
    final CorrelationData correlationData3 = DIA.corrFeatureShape(mainX, mainY, xShiftedBy0_05,
        y_shifted_distorted, 5, 2, 0.0001);
    logger.info(() -> "Pearson shifted_distorted " + correlationData3.getPearsonR());
    logger.info(() -> "Cosine shifted_distorted " + correlationData3.getCosineSimilarity());
    Assertions.assertTrue(correlationData2.getPearsonR() > correlationData3.getPearsonR());
    Assertions.assertTrue(
        correlationData2.getCosineSimilarity() > correlationData3.getCosineSimilarity());

    // less points, more to interpolate
    double[] x_lessPoints = new double[10];
    for (int i = 0; i < x_lessPoints.length; i++) {
      x_lessPoints[i] = xShiftedBy0_05[(int) (i * ((double) xShiftedBy0_05.length
          / x_lessPoints.length))];
    }
    double[] y_lessPoints = new double[x_lessPoints.length];
    for (int i = 0; i < x_lessPoints.length; i++) {
      y_lessPoints[i] = gauss(x_lessPoints[i], 0.25, 0);
    }
    final CorrelationData correlationData4 = DIA.corrFeatureShape(mainX, mainY, x_lessPoints,
        y_lessPoints, 5, 2, 0.0001);
    logger.info(() -> "Pearson less points " + correlationData4.getPearsonR());
    logger.info(() -> "Cosine less points " + correlationData4.getCosineSimilarity());
//    Assertions.assertTrue(correlationData2.getPearsonR() > correlationData4.getPearsonR());
//    Assertions.assertTrue(correlationData2.getCosineSimilarity() > correlationData4.getCosineSimilarity());

    /*final double[][] interpolatedShape = DIA.getInterpolatedShape(mainX, mainY, x_lessPoints,
        y_lessPoints);
    for (int i = 0; i < interpolatedShape[0].length; i++) {
      System.out.println(interpolatedShape[0][i]);
    }
    System.out.println();
    System.out.println();
    for (int i = 0; i < interpolatedShape[0].length; i++) {
      System.out.println(interpolatedShape[1][i]);
    }*/

    final CorrelationData correlationData5 = DIA.corrFeatureShape(x_lessPoints, y_lessPoints, mainX,
        mainY, 5, 2, 0.0001);
    logger.info(() -> "Pearson less points_reversed " + correlationData5.getPearsonR());
    logger.info(() -> "Cosine less points_reversed " + correlationData5.getCosineSimilarity());

    /*Instant start = Instant.now();
    for(int i = 0; i < 1_000_000; i++) {
      final CorrelationData performance = DIA.corrFeatureShape(x_lessPoints,
          y_lessPoints, mainX, mainY, 5, 2, 0.0001);
    }
    Instant end = Instant.now();
    logger.info(() -> "Time to execute: " + (end.toEpochMilli() - start.toEpochMilli()));*/
  }

  private double gauss(double x, double sigma, double mu) {
    return 1 / (sigma * Math.sqrt(2 * Math.PI)) * Math.exp(
        -0.5 * (Math.pow(mu - x, 2) / Math.pow(sigma, 2)));
  }

  @Test
  void testCorr() {
    final CorrelationData correlationData = DIA.corrFeatureShape(ms1rts, ms1intensities, ms2rts,
        ms2intensities, 5, 2, 0);
    logger.info("Correlation: ");
    logger.info("correlated " + correlationData.getDPCount() + " points");
    logger.info("R:" + correlationData.getPearsonR() + "");
    logger.info("R2:" + Math.pow(correlationData.getPearsonR(), 2) + "");
    logger.info("Cosine: " + correlationData.getCosineSimilarity() + "");

    final int maxIndex = Math.abs(ArrayUtils.indexOf(3458844d, ms1intensities));
    double[] trimmedRts = Arrays.copyOfRange(ms1rts, maxIndex - 5, maxIndex + 5);
    double[] trimmedIntensities = Arrays.copyOfRange(ms1intensities, maxIndex - 5, maxIndex + 5);
    final CorrelationData correlationData2 = DIA.corrFeatureShape(trimmedRts, trimmedIntensities, ms2rts,
        ms2intensities, 5, 2, 0);
    logger.info("Correlation: ");
    logger.info("correlated " + correlationData2.getDPCount() + " points");
    logger.info("R:" + correlationData2.getPearsonR() + "");
    logger.info("R2:" + Math.pow(correlationData2.getPearsonR(), 2) + "");
    logger.info("Cosine: " + correlationData2.getCosineSimilarity() + "");

  }

  private static double[] ms1rts = new double[]{10.224934, 10.2642, 10.303467, 10.3429, 10.382167,
      10.421433, 10.460683, 10.49995, 10.539217, 10.57865, 10.617917, 10.657184, 10.696433, 10.7357,
      10.774966, 10.814234, 10.853666, 10.892917, 10.932183, 10.97145, 11.010716, 11.049983,
      11.0894165, 11.128667, 11.167933, 11.2072, 11.246467, 11.285733, 11.325, 11.364417, 11.403684,
      11.44295, 11.482217, 11.521483, 11.56075};

  private static double[] ms1intensities = new double[]{4555, 9525, 18009, 28950, 38919, 45017,
      43402, 39637, 40766, 69359, 150927, 439667, 1210074, 2021947, 3011063, 3458844, 3398783,
      2787659, 1777083, 1057632, 538266, 288249, 214723, 177263, 156351, 116322, 73630, 42625,
      26202, 21112, 17866, 17160, 15851, 12014, 9090
  };

  private static double[] ms2rts = new double[]{0.066516668, 0.105783336, 0.145199999, 0.18446666,
      0.223733336,0.263000011,0.302266657,0.341533333,0.380783349,0.420266658,0.459533334,
      0.498800009,0.538066685,0.577333331,0.616599977,0.656016648,0.695283353,0.73454994,
      0.773816645,0.813083351,0.852333307,0.891600013,0.931033313,0.970300019,1.009566665,
      1.04883337,1.088083386,1.127349973,1.166783452,1.206050038,1.245316625,1.28458333,
      1.323833346,1.363100052,1.402366638,1.441799998,1.481066585,1.520333409,1.559583306,
      1.598850012,1.638116717,1.677549958,1.716816664,1.756083369,1.795333385,1.834599972,
      1.873866677,1.913133383,1.952566624,1.99181664,2.031083345,2.070349932,2.109616756,
      2.148883343,2.188633442,2.228233337,2.267499685,2.306766748,2.346033335,2.385299921,
      2.424550056,2.463983297,2.503249884,2.54251647,2.581783295,2.621050119,2.660300016,
      2.699733257,2.739000082,2.778266668,2.817549944,2.856816769,2.896083355,2.935349941,
      2.974783421,3.014033318,3.053299904,3.092566729,3.131833315,3.171099901,3.210516691,
      3.249783278,3.289050102,3.328316689,3.367583275,3.4068501,3.446266651,3.485533237,
      3.524800062,3.564066648,3.603333235,3.64260006,3.681849957,3.721283436,3.760550022,
      3.799816608,3.839083433,3.878350019,3.917599916,3.957033396,3.996299982,4.035566807,
      4.074833393,4.114099979,4.153349876,4.192616463,4.232049942,4.271316528,4.310583591,
      4.349850178,4.389100075,4.428366661,4.467799664,4.507066727,4.546333313,4.585599899,
      4.624849796,4.664116859,4.703383446,4.742816448,4.782083511,4.821333408,4.860599995,
      4.899866581,4.939133167,4.978566647,5.017833233,5.05708313,5.096350193,5.135616779,
      5.174883366,5.214149952,5.253583431,5.292833328,5.332099915,5.371366501,5.410633564,
      5.44990015,5.489649773,5.529250145,5.568516731,5.607783318,5.647049904,5.686299801,
      5.725566387,5.764999866,5.804266453,5.843533516,5.882800102,5.922049999,5.961316586,
      6.000750065,6.040016651,6.079283237,6.118533134,6.157800198,6.197066784,6.23633337,
      6.27576685,6.315033436,6.354283333,6.393549919,6.432816505,6.472083569,6.511516094,
      6.550783157,6.590033531,6.629300117,6.668566704,6.70783329,6.747099876,6.786533833,
      6.825783253,6.865049839,6.904316902,6.943583488,6.982850075,7.022283554,7.061533451,
      7.100800037,7.140066624,7.17933321,7.218599796,7.257866859,7.297282696,7.336550236,
      7.375816822,7.415083408,7.454349995,7.493616581,7.533033371,7.572300434,7.611566544,
      7.65083313,7.690100193,7.72935009,7.768616676,7.808050156,7.847316265,7.886583328,
      7.925849915,7.965099812,8.004366875,8.043800354,8.08306694,8.122333527,8.161600113,
      8.200849533,8.240117073,8.279383659,8.318817139,8.358083725,8.397350311,8.436599731,
      8.475866318,8.515132904,8.554900169,8.594483376,8.633749962,8.673016548,8.712283134,
      8.751549721,8.790816307,8.830233574,8.86950016,8.908766747,8.948033333,8.987299919,
      9.026566505,9.065983772,9.105250359,9.144516945,9.183783531,9.223050117,9.262316704,
      9.301566124,9.340999603,9.380267143,9.41953373,9.458800316,9.498066902,9.537316322,
      9.576749802,9.616016388,9.655282974,9.694549561,9.733817101,9.773066521,9.8125,
      9.851766586,9.891033173,9.930299759,9.969550133,10.00881672,10.04808331,10.08751583,
      10.12678337,10.16604996,10.20530033,10.24456692,10.2838335,10.32326698,10.36253357,
      10.40180016,10.44104958,10.48031712,10.5195837,10.55885029,10.59828377,10.63755131,
      10.67679977,10.71606636,10.75533199,10.79459953,10.83403301,10.8732996,10.91254997,
      10.95181656,10.99108315,11.03034973,11.06961632,11.10903358,11.14830017,11.18756676,
      11.22683334,11.26609993,11.30536747,11.34478378,11.38405037,11.42331791,11.46258354,
      11.50185013,11.54111671,11.58036709,11.61979961,11.6590662,11.69833374,11.73760033,
      11.77686691,11.81611633,11.8558836,11.89548302,11.9347496,11.97401714,12.01326656,
      12.05253315,12.09179974,12.13123417,12.1704998,12.20975018,12.24901676,12.28828335,
      12.32754993,12.36698341,12.40625,12.44550037,12.48476696,12.52403355,12.56330013,
      12.60256672,12.6420002,12.68124962,12.7205162,12.75978374,12.79905033,12.83831692,
      12.8777504,12.91700077,12.9562664,12.99553299,13.03479958,13.07406712,13.1133337,
      13.15275002,13.1920166,13.23128319,13.27054977,13.30981636,13.34908199,13.38850021,
      13.4277668,13.46703243,13.50629997,13.54556656,13.58483315,13.62408352,13.663517,
      13.70278358,13.74205017,13.78131676,13.82056713,13.85983372,13.89926624,13.93853378,
      13.97780037,14.01706791,14.05631638,14.09558296,14.13484859,14.17428303,14.21354961,
      14.2528162,14.29206657,14.33133316,14.37059975,14.41003323,14.44929981,14.4885664,
      14.52781677,14.56708336,14.60634995,14.64578342,14.68505001,14.7243166,14.76356697,
      14.80283451,14.84210014,14.88136673,14.92080021,14.96004963,14.99931622,15.03858376,
      15.07785034,15.11711693,15.15686703,15.19646645,15.23573303,15.27499962,15.3142662,
      15.35353374,15.39278316,15.43221664,15.47148323,15.51075077,15.5500164,15.58928299,
      15.62853336,15.66796684,15.70723343,15.74650002,15.7857666,15.82503319,15.86428356,
      15.90355015,15.94298363,15.98225021,16.0215168,16.06076622,16.10003281,16.13929939,
      16.17873383,16.21800041,16.257267,16.29651642,16.335783,16.37504959,16.41431618,
      16.45375061,16.4930172,16.53226662,16.5715332,16.61079979,16.65006638,16.68950081,
      16.7287674,16.76801682,16.8072834,16.84654999,16.88581657,16.92508316,16.96451759,
      17.00376701,17.0430336,17.08230019,17.12156487,17.16083336,17.20026588,17.23951912,
      17.2787838,17.31805038,17.35731697,17.39658356,17.43585014,17.47526741,17.514534,
      17.55380058,17.59306717,17.63233376,17.67158318,17.71101761,17.75028419,17.78955078,
      17.82881737,17.86808395,17.90733147,17.94659996,17.98603249,18.02529907,18.06456757,
      18.10383415,18.14308357,18.18235016,18.22211647,18.26171684,18.30096626,18.34023285,
      18.37949944,18.41876602,18.45803261,18.49746704,18.53671646,18.57598305,18.61525154,
      18.65451622,18.69378281,18.73321724,18.77246666,18.81173325,18.85099983,18.89026642,
      18.929533,18.96879959};

private static double[]ms2intensities={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,195,3779,
    6305,6959,3623,1244,357,88,210,380,818,685,604,400,272,355,424,661,567,689,
    583,837,575,979,1371,1579,1164,519,167,858,1468,1683,2116,3591,4524,3790,2784,
    3075,6954,11838,12939,9971,4723,5896,13595,31898,70330,103208,96667,67823,61998,
    130643,194662,192938,130836,62350,48312,147515,177913,183801,90200,94524,245748,
    400929,430006,272555,170107,244811,315791,266913,138737,64067,42806,28550,30321,
    39164,37921,22642,8391,7486,32923,63272,66637,43761,20748,97065,176199,187975,
    112841,31211,12268,6773,5332,3984,3078,2676,2694,2396,1952,3063,4200,4766,4097,
    4315,4848,4976,4141,5800,12625,15326,12535,4700,1371,1478,1606,1265,2599,34810,
    192347,328710,335316,185981,52636,17975,15110,12663,8789,3768,1992,2157,1995,
    1983,1593,1979,3408,4708,6650,5893,5221,4314,5709,6608,5882,3563,2469,2284,
    1954,1732,1584,2073,2491,2884,3438,3737,3079,2472,2156,2738,3170,3107,3096,
    3079,3508,3643,3491,3455,2726,3527,4064,5582,9386,25342,53313,76952,75367,52238,
    26423,14724,15196,29955,65860,111224,131821,111803,72614,47405,39635,35461,28493,
    20885,19098,27474,41499,56767,53054,45843,39068,49473,62326,77895,82643,79238,
    63661,54364,61529,95102,137476,163533,148129,111069,75355,65837,79457,120779,
    169380,200633,201449,184692,183031,203076,247659,292133,295728,234015,142705,83229,
    103516,207597,363311,510606,599544,620432,597107,546113,471143,385953,325493,
    322369,371015,426203,455374,444494,404642,348548,310063,324019,408221,530399,
    628986,660511,610318,499235,378483,319050,378302,532655,711602,836661,885981,
    876194,816617,710956,572189,450842,404025,437320,506761,546355,576377,644031,
    728300,767147,757035,700840,635451,532959,494361,525244,673467,805069,907509,
    974581,1002579,991990,909399,879248,816481,749966,701427,754078,798718,705188,
    544391,410402,342006,348903,465262,551418,534457,380068,329469,303028,267680,
    178449,177369,197057,200837,135932,81432,53961,37613,30971,22953,16664,13379,
    12208,10707,8485,6766,6599,5754,6131,5579,5759,4621,4590,4504,5065,4751,4511,
    4127,3741,3218,3075,2428,2230,2252,2084,2234,2139,1978,1884,1895,2428,2618,
    2339,2247,2300,1947,1918,1909,2280,2023,2071,1634,1544,1074,1041,924,1330,1174,
    1215,945,1230,1203,1360,1406,1395,1031,872,1186,1270,1353,1176,1267,1433,1157,
    985,899,922,970,1479,1296,1178,502,677,667,649,293,570,660,714,641,661,808,
    759,959,715,482,506,593,485,240,446,443,700,682,744,500,180,185,422,510,391,
    279,425,646,842,699,811,635,640,609,603,474,271,352,199,343,464,573,499,616,
    555,568,456,294,291,277,229,79,562,2321,3162,2800,1120,322,128,181,44,64,0,
    117,84,84,0,0,0,0,0,50,50,50,0,140,203,189};
    }
