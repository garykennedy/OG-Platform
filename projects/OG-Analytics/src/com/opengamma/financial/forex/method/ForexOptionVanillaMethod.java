/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.forex.calculator.ForexDerivative;
import com.opengamma.financial.forex.derivative.ForexOptionVanilla;
import com.opengamma.financial.interestrate.PresentValueSensitivity;
import com.opengamma.financial.interestrate.YieldCurveBundle;
import com.opengamma.financial.model.option.definition.SmileDeltaTermStructureDataBundle;
import com.opengamma.financial.model.option.pricing.analytic.formula.BlackFunctionData;
import com.opengamma.financial.model.option.pricing.analytic.formula.BlackPriceFunction;
import com.opengamma.math.function.Function1D;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.Triple;

/**
 * Pricing method for vanilla Forex option transactions with Black function and a volatility provider.
 */
public class ForexOptionVanillaMethod implements ForexPricingMethod {

  /**
   * The Black function used in the pricing.
   */
  private static final BlackPriceFunction BLACK_FUNCTION = new BlackPriceFunction();

  public MultipleCurrencyAmount presentValue(final ForexOptionVanilla optionForex, final SmileDeltaTermStructureDataBundle smile) {
    Validate.notNull(optionForex, "Forex option");
    Validate.notNull(smile, "Smile");
    double df = smile.getCurve(optionForex.getUnderlyingForex().getPaymentCurrency2().getFundingCurveName()).getDiscountFactor(optionForex.getUnderlyingForex().getPaymentTime());
    double spot = smile.getSpot();
    double forward = spot * smile.getCurve(optionForex.getUnderlyingForex().getPaymentCurrency1().getFundingCurveName()).getDiscountFactor(optionForex.getUnderlyingForex().getPaymentTime()) / df;
    double volatility = smile.getSmile().getVolatility(new Triple<Double, Double, Double>(optionForex.getTimeToExpiry(), optionForex.getStrike(), forward));
    BlackFunctionData dataBlack = new BlackFunctionData(forward, df, volatility);
    Function1D<BlackFunctionData, Double> func = BLACK_FUNCTION.getPriceFunction(optionForex);
    double price = func.evaluate(dataBlack) * Math.abs(optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount());
    CurrencyAmount priceCurrency = CurrencyAmount.of(optionForex.getUnderlyingForex().getCurrency2(), price);
    return MultipleCurrencyAmount.of(priceCurrency);
  }

  @Override
  public MultipleCurrencyAmount presentValue(final ForexDerivative instrument, final YieldCurveBundle curves) {
    Validate.isTrue(instrument instanceof ForexOptionVanilla, "Vanilla Forex option");
    Validate.isTrue(curves instanceof SmileDeltaTermStructureDataBundle, "Smile delta data bundle required");
    return presentValue((ForexOptionVanilla) instrument, (SmileDeltaTermStructureDataBundle) curves);
  }

  public MultipleCurrencyAmount currencyExposure(final ForexOptionVanilla optionForex, final SmileDeltaTermStructureDataBundle smile) {
    Validate.notNull(optionForex, "Forex option");
    Validate.notNull(smile, "Smile");
    double dfDomestic = smile.getCurve(optionForex.getUnderlyingForex().getPaymentCurrency2().getFundingCurveName()).getDiscountFactor(optionForex.getUnderlyingForex().getPaymentTime());
    double dfForeign = smile.getCurve(optionForex.getUnderlyingForex().getPaymentCurrency1().getFundingCurveName()).getDiscountFactor(optionForex.getUnderlyingForex().getPaymentTime());
    double spot = smile.getSpot();
    double forward = spot * dfForeign / dfDomestic;
    double volatility = smile.getSmile().getVolatility(new Triple<Double, Double, Double>(optionForex.getTimeToExpiry(), optionForex.getStrike(), forward));
    BlackFunctionData dataBlack = new BlackFunctionData(forward, dfDomestic, volatility);
    double[] priceAdjoint = BLACK_FUNCTION.getPriceAdjoint(optionForex, dataBlack);
    double price = priceAdjoint[0] * Math.abs(optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount());
    double deltaSpot = priceAdjoint[1] * dfForeign / dfDomestic;
    CurrencyAmount[] currencyExposure = new CurrencyAmount[2];
    // Implementation note: foreign currency (currency 1) exposure = Delta_spot * amount1.
    currencyExposure[0] = CurrencyAmount.of(optionForex.getUnderlyingForex().getCurrency1(), deltaSpot * Math.abs(optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount()));
    // Implementation note: domestic currency (currency 2) exposure = -Delta_spot * amount1 * spot+PV
    currencyExposure[1] = CurrencyAmount.of(optionForex.getUnderlyingForex().getCurrency2(),
        -deltaSpot * Math.abs(optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount()) * smile.getSpot() + price);
    return MultipleCurrencyAmount.of(currencyExposure);
  }

  @Override
  public MultipleCurrencyAmount currencyExposure(final ForexDerivative instrument, final YieldCurveBundle curves) {
    Validate.isTrue(instrument instanceof ForexOptionVanilla, "Vanilla Forex option");
    Validate.isTrue(curves instanceof SmileDeltaTermStructureDataBundle, "Smile delta data bundle required");
    return currencyExposure((ForexOptionVanilla) instrument, (SmileDeltaTermStructureDataBundle) curves);
  }

  /**
   * Computes the curve sensitivity of the option present value. The sensitivity of the volatility on the forward (and on the curves) is not taken into account. It is the curve
   * sensitivity in the Black model where the volatility is suppose to be constant for curve and forward changes.
   * @param optionForex The Forex option.
   * @param smile The smile data.
   * @return The curve sensitivity.
   */
  public PresentValueSensitivity presentValueCurveSensitivity(final ForexOptionVanilla optionForex, final SmileDeltaTermStructureDataBundle smile) {
    Validate.notNull(optionForex, "Forex option");
    Validate.notNull(smile, "Smile");
    double spot = smile.getSpot();
    double payTime = optionForex.getUnderlyingForex().getPaymentTime();
    String domesticCurveName = optionForex.getUnderlyingForex().getPaymentCurrency2().getFundingCurveName();
    String foreignCurveName = optionForex.getUnderlyingForex().getPaymentCurrency1().getFundingCurveName();
    // Forward sweep
    double dfDomestic = smile.getCurve(domesticCurveName).getDiscountFactor(payTime);
    double dfForeign = smile.getCurve(foreignCurveName).getDiscountFactor(payTime);
    double forward = spot * dfForeign / dfDomestic;
    double volatility = smile.getSmile().getVolatility(new Triple<Double, Double, Double>(optionForex.getTimeToExpiry(), optionForex.getStrike(), forward));
    BlackFunctionData dataBlack = new BlackFunctionData(forward, 1.0, volatility);
    double[] priceAdjoint = BLACK_FUNCTION.getPriceAdjoint(optionForex, dataBlack);
    // Backward sweep
    double priceBar = 1.0;
    double forwardBar = priceAdjoint[1] * dfDomestic;
    double dfForeignBar = spot / dfDomestic * forwardBar;
    double dfDomesticBar = -spot / (dfDomestic * dfDomestic) * dfForeign * forwardBar + priceAdjoint[0] * priceBar;
    double rForeignBar = -payTime * dfForeign * dfForeignBar;
    double rDomesticBar = -payTime * dfDomestic * dfDomesticBar;
    // Sensitivity object
    final List<DoublesPair> listForeign = new ArrayList<DoublesPair>();
    listForeign.add(new DoublesPair(payTime, rForeignBar * optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount()));
    final Map<String, List<DoublesPair>> resultForeignMap = new HashMap<String, List<DoublesPair>>();
    resultForeignMap.put(foreignCurveName, listForeign);
    PresentValueSensitivity result = new PresentValueSensitivity(resultForeignMap);
    final List<DoublesPair> listDomestic = new ArrayList<DoublesPair>();
    listDomestic.add(new DoublesPair(payTime, rDomesticBar * optionForex.getUnderlyingForex().getPaymentCurrency1().getAmount()));
    final Map<String, List<DoublesPair>> resultDomesticMap = new HashMap<String, List<DoublesPair>>();
    resultDomesticMap.put(domesticCurveName, listDomestic);
    result = result.add(new PresentValueSensitivity(resultDomesticMap));
    return result;
  }

}