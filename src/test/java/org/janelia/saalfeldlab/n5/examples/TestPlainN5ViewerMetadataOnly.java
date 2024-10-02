package org.janelia.saalfeldlab.n5.examples;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.config.N5ViewerBuilder;
import org.janelia.saalfeldlab.n5.metadata.DatasetUtils;
import org.janelia.saalfeldlab.n5.metadata.MetadataHierarchyWriter;
import org.janelia.saalfeldlab.n5.metadata.MultiscaleHierarchyWriter;
import org.janelia.saalfeldlab.n5.metadata.N5ViewerMultiscaleHierarchyWriter;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5Factory.StorageFormat;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.AxisUtils;
import org.janelia.steffi.SaveN5Api;
import org.janelia.steffi.compute.RandomAccessibleIntervalComputeBlock;
import org.janelia.steffi.compute.RandomIntensityS0Block;
import org.janelia.steffi.metadata.N5ViewerScheme;

import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

public class TestPlainN5ViewerMetadataOnly {

	public static void main(String[] args) {

		testN5VFromRaiMultichannel();
	}

	public static void testN5VFromRaiMultichannel() {

		final String baseDirectory = "n5v";

//		final ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs.unsignedBytes(32, 32, 16);
//		final int[] blockSize = new int[]{128, 128, 64};
//
//		final double[] baseResolution = new double[]{1.1, 2.2, 3.3};
//		final int[][] downsamplings = new int[][]{{1, 1, 1}, {2, 2, 1}, {4, 4, 2}};
//		final String unit = "um";
//		
//		final Map< Integer, int[] > blockSizePerLevel = new HashMap<>();
//		for ( int s = 0; s < downsamplings.length; ++s )
//			blockSizePerLevel.put( s, blockSize );

//		N5ViewerMultiscaleHierarchyWriter n5vWriter = N5ViewerMultiscaleHierarchyWriter.build(
//				DatasetUtils.datasetAttributes(img, () -> blockSize, () -> new RawCompression()),
//				downsamplings.length,
//				baseResolution,
//				unit,
//				i -> downsamplings[i]);

		N5ViewerMultiscaleHierarchyWriter n5vWriter = (N5ViewerMultiscaleHierarchyWriter)new N5ViewerBuilder()
			.image(ArrayImgs.unsignedBytes(32, 32, 16), AxisUtils.buildAxes("x", "y", "z"))
			.blockSize(128, 128, 64)
			.resolution(1.1, 2.2, 3.3)
			.unit("um")
			.downsampleFactors(new int[][]{{1, 1, 1}, {2, 2, 1}, {4, 4, 2}})
			.getWriter();

		final StorageFormat sf = StorageFormat.N5;
		try( final N5Writer n5 = new N5Factory().openWriter( sf,
				URI.create( "/home/john/tests/n5-api-saving/test-n5v-mc-metaOnly." + sf.toString().toLowerCase() ) )){

			n5.createGroup("");
			n5vWriter.write(n5, baseDirectory);
		}

	}

}
