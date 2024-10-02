package org.janelia.saalfeldlab.n5.config;

import java.util.Arrays;
import java.util.function.IntFunction;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.GzipCompression;

public class Parameters {
	
	// single (base) dataset
	public long[] dimensions;
	public int[] blockSize;
	public DataType dataType;
	public Compression compression = new GzipCompression();

	// spatial and time resolution 
	public double[] resolution;
	public double[] offset;
	public String[] units;

	// multichannel
	public int numChannels = 1;
	public IntFunction<String> relativeChannelGroupName = c -> String.format("c%d", c);

	// multiscale
	public int numScales;
	public IntFunction<String> relativeScaleLevelGroupName = s -> String.format("s%d", s);
	public IntFunction<int[]> blockSizePerScaleLevel = s -> blockSize;
	public IntFunction<int[]> downsamplingFactors = s -> {
		final int[] factors = new int[dimensions.length];
		Arrays.fill(factors, (int)Math.pow(2, s));
		return factors;
	};

	public String toString() {

		return String.format(
				"dimensions : %s\n" +
				"dataType : %s\n" +
				"blockSize : %s\n" +
				"resolution : %s\n" +
				"offset : %s\n",
				Arrays.toString(dimensions),
				dataType,
				Arrays.toString(blockSize),
				Arrays.toString(resolution),
				Arrays.toString(offset));
	}
}
