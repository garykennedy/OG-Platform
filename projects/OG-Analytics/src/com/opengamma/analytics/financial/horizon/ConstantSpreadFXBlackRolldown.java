/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.horizon;

import com.opengamma.analytics.financial.interestrate.YieldCurveBundle;
import com.opengamma.analytics.financial.model.option.definition.SmileDeltaTermStructureDataBundle;
import com.opengamma.analytics.financial.model.volatility.surface.SmileDeltaTermStructureParametersStrikeInterpolation;
import com.opengamma.util.money.Currency;
import com.opengamma.util.tuple.Pair;

/**
 * 
 */
public final class ConstantSpreadFXBlackRolldown implements RolldownFunction<SmileDeltaTermStructureDataBundle> {
  private static final ConstantSpreadYieldCurveBundleRolldownFunction CURVES_ROLLDOWN = ConstantSpreadYieldCurveBundleRolldownFunction.getInstance();
  private static final ConstantSpreadFXBlackRolldown INSTANCE = new ConstantSpreadFXBlackRolldown();

  public static ConstantSpreadFXBlackRolldown getInstance() {
    return INSTANCE;
  }

  private ConstantSpreadFXBlackRolldown() {
  }

  @Override
  public SmileDeltaTermStructureDataBundle rollDown(final SmileDeltaTermStructureDataBundle data, final double shiftTime) {
    final YieldCurveBundle shiftedCurves = CURVES_ROLLDOWN.rollDown(data, shiftTime);
    final Pair<Currency, Currency> currencyPair = data.getCurrencyPair();
    final SmileDeltaTermStructureParametersStrikeInterpolation volatilityData = data.getVolatilityModel();
    final SmileDeltaTermStructureParametersStrikeInterpolation smile = new SmileDeltaTermStructureParametersStrikeInterpolation(volatilityData.getVolatilityTerm(),
        volatilityData.getStrikeInterpolator()) {

      @Override
      public double getVolatility(final double time, final double strike, final double forward) {
        return volatilityData.getVolatility(time + shiftTime, strike, forward);
      }

      //      @Override
      //      public double getVolatility(final double time, final double strike, final double forward, final double[][] bucketSensitivity) {
      //        return volatilityData.getVolatility(time + shiftTime, strike, forward, bucketSensitivity);
      //      }
    };
    return new SmileDeltaTermStructureDataBundle(shiftedCurves, smile, currencyPair);
  }

}
