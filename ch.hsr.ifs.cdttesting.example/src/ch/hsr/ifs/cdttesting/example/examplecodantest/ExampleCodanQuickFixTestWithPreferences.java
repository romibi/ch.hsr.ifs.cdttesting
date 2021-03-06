package ch.hsr.ifs.cdttesting.example.examplecodantest;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Test;

import ch.hsr.ifs.cdttesting.TestingPlugin;
import ch.hsr.ifs.cdttesting.cdttest.CDTTestingCodanQuickfixTestWithPreferences;

public class ExampleCodanQuickFixTestWithPreferences extends CDTTestingCodanQuickfixTestWithPreferences {

	@Override
	protected String getProblemId() {
		return MyCodanChecker.MY_PROBLEM_ID;
	}

	@Override
	@Test
	public void runTest() throws Throwable {
		runQuickFix(new MyQuickFix());
		assertEquals(getExpectedSource(), getCurrentSource());
	}

	@Override
	public IPreferenceStore initPrefs() {
		return TestingPlugin.getDefault().getPreferenceStore();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Class getPreferenceConstants() {
		return PrefConstants.class;
	}

	// Can be anywhere in the project.
	class PrefConstants {
		public static final String P_PREF_FOO = TestingPlugin.PLUGIN_ID + ".preference.foo";
		public static final String P_PREF_BAR = TestingPlugin.PLUGIN_ID + ".preference.bar";
		public static final String P_PREF_BAZ = TestingPlugin.PLUGIN_ID + ".preference.baz";
	};
}
