/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.method;

import com.opengamma.financial.forex.calculator.ForexDerivative;
import com.opengamma.financial.interestrate.YieldCurveBundle;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * Interface for Forex pricing methods.
 */
//TODO: Class name to change very soon! Pricing method interface should probably be the same for all asset classes.
public interface ForexPricingMethod {

  /**
   * Computes the present value of the instrument.
   * @param instrument The instrument.
   * @param curves The yield curves.
   * @return The present value.
   */
  MultipleCurrencyAmount presentValue(final ForexDerivative instrument, final YieldCurveBundle curves);

}