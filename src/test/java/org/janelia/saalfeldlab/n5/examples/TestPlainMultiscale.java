package org.janelia.saalfeldlab.n5.examples;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.metadata.MultiscaleHierarchyWriter;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

public class TestPlainMultiscale {

	public static void main(String[] args) {
		
		final N5Writer n5w = new N5Factory().openWriter("/home/john/tests/n5-api-saving/tmp.n5");
		final int[] downsamplingFactors = new int[]{2,2,2};
		final DatasetAttributes baseAttributes = new DatasetAttributes(
				new long[] {32, 16, 8 },
				new int[] {16, 16, 16 },
				DataType.UINT8,
				new RawCompression());

		final MultiscaleHierarchyWriter writer = MultiscaleHierarchyWriter.build(baseAttributes, 2, s -> downsamplingFactors);
		writer.write(n5w, "c0");
	}

}
