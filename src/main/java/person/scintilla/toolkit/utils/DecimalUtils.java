package person.scintilla.toolkit.utils;

import static java.math.RoundingMode.*;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Copyright (c) 2023-2024 scintilla0 (<a href="https://github.com/scintilla0">https://github.com/scintilla0</a>)<br>
 * license MIT License <a href="http://www.opensource.org/licenses/mit-license.html">http://www.opensource.org/licenses/mit-license.html</a><br>
 * license GPL2 License <a href="http://www.gnu.org/licenses/gpl.html">http://www.gnu.org/licenses/gpl.html</a><br>
 * <br>
 * This class provides an assortment of calculation and output methods, both static and instance,
 * most of which have auto-parsing support using {@link #parseDecimal(Object)}.<br>
 * Instance calculation methods have log recording function that you can look into during debugging.<br>
 * All instance calculation methods automatically ignore {@code null}, while static methods are provided separately for ease of use.<br>
 * <b><u>ReserveNull</u></b> in static calculation methods means that once a not {@code null} decimal is put in,
 * the result definitely won't be {@code null}, while <b><u>NoticeNull</u></b> means that a single appearance of {@code null}
 * will result in concluding {@code null} to be the final result.<br>
 * All static methods with <b><u>W0</u></b>, i.e. wrap0, will automatically treat their arguments or final result
 * as {@link BigDecimal#ZERO} if they are {@code null}.
 * @version 1.4.0 - 2025-06-13
 * @author scintilla0
 */
public class DecimalUtils {

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static decimal getter

	/**
	 * Parses an instance of a supported type into a <b>BigDecimal</b> object.<br>
	 * Supports instances of: <b>DecimalWrapper</b>, <b>BigDecimal</b>, <b>Integer(int)</b>, <b>Long(long)</b>,
	 * <b>Double(double)</b>, <b>String</b>.<br>
	 * Passing {@code null} will return {@code null}.<br>
	 * Passing an unsupported argument will throw a <b>NumberFormatException</b>.<br>
	 * This method may not work properly when passing a <b>DecimalWrapper</b> object in web templates such as <b><i>Thymeleaf</i></b>.
	 * @param sourceObject Target object to be parsed into <b>BigDecimal</b>.
	 * @return Parsed <b>BigDecimal</b> value.
	 */
	public static BigDecimal parseDecimal(Object sourceObject) {
		BigDecimal result = null;
		if (sourceObject == null) {
			return null;
		} else if (sourceObject instanceof BigDecimal) {
			result = ((BigDecimal) sourceObject).add(ZERO);
		} else if (sourceObject instanceof Integer) {
			result = new BigDecimal((Integer) sourceObject);
		} else if (sourceObject instanceof Long) {
			result = new BigDecimal((Long) sourceObject);
		} else if (sourceObject instanceof Double) {
			Double sourceDouble = (Double) sourceObject;
			String sourceDoubleStr = String.valueOf(sourceDouble);
			result = new BigDecimal(sourceDoubleStr);
		} else if (sourceObject instanceof String) {
			String sourceString = (String) sourceObject;
			if (EmbeddedStringUtils.isEmpty(sourceString)) {
				return null;
			}
			try {
				result = new BigDecimal(sourceString.replace(",", BLANK));
			} catch (NumberFormatException exception) {
				return null;
			}
		} else if (sourceObject instanceof DecimalWrapper) {
			result = ((DecimalWrapper) sourceObject).value();
		} else {
			throw new NumberFormatException("Unparseable argument passed in");
		}
		return result;
	}

	/**
	 * Returns {@code null} if the target decimal equals to {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object to be wrapped.
	 * @return Wrapped <b>BigDecimal</b> value.
	 */
	public static BigDecimal wrapNull(Object sourceObject) {
		BigDecimal result = parseDecimal(sourceObject);
		if (isSameDecimal(ZERO, result)) {
			return null;
		}
		return result;
	}

	/**
	 * Returns {@link BigDecimal#ZERO} if the target decimal is {@code null}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object to be wrapped.
	 * @return Wrapped <b>BigDecimal</b> value.
	 */
	public static BigDecimal wrap0(Object sourceObject) {
		return ifNullThen(sourceObject, ZERO);
	}

	/**
	 * Returns the first decimal if it is not {@code null}, otherwise returns the second.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;ifNullThen("20", 30) -> [20]
	 * &#9;ifNullThen("A20", 30) -> [30]</pre>
	 * @param sourceObject1 Primary target decimal object.
	 * @param sourceObject2 Alternative target decimal object.
	 * @return Selected <b>BigDecimal</b> value.
	 */
	public static BigDecimal ifNullThen(Object sourceObject1, Object sourceObject2) {
		BigDecimal result = parseDecimal(sourceObject1);
		if (result == null) {
			result = parseDecimal(sourceObject2);
		}
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static calculation

	/**
	 * Reverses the sign of the target decimal to its opposite.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object to be negated.
	 * @return Negated <b>BigDecimal</b> value.
	 */
	public static BigDecimal minus(Object sourceObject) {
		BigDecimal result = parseDecimal(sourceObject);
		if (result == null) {
			return null;
		}
		return result.negate();
	}

	/**
	 * Gets absolute value of the target decimal.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object to get absolute value.
	 * @return Absolute <b>BigDecimal</b> value.
	 */
	public static BigDecimal absolute(Object sourceObject) {
		BigDecimal result = parseDecimal(sourceObject);
		if (result == null) {
			return null;
		}
		return result.abs();
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;sum(20, "-35", "50") -> [35]
	 * &#9;sum(20, "A35", "50") -> [70]
	 * &#9;sum(null, "A35", false) -> [0]</pre>
	 * @param addendObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.<br>
	 */
	public static BigDecimal sum(Object... addendObjects) {
		return wrap0(sumReserveNull(addendObjects));
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Returns {@code null} if there is no valid addend in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;sumReserveNull(20, "-35", "50") -> [35]
	 * &#9;sumReserveNull(20, "A35", "50") -> [70]
	 * &#9;sumReserveNull(null, "A35", false) -> null</pre>
	 * @param addendObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static BigDecimal sumReserveNull(Object... addendObjects) {
		BigDecimal result = null;
		for (Object addendObject : addendObjects) {
			BigDecimal addend = parseDecimal(addendObject);
			if (addend != null) {
				result = wrap0(result).add(addend);
			}
		}
		return result;
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Returns {@code null} once an invalid argument appears in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;sumNoticeNull(20, "-35", "50") -> [35]
	 * &#9;sumNoticeNull(20, "A35", "50") -> null
	 * &#9;sumNoticeNull(null, "A35", false) -> null</pre>
	 * @param addendObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static BigDecimal sumNoticeNull(Object... addendObjects) {
		BigDecimal result = ZERO;
		for (Object addendObject : addendObjects) {
			BigDecimal addend = parseDecimal(addendObject);
			if (addend == null) {
				return null;
			}
			result = result.add(addend);
		}
		return result;
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Insert <b>boolean</b> values to manipulate the plus-minus signs of the arguments that follow them.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;blendSum(20, false, "-35", "50") -> [5]
	 * &#9;blendSum(false, 20, "A35", true, "50") -> [30]
	 * &#9;blendSum(null, "A35", false) -> [0]</pre>
	 * @param paramObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static BigDecimal blendSum(Object... paramObjects) {
		return sum(getBlendAddends(paramObjects));
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Insert <b>boolean</b> values to manipulate the plus-minus signs of the arguments that follow them.<br>
	 * Returns {@code null} if there is no valid addend in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;blendSumReserveNull(20, false, "-35", "50") -> [5]
	 * &#9;blendSumReserveNull(false, 20, "A35", true, "50") -> [30]
	 * &#9;blendSumReserveNull(null, "A35", false) -> null</pre>
	 * @param paramObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static BigDecimal blendSumReserveNull(Object... paramObjects) {
		return sumReserveNull(getBlendAddends(paramObjects));
	}

	/**
	 * Calculates the sum of all the target decimals.<br>
	 * Insert <b>boolean</b> values to manipulate the plus-minus signs of the arguments that follow them.<br>
	 * Returns {@code null} once an invalid argument appears in arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;blendSumNoticeNull(20, false, "-35", "50") -> [5]
	 * &#9;blendSumNoticeNull(false, 20, "A35", true, "50") -> null
	 * &#9;blendSumNoticeNull(null, "A35", false) -> null</pre>
	 * @param paramObjects Target decimal objects to be summed up.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static BigDecimal blendSumNoticeNull(Object... paramObjects) {
		return sumNoticeNull(getBlendAddends(paramObjects));
	}

	private static Object[] getBlendAddends(Object... paramObjects) {
		boolean positive = true;
		List<Object> blendAddends = new ArrayList<>();
		for (Object paramObject : paramObjects) {
			if (paramObject instanceof Boolean) {
				positive = (Boolean) paramObject;
			} else {
				BigDecimal addend = parseDecimal(paramObject);
				if (addend != null && !positive) {
					addend = addend.negate();
				}
				blendAddends.add(addend);
			}
		}
		return blendAddends.toArray();
	}

	/**
	 * Calculates the product of all the target decimals.<br>
	 * Calculating result keeps {@link BigDecimal#ZERO} until any valid multiplier appears.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;product(2, "-3", "5") -> [-30]
	 * &#9;product(2, "A3", "5") -> [10]
	 * &#9;product(null, "A3", false) -> [0]</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied <b>BigDecimal</b> result.
	 */
	public static BigDecimal product(Object... multiplierObjects) {
		return wrap0(productReserveNull(multiplierObjects));
	}

	/**
	 * Calculates the product of all the target decimals.<br>
	 * Returns {@code null} if there is no valid addend in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;productReserveNull(2, "-3", "5") -> [-30]
	 * &#9;productReserveNull(2, "A3", "5") -> [10]
	 * &#9;productReserveNull(null, "A3", false) -> null</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied <b>BigDecimal</b> result.
	 */
	public static BigDecimal productReserveNull(Object... multiplierObjects) {
		BigDecimal result = null;
		for (Object multiplierObject : multiplierObjects) {
			BigDecimal multiplier = parseDecimal(multiplierObject);
			if (multiplier != null) {
				result = ifNullThen(result, ONE).multiply(multiplier);
			}
		}
		return result;
	}

	/**
	 * Calculates the product of all the target decimals.<br>
	 * Returns {@code null} once an invalid argument appears in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;productNoticeNull(2, "-3", "5") -> [-30]
	 * &#9;productNoticeNull(2, "A3", "5") -> null
	 * &#9;productNoticeNull(null, "A3", false) -> null</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied <b>BigDecimal</b> result.
	 */
	public static BigDecimal productNoticeNull(Object... multiplierObjects) {
		BigDecimal result = ONE;
		for (Object multiplierObject : multiplierObjects) {
			BigDecimal multiplier = parseDecimal(multiplierObject);
			if (multiplier == null) {
				return null;
			}
			result = result.multiply(multiplier);
		}
		return result;
	}

	/**
	 * Calculates the product of all the target decimals, and divides by {@code 100} as one of them is a percentage.<br>
	 * Calculating result keeps {@link BigDecimal#ZERO} until any valid multiplier appears.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;productDepercent(2, "-3", "50") -> [-3]
	 * &#9;productDepercent(2, "A3", "50") -> [1]
	 * &#9;productDepercent(null, "A3", false) -> [0]</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied and shrank <b>BigDecimal</b> result.
	 */
	public static BigDecimal productDepercent(Object... multiplierObjects) {
		return DEPERCENT_MULTIPLICATOR.multiply(product(multiplierObjects));
	}

	/**
	 * Calculates the product of all the target decimals, and divides by {@code 100} as one of them is a percentage.<br>
	 * Returns {@code null} if there is no valid multiplier in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;productDepercentReserveNull(2, "-3", "50") -> [-3]
	 * &#9;productDepercentReserveNull(2, "A3", "50") -> [1]
	 * &#9;productDepercentReserveNull(null, "A3", false) -> null</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied and shrank <b>BigDecimal</b> result.
	 */
	public static BigDecimal productDepercentReserveNull(Object... multiplierObjects) {
		return productNoticeNull(DEPERCENT_MULTIPLICATOR, productReserveNull(multiplierObjects));
	}

	/**
	 * Calculates the product of all the target decimals, and divides by {@code 100} as one of them is a percentage.<br>
	 * Returns {@code null} once an invalid argument appears in the arguments.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;productDepercentNoticeNull(2, "-3", "50") -> [-3]
	 * &#9;productDepercentNoticeNull(2, "A3", "50") -> null
	 * &#9;productDepercentNoticeNull(null, "A3", false) -> null</pre>
	 * @param multiplierObjects Target decimal objects to be multiplied.
	 * @return Multiplied and shrank <b>BigDecimal</b> result.
	 */
	public static BigDecimal productDepercentNoticeNull(Object... multiplierObjects) {
		return productNoticeNull(DEPERCENT_MULTIPLICATOR, productNoticeNull(multiplierObjects));
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link RoundingMode#HALF_UP} as default rounding mode.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotient("41", "8", 2) -> [5.13]
	 * &#9;quotient("A41", "8", 2) -> [0.00]
	 * &#9;quotient("41", "A8", 2) -> [41.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotient(Object dividendObject, Object divisorObject, int scale) {
		return quotient(dividendObject, divisorObject, scale, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotient("41", "8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [5.13]
	 * &#9;quotient("A40", "8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [0.00]
	 * &#9;quotient("40", "A8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [40.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode using int value preset in <b>BigDecimal</b>.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotient(Object dividendObject, Object divisorObject, int scale, int roundingMode) {
		return quotient(dividendObject, divisorObject, scale, valueOf(roundingMode));
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotient("41", "8", 2, {@link RoundingMode#HALF_UP}) -> [5.13]
	 * &#9;quotient("A41", "8", 2, {@link RoundingMode#HALF_UP}) -> [0.00]
	 * &#9;quotient("41", "A8", 2, {@link RoundingMode#HALF_UP}) -> [40.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode}.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotient(Object dividendObject, Object divisorObject, int scale, RoundingMode roundingMode) {
		if (isUnusableOr0(divisorObject)) {
			divisorObject = ONE;
		}
		return quotientNoticeNull(wrap0(dividendObject), divisorObject, scale, roundingMode);
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link RoundingMode#HALF_UP} as default rounding mode.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotientNoticeNull("41", "8", 2) -> [5.13]
	 * &#9;quotientNoticeNull("A41", "8", 2) -> null
	 * &#9;quotientNoticeNull("41", "A8", 2) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotientNoticeNull(Object dividendObject, Object divisorObject, int scale) {
		return quotientNoticeNull(dividendObject, divisorObject, scale, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotientNoticeNull("41", "8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [5.13]
	 * &#9;quotientNoticeNull("A41", "8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> null
	 * &#9;quotientNoticeNull("41", "A8", 2, {@link BigDecimal#ROUND_HALF_UP}) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode using int value preset in <b>BigDecimal</b>.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotientNoticeNull(Object dividendObject, Object divisorObject, int scale, int roundingMode) {
		return quotientNoticeNull(dividendObject, divisorObject, scale, valueOf(roundingMode));
	}

	/**
	 * Calculates the quotient of the two target decimals.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;quotientNoticeNull("41", "8", 2, {@link RoundingMode#HALF_UP}) -> [5.13]
	 * &#9;quotientNoticeNull("A41", "8", 2, {@link RoundingMode#HALF_UP}) -> null
	 * &#9;quotientNoticeNull("41", "A8", 2, {@link RoundingMode#HALF_UP}) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode}.
	 * @return Divided <b>BigDecimal</b> result.
	 */
	public static BigDecimal quotientNoticeNull(Object dividendObject, Object divisorObject, int scale, RoundingMode roundingMode) {
		BigDecimal dividend = parseDecimal(dividendObject);
		BigDecimal divisor = parseDecimal(divisorObject);
		if (dividend == null || isNullOr0(divisor)) {
			return null;
		}
		return dividend.divide(divisor, scale, roundingMode);
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link RoundingMode#HALF_UP} as default rounding mode.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercent("8", "30", 2) -> [26.67]
	 * &#9;quotientPercent("A8", "30", 2) -> [0.00]
	 * &#9;quotientPercent("8", "A30", 2) -> [800.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercent(Object dividendObject, Object divisorObject, int scale) {
		return quotientPercent(dividendObject, divisorObject, scale, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercent("8", "30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [26.67]
	 * &#9;quotientPercent("A8", "30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [0.00]
	 * &#9;quotientPercent("8", "A30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [800.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode using int value preset in <b>BigDecimal</b>.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercent(Object dividendObject, Object divisorObject, int scale, int roundingMode) {
		return quotientPercent(dividendObject, divisorObject, scale, valueOf(roundingMode));
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercent("8", "30", 2, {@link RoundingMode#HALF_UP}) -> [26.67]
	 * &#9;quotientPercent("A8", "30", 2, {@link RoundingMode#HALF_UP}) -> [0.00]
	 * &#9;quotientPercent("8", "A30", 2, {@link RoundingMode#HALF_UP}) -> [800.00]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode}.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercent(Object dividendObject, Object divisorObject, int scale, RoundingMode roundingMode) {
		if (isUnusableOr0(divisorObject)) {
			divisorObject = ONE;
		}
		return quotientPercentNoticeNull(wrap0(dividendObject), divisorObject, scale, roundingMode);
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link RoundingMode#HALF_UP} as default rounding mode.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercentNoticeNull("8", "30", 2) -> [26.67]
	 * &#9;quotientPercentNoticeNull("A8", "30", 2) -> null
	 * &#9;quotientPercentNoticeNull("8", "A30", 2) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercentNoticeNull(Object dividendObject, Object divisorObject, int scale) {
		return quotientPercentNoticeNull(dividendObject, divisorObject, scale, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercentNoticeNull("8", "30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [26.67]
	 * &#9;quotientPercentNoticeNull("A8", "30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> null
	 * &#9;quotientPercentNoticeNull("8", "A30", 2, {@link BigDecimal#ROUND_HALF_UP}) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode using int value preset in <b>BigDecimal</b>.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercentNoticeNull(Object dividendObject, Object divisorObject, int scale, int roundingMode) {
		return quotientPercentNoticeNull(dividendObject, divisorObject, scale, valueOf(roundingMode));
	}

	/**
	 * Calculates the quotient of the two target decimals, and then multiplies by {@code 100} to get a percentage.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.<br>
	 * <font color="#EE2222"><b>This method should not be used with {@link #percent(Object, Integer)} at the same time.</b></font>
	 * <pre><b><i>Eg.:</i></b>&#9;quotientPercentNoticeNull("8", "30", 2, {@link RoundingMode#HALF_UP}) -> [26.67]
	 * &#9;quotientPercentNoticeNull("A8", "30", 2, {@link RoundingMode#HALF_UP}) -> null
	 * &#9;quotientPercentNoticeNull("8", "A30", 2, {@link RoundingMode#HALF_UP}) -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode}.
	 * @return Divided and stretched <b>BigDecimal</b> percentage result.
	 */
	public static BigDecimal quotientPercentNoticeNull(Object dividendObject, Object divisorObject, int scale, RoundingMode roundingMode) {
		BigDecimal newDividend = productNoticeNull(dividendObject, PERCENT_MULTIPLICATOR);
		return quotientNoticeNull(newDividend, divisorObject, scale, roundingMode);
	}

	/**
	 * Calculates the remainder of the two target decimals.<br>
	 * Returns {@link BigDecimal#ZERO} if the dividend decimal is {@code null}.<br>
	 * Calculating result remains the same as the dividend if the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;mod("41", "8") -> [1]
	 * &#9;mod("A41", "8") -> [0]
	 * &#9;mod("41", "A8") -> [41]</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @return <b>BigDecimal</b> mod result.
	 */
	public static BigDecimal mod(Object dividendObject, Object divisorObject) {
		BigDecimal divisor = parseDecimal(divisorObject);
		if (isNullOr0(divisor)) {
			return parseDecimal(dividendObject);
		}
		return modNoticeNull(wrap0(dividendObject), divisor);
	}

	/**
	 * Calculates the remainder of the two target decimals.<br>
	 * Returns {@code null} if the dividend is {@code null} or the divisor is invalid,
	 * i.e. {@code null} or {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;modNoticeNull("41", "8") -> [1]
	 * &#9;modNoticeNull("A41", "8") -> null
	 * &#9;modNoticeNull("41", "A8") -> null</pre>
	 * @param dividendObject Dividend decimal object.
	 * @param divisorObject Divisor decimal object.
	 * @return <b>BigDecimal</b> mod result.
	 */
	public static BigDecimal modNoticeNull(Object dividendObject, Object divisorObject) {
		BigDecimal dividend = parseDecimal(dividendObject);
		BigDecimal divisor = parseDecimal(divisorObject);
		if (dividend == null || isNullOr0(divisor)) {
			return null;
		}
		return dividend.divideAndRemainder(divisor)[1];
	}

	/**
	 * Rounds the target decimal to the specified scale.<br>
	 * Returns {@code null} if the target decimal is {@code null}.<br>
	 * Uses {@link RoundingMode#HALF_UP} as default rounding mode.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;setScale("41", 2) -> [41.00]
	 * &#9;setScale("52.84", 0) -> [53]
	 * &#9;setScale("A10", 2) -> null</pre>
	 * @param sourceObject Target decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @return Rounded <b>BigDecimal</b> result.
	 */
	public static BigDecimal setScale(Object sourceObject, int scale) {
		return setScale(sourceObject, scale, DEFAULT_ROUNDING_MODE);
	}

	/**
	 * Rounds the target decimal to the specified scale.<br>
	 * Returns {@code null} if target decimal is {@code null}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;setScale("41", 2, {@link BigDecimal#ROUND_HALF_UP}) -> [41.00]
	 * &#9;setScale("52.84", 0, {@link BigDecimal#ROUND_HALF_UP}) -> [53]
	 * &#9;setScale("A10", 2, {@link BigDecimal#ROUND_HALF_UP}) -> null</pre>
	 * @param sourceObject Target decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode using int value preset in <b>BigDecimal</b>.
	 * @return Rounded <b>BigDecimal</b> result.
	 */
	public static BigDecimal setScale(Object sourceObject, int scale, int roundingMode) {
		return setScale(sourceObject, scale, valueOf(roundingMode));
	}

	/**
	 * Rounds the target decimal to the specified scale.<br>
	 * Returns {@code null} if target decimal is {@code null}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;setScale("41", 2, {@link RoundingMode#HALF_UP}) -> [41.00]
	 * &#9;setScale("52.84", 0, {@link RoundingMode#HALF_UP}) -> [53]
	 * &#9;setScale("A10", 2, {@link RoundingMode#HALF_UP}) -> null</pre>
	 * @param sourceObject Target decimal object.
	 * @param scale Number of decimal places to be retained.
	 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode}.
	 * @return Rounded <b>BigDecimal</b> result.
	 */
	public static BigDecimal setScale(Object sourceObject, int scale, RoundingMode roundingMode) {
		BigDecimal result = parseDecimal(sourceObject);
		if (result == null) {
			return null;
		}
		return result.setScale(scale, roundingMode);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static comparing

	/**
	 * Evaluates if the target <b>BigDecimal</b> object is {@code null} or equals to {@link BigDecimal#ZERO}.
	 * @param sourceObject Target <b>BigDecimal</b> object.
	 * @return {@code true} if the target value is {@code null} or equals to {@link BigDecimal#ZERO}.
	 */
	public static boolean isNullOr0(BigDecimal sourceObject) {
		return sourceObject == null || isSameDecimal(ZERO, sourceObject);
	}

	/**
	 * Evaluates if the target decimal is {@code null} or equals to {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if the target value is {@code null} or equals to {@link BigDecimal#ZERO}.
	 */
	public static boolean isUnusableOr0(Object sourceObject) {
		return isNullOr0(parseDecimal(sourceObject));
	}

	/**
	 * Evaluates if the target object can be parsed into a valid <b>BigDecimal</b> object.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isDecimal(Object sourceObject) {
		return parseDecimal(sourceObject) != null;
	}

	/**
	 * Evaluates if the target object can be parsed into a valid <b>Integer</b> object.<br>
	 * <font color="#EE2222"><b>It should be known that an <b>Integer</b> parsed from a <b>Long</b>
	 * may not have the same value as the original.
	 * In this case, the evaluating result would be {@code false}.</b></font><br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isInteger(Object sourceObject) {
		return toInteger(sourceObject) != null;
	}

	/**
	 * Evaluates if the target object can be parses into a valid <b>Long</b> object.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isLong(Object sourceObject) {
		return toLong(sourceObject) != null;
	}

	/**
	 * Evaluates if the target object can be parsed into a valid natural number,
	 * i.e. non-negative integral number, which may not be an <b>Integer</b>.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isNaturalNumber(Object sourceObject) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (!isLong(decimal)) {
			return false;
		}
		int compareResult = compare(decimal, ZERO);
		return compareResult == 0 || compareResult == 1;
	}

	/**
	 * Evaluates if the target object can be parsed into a valid positive integral number, which may not be an <b>Integer</b>.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal object.
	 * @return {@code true} if successfully parsed.
	 */
	public static boolean isPositiveIntegral(Object sourceObject) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (!isLong(decimal)) {
			return false;
		}
		return compare(decimal, ZERO) == 1;
	}

	/**
	 * Evaluates if the target decimal has the same numeric value as one of the options, regardless of their data types.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isInScope(10, 10, "20", 30L) -> true
	 * &#9;isInScope(50, 10, "20", 30L) -> false</pre>
	 * @param targetObject Target decimal object to be compared.
	 * @param optionObjects Option decimal objects to be compared.
	 * @return {@code true} if any equal.
	 */
	public static boolean isInScope(Object targetObject, Object... optionObjects) {
		return isInScope(targetObject, Arrays.asList(optionObjects));
	}

	/**
	 * Evaluates if the target decimal has the same numeric value as one of the options, regardless of their data types.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isInScope(10, Arrays.asList(10, "20", 30L)) -> true
	 * &#9;isInScope(50, Arrays.asList(10, "20", 30L)) -> false</pre>
	 * @param targetObject Target decimal object to be compared.
	 * @param optionObjects Option decimal objects to be compared.
	 * @return {@code true} if any equal.
	 */
	public static boolean isInScope(Object targetObject, Collection<Object> optionObjects) {
		if (optionObjects == null || optionObjects.isEmpty()) {
			return false;
		}
		BigDecimal target = parseDecimal(targetObject);
		return optionObjects.stream().anyMatch(option -> compare(target, option) == 0);
	}

	/**
	 * Selects the map value corresponding to its map key with the same decimal value as the target object.<br>
	 * Returns null if there is no matched option.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;select(10, [[10, "a"], ["20", "b"], [30, "c"]]) -> "a"
	 * &#9;select(10, [[0, "a"], ["20", "b"], [30, "c"]]) -> null
	 * &#9;select(10, []) -> null</pre>
	 * @param <Type> The object type of the return map value.
	 * @param targetObject Target decimal object to be compared.
	 * @param optionMap Option map to be searched in.
	 * @return selected value.
	 */
	public static <Type> Type select(Object targetObject, Map<Object, Type> optionMap) {
		if (optionMap == null || optionMap.isEmpty()) {
			return null;
		}
		BigDecimal target = parseDecimal(targetObject);
		return optionMap.entrySet().stream().filter(entry -> compare(target, entry.getKey()) == 0).map(Entry::getValue).findFirst().orElse(null);
	}

	/**
	 * Selects the scope value corresponding to its scope key with the same decimal value as the target object.<br>
	 * Returns null if there is no matched option.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;select(10, DecimalOption.build(10, "a").and("20", "b")) -> "a"
	 * &#9;select(10, DecimalOption.build(0, "a").and("20", "b")) -> null
	 * &#9;select(10, null) -> null</pre>
	 * @param <Type> The object type of the return scope value.
	 * @param targetObject Target decimal object to be compared.
	 * @param scope Option scope to be searched in.
	 * @return selected value.
	 */
	public static <Type> Type select(Object targetObject, DecimalOption<Type> scope) {
		if (scope == null) {
			return null;
		}
		return select(targetObject, scope.optionMap);
	}

	/**
	 * Evaluates if the target <b>Integer</b> objects have the same numeric value.
	 * @param comparands Target <b>Integer</b> objects to be compared.
	 * @return {@code true} if all equal.
	 */
	public static boolean isSameInteger(Integer... comparands) {
		return haveSameValue((Object[]) comparands);
	}

	/**
	 * Evaluates if the target <b>Long</b> objects have the same numeric value.
	 * @param comparands Target <b>Long</b> objects to be compared.
	 * @return {@code true} if all equal.
	 */
	public static boolean isSameLong(Long... comparands) {
		return haveSameValue((Object[]) comparands);
	}

	/**
	 * Evaluates if the target <b>Double</b> objects have the same numeric value.
	 * @param comparands Target <b>Double</b> objects to be compared.
	 * @return {@code true} if all equal.
	 */
	public static boolean isSameDouble(Double... comparands) {
		return haveSameValue((Object[]) comparands);
	}

	/**
	 * Evaluates if the target <b>BigDecimal</b> objects have the same numeric value.
	 * @param comparands Target <b>BigDecimal</b> objects to be compared.
	 * @return {@code true} if all equal.
	 */
	public static boolean isSameDecimal(BigDecimal... comparands) {
		return haveSameValue((Object[]) comparands);
	}

	/**
	 * Evaluates if the target decimals have the same numeric value, regardless of their data types.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param comparandObjects Target decimal objects to be compared.
	 * @return {@code true} if all equal.
	 */
	public static boolean haveSameValue(Object... comparandObjects) {
		return Arrays.stream(comparandObjects).allMatch(comparand -> compare(comparandObjects[0], comparand) == 0);
	}

	/**
	 * Evaluates if the target decimals are in order from the smallest to the largest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isAscending(10, 20, "30", "40") -> true
	 * &#9;isAscending(10, 20, "40", "30") -> false
	 * &#9;isAscending(10, 20, "20", "40") -> true
	 * &#9;isAscending(10, 20, "A0", "40") -> true</pre>
	 * @param comparandObjects Target decimal objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean isAscending(Object... comparandObjects) {
		return isAscendingCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_PLAIN);
	}

	/**
	 * Evaluates if the target decimals are in order from the smallest to the largest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * Any argument whose parse result is {@code null} will be ignored, hence no effect to the evaluating result.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isAscendingNotEqual(10, 20, "30", "40") -> true
	 * &#9;isAscendingNotEqual(10, 20, "40", "30") -> false
	 * &#9;isAscendingNotEqual(10, 20, "20", "40") -> false
	 * &#9;isAscendingNotEqual(10, 20, "A0", "40") -> true</pre>
	 * @param comparandObjects Target decimal objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean isAscendingNotEqual(Object... comparandObjects) {
		return isAscendingCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL);
	}

	/**
	 * Evaluates if the target decimals are in order from the smallest to the largest.<br>
	 * Adjacent arguments with same value are considered in proper sequence, hence no effect to the evaluating result.<br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isAscendingNotNull(10, 20, "30", "40") -> true
	 * &#9;isAscendingNotNull(10, 20, "40", "30") -> false
	 * &#9;isAscendingNotNull(10, 20, "20", "40") -> true
	 * &#9;isAscendingNotNull(10, 20, "A0", "40") -> false</pre>
	 * @param comparandObjects Target decimal objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean isAscendingNotNull(Object... comparandObjects) {
		return isAscendingCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL);
	}

	/**
	 * Evaluates if the target decimals are in order from the smallest to the largest.<br>
	 * <font color="#EE2222"><b>Appearance of adjacent arguments with same value is considered to be violating the proper sequence.</b></font><br>
	 * <font color="#EE2222"><b>Any argument whose parse result is {@code null} is considered to be violating the proper sequence.</b></font><br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;isAscendingNotEqualNull(10, 20, "30", "40") -> true
	 * &#9;isAscendingNotEqualNull(10, 20, "40", "30") -> false
	 * &#9;isAscendingNotEqualNull(10, 20, "20", "40") -> false
	 * &#9;isAscendingNotEqualNull(10, 20, "A0", "40") -> false</pre>
	 * @param comparandObjects Target decimal objects to be compared.
	 * @return {@code true} if in proper sequence.
	 */
	public static boolean isAscendingNotEqualNull(Object... comparandObjects) {
		return isAscendingCore(comparandObjects, SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL);
	}

	private static boolean isAscendingCore(Object[] comparandObjects, List<Integer> sequenceInvalidCompareResult) {
		Object previousValidComparand = comparandObjects.length != 0 ? comparandObjects[0] : null;
		for (int index = 1; index < comparandObjects.length; index ++) {
			Object thisComparand = comparandObjects[index];
			if (sequenceInvalidCompareResult.contains(compare(previousValidComparand, thisComparand))) {
				return false;
			}
			previousValidComparand = isDecimal(thisComparand) ? thisComparand : previousValidComparand;
		}
		return true;
	}

	/**
	 * Evaluates size relationship between the two target decimals.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param comparandObject1 The first target decimal object to be compared.
	 * @param comparandObject2 The second target decimal object to be compared.
	 * @return Comparison result.<br>
	 * The following comparison results are for reference.<br><br>
	 * <table border style="width: 240px; text-align: center;">
	 * <tr><td><b>0</b></td><td>comparand1 = comparand2</td></tr>
	 * <tr><td><b>1</b></td><td>comparand1 > comparand2</td></tr>
	 * <tr><td><b>-1</b></td><td>comparand1 < comparand2</td></tr>
	 * <tr><td><b>2</b></td><td>only comparand2 is {@code null}</td></tr>
	 * <tr><td><b>-2</b></td><td>only comparand1 is {@code null}</td></tr>
	 * <tr><td><b>22</b></td><td>both comparands are {@code null}</td></tr>
	 * </table>
	 */
	public static int compare(Object comparandObject1, Object comparandObject2) {
		BigDecimal comparand1 = parseDecimal(comparandObject1), comparand2 = parseDecimal(comparandObject2);
		if (comparand1 == null && comparand2 == null) {
			return 22;
		} else if (comparand1 == null) {
			return -2;
		} else if (comparand2 == null) {
			return 2;
		}
		return comparand1.compareTo(comparand2);
	}

	/**
	 * Evaluates size relationship between the two target decimals.<br>
	 * Any decimal that is {@code null} will be wrapped into {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param comparandObject1 The first target decimal object to be compared.
	 * @param comparandObject2 The second target decimal object to be compared.
	 * @return Comparison result.
	 * @see #compare(Object, Object)
	 */
	public static int compareW0(Object comparandObject1, Object comparandObject2) {
		return compare(wrap0(comparandObject1), wrap0(comparandObject2));
	}

	/**
	 * Provides an ascending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compare(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;objectList.sort(DecimalUtils.compareAsc(User::getUserId))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareAsc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compare(fieldGetter.apply(entity1), fieldGetter.apply(entity2));
	}

	/**
	 * Provides a descending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compare(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;objectList.sort(DecimalUtils.compareDesc(User::getUserId))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareDesc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compare(fieldGetter.apply(entity2), fieldGetter.apply(entity1));
	}

	/**
	 * Provides an ascending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compareW0(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;objectList.sort(DecimalUtils.compareW0Asc(User::getUserId))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareW0Asc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareW0(fieldGetter.apply(entity1), fieldGetter.apply(entity2));
	}

	/**
	 * Provides a descending <b>Comparator&lt;Type&gt;</b> available in methods such as {@link List#sort(Comparator)}.<br>
	 * Uses {@link #compareW0(Object, Object)} as base method.
	 * <pre><b><i>Eg.:</i></b>&#9;objectList.sort(DecimalUtils.compareW0Desc(User::getUserId))</pre>
	 * @param <Type> The type of object to be compared.
	 * @param fieldGetter The getter of the field to be used for comparing.
	 * @return <b>Comparator&lt;Type&gt;</b><br>
	 */
	public static <Type> Comparator<Type> compareW0Desc(Function<Type, Object> fieldGetter) {
		return (entity1, entity2) -> compareW0(fieldGetter.apply(entity2), fieldGetter.apply(entity1));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static output

	/**
	 * Parses and converts the target object into an <b>Integer</b> object.<br>
	 * Returns {@code null} if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Integer</b> value.
	 */
	public static Integer toInteger(Object sourceObject) {
		return toType(sourceObject, BigDecimal::intValue);
	}

	/**
	 * Parses and converts the target object into an <b>Integer</b> object.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Integer</b> value.
	 */
	public static Integer toIntegerW0(Object sourceObject) {
		return toInteger(wrap0(sourceObject));
	}

	/**
	 * Parses and converts the target object into a <b>Long</b> object.<br>
	 * Returns {@code null} if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Long</b> value.
	 */
	public static Long toLong(Object sourceObject) {
		return toType(sourceObject, BigDecimal::longValue);
	}

	/**
	 * Parses and converts the target object into a <b>Long</b> object.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Long</b> value.
	 */
	public static Long toLongW0(Object sourceObject) {
		return toLong(wrap0(sourceObject));
	}

	/**
	 * Parses and converts the target object into a <b>Double</b> object.<br>
	 * Returns {@code null} if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Double</b> value.
	 */
	public static Double toDouble(Object sourceObject) {
		return toType(sourceObject, BigDecimal::doubleValue);
	}

	/**
	 * Parses and converts the target object into a <b>Double</b> object.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Converted <b>Double</b> value.
	 */
	public static Double toDoubleW0(Object sourceObject) {
		return toDouble(wrap0(sourceObject));
	}

	private static <Type> Type toType(Object sourceObject, Function<BigDecimal, Type> parser) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (decimal == null) {
			return null;
		}
		Type result = parser.apply(decimal);
		if (!haveSameValue(decimal, result)) {
			return null;
		}
		return result;
	}

	/**
	 * Parses and converts the target object into a plain <b>String</b> char sequence without any extra operation.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String stringify(Object sourceObject) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (decimal == null) {
			return BLANK;
		}
		return decimal.stripTrailingZeros().toPlainString();
	}

	/**
	 * Parses and wraps the target object into a plain <b>String</b> char sequence without any extra operation.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String stringifyW0(Object sourceObject) {
		return stringify(wrap0(sourceObject));
	}

	/**
	 * Parses and wraps the target object into a <b>String</b> char sequence with thousands separators.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String dress(Object sourceObject) {
		return format(sourceObject, FORMAT_COMMA_0);
	}

	/**
	 * Parses and wraps the target object into a <b>String</b> char sequence with thousands separators.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String dressW0(Object sourceObject) {
		return dress(wrap0(sourceObject));
	}

	/**
	 * Parses and wraps the target object into a <b>String</b> char sequence with thousands separators and 2 decimal places.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String dress2DP(Object sourceObject) {
		return format(sourceObject, FORMAT_COMMA_2);
	}

	/**
	 * Parses and wraps the target object into a <b>String</b> char sequence with thousands separators and 2 decimal places.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String dress2DPW0(Object sourceObject) {
		return dress2DP(wrap0(sourceObject));
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with the specified format.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed or formatted correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param formatPattern Target number format presented by a <b>String</b> char sequence.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String format(Object sourceObject, String formatPattern) {
		if (EmbeddedStringUtils.isEmpty(formatPattern)) {
			return null;
		}
		return format(sourceObject, new DecimalFormat(formatPattern));
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with the specified format.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param formatPattern Target number format presented by a <b>String</b> char sequence.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String formatW0(Object sourceObject, String formatPattern) {
		return format(wrap0(sourceObject), formatPattern);
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with the specified format.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed or formatted correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param format Target number format presented by a {@link DecimalFormat} object.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String format(Object sourceObject, DecimalFormat format) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (decimal == null) {
			return BLANK;
		}
		return format.format(decimal);
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with the specified format.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param format Target number format presented by a {@link DecimalFormat} object.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String formatW0(Object sourceObject, DecimalFormat format) {
		return format(wrap0(sourceObject), format);
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with percentage format.<br>
	 * Use negative decimal places or {@code null} to insert a single space between decimal value and percent sign.<br>
	 * Returns an empty <b>String</b> char sequence if the target object cannot be parsed or formatted correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing the source object.
	 * <pre><b><i>Eg.:</i></b>&#9;percent("A", -2) -> ""
	 * &#9;percent("0.25", -2) -> "25.00 %"
	 * &#9;percent("0.125", 0) -> "12%"
	 * &#9;percent("0.125", null) -> "12 %"</pre>
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param decimalPlace Number of decimal places to be retained.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String percent(Object sourceObject, Integer decimalPlace) {
		BigDecimal decimal = parseDecimal(sourceObject);
		if (decimal == null) {
			return BLANK;
		}
		int exactDecimalPlace = wrap0(decimalPlace).abs().intValue();
		decimal = decimal.multiply(PERCENT_MULTIPLICATOR).setScale(exactDecimalPlace, DEFAULT_ROUNDING_MODE);
		boolean hasSpace = decimalPlace == null || decimalPlace.compareTo(0) < 0;
		StringBuilder format = new StringBuilder("##,##0");
		for (int index = 0; index < exactDecimalPlace; index ++) {
			if (index == 0) {
				format.append(".");
			}
			format.append("0");
		}
		return new DecimalFormat(format.toString()).format(decimal) + (hasSpace ? " " : BLANK) + "%";
	}

	/**
	 * Parses and formats the target object into a <b>String</b> char sequence with percentage format.<br>
	 * Use negative decimal places or {@code null} to insert a single space between decimal value and percent sign.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing the source object.
	 * <pre><b><i>Eg.:</i></b>&#9;percentW0("A", -2) -> "0.00 %"
	 * &#9;percentW0("0.25", -2) -> "25.00 %"
	 * &#9;percentW0("0.125", 0) -> "12%"
	 * &#9;percentW0("0.125", null) -> "12 %"</pre>
	 * @param sourceObject Target object to be parsed and wrapped.
	 * @param decimalPlace Number of decimal places to be retained.
	 * @return Wrapped and formatted <b>String</b> char sequence.
	 */
	public static String percentW0(Object sourceObject, Integer decimalPlace) {
		return percent(wrap0(sourceObject), decimalPlace);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static scientific calculation

	/**
	 * Fetches the maximum value in the target decimals.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Returns {@code null} if there is no valid decimal.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObjects Target decimal objects to be compared.
	 * @return The maximum <b>BigDecimal</b> value.
	 */
	public static BigDecimal max(Object... sourceObjects) {
		return extremum(sourceObjects, 1);
	}

	/**
	 * Fetches the minimum value in the target decimals.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Returns {@code null} if there is no valid decimal.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObjects Target decimal objects to be compared.
	 * @return The minimum <b>BigDecimal</b> value.
	 */
	public static BigDecimal min(Object... sourceObjects) {
		return extremum(sourceObjects, -1);
	}

	private static BigDecimal extremum(Object[] sourceObjects, int direction) {
		BigDecimal result = null;
		for (Object sourceObject : sourceObjects) {
			BigDecimal candidate = parseDecimal(sourceObject);
			int compareResult = compare(candidate, result);
			if (compareResult == direction || compareResult == 2) {
				result = candidate;
			}
		}
		return result;
	}

	/**
	 * Calculates the average value of all target decimals.<br>
	 * Any argument whose parse result is {@code null} will be calculated as {@link BigDecimal#ZERO}.<br>
	 * Returns {@link BigDecimal#ZERO} if there is no valid decimal.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;average(15, "30", "45", "60") -> [37.5]
	 * &#9;average(15, "A30", "45", "60") -> [30]
	 * &#9;average(null, "A30", false, "") -> [0]</pre>
	 * @param sourceObjects Target decimal objects to be evaluated.
	 * @return Average <b>BigDecimal</b> value.
	 */
	public static BigDecimal average(Object... sourceObjects) {
		return quotient(sum(sourceObjects), sourceObjects.length, SCIENTIFIC_DECIMAL_SCALE).stripTrailingZeros();
	}

	/**
	 * Calculates the average value of all target decimals.<br>
	 * Any argument whose parse result is {@code null} will be ignored.<br>
	 * Returns {@link BigDecimal#ZERO} if there is no valid decimal.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * <pre><b><i>Eg.:</i></b>&#9;averageIgnoreNull(15, "30", "45", "60") -> [37.5]
	 * &#9;averageIgnoreNull(15, "A30", "45", "60") -> [40]
	 * &#9;averageIgnoreNull(null, "A30", false, "") -> [0]</pre>
	 * @param sourceObjects Target decimal objects to be evaluated.
	 * @return Average <b>BigDecimal</b> value.
	 */
	public static BigDecimal averageIgnoreNull(Object... sourceObjects) {
		List<Object> notNullParams = new ArrayList<>();
		for (Object sourceObject : sourceObjects) {
			BigDecimal decimal = parseDecimal(sourceObject);
			if (decimal != null) {
				notNullParams.add(decimal);
			}
		}
		return average(notNullParams.toArray());
	}

	/**
	 * Calculates the value of the first argument raised to the power of the second argument.<br>
	 * If the root's parse result is {@code null}, it will be calculated as {@link BigDecimal#ZERO}.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing the root object.
	 * @param rootObject Target decimal objects to be raised.
	 * @param exponential Number of times to be raised.
	 * @return Raised <b>BigDecimal</b> value.
	 */
	public static BigDecimal power(Object rootObject, int exponential) {
		BigDecimal decimal = wrap0(rootObject);
		if (isNullOr0(decimal)) {
			return ZERO;
		} else if (exponential == 0) {
			return ONE;
		}
		List<BigDecimal> multipierList = new ArrayList<>();
		for (int index = 0; index < Math.abs(exponential); index ++) {
			multipierList.add(decimal);
		}
		BigDecimal result = product(multipierList.toArray());
		if (exponential < 0) {
			result = quotient(1, result, SCIENTIFIC_DECIMAL_SCALE, HALF_UP);
		}
		return result.stripTrailingZeros();
	}

	/**
	 * Extracts the integral part of the target decimal along with its original sign.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal objects.
	 * @return <b>BigDecimal</b> value.
	 */
	public static BigDecimal getIntegralPart(Object sourceObject) {
		return wrap0(sourceObject).setScale(DEFAULT_SCALE, DOWN);
	}

	/**
	 * Extracts the fractional part of the target decimal along with its original sign.<br>
	 * Returns {@link BigDecimal#ZERO} if the target decimal has no fractional part.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal objects.
	 * @return <b>BigDecimal</b> value.
	 */
	public static BigDecimal getFractionalPart(Object sourceObject) {
		BigDecimal result = wrap0(parseDecimal(sourceObject)).subtract(getIntegralPart(sourceObject)).stripTrailingZeros();
		if (isSameDecimal(ZERO, result)) {
			result = result.setScale(1, HALF_UP);
		}
		return result;
	}

	/**
	 * Gets the char sequence length of the target decimal's integral part.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal objects.
	 * @return <b>int</b> length.
	 */
	public static int getIntegralLength(Object sourceObject) {
		return getIntegralPart(sourceObject).abs().toPlainString().length();
	}

	/**
	 * Gets the char sequence length of the target decimal's fractional part.<br>
	 * Obviously, the result will be {@code 0} if the target decimal has no fractional part.<br>
	 * Uses {@link BigDecimal#ZERO} as an alternative if the target object cannot be parsed correctly.<br>
	 * Uses {@link #parseDecimal(Object)} for automatic parsing.
	 * @param sourceObject Target decimal objects.
	 * @return <b>int</b> length.
	 */
	public static int getFractionalLength(Object sourceObject) {
		BigDecimal fractionalPartAbsoluteValue = getFractionalPart(sourceObject).abs().stripTrailingZeros();
		if (isSameDecimal(ZERO, fractionalPartAbsoluteValue)) {
			return 0;
		}
		return fractionalPartAbsoluteValue.toPlainString().length() - 2;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static extra practical calculation

	/**
	 * Converts Excel column number to column name.
	 * <pre><b><i>Eg.:</i></b>&#9;columnNoToName(1) -> "A"
	 * &#9;columnNoToName(50) -> "AX"</pre>
	 * @param source Target column number's <b>int</b> value.
	 * @return <b>String</b> column name char sequence.
	 */
	public static String columnNoToName(int source) {
		int length = 0, power = 1, prediction = 0;
		while (prediction < source) {
			power *= 26;
			prediction += power;
			length ++;
		}
		char[] name = new char[length];
		source -= prediction - power;
		source --;
		for (int index = 0; index < length; index ++) {
			power /= 26;
			name[index] = (char) (source / power + 'A');
			source %= power;
		}
		return String.valueOf(name);
	}

	/**
	 * Converts Excel column name to column number.
	 * <pre><b><i>Eg.:</i></b>&#9;columnNameToNo("A") -> 1
	 * &#9;columnNameToNo("AX") -> 50</pre>
	 * @param source Target column name's <b>String</b> char sequence.
	 * @return <b>int</b> column number value.
	 */
	public static int columnNameToNo(String source) {
		if (EmbeddedStringUtils.isEmpty(source)) {
			return 0;
		}
		int result = 0;
		for (int index = source.length() - 1, exponential = 1; index >= 0; index --, exponential ++) {
			char unidigit = source.charAt(index);
			if (!Character.isLetter(unidigit)) {
				return 0;
			}
			int unidigitValue = unidigit - 'A' + 1;
			for (int times = 1; times < exponential; times ++) {
				unidigitValue *= 26;
			}
			result += unidigitValue;
		}
		return result;
	}

	/**
	 * Extracts all valid decimals from the target <b>String</b> char sequence using {@link #parseDecimal(Object)}.<br>
	 * This method does not perform calculations, so only the last of plus-minus sign in a series has its effect.<br>
	 * Returns an empty array instead of {@code null} if no decimal is found in the source char sequence.
	 * <pre><b><i>Eg.:</i></b>&#9;blendExtract("12 A 45 6 D 7.1dd156") ->
	 * &#9;&#9;[[12], [45], [6], [7.1], [156]]
	 * &#9;blendExtract("-12+34.5- -6 ++7 -88") ->
	 * &#9;&#9;[[-12], [34.5], [-6], [7], [-88]]</pre>
	 * @param source Target <b>String</b> char sequence containing a series of decimals.
	 * @return An array of parsed <b>BigDecimal</b> values.
	 */
	public static BigDecimal[] blendExtract(String source) {
		List<BigDecimal> resultList = new ArrayList<>();
		if (source != null) {
			StringBuilder decimalBuilder = new StringBuilder();
			boolean negative = false;
			for (int index = 0; index < source.length(); index ++) {
				char character = source.charAt(index);
				boolean decimalEnd = index == source.length() - 1;
				Boolean nextNegative = null;
				if (Character.isDigit(character) || character == DOT) {
					decimalBuilder.append(character);
				} else if (character == PLUS || character == MINUS) {
					nextNegative = character == MINUS;
					if (decimalBuilder.length() != 0) {
						decimalEnd = true;
					}
				} else {
					decimalEnd = true;
				}
				if (decimalEnd && decimalBuilder.length() != 0) {
					BigDecimal result = parseDecimal(decimalBuilder.toString());
					if (negative) {
						result = minus(result);
					}
					resultList.add(result);
					decimalBuilder.setLength(0);
				}
				negative = nextNegative == null ? negative : nextNegative;
			}
		}
		return resultList.toArray(new BigDecimal[] {});
	}

	/**
	 * Parses the entire <b>String</b> char sequence into a mathematical expression using {@link #parseDecimal(Object)} and evaluates its result.<br>
	 * Supports addition, subtraction, multiplication, division, power operations, and can change the order of operations with parentheses.<br>
	 * Automatically ignore spaces and invalid characters, but only support expressions that strictly correspond to rules of operations.<br>
	 * Unclosed parentheses are automatically closed at either the beginning or the end of the expression.
	 * <pre><b><i>Eg.:</i></b>&#9;blendEvaluate("1 + 2 - 3 * 4^5 /6") -> [-509]
	 * &#9;blendEvaluate("(1+2- 3 * ((4+5)*1.2)^2") -> [-346.92]
	 * &#9;blendEvaluate("1+-2") -> null</pre>
	 * @param source Target <b>String</b> char sequence containing a complete calculation expression.
	 * @return Parsed <b>BigDecimal</b> calculation result.
	 */
	public static BigDecimal blendEvaluate(String source) {
		source = source.replaceAll("\\s+", BLANK);
		int parenthesisCount = 0;
		for (int index = 0; index < source.length(); index ++) {
			if (source.charAt(index) == LEFT_PARENTHESIS) {
				parenthesisCount ++;
			} else if (source.charAt(index) == RIGHT_PARENTHESIS) {
				parenthesisCount --;
			}
		}
		char parenthesis = parenthesisCount < 0 ? LEFT_PARENTHESIS : RIGHT_PARENTHESIS;
		StringBuilder appendedParenthesisBuilder = new StringBuilder();
		for (int index = 0; index < Math.abs(parenthesisCount); index ++) {
			appendedParenthesisBuilder.append(parenthesis);
		}
		source = parenthesis == LEFT_PARENTHESIS ? appendedParenthesisBuilder.toString() + source : source + appendedParenthesisBuilder.toString();
		Stack<BigDecimal> operandStack = new Stack<>();
		Stack<Character> operatorStack = new Stack<>();
		int index = 0;
		try {
			while (index < source.length()) {
				char character = source.charAt(index);
				if (Character.isDigit(character) || character == DOT) {
					StringBuilder decimalBuilder = new StringBuilder();
					while (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == DOT)) {
						decimalBuilder.append(source.charAt(index));
						index ++;
					}
					BigDecimal result = parseDecimal(decimalBuilder.toString());
					operandStack.push(result);
				} else if (character == LEFT_PARENTHESIS) {
					operatorStack.push(LEFT_PARENTHESIS);
					index ++;
				} else if (character == RIGHT_PARENTHESIS) {
					while (operatorStack.peek() != LEFT_PARENTHESIS) {
						BigDecimal result = OPERATOR_EVALUATION_MAP.get(operatorStack.pop()).apply(operandStack.pop(), operandStack.pop());
						operandStack.push(result);
					}
					operatorStack.pop();
					index ++;
				} else if (PRECEDENCE_MAP.containsKey(character)) {
					while (!operatorStack.isEmpty() && operatorStack.peek() != LEFT_PARENTHESIS &&
							(PRECEDENCE_MAP.get(operatorStack.peek()) >= PRECEDENCE_MAP.get(character))) {
						BigDecimal result = OPERATOR_EVALUATION_MAP.get(operatorStack.pop()).apply(operandStack.pop(), operandStack.pop());
						operandStack.push(result);
					}
					operatorStack.push(character);
					index ++;
				} else {
					index ++;
				}
			}
			while (!operatorStack.isEmpty()) {
				char operator = operatorStack.pop();
				if (operandStack.size() >= 2) {
					BigDecimal result = OPERATOR_EVALUATION_MAP.get(operator).apply(operandStack.pop(), operandStack.pop());
					operandStack.push(result);
				}
			}
			return operandStack.pop();
		} catch (EmptyStackException exception) {
			return null;
		}
	}

	/**
	 * Sums up a specified field of the objects in the target collection using {@link #parseDecimal(Object)}.
	 * @param <Type> The object type of the collection elements.
	 * @param objectCollection Target collection to be summed up.
	 * @param addendGetter A lambda expression for the method used to get the addend field.
	 * @return Summed <b>BigDecimal</b> result.
	 */
	public static <Type> BigDecimal collectionSum(Collection<Type> objectCollection, Function<Type, Object> addendGetter) {
		if (objectCollection == null || objectCollection.isEmpty()) {
			return null;
		}
		DecimalWrapper sum = new DecimalWrapper();
		objectCollection.forEach(object -> sum.add(addendGetter.apply(object)));
		return sum.value();
	}

	/**
	 * Sums up the specified fields of the objects in the target collection using {@link #parseDecimal(Object)}.<br>
	 * If no field name is specified, all calculatable fields are calculated.<br>
	 * Passing a field name that does not exist in the element type will throw a <b>NoSuchFieldException</b>.
	 * @param <Type> The object type of the collection elements.
	 * @param objectCollection Target collection to be summed up.
	 * @param specifiedFieldNames Names of target fields to be summed up.
	 * @return A sum object of the element type <b>Type</b>.
	 */
	public static <Type> Type collectionSum(Collection<Type> objectCollection, String... specifiedFieldNames) {
		if (objectCollection == null || objectCollection.isEmpty()) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Class<Type> typeClass = (Class<Type>) objectCollection.iterator().next().getClass();
		Type sumObject = EmbeddedReflectiveUtils.createInstance(typeClass);
		List<Field> fieldList = new ArrayList<>();
		Class<?> superTypeClass = typeClass;
		Set<Class<?>> supportedClassSet = WRAPPER_VALUE_GETTER_MAP.keySet();
		while (superTypeClass != null) {
			Field[] fields = superTypeClass.getDeclaredFields();
			List<Field> filteredFields = Arrays.stream(fields).filter(field -> supportedClassSet.contains(field.getType())).collect(Collectors.toList());
			fieldList.addAll(filteredFields);
			superTypeClass = superTypeClass.getSuperclass();
		}
		if (specifiedFieldNames.length > 0) {
			List<Field> finalFieldList = fieldList;
			Optional<String> exceptionFieldName = Arrays.stream(specifiedFieldNames).filter(fieldName ->
					finalFieldList.stream().noneMatch(field -> field.getName().endsWith(fieldName))).findFirst();
			if (exceptionFieldName.isPresent()) {
				throw new RuntimeException(new NoSuchFieldException(exceptionFieldName.get()));
			}
			fieldList = fieldList.stream().filter(field ->
					Arrays.stream(specifiedFieldNames).anyMatch(fieldName -> field.getName().endsWith(fieldName))).collect(Collectors.toList());
		}
		Map<String, DecimalWrapper> sumMap = new HashMap<>();
		fieldList.forEach(field -> sumMap.put(field.getName(), new DecimalWrapper(2, DEFAULT_ROUNDING_MODE)));
		for (Type data : objectCollection) {
			for (Field field : fieldList) {
				DecimalWrapper sum = sumMap.get(field.getName());
				Object value = EmbeddedReflectiveUtils.getField(data, field.getName(), Object.class);
				sum.add(value);
			}
		}
		for (Field field : fieldList) {
			DecimalWrapper sum = sumMap.get(field.getName());
			Object value = WRAPPER_VALUE_GETTER_MAP.get(field.getType()).apply(sum);
			EmbeddedReflectiveUtils.setField(sumObject, field.getName(), value);
		}
		return sumObject;
	}

	/**
	 * A wrapper class of <b>BigDecimal</b>.<br>
	 * Reserves decimal value, scale and rounding mode for calculation.<br>
	 * All calculation methods automatically ignore {@code null}.<br>
	 * All calculation methods return the instance itself, allowing for method chaining.<br>
	 * Has log recording function that you can look into during debugging.
	 */
	public static class DecimalWrapper {

		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance basic

		private BigDecimal value;
		private int scale;
		private RoundingMode roundingMode;
		private String roundingModeName;

		private final StringBuilder log = new StringBuilder();

		/**
		 * Creates a new instance for proceeding instance calculation.<br>
		 * With default scale of {@value #DEFAULT_SCALE}.<br>
		 * With default rounding mode of {@link RoundingMode#HALF_UP}.
		 */
		public DecimalWrapper() {
			this(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
		}

		/**
		 * Creates a new instance for proceeding instance calculation.
		 * @param scale Number of decimal places to be reserved for later calculation.
		 * @param roundingMode Rounding mode to be reserved for later calculation, using int value preset in <b>BigDecimal</b>.
		 */
		public DecimalWrapper(int scale, int roundingMode) {
			this(scale, valueOf(roundingMode));
		}

		/**
		 * Creates a new instance for proceeding instance calculation.
		 * @param scale Number of decimal places to be reserved for later calculation.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved for later calculation.
		 */
		public DecimalWrapper(int scale, RoundingMode roundingMode) {
			this.value = DEFAULT_VALUE;
			this.scale = scale;
			this.roundingMode = roundingMode;
			this.roundingModeName = ROUNDING_MODE_NAME.get(roundingMode);
			_log("initialized");
			_log("set value to default: " + DEFAULT_VALUE);
			_log(scale, roundingMode);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance batch operation

		/**
		 * Efficiently creates an array of <b>DecimalWrapper</b> objects with the specified size.
		 * @param arraySize Desired size of the resulting array.
		 * @return An array of <b>DecimalWrapper</b>.
		 */
		public static DecimalWrapper[] createArray(int arraySize) {
			DecimalWrapper[] resultArray = new DecimalWrapper[arraySize];
			for (int index = 0; index < arraySize; index ++) {
				resultArray[index] = new DecimalWrapper();
			}
			return resultArray;
		}

		/**
		 * Efficiently creates a <b>Map</b> of <b>DecimalWrapper</b> objects with the specified keys.
		 * @param keys Desired keys of the entries in resulting map.
		 * @return A <b>DecimalWrapper</b> <b>Map</b>.
		 */
		public static Map<String, DecimalWrapper> createMap(String... keys) {
			Map<String, DecimalWrapper> resultMap = new HashMap<>();
			for (String key : keys) {
				resultMap.put(key, new DecimalWrapper());
			}
			return resultMap;
		}

		/**
		 * Efficiently resets all <b>DecimalWrapper</b> values and logs in the target array.
		 * @param targetArray Target array of <b>DecimalWrapper</b> to be cleared.
		 */
		public static void clearArray(DecimalWrapper[] targetArray) {
			for (DecimalWrapper wrapper : targetArray) {
				wrapper.clear();
			}
		}

		/**
		 * Efficiently resets all <b>DecimalWrapper</b> values and logs in the target map.
		 * @param targetMap Target <b>DecimalWrapper</b> <b>Map</b> to be cleared.
		 */
		public static void clearMap(Map<String, DecimalWrapper> targetMap) {
			for (DecimalWrapper wrapper : targetMap.values()) {
				wrapper.clear();
			}
		}

		/**
		 * Transfers the <b>DecimalWrapper</b> value from one to another element of the same array.
		 * @param array Target array of <b>DecimalWrapper</b>.
		 * @param targetIndex Index of the <b>DecimalWrapper</b> to transfer from.
		 * @param destinationIndex Index of the <b>DecimalWrapper</b> to transfer to.
		 */
		public static void transferValue(DecimalWrapper[] array, int targetIndex, int destinationIndex) {
			array[destinationIndex].add(array[targetIndex]);
			array[targetIndex].clear();
		}

		/**
		 * Transfers the <b>DecimalWrapper</b> value from one to another entry of the same <b>Map</b>.
		 * @param map Target <b>Map</b> with <b>DecimalWrapper</b> as its parameterized value type.
		 * @param targetKey Key of the <b>DecimalWrapper</b>'s entry to transfer from.
		 * @param destinationKey Key of the <b>DecimalWrapper</b>'s entry to transfer to.
		 */
		public static void transferValue(Map<String, DecimalWrapper> map, String targetKey, String destinationKey) {
			map.get(destinationKey).add(map.get(targetKey));
			map.get(targetKey).clear();
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance calculation

		/**
		 * Resets reserved values to default settings, and deletes all logs.
		 */
		public DecimalWrapper clear() {
			this.value = DEFAULT_VALUE;
			this.log.delete(0, this.log.length());
			_log("(re)set value to default: " + DEFAULT_VALUE);
			return this;
		}

		/**
		 * Reverses the sign of the reserved value to its opposite.
		 * @see DecimalUtils#minus(Object)
		 */
		public DecimalWrapper negate() {
			this.value = minus(this.value);
			_log("negate", new Object[] {});
			return this;
		}

		/**
		 * Makes the reserved value positive.
		 * @see DecimalUtils#absolute(Object)
		 */
		public DecimalWrapper absolute() {
			this.value = DecimalUtils.absolute(this.value);
			_log("absolute", new Object[] {});
			return this;
		}

		/**
		 * Adds all the target decimals to the reserved value.
		 * @param addendObjects Target decimal objects to be added.
		 * @see DecimalUtils#sum(Object...)
		 */
		public DecimalWrapper add(Object... addendObjects) {
			this.value = sum(this.value, sumReserveNull(addendObjects));
			_log("add", addendObjects);
			return this;
		}

		/**
		 * Subtracts all the target decimals from the reserved value.
		 * @param subtrahendObjects Target decimal objects to be subtracted.
		 * @see DecimalUtils#sum(Object...)
		 * @see DecimalUtils#minus(Object)
		 */
		public DecimalWrapper subtract(Object... subtrahendObjects) {
			this.value = sum(this.value, minus(sumReserveNull(subtrahendObjects)));
			_log("subtract", subtrahendObjects);
			return this;
		}

		/**
		 * Multiplies all the target decimals to the reserved value.
		 * @param multiplierObjects Target decimal objects to be multiplied.
		 * @see DecimalUtils#product(Object...)
		 */
		public DecimalWrapper multiply(Object... multiplierObjects) {
			this.value = product(this.value, productReserveNull(multiplierObjects));
			_log("multiply", multiplierObjects);
			return this;
		}

		/**
		 * Multiplies all the target decimals to the reserved value, and divides by {@code 100} as one of them is a percentage.
		 * @param multiplierObjects Target decimal objects to be multiplied.
		 * @see DecimalUtils#productDepercent(Object...)
		 */
		public DecimalWrapper multiplyDepercent(Object... multiplierObjects) {
			this.value = productDepercent(this.value, productReserveNull(multiplierObjects));
			_log("multiply into depercent", multiplierObjects);
			return this;
		}

		/**
		 * Divides the reserved value by the target decimal.<br>
		 * Uses reserved scale and rounding mode.
		 * @param divisorObject Divisor decimal object.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divide(Object divisorObject) {
			this.value = quotient(this.value, divisorObject, this.scale, this.roundingMode);
			_log("divide", divisorObject);
			return this;
		}

		/**
		 * Divides the reserved value by the target decimal.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param divisorObject Divisor decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode to be reserved using int value preset in <b>BigDecimal</b>.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divide(Object divisorObject, int scale, int roundingMode) {
			return divide(divisorObject, scale, valueOf(roundingMode));
		}

		/**
		 * Divides the reserved value by the target decimal.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param divisorObject Divisor decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divide(Object divisorObject, int scale, RoundingMode roundingMode) {
			setScaleCore(scale, roundingMode);
			_log(scale, roundingMode);
			return this.divide(divisorObject);
		}

		/**
		 * Divides the reserved value by the target decimal, and then multiplies by {@code 100} to get a percentage.<br>
		 * Uses reserved scale and rounding mode.
		 * @param divisorObject Divisor decimal object.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper dividePercent(Object divisorObject) {
			this.value = quotientPercent(this.value, divisorObject, this.scale, this.roundingMode);
			_log("divide into percent", divisorObject);
			return this;
		}

		/**
		 * Divides the reserved value by the target decimal, and then multiplies by {@code 100} to get a percentage.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param divisorObject Divisor decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode to be reserved using int value preset in <b>BigDecimal</b>.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper dividePercent(Object divisorObject, int scale, int roundingMode) {
			return dividePercent(divisorObject, scale, valueOf(roundingMode));
		}

		/**
		 * Divides the reserved value by the target decimal, and then multiplies by {@code 100} to get a percentage.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param divisorObject Divisor decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper dividePercent(Object divisorObject, int scale, RoundingMode roundingMode) {
			setScaleCore(scale, roundingMode);
			_log(scale, roundingMode);
			return this.dividePercent(divisorObject);
		}

		/**
		 * Divides the target decimal by the reserved value.<br>
		 * Uses reserved scale and rounding mode.
		 * @param dividendObject Dividend decimal object.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisor(Object dividendObject) {
			this.value = quotient(dividendObject, this.value, this.scale, this.roundingMode);
			_log("divide as divisor", dividendObject);
			return this;
		}

		/**
		 * Divides the target decimal by the reserved value.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param dividendObject Dividend decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode to be reserved using int value preset in <b>BigDecimal</b>.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisor(Object dividendObject, int scale, int roundingMode) {
			return divideAsDivisor(dividendObject, scale, valueOf(roundingMode));
		}

		/**
		 * Divides the target decimal by the reserved value.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param dividendObject Dividend decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisor(Object dividendObject, int scale, RoundingMode roundingMode) {
			setScaleCore(scale, roundingMode);
			_log(scale, roundingMode);
			return divideAsDivisor(dividendObject);
		}

		/**
		 * Divides the target decimal by the reserved value, and then multiplies by {@code 100} to get a percentage.<br>
		 * Uses reserved scale and rounding mode.
		 * @param dividendObject Dividend decimal object.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisorPercent(Object dividendObject) {
			this.value = quotientPercent(dividendObject, this.value, this.scale, this.roundingMode);
			_log("divide as divisor into percent", dividendObject);
			return this;
		}

		/**
		 * Divides the target decimal by the reserved value, and then multiplies by {@code 100} to get a percentage.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param dividendObject Dividend decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode to be reserved using int value preset in <b>BigDecimal</b>.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisorPercent(Object dividendObject, int scale, int roundingMode) {
			return divideAsDivisorPercent(dividendObject, scale, valueOf(roundingMode));
		}

		/**
		 * Divides the target decimal by the reserved value, and then multiplies by {@code 100} to get a percentage.<br>
		 * Accepts the new scale and rounding mode for current and later calculation.
		 * @param dividendObject Dividend decimal object.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved.
		 * @see DecimalUtils#quotient(Object, Object, int, RoundingMode)
		 */
		public DecimalWrapper divideAsDivisorPercent(Object dividendObject, int scale, RoundingMode roundingMode) {
			setScaleCore(scale, roundingMode);
			_log(scale, roundingMode);
			return divideAsDivisorPercent(dividendObject);
		}

		/**
		 * Divides the reserved value by the target decimal to reserve the remainder.
		 * @param divisorObject Divisor decimal object.
		 * @see DecimalUtils#mod(Object, Object)
		 */
		public DecimalWrapper mod(Object divisorObject) {
			this.value = DecimalUtils.mod(this.value, divisorObject);
			_log("mod", divisorObject);
			return this;
		}

		/**
		 * Divides the target decimal by the reserved value to reserve the remainder.
		 * @param dividendObject Divisor decimal object.
		 * @see DecimalUtils#mod(Object, Object)
		 */
		public DecimalWrapper modAsDivisor(Object dividendObject) {
			this.value = DecimalUtils.mod(dividendObject, this.value);
			_log("mod", dividendObject);
			return this;
		}

		/**
		 * Accepts new scale for later calculation.
		 * @param scale Number of decimal places to be reserved.
		 * @see DecimalUtils#setScale(Object, int, RoundingMode)
		 */
		public DecimalWrapper setScale(int scale) {
			setScaleCore(scale, this.roundingMode);
			this.value = DecimalUtils.setScale(this.value, scale, this.roundingMode);
			_log(scale, this.roundingMode);
			return this;
		}

		/**
		 * Accepts the new scale and rounding mode for later calculation.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode to be reserved using int value preset in <b>BigDecimal</b>.
		 * @see DecimalUtils#setScale(Object, int)
		 */
		public DecimalWrapper setScale(int scale, int roundingMode) {
			return setScale(scale, valueOf(roundingMode));
		}

		/**
		 * Accepts the new scale and rounding mode for later calculation.
		 * @param scale Number of decimal places to be reserved.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved.
		 * @see DecimalUtils#setScale(Object, int, RoundingMode)
		 */
		public DecimalWrapper setScale(int scale, RoundingMode roundingMode) {
			setScaleCore(scale, roundingMode);
			this.value = DecimalUtils.setScale(this.value, scale, roundingMode);
			_log(scale, roundingMode);
			return this;
		}

		private void setScaleCore(int scale, RoundingMode roundingMode) {
			this.scale = scale;
			this.roundingMode = roundingMode;
			this.roundingModeName = ROUNDING_MODE_NAME.get(roundingMode);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance comparing

		/**
		 * Evaluates if the reserved value is equal to the target decimal.
		 * @param comparandObject Target decimal object to be compared.
		 * @return {@code true} if equal.
		 * @see DecimalUtils#compare(Object, Object)
		 */
		public boolean isEquivalentTo(Object comparandObject) {
			return compareW0(this.value, comparandObject) == 0;
		}

		/**
		 * Evaluates if the reserved value is greater than the target decimal.
		 * @param comparandObject Target decimal object to be compared.
		 * @return {@code true} if greater than.
		 * @see DecimalUtils#compare(Object, Object)
		 */
		public boolean isGreaterThan(Object comparandObject) {
			return compareW0(this.value, comparandObject) == 1;
		}

		/**
		 * Evaluates if the reserved value is greater than or equal to the target decimal.
		 * @param comparandObject Target decimal object to be compared.
		 * @return {@code true} if not less than.
		 * @see DecimalUtils#compare(Object, Object)
		 */
		public boolean isGreaterEqual(Object comparandObject) {
			int compareResult = compareW0(this.value, comparandObject);
			return compareResult == 0 || compareResult == 1;
		}

		/**
		 * Evaluates if the reserved value is less than the target decimal.
		 * @param comparandObject Target decimal object to be compared.
		 * @return {@code true} if less than.
		 * @see DecimalUtils#compare(Object, Object)
		 */
		public boolean isLessThan(Object comparandObject) {
			return compareW0(this.value, comparandObject) == -1;
		}

		/**
		 * Evaluates if the reserved value is less than or equal to the target decimal.
		 * @param comparandObject Target decimal object to be compared.
		 * @return {@code true} if not greater than.
		 * @see DecimalUtils#compare(Object, Object)
		 */
		public boolean isLessEqual(Object comparandObject) {
			int compareResult = compareW0(this.value, comparandObject);
			return compareResult == 0 || compareResult == -1;
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance output

		/**
		 * Gets the reserved value.
		 * @return <b>BigDecimal</b> value.
		 */
		public BigDecimal value() {
			return this.value;
		}

		/**
		 * Extracts the integral part of the reserved value.
		 * @return <b>BigDecimal</b> value.
		 * @see DecimalUtils#getIntegralPart(Object)
		 */
		public BigDecimal integralPart() {
			return getIntegralPart(this.value);
		}

		/**
		 * Extracts the fractional part of the reserved value.
		 * @return <b>BigDecimal</b> value.
		 * @see DecimalUtils#getFractionalPart(Object)
		 */
		public BigDecimal fractionalPart() {
			return getFractionalPart(this.value);
		}

		/**
		 * Wraps the reserved value into an <b>Integer</b> object.
		 * @return Wrapped <b>Integer</b> value.
		 * @see DecimalUtils#toInteger(Object)
		 */
		public Integer integerValue() {
			return toInteger(this.value);
		}

		/**
		 * Wraps the reserved value into a <b>Long</b> object.
		 * @return Wrapped <b>Long</b> value.
		 * @see DecimalUtils#toLong(Object)
		 */
		public Long longValue() {
			return toLong(this.value);
		}

		/**
		 * Wraps the reserved value into a <b>Double</b> object.
		 * @return Wrapped <b>Double</b> value.
		 * @see DecimalUtils#toDouble(Object)
		 */
		public Double doubleValue() {
			return toDouble(this.value);
		}

		/**
		 * Wraps the reserved value into a plain <b>String</b> char sequence without any extra operation.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#stringify(Object)
		 */
		public String stringify() {
			return DecimalUtils.stringify(this.value);
		}

		/**
		 * Wraps the reserved value into a <b>String</b> char sequence with thousands separators.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#dress(Object)
		 */
		public String dress() {
			return DecimalUtils.dress(this.value);
		}

		/**
		 * Wraps the reserved value into a <b>String</b> char sequence with thousands separators and 2 decimal places.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#dress2DP(Object)
		 */
		public String dress2DP() {
			return DecimalUtils.dress2DP(this.value);
		}

		/**
		 * Formats the reserved value into a <b>String</b> char sequence with the specified format.<br>
		 * Returns an empty <b>String</b> char sequence if the reserved value cannot be formatted correctly.
		 * @param formatPattern Target number format presented by a <b>String</b> char sequence.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#format(Object, String)
		 */
		public String format(String formatPattern) {
			return DecimalUtils.format(this.value, formatPattern);
		}

		/**
		 * Formats the reserved value into a <b>String</b> char sequence with the specified format.<br>
		 * Returns an empty <b>String</b> char sequence if the reserved value cannot be formatted correctly.
		 * @param format Target number format presented by a {@link DecimalFormat} object.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#format(Object, String)
		 */
		public String format(DecimalFormat format) {
			return DecimalUtils.format(this.value, format);
		}

		/**
		 * Formats the reserved value into a <b>String</b> char sequence with pecentage format.<br>
		 * Returns an empty <b>String</b> char sequence if the reserved value cannot be formatted correctly.
		 * @param decimalPlace Number of decimal places to be retained.
		 * @return Wrapped and formatted <b>String</b> char sequence.
		 * @see DecimalUtils#percent(Object, Integer)
		 */
		public String percent(Integer decimalPlace) {
			return DecimalUtils.percent(this.value, decimalPlace);
		}

		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// instance log

		private void _log(String command) {
			this.log.append(command);
			this.log.append("\n");
		}

		private void _log(String command, Object... params) {
			boolean isMultipleParams = params.length > 1;
			this.log.append(command).append(" ").append(isMultipleParams ? "(" : "");
			for (int index = 0; index < params.length; index ++) {
				BigDecimal param = parseDecimal(params[index]);
				this.log.append(param != null ? param.toPlainString() : "null");
				if (index < params.length - 1) {
					this.log.append(", ");
				}
			}
			this.log.append(isMultipleParams ? ")" : "");
			if (command.contains("divide")) {
				this.log.append(" (").append(this.scale).append(", ").append(this.roundingModeName).append(")");
			}
			this.log.append(", current value: ").append(this.value.toPlainString());
			this.log.append("\n");
		}

		private void _log(int scale, RoundingMode roundingMode) {
			this.log.append("set scale: ").append(scale).append(", roundingMode: ").append(ROUNDING_MODE_NAME.get(roundingMode));
			this.log.append("\n");
		}

		/**
		 * Output logs.
		 * @return <b>String</b> logs.
		 */
		public String getLog() {
			return this.log.toString();
		}

	}

	/**
	 * A Wrapper class of <b>BigDecimal</b> for tax calculation.<br>
	 * Reserves scale and rounding mode for calculation and calculating results in the sequence of
	 * <b>[</b><b>payAmount</b>, <b>taxAmount</b>, <b>allAmount</b><b>]</b>.<br>
	 * The scale setting is used for amount calculating and then tax calculating.<br>
	 * All calculation methods return the instance itself, allowing for method chaining.
	 */
	public static class TaxWrapper {

		private final int scale;
		private final RoundingMode roundingMode;
		private final DecimalWrapper payAmount;
		private final DecimalWrapper taxAmount;
		private final DecimalWrapper allAmount;

		/**
		 * Creates a new instance for proceeding tax calculation.<br>
		 * With default scale of {@value #DEFAULT_SCALE}.<br>
		 * With default rounding mode of {@link RoundingMode#HALF_UP}.
		 */
		public TaxWrapper() {
			this(DEFAULT_SCALE, DEFAULT_ROUNDING_MODE);
		}

		/**
		 * Creates a new instance for proceeding tax calculation.
		 * @param scale Number of decimal places to be reserved for later calculation.
		 * @param roundingMode Rounding mode to be reserved for later calculation, using int value preset in <b>BigDecimal</b>.
		 */
		public TaxWrapper(int scale, int roundingMode) {
			this(scale, valueOf(roundingMode));
		}

		/**
		 * Creates a new instance for proceeding tax calculation.
		 * @param scale Number of decimal places to be reserved for later calculation.
		 * @param roundingMode Rounding mode presented by enum of {@link RoundingMode} to be reserved for later calculation.
		 */
		public TaxWrapper(int scale, RoundingMode roundingMode) {
			this.scale = scale;
			this.roundingMode = roundingMode;
			this.payAmount = new DecimalWrapper(this.scale, this.roundingMode);
			this.taxAmount = new DecimalWrapper(this.scale, this.roundingMode);
			this.allAmount = new DecimalWrapper(this.scale, this.roundingMode);
		}

		/**
		 * Gets the calculated and summed pay amount value.
		 * @return <b>DecimalWrapper</b> value.
		 */
		public DecimalWrapper getPayAmount() {
			return this.payAmount;
		}

		/**
		 * Gets the calculated and summed tax amount value.
		 * @return <b>DecimalWrapper</b> value.
		 */
		public DecimalWrapper getTaxAmount() {
			return this.taxAmount;
		}

		/**
		 * Gets the calculated and summed all amount value.
		 * @return <b>DecimalWrapper</b> value.
		 */
		public DecimalWrapper getAllAmount() {
			return this.allAmount;
		}

		/**
		 * Calculates in tax according to the specified count, unit price and tax rate.<br>
		 * Uses {@link #parseDecimal(Object)} for automatic parsing.
		 * <pre><b><i>Eg.:</i></b>&#9;addInTax("100", "10", "0.1") [scale = 0, roundingMode = HALF_UP] ->
		 * &#9;&#9;[payAmount = 909, taxAmount = 91, allAmount = 1000]</pre>
		 * @param unitPriceObject Target object contains unit price information.
		 * @param countObject Target object contains count information.
		 * @param taxRateObject Target object contains tax rate information in percent format.
		 */
		public TaxWrapper addInTax(Object unitPriceObject, Object countObject, Object taxRateObject) {
			return addInTax(calculateAmount(unitPriceObject, countObject), taxRateObject);
		}

		/**
		 * Calculates in tax according to the specified count, unit price and tax rate.<br>
		 * Uses {@link #parseDecimal(Object)} for automatic parsing.
		 * <pre><b><i>Eg.:</i></b>&#9;addInTax("1000", "0.1") [scale = 0, roundingMode = HALF_UP] ->
		 * &#9;&#9;[payAmount = 909, taxAmount = 91, allAmount = 1000]</pre>
		 * @param amountObject Target object contains amount information.
		 * @param taxRateObject Target object contains tax rate information in percent format.
		 */
		public TaxWrapper addInTax(Object amountObject, Object taxRateObject) {
			this.allAmount.add(parseDecimal(amountObject));
			this.taxAmount.add(calculateTaxAmount(this.allAmount.value(), taxRateObject, true));
			this.payAmount.add(this.allAmount.value()).subtract(this.taxAmount.value());
			return this;
		}

		/**
		 * Calculates out tax according to the specified count, unit price and tax rate.<br>
		 * Uses {@link #parseDecimal(Object)} for automatic parsing.
		 * <pre><b><i>Eg.:</i></b>&#9;addOutTax("100", "10", "0.1") [scale = 0, roundingMode = HALF_UP] ->
		 * &#9;&#9;[payAmount = 1000, taxAmount = 100, allAmount = 1100]</pre>
		 * @param unitPriceObject Target object contains unit price information.
		 * @param countObject Target object contains count information.
		 * @param taxRateObject Target object contains tax rate information in percent format.
		 */
		public TaxWrapper addOutTax(Object unitPriceObject, Object countObject, Object taxRateObject) {
			return addOutTax(calculateAmount(unitPriceObject, countObject), taxRateObject);
		}

		/**
		 * Calculates out tax according to the specified count, unit price and tax rate.<br>
		 * Uses {@link #parseDecimal(Object)} for automatic parsing.
		 * <pre><b><i>Eg.:</i></b>&#9;addOutTax("1000", "0.1") [scale = 0, roundingMode = HALF_UP] ->
		 * &#9;&#9;[payAmount = 1000, taxAmount = 100, allAmount = 1100]</pre>
		 * @param amountObject Target object contains amount information.
		 * @param taxRateObject Target object contains tax rate information in percent format.
		 */
		public TaxWrapper addOutTax(Object amountObject, Object taxRateObject) {
			this.payAmount.add(parseDecimal(amountObject));
			this.taxAmount.add(calculateTaxAmount(this.payAmount.value(), taxRateObject, false));
			this.allAmount.add(this.payAmount.value(), this.taxAmount.value());
			return this;
		}

		private BigDecimal calculateAmount(Object unitPriceObject, Object countObject) {
			BigDecimal result = ZERO;
			if (!isUnusableOr0(unitPriceObject) && !isUnusableOr0(countObject)) {
				result = setScale(product(unitPriceObject, countObject), this.scale, this.roundingMode);
			}
			return result;
		}

		private BigDecimal calculateTaxAmount(BigDecimal amount, Object taxRateObject, boolean inTax) {
			BigDecimal result = ZERO;
			if (!isNullOr0(amount) && !isUnusableOr0(taxRateObject)) {
				if (inTax) {
					result = quotient(product(amount, taxRateObject), sum(PERCENT_ADDEND, taxRateObject), this.scale, this.roundingMode);
				} else {
					result = quotient(product(amount, taxRateObject), PERCENT_ADDEND, this.scale, this.roundingMode);
				}
			}
			return result;
		}

	}

	/**
	 * A wrapper class of <b>&lt;Type&gt; Map&lt;Object, Type&gt;</b>.<br>
	 * Used in {@link #select(Object, DecimalOption)} to quickly create an option scope.
	 * <pre><b><i>Eg.:</i></b>&#9;DecimalOption&lt;String&gt; scope = DecimalOption.build(10, "a").and("20", "b"));
	 * &#9;&#9; -> [[10, "a"], [20, "b"]]
	 * &#9;DecimalOption&lt;Integer&gt; scope = DecimalOption.build("1", 1).and("2", 2));
	 * &#9;&#9; -> [[1, 1], [2, 2]]</pre>
	 */
	public static class DecimalOption<Type> {

		private final Map<Object, Type> optionMap;

		private DecimalOption() {
			this.optionMap = new HashMap<>();
		}

		/**
		 * Builds an option scope and assigns the first entry.<br>
		 * @param <Type> The object type of the value.
		 * @param keyObject Option key Object.
		 * @param value Option value.
		 */
		public static <Type> DecimalOption<Type> build(Object keyObject, Type value) {
			DecimalOption<Type> decimalOption = new DecimalOption<>();
			decimalOption.and(keyObject, value);
			return decimalOption;
		}

		/**
		 * Assigns a new entry.<br>
		 * @param keyObject Option key Object.
		 * @param value Option value.
		 */
		public DecimalOption<Type> and(Object keyObject, Type value) {
			this.optionMap.put(keyObject, value);
			return this;
		}

	}

	/**
	 * An instance class for managing multiple option components with one <b>int</b> unique combined id.
	 */
	public static class CombinedIdManager implements Serializable {

		private final int combinationCount;
		private final List<Map<Integer, Object>> componentContainer;
		private final List<BiPredicate<Object, Object>> comparatorContainer;

		/**
		 * Prepares all combinations and comparators.
		 * @param componentCollections All component collections.
		 */
		public CombinedIdManager(Collection<?>... componentCollections) {
			notEmptyCheck(componentCollections);
			if (Arrays.stream(componentCollections).anyMatch(componentCollection -> componentCollection == null || componentCollection.isEmpty())) {
				throw new IllegalArgumentException("Empty component collection is not permitted");
			}
			combinationCount = Arrays.stream(componentCollections).map(Collection::size).reduce(1, (product, element) -> product * element);
			componentContainer = new ArrayList<>();
			comparatorContainer = new ArrayList<>();
			BiPredicate<Object, Object> defaultComparator = nullConsideredComparator(null);
			for (int count = 0; count < componentCollections.length; count ++) {
				List<?> componentList = new ArrayList<>(componentCollections[count]);
				int multiplier = IntStream.range(0, count).reduce(1, (product, element) -> product * componentCollections[element].size());
				Map<Integer, Object> componentMap = IntStream.range(0, componentList.size()).boxed()
						.collect(Collectors.toMap(index -> index * multiplier, componentList::get));
				componentContainer.add(componentMap);
				comparatorContainer.add(defaultComparator);
			}
		}

		/**
		 * Pushes an equality comparator for the component for the specified index.<br>
		 * If a component's equality comparator is not pushed, it will use {@code equals()} by default.
		 * <pre><b><i>Eg.:</i></b>&#9;Object[] cA = {0, 1, 2, 3};
		 * &#9;Object[] cB = {0, 4, 8, 12};
		 * &#9;Object[] cC = {0, 16, 32, 48};
		 * &#9;CombinedIdManager manager = new CombinedIdManager(cA, cB, cC);
		 * &#9;manager.pushComparator(1, int.class, (x, y) -> x == (-y));
		 * &#9;int combinedId = manager.encode(1, -4, 32); -> 37</pre>
		 * @param <ObjectType> The class of the specified component.
		 * @param componentTypeIndex The index of component to be push equality comparator.
		 * 		This index is same as the index of component arguments when you created the manager instance.
		 * @param objectClass The class object of the specified component.
		 * 		You can write proper code in comparator only when use a correct <b>objectClass</b>.
		 * @param comparator Assigned comparator.
		 */
		@SuppressWarnings("unchecked")
		public <ObjectType> void pushComparator(int componentTypeIndex, Class<ObjectType> objectClass, BiPredicate<ObjectType, ObjectType> comparator) {
			if (componentTypeIndex < 0 || componentTypeIndex >= componentContainer.size()) {
				throw new IllegalArgumentException("Component type index doesn't exist");
			} else if (!EmbeddedReflectiveUtils.matchType(componentContainer.get(componentTypeIndex).values().iterator().next(), objectClass)) {
				throw new IllegalArgumentException("Comparator class doesn't match component class");
			}
			comparatorContainer.set(componentTypeIndex, (BiPredicate<Object, Object>) nullConsideredComparator(comparator));
		}

		/**
		 * Encodes unique combined id according to the indexes of the selected component options.
		 * This method will be invalid if you created manager with unordered component collections.
		 * <pre><b><i>Eg.:</i></b>&#9;Object[] cA = {0, 1, 2, 3};
		 * &#9;Object[] cB = {0, 4, 8, 12};
		 * &#9;Object[] cC = {0, 16, 32, 48};
		 * &#9;CombinedIdManager manager = new CombinedIdManager(cA, cB, cC);
		 * &#9;int combinedId = manager.encode(1, 1, 2); -> 37</pre>
		 * @param selectedIndexes The indexes of the selected component options in every component.
		 * 		This index is same as the index of the options in every component when you created the manager instance.
		 * 		The index count must match component type count.
		 * @return <b>int</b> Encoded unique combined id.
		 */
		public int encodeWithIndex(int... selectedIndexes) {
			notEmptyCheck(selectedIndexes);
			if (selectedIndexes.length != componentContainer.size()) {
				throw new IllegalArgumentException("Component count doesn't match component type count");
			}
			int combinedId = 0;
			for (int iteratingIndex = 0; iteratingIndex < selectedIndexes.length; iteratingIndex ++) {
				int index = selectedIndexes[iteratingIndex];
				Set<Integer> componentIdSet = componentContainer.get(iteratingIndex).keySet();
				if (index < 0 || index > componentIdSet.size()) {
					throw new IllegalArgumentException("Index " + index + "(" + iteratingIndex + ") doesn't exist");
				}
				int selectedId = componentIdSet.stream().sorted().skip(index).findFirst().orElse(0);
				combinedId += selectedId;
			}
			return combinedId;
		}

		/**
		 * Encodes unique combined id according to the selected component options.
		 * <pre><b><i>Eg.:</i></b>&#9;Object[] cA = {0, 1, 2, 3};
		 * &#9;Object[] cB = {0, 4, 8, 12};
		 * &#9;Object[] cC = {0, 16, 32, 48};
		 * &#9;CombinedIdManager manager = new CombinedIdManager(cA, cB, cC);
		 * &#9;int combinedId = manager.encode(1, 4, 32); -> 37</pre>
		 * @param selectedComponents The selected component options in every component.
		 * 		If the selected component option count is less than component type count, then the rest will be 0.
		 * @return <b>int</b> Encoded unique combined id.
		 */
		public int encode(Object... selectedComponents) {
			notEmptyCheck(selectedComponents);
			if (selectedComponents.length > componentContainer.size()) {
				throw new IllegalArgumentException("Component count exceeds component type count");
			}
			int combinedId = 0;
			for (int index = 0; index < componentContainer.size(); index ++) {
				Object selectedComponent = selectedComponents[index];
				if (selectedComponent != null) {
					Map<Integer, Object> componentMap = componentContainer.get(index);
					BiPredicate<Object, Object> comparator = comparatorContainer.get(index);
					final int finalIndex = index;
					int selectedId = componentMap.entrySet().stream().filter(entry -> comparator.test(entry.getValue(), selectedComponent))
							.findFirst().orElseThrow(() -> new IllegalArgumentException("Component " + finalIndex + " doesn't exist")).getKey();
					combinedId += selectedId;
				}
			}
			return combinedId;
		}

		/**
		 * Decodes unique combined id to fetch the selected component options.
		 * <pre><b><i>Eg.:</i></b>&#9;Object[] cA = {0, 1, 2, 3};
		 * &#9;Object[] cB = {0, 4, 8, 12};
		 * &#9;Object[] cC = {0, 16, 32, 48};
		 * &#9;CombinedIdManager manager = new CombinedIdManager(cA, cB, cC);
		 * &#9;Object[] selectedComponents = manager.decode(35); -> [3, 0, 32]</pre>
		 * @param combinedId Unique combined id.
		 * @return The selected component options in every component.
		 */
		public Object[] decode(int combinedId) {
			if (combinedId < 0) {
				throw new IllegalArgumentException("CombinedId should not be less than 0");
			} else if (combinedId > combinationCount) {
				throw new IllegalArgumentException("CombinedId should not be greater than component count: " + combinationCount);
			}
			List<Object> selectedComponents = new ArrayList<>();
			for (int index = componentContainer.size() - 1; index >= 0; index --) {
				Map<Integer, Object> componentMap = componentContainer.get(index);
				final int remainingNumeralId = combinedId;
				int selectedId = componentMap.keySet().stream().filter(componentCode -> componentCode <= remainingNumeralId)
						.max(Comparator.comparing(Integer::intValue)).orElse(0);
				selectedComponents.add(componentMap.get(selectedId));
				combinedId -= selectedId;
			}
			Collections.reverse(selectedComponents);
			return selectedComponents.toArray();
		}

		private void notEmptyCheck(int[] arguments) {
			if (arguments == null || arguments.length == 0) {
				throw new IllegalArgumentException("Invoking with no argument is not permitted");
			}
		}

		private void notEmptyCheck(Object[] arguments) {
			if (arguments == null || arguments.length == 0) {
				throw new IllegalArgumentException("Invoking with no argument is not permitted");
			}
		}

		private <ObjectType> BiPredicate<ObjectType, ObjectType> nullConsideredComparator(BiPredicate<ObjectType, ObjectType> comparator) {
			final BiPredicate<ObjectType, ObjectType> finalComparator = comparator != null ? comparator : Object::equals;
			return (comparand1, comparand2) -> {
				if (comparand1 == null && comparand2 == null) {
					return true;
				} else if (comparand1 == null || comparand2 == null) {
					return false;
				} else {
					return finalComparator.test(comparand1, comparand2);
				}
			};
		}

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// constants

	private static final char DOT = '.';
	private static final char PLUS = '+';
	private static final char MINUS = '-';
	private static final char LEFT_PARENTHESIS = '(';
	private static final char RIGHT_PARENTHESIS = ')';
	private static final String BLANK = EmbeddedStringUtils.BLANK;
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal ONE = BigDecimal.ONE;
	private static final BigDecimal DEFAULT_VALUE = ZERO;
	private static final int DEFAULT_SCALE = 0;
	private static final int DEFAULT_DECIMAL_SCALE = 2;
	private static final int SCIENTIFIC_DECIMAL_SCALE = 10;
	private static final RoundingMode DEFAULT_ROUNDING_MODE = HALF_UP;

	private static final BigDecimal DEPERCENT_MULTIPLICATOR = new BigDecimal("0.01");
	private static final BigDecimal PERCENT_MULTIPLICATOR = new BigDecimal("100");
	private static final BigDecimal PERCENT_ADDEND = ONE;

	private static final DecimalFormat FORMAT_COMMA_0 = new DecimalFormat("##,##0");
	private static final DecimalFormat FORMAT_COMMA_2 = new DecimalFormat("##,##0.00");

	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_PLAIN = Collections.unmodifiableList(Arrays.asList(1));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL = Collections.unmodifiableList(Arrays.asList(1, 0));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_NULL = Collections.unmodifiableList(Arrays.asList(1, 2, -2, 22));
	private static final List<Integer> SEQUENCE_INVALID_COMPARE_RESULT_NOT_EQUAL_NULL = Collections.unmodifiableList(Arrays.asList(1, 0, 2, -2, 22));

	private static final Map<RoundingMode, String> ROUNDING_MODE_NAME;
	private static final Map<Character, Integer> PRECEDENCE_MAP;
	private static final Map<Character, BiFunction<BigDecimal, BigDecimal, BigDecimal>> OPERATOR_EVALUATION_MAP;
	private static final Map<Class<?>, Function<DecimalWrapper, Object>> WRAPPER_VALUE_GETTER_MAP;

	static {
		Map<RoundingMode, String> roundingModeNameMap = new HashMap<>();
		Map<Character, Integer> precedenceMap = new HashMap<>();
		Map<Character, BiFunction<BigDecimal, BigDecimal, BigDecimal>> operatorEvaluationMap = new HashMap<>();
		Map<Class<?>, Function<DecimalWrapper, Object>> wrapperValueGetterMap = new HashMap<>();

		roundingModeNameMap.put(UP, "up");
		roundingModeNameMap.put(DOWN, "down");
		roundingModeNameMap.put(CEILING, "ceiling");
		roundingModeNameMap.put(FLOOR, "floor");
		roundingModeNameMap.put(HALF_UP, "half_up");
		roundingModeNameMap.put(HALF_DOWN, "half_down");
		roundingModeNameMap.put(HALF_EVEN, "half_even");
		roundingModeNameMap.put(UNNECESSARY, "unnecessary");

		precedenceMap.put('+', 1);
		precedenceMap.put('-', 1);
		precedenceMap.put('*', 2);
		precedenceMap.put('/', 2);
		precedenceMap.put('^', 3);

		operatorEvaluationMap.put('+', (b, a) -> a.add(b));
		operatorEvaluationMap.put('-', (b, a) -> a.subtract(b));
		operatorEvaluationMap.put('*', (b, a) -> a.multiply(b));
		operatorEvaluationMap.put('/', (b, a) -> a.divide(b, DEFAULT_DECIMAL_SCALE, DEFAULT_ROUNDING_MODE));
		operatorEvaluationMap.put('^', (b, a) -> a.pow(b.intValue()));

		wrapperValueGetterMap.put(DecimalWrapper.class, decimalWrapper -> decimalWrapper);
		wrapperValueGetterMap.put(BigDecimal.class, DecimalWrapper::value);
		wrapperValueGetterMap.put(String.class, DecimalWrapper::dress2DP);
		wrapperValueGetterMap.put(Integer.class, DecimalWrapper::integerValue);
		wrapperValueGetterMap.put(Long.class, DecimalWrapper::longValue);
		wrapperValueGetterMap.put(Double.class, DecimalWrapper::doubleValue);

		ROUNDING_MODE_NAME = Collections.unmodifiableMap(roundingModeNameMap);
		PRECEDENCE_MAP = Collections.unmodifiableMap(precedenceMap);
		OPERATOR_EVALUATION_MAP = Collections.unmodifiableMap(operatorEvaluationMap);
		WRAPPER_VALUE_GETTER_MAP = Collections.unmodifiableMap(wrapperValueGetterMap);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// embedded utils

	private static class EmbeddedStringUtils {
		private static final String BLANK = "";
		private static final String SPACE_CHARS = "\\s\\u3000";

		private static String trimSpace(String source) {
			if (source == null || source.isEmpty()) {
				return source;
			}
			return source.replaceAll("^[" + SPACE_CHARS + "]+|[" + SPACE_CHARS + "]+$", BLANK);
		}

		static boolean isEmpty(String source) {
			return source == null || trimSpace(source).isEmpty();
		}
	}

	@SuppressWarnings("unchecked")
	private static class EmbeddedReflectiveUtils {
		static <ReturnType> ReturnType getField(Object object, String fieldName, Class<ReturnType> returnClass) {
			try {
				return (ReturnType) fetchPropertyDescriptor(object.getClass(), fieldName).getReadMethod().invoke(object);
			} catch (IntrospectionException | NullPointerException | IllegalAccessException | InvocationTargetException caught) {
				Field field = fetchField(object.getClass(), fieldName);
				return getField(object, field, returnClass);
			}
		}
		private static <ReturnType> ReturnType getField(Object object, Field field, Class<ReturnType> returnClass) {
			if (object == null) {
				return null;
			}
			if (!returnClass.isAssignableFrom(field.getType()) && !returnClass.equals(Object.class)) {
				throw new IllegalArgumentException("Incorrect return type: " + returnClass.getName());
			}
			boolean isAccessible = field.isAccessible();
			field.setAccessible(true);
			try {
				return (ReturnType) field.get(object);
			} catch (IllegalArgumentException | IllegalAccessException exception) {
				throw new RuntimeException(exception);
			} finally {
				field.setAccessible(isAccessible);
			}
		}

		static void setField(Object object, String fieldName, Object value) {
			try {
				fetchPropertyDescriptor(object.getClass(), fieldName).getWriteMethod().invoke(object, value);
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("Incorrect value type: " + value.getClass().getName());
			} catch (IntrospectionException | NullPointerException | IllegalAccessException | InvocationTargetException caught) {
				Field field = fetchField(object.getClass(), fieldName);
				setField(object, field, value);
			}
		}
		private static void setField(Object object, Field field, Object value) {
			if (object == null) {
				return;
			}
			if (value != null && (!field.getType().isAssignableFrom(value.getClass()) && !field.getType().equals(Object.class))) {
				throw new IllegalArgumentException("Incorrect value type: " + value.getClass().getName());
			}
			boolean isAccessible = field.isAccessible();
			field.setAccessible(true);
			try {
				field.set(object, value);
			} catch (IllegalArgumentException | IllegalAccessException exception) {
				throw new RuntimeException(exception);
			} finally {
				field.setAccessible(isAccessible);
			}
		}

		private static PropertyDescriptor fetchPropertyDescriptor(Class<?> objectClass, String fieldName) throws IntrospectionException {
			return Arrays.stream(Introspector.getBeanInfo(objectClass).getPropertyDescriptors())
					.filter(property -> property.getName().equals(fieldName)).findAny().orElse(null);
		}

		private static Field fetchField(Class<?> objectClass, String fieldName) {
			Field field = null;
			Class<?> superClass = objectClass;
			while (field == null && superClass != null && !TOP_SUPER_CLASSES.contains(superClass)) {
				try {
					field = superClass.getDeclaredField(fieldName);
				} catch (NoSuchFieldException | SecurityException ignored) {
				} finally {
					superClass = superClass.getSuperclass();
				}
			}
			if (field == null) {
				throw new RuntimeException(new NoSuchFieldException(fieldName));
			}
			return field;
		}

		static <ObjectType> ObjectType createInstance(Class<ObjectType> objectClass) {
			try {
				Constructor<ObjectType> constructor = objectClass.getDeclaredConstructor();
				boolean isAccessible = constructor.isAccessible();
				constructor.setAccessible(true);
				try {
					return constructor.newInstance();
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
					throw new RuntimeException(exception);
				} finally {
					constructor.setAccessible(isAccessible);
				}
			} catch (NoSuchMethodException exception) {
				throw new RuntimeException(exception);
			}
		}

		static boolean matchType(Object object, Class<?> targetClass) {
			if (targetClass.equals(byte.class) || targetClass.equals(Byte.class)) {
				return object instanceof Byte;
			} else if (targetClass.equals(short.class) || targetClass.equals(Short.class)) {
				return object instanceof Short;
			} else if (targetClass.equals(int.class) || targetClass.equals(Integer.class)) {
				return object instanceof Integer;
			} else if (targetClass.equals(long.class) || targetClass.equals(Long.class)) {
				return object instanceof Long;
			} else if (targetClass.equals(float.class) || targetClass.equals(Float.class)) {
				return object instanceof Float;
			} else if (targetClass.equals(double.class) || targetClass.equals(Double.class)) {
				return object instanceof Double;
			} else if (targetClass.equals(boolean.class) || targetClass.equals(Boolean.class)) {
				return object instanceof Boolean;
			} else if (targetClass.equals(char.class) || targetClass.equals(Character.class)) {
				return object instanceof Character;
			} else {
				return targetClass.isInstance(object);
			}
		}

		private static final List<Class<?>> TOP_SUPER_CLASSES = Collections.singletonList(Object.class);
	}

}
