package com.j256.ormlite.android.compat;

import android.os.Build;

/**
 * Utility class which loads the various classes based on which API version is being supported.
 * 
 * @author graywatson
 */
@SuppressWarnings("unused")
public class ApiCompatibilityUtils {

	private static ApiCompatibility compatibility;

	/**
	 * Copied from {@link Build.VERSION_CODES}. We don't use those codes because they won't be in certain versions of
	 * Build.
	 */
	private static final int BASE = 1;
	private static final int BASE_1_1 = 2;
	private static final int CUPCAKE = 3;
	private static final int DONUT = 4;
	private static final int ECLAIR = 5;
	private static final int ECLAIR_0_1 = 6;
	private static final int ECLAIR_MR1 = 7;
	private static final int FROYO = 8;
	private static final int GINGERBREAD = 9;
	private static final int GINGERBREAD_MR1 = 10;
	private static final int HONEYCOMB = 11;
	private static final int HONEYCOMB_MR1 = 12;
	private static final int HONEYCOMB_MR2 = 13;
	private static final int ICE_CREAM_SANDWICH = 14;
	private static final int ICE_CREAM_SANDWICH_MR1 = 15;
	private static final int JELLY_BEAN = 16;
	private static final int JELLY_BEAN_MR1 = 17;
	private static final int JELLY_BEAN_MR2 = 18;

	static {
		if (Build.VERSION.SDK_INT >= JELLY_BEAN) {
			compatibility = new JellyBeanApiCompatibility();
		} else {
			compatibility = new BasicApiCompatibility();
		}
	}

	/**
	 * Return the compatibility class that matches our build number.
	 */
	public static ApiCompatibility getCompatibility() {
		return compatibility;
	}
}
