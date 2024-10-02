package org.janelia.saalfeldlab.n5.metadata;

import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.scicomp.n5.zstandard.ZstandardCompression;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;

public class DatasetUtils {

	public static <T extends NativeType<T>> DatasetAttributes defaultDatasetAttributes(
			final RandomAccessibleInterval<T> img ) {

		return datasetAttributes(img,
				() -> IntStream.generate(() -> 64).limit(img.numDimensions()).toArray(),
				ZstandardCompression::new);
	}

	public static <T extends NativeType<T>> DatasetAttributes datasetAttributes(
			final RandomAccessibleInterval<T> img,
			final Supplier<int[]> blockSize,
			final Supplier<Compression> compression) {

		return new DatasetAttributes(
				img.dimensionsAsLongArray(),
				blockSize.get(),
				N5Utils.dataType(img.getType()),
				compression.get());
	}

	public static IntFunction<int[]> toFunction(int[][] arr) {

		return i -> arr[i];
	}

}
