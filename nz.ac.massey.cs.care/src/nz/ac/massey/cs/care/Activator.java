package nz.ac.massey.cs.care;

import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "nz.ac.massey.cs.care"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	//preference page code
	
		//The entry delimiter
			private static String PREFERENCE_DELIMITER = ";";


			//The identifiers for the preferences	
			public static final String BLACKLISTED_PREFERENCE = "blacklisted";
			public static final String HIGHLIGHT_PREFERENCE = "highlight";

			//The default values for the preferences
			public static final String DEFAULT_BLACKLISTED = "java.lang.Object";
			public static final int DEFAULT_HIGHLIGHT = SWT.COLOR_BLUE;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Return the blacklisted preference default
	 * as an array of Strings.
	 * @return String[]
	 */
	public String[] getDefaultBlacklistedPreference() {
		return convert(
			getPreferenceStore().getDefaultString(BLACKLISTED_PREFERENCE));
	}

	/**
	 * Return the blacklisted classes preference as an array of
	 * Strings.
	 * @return String[]
	 */
	public String[] getBlacklistedPreference() {
		return convert(getPreferenceStore().getString(BLACKLISTED_PREFERENCE));
	}

	/**
	 * Convert the supplied PREFERENCE_DELIMITER delimited
	 * String to a String array.
	 * @return String[]
	 */
	private String[] convert(String preferenceValue) {
		StringTokenizer tokenizer =
			new StringTokenizer(preferenceValue, PREFERENCE_DELIMITER);
		int tokenCount = tokenizer.countTokens();
		String[] elements = new String[tokenCount];

		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}

		return elements;
	}

	/**
	 * Set the blacklisted classes preference
	 * @param String [] elements - the Strings to be 
	 * 	converted to the preference value
	 */
	public void setBlacklistedPreference(String[] elements) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(elements[i]);
			buffer.append(PREFERENCE_DELIMITER);
		}
		getPreferenceStore().setValue(BLACKLISTED_PREFERENCE, buffer.toString());
	}
	/** 
	 * Initializes a preference store with default preference values 
	 * for this plug-in.
	 * @param store the preference store to fill
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault(BLACKLISTED_PREFERENCE, DEFAULT_BLACKLISTED);
		Color color = Display.getDefault().getSystemColor(DEFAULT_HIGHLIGHT);
		PreferenceConverter.setDefault(
			store,
			HIGHLIGHT_PREFERENCE,
			color.getRGB());

	}
}
