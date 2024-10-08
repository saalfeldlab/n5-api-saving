package org.janelia.steffi.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.N5Writer;

public abstract class SchemeCreator implements Serializable
{
	private static final long serialVersionUID = -1412462576026211843L;

	final Map< Integer, int[] > blockSizePerLevel; // downsampling level to computeBlockSize)

	public SchemeCreator( final Map< Integer, int[] > blockSizePerLevel )
	{
		this.blockSizePerLevel = blockSizePerLevel;
	}

	/**
	 * sets up the datasets and metadata
	 */
	public abstract boolean setup();

	public abstract Supplier< N5Writer > getN5WriterSupplier();

	public abstract DataType dataType( long[][] gridBlock, int level );

	public abstract String dataset( long[][] gridBlock, int level );

	public abstract int[] blockSize( long[][] gridBlock, int level );

	public abstract int[] absoluteDownsampling( long[][] gridBlock, int level );

	public abstract int[] relativeDownsampling( long[][] gridBlock, int level );

	public abstract List<long[][]> assembleJobs( final int level );

	public abstract int numDownsamplingLevels();

	/**
	 * Called after s0 and downsampling is written
	 * @return - if successful
	 */
	public abstract boolean finish();
}
