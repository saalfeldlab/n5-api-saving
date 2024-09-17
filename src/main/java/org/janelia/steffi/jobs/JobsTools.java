package org.janelia.steffi.jobs;

import java.util.ArrayList;
import java.util.List;

import org.janelia.steffi.metadata.SchemeCreator;
import org.janelia.steffi.multires.MultiResTools.MultiResolutionLevelInfo;

import mpicbg.spim.data.sequence.ViewId;

public class JobsTools
{
	public static ArrayList<long[][]> assembleJobs(
			final ViewId viewId, //can be null 
			final long[] dimensions,
			final int[] blockSize,
			final int[] computeBlockSize,
			final SchemeCreator schemeCreator )
	{
		// all blocks (a.k.a. grids)
		final ArrayList<long[][]> allBlocks = new ArrayList<>();

		final List<long[][]> grid = Grid.create(
				dimensions,
				computeBlockSize,
				blockSize);

		if ( viewId != null )
		{
			// add timepointId and ViewSetupId & dimensions to the gridblock
			for ( final long[][] gridBlock : grid )
				allBlocks.add( new long[][]{
					gridBlock[ 0 ].clone(),
					gridBlock[ 1 ].clone(),
					gridBlock[ 2 ].clone(),
					new long[] { viewId.getTimePointId(), viewId.getViewSetupId() }
				});
		}

		return allBlocks;
	}

}
