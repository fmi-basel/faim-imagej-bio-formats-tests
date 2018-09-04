
package ch.fmi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ij.ImagePlus;
import java.io.IOException;

import loci.common.DebugTools;
import loci.formats.FormatException;
import loci.plugins.BF;
import net.imagej.ImageJ;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class VisiViewFormatTest {

	@Parameters(name = "{0}")
	public static String[] data() {
		return new String[] { //
			"benchmark_v1_2018_x64y64z1c2s1t11", //
			"benchmark_v1_2018_x64y64z1c2s4t11", //
			"benchmark_v1_2018_x64y64z1c2s4t12", //
			"benchmark_v1_2018_x64y64z5c2s1t11", //
			"benchmark_v1_2018_x64y64z5c2s1t41", //
			"benchmark_v1_2018_x64y64z5c2s4t41" };
	}

	@Parameter
	public String dataset;

	private static ImageJ ij;
	private static boolean isWindows;

	private ImagePlus actual = null;
	private ImagePlus expected = null;

	@BeforeClass
	public static void initialize() {
		ij = new ImageJ();
		String platform = ij.platform().getTargetPlatforms().get(0).osName();
		isWindows = platform != null && platform.startsWith("Windows");
		// NB: the DEBUG output of bioformats is too verbose, so the log gets truncated on CI
		DebugTools.setRootLevel("INFO");
	}

	@AfterClass
	public static void cleanUp() {
		ij.context().dispose();
	}

	@Before
	public void loadImages() {
		String mainFolderLocation = dataset + ".nd";
		String subFolderLocation = dataset + "/" + dataset + ".nd";
		String mainFolderpath = getClass().getResource(mainFolderLocation)
			.getPath();
		String subFolderpath = getClass().getResource(subFolderLocation).getPath();

		// Sanitize path for Windows
		// (without this, Bio-Formats finds the nd file but can't find the stk file)
		if (isWindows) {
			mainFolderpath = mainFolderpath.replaceAll("/", "\\\\");
			mainFolderpath = mainFolderpath.substring(1);
			subFolderpath = subFolderpath.replaceAll("/", "\\\\");
			subFolderpath = subFolderpath.substring(1);
		}

		try {
			ImagePlus[] imps = BF.openImagePlus(mainFolderpath);
			actual = imps[0];
			imps = BF.openImagePlus(subFolderpath);
			expected = imps[0];
		}
		catch (FormatException | IOException exc) {
			exc.printStackTrace(System.err);
		}
	}

	@Test
	public void testOpenSingleDataset() {
		assertNotNull("Read image (single dataset)", expected);
	}

	@Test
	public void testOpenWithOthersInFolder() {
		assertNotNull("Read image (from folder with other datasets)", actual);
	}

	@Test
	public void testEqualDimensions() {
		if (expected != null && actual != null) {
			assertArrayEquals("Image dimensionality", expected.getDimensions(), actual
				.getDimensions());
		}
	}

	@Test
	public void testEqualTitles() {
		if (expected != null && actual != null) {
			assertEquals("Image titles", expected.getTitle(), actual.getTitle());
		}
	}

	@Test
	public void testTitle() {
		if (expected != null) {
			assertTrue("Title starts with dataset name", expected.getTitle()
				.startsWith(dataset));
		}
	}

	@Test
	public void testChannelNotInTitle() {
		if (expected != null) {
			assertFalse("Channel identifier in title of multi-channel image", expected
				.getTitle().contains("_w"));
		}
	}
}
