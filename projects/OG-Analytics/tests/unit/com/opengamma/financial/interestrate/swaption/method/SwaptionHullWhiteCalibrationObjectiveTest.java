/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.swaption.method;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.Period;
import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.daycount.DayCountFactory;
import com.opengamma.financial.instrument.index.CMSIndex;
import com.opengamma.financial.instrument.index.IborIndex;
import com.opengamma.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.financial.instrument.swaption.SwaptionPhysicalFixedIborDefinition;
import com.opengamma.financial.interestrate.TestsDataSets;
import com.opengamma.financial.interestrate.YieldCurveBundle;
import com.opengamma.financial.interestrate.method.SuccessiveRootFinderCalibrationEngine;
import com.opengamma.financial.interestrate.swaption.SwaptionPhysicalFixedIbor;
import com.opengamma.financial.model.interestrate.definition.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.financial.model.option.definition.SABRInterestRateDataBundle;
import com.opengamma.financial.model.option.definition.SABRInterestRateParameters;
import com.opengamma.financial.schedule.ScheduleCalculator;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.time.DateUtil;

public class SwaptionHullWhiteCalibrationObjectiveTest {

  // Swaption description
  private static final boolean IS_LONG = true;
  private static final int SETTLEMENT_DAYS = 2;
  // Swap 5Y description
  private static final Currency CUR = Currency.USD;
  private static final Calendar CALENDAR = new MondayToFridayCalendar("A");
  private static final BusinessDayConvention BUSINESS_DAY = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Modified Following");
  private static final boolean IS_EOM = true;
  private static final double NOTIONAL = 100000000; //100m
  //  Fixed leg: Semi-annual bond
  private static final Period FIXED_PAYMENT_PERIOD = Period.ofMonths(6);
  private static final DayCount FIXED_DAY_COUNT = DayCountFactory.INSTANCE.getDayCount("30/360");
  private static final double RATE = 0.0325;
  private static final boolean FIXED_IS_PAYER = true;
  //  Ibor leg: quarterly money
  private static final Period INDEX_TENOR = Period.ofMonths(3);
  private static final DayCount DAY_COUNT = DayCountFactory.INSTANCE.getDayCount("Actual/360");
  private static final IborIndex IBOR_INDEX = new IborIndex(CUR, INDEX_TENOR, SETTLEMENT_DAYS, CALENDAR, DAY_COUNT, BUSINESS_DAY, IS_EOM);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final CMSIndex CMS_INDEX = new CMSIndex(FIXED_PAYMENT_PERIOD, FIXED_DAY_COUNT, IBOR_INDEX, Period.ofYears(SWAP_TENOR_YEAR));

  private static final ZonedDateTime REFERENCE_DATE = DateUtil.getUTCDate(2011, 8, 18);
  private static final int[] EXPIRY_TENOR = new int[] {1, 2, 3, 4, 5};

  private static final ZonedDateTime[] EXPIRY_DATE = new ZonedDateTime[EXPIRY_TENOR.length];
  private static final ZonedDateTime[] SETTLEMENT_DATE = new ZonedDateTime[EXPIRY_TENOR.length];
  private static final SwapFixedIborDefinition[] SWAP_PAYER_DEFINITION = new SwapFixedIborDefinition[EXPIRY_TENOR.length];
  private static final SwaptionPhysicalFixedIborDefinition[] SWAPTION_LONG_PAYER_DEFINITION = new SwaptionPhysicalFixedIborDefinition[EXPIRY_TENOR.length];
  static {
    for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
      EXPIRY_DATE[loopexp] = ScheduleCalculator.getAdjustedDate(REFERENCE_DATE, BUSINESS_DAY, CALENDAR, Period.ofYears(EXPIRY_TENOR[loopexp]));
      SETTLEMENT_DATE[loopexp] = ScheduleCalculator.getAdjustedDate(EXPIRY_DATE[loopexp], BUSINESS_DAY, CALENDAR, SETTLEMENT_DAYS);
      SWAP_PAYER_DEFINITION[loopexp] = SwapFixedIborDefinition.from(SETTLEMENT_DATE[loopexp], CMS_INDEX, NOTIONAL, RATE, FIXED_IS_PAYER);
      SWAPTION_LONG_PAYER_DEFINITION[loopexp] = SwaptionPhysicalFixedIborDefinition.from(EXPIRY_DATE[loopexp], SWAP_PAYER_DEFINITION[loopexp], IS_LONG);
    }
  }
  // to derivatives
  private static final String FUNDING_CURVE_NAME = "Funding";
  private static final String FORWARD_CURVE_NAME = "Forward";
  private static final String[] CURVES_NAME = {FUNDING_CURVE_NAME, FORWARD_CURVE_NAME};
  private static final YieldCurveBundle CURVES = TestsDataSets.createCurves1();
  private static final SABRInterestRateParameters SABR_PARAMETER = TestsDataSets.createSABR1();
  private static final SABRInterestRateDataBundle SABR_BUNDLE = new SABRInterestRateDataBundle(SABR_PARAMETER, CURVES);
  private static final SwaptionPhysicalFixedIbor[] SWAPTION_LONG_PAYER = new SwaptionPhysicalFixedIbor[EXPIRY_TENOR.length];
  static {
    for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
      SWAPTION_LONG_PAYER[loopexp] = SWAPTION_LONG_PAYER_DEFINITION[loopexp].toDerivative(REFERENCE_DATE, CURVES_NAME);
    }
  }
  private static final SwaptionPhysicalFixedIborSABRMethod METHOD_SABR = SwaptionPhysicalFixedIborSABRMethod.getInstance();
  private static final SwaptionPhysicalFixedIborHullWhiteMethod METHOD_HW = new SwaptionPhysicalFixedIborHullWhiteMethod();

  @Test
  /**
   * Tests the correctness of Hull-White one factor calibration to swaptions with SABR price.
   */
  public void calibration() {
    double meanReversion = 0.01;
    HullWhiteOneFactorPiecewiseConstantParameters HwParameters = new HullWhiteOneFactorPiecewiseConstantParameters(meanReversion, new double[] {0.01}, new double[0]);
    SwaptionPhysicalHullWhiteCalibrationObjective objective = new SwaptionPhysicalHullWhiteCalibrationObjective(HwParameters);
    SuccessiveRootFinderCalibrationEngine calibrationEngine = new SwaptionPhysicalHullWhiteSuccessiveRootFinderCalibrationEngine(objective);
    for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
      calibrationEngine.addInstrument(SWAPTION_LONG_PAYER[loopexp], METHOD_SABR);
    }
    calibrationEngine.calibrate(SABR_BUNDLE);
    CurrencyAmount[] pvSabr = new CurrencyAmount[EXPIRY_TENOR.length];
    CurrencyAmount[] pvHw = new CurrencyAmount[EXPIRY_TENOR.length];
    for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
      pvSabr[loopexp] = METHOD_SABR.presentValue(SWAPTION_LONG_PAYER[loopexp], SABR_BUNDLE);
      pvHw[loopexp] = METHOD_HW.presentValue(SWAPTION_LONG_PAYER[loopexp], objective.getHwBundle());
      assertEquals("Hull-White calibration: swaption " + loopexp, pvSabr[loopexp].getAmount(), pvHw[loopexp].getAmount(), 1E-2);
    }
  }

  @Test(enabled = true)
  /**
   * Test of performance. In normal testing, "enabled = false".
   */
  public void performance() {
    double meanReversion = 0.01;
    long startTime, endTime;
    final int nbTest = 100;
    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      HullWhiteOneFactorPiecewiseConstantParameters HwParameters = new HullWhiteOneFactorPiecewiseConstantParameters(meanReversion, new double[] {0.01}, new double[0]);
      SwaptionPhysicalHullWhiteCalibrationObjective objective = new SwaptionPhysicalHullWhiteCalibrationObjective(HwParameters);
      SuccessiveRootFinderCalibrationEngine calibrationEngine = new SwaptionPhysicalHullWhiteSuccessiveRootFinderCalibrationEngine(objective);
      for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
        calibrationEngine.addInstrument(SWAPTION_LONG_PAYER[loopexp], METHOD_SABR);
      }
      calibrationEngine.calibrate(SABR_BUNDLE);
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " Hull-White calibration to swaption: " + (endTime - startTime) + " ms");
    // Performance note: calibration: 15-Jul-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 260 ms for 100 calibration with 5 swaptions.

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      HullWhiteOneFactorPiecewiseConstantParameters HwParameters = new HullWhiteOneFactorPiecewiseConstantParameters(meanReversion, new double[] {0.01}, new double[0]);
      SwaptionPhysicalHullWhiteCalibrationObjective objective = new SwaptionPhysicalHullWhiteCalibrationObjective(HwParameters);
      SuccessiveRootFinderCalibrationEngine calibrationEngine = new SwaptionPhysicalHullWhiteSuccessiveRootFinderCalibrationEngine(objective);
      for (int loopexp = 0; loopexp < EXPIRY_TENOR.length; loopexp++) {
        calibrationEngine.addInstrument(SWAPTION_LONG_PAYER[loopexp], METHOD_SABR);
      }
      calibrationEngine.calibrate(SABR_BUNDLE);
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " Hull-White calibration to swaption: " + (endTime - startTime) + " ms");
  }

}