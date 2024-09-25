package org.janelia.steffi;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.steffi.compute.ComputeS0Block;
import org.janelia.steffi.metadata.SchemeCreator;
import org.janelia.steffi.multires.MultiResTools;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.view.Views;

public class SaveN5Api
{
	final SchemeCreator creator;
	final Supplier<ComputeS0Block> s0compute;

	public SaveN5Api(
			final SchemeCreator creator,
			final Supplier<ComputeS0Block> s0compute )
	{
		this.creator = creator;
		this.s0compute = s0compute;
	}

	public boolean executeMultiThreaded( final int numThreads )
	{
		// create all datasets and metadata
		creator.setup();

		// get all jobs for s0
		final List<long[][]> gridS0 = creator.assembleJobs( 0 );

		//
		// Save full resolution dataset (s0)
		//
		final ForkJoinPool myPool = new ForkJoinPool( numThreads );

		long time = System.currentTimeMillis();
		final AtomicInteger progress = new AtomicInteger( 0 );

		try
		{
			myPool.submit(() -> gridS0.parallelStream().forEach(
					gridBlock ->
					{
						saveS0Block( gridBlock, s0compute, creator );

						System.out.println( "s0: " + progress.incrementAndGet() + "/" + gridS0.size() + " done." );
					})).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			System.out.println( "Failed to write s0 for " + creator + ". Error: " + e );
			e.printStackTrace();
			return false;
		}

		System.out.println( "Saved level s0, took: " + (System.currentTimeMillis() - time ) + " ms." );

		//
		// Save remaining downsampling levels (s1 ... sN)
		//
		for ( int level = 1; level < creator.numDownsamplingLevels(); ++level )
		{
			final int s = level;

			final List<long[][]> grid = creator.assembleJobs( level );

			System.out.println( "Downsampling level s" + s + "... " );
			System.out.println( "Number of compute blocks: " + grid.size() );

			time = System.currentTimeMillis();
			progress.set( 0 );

			try
			{
				myPool.submit(() -> grid.parallelStream().forEach(
						gridBlock ->
						{
							// make this function customizable -JB 
							MultiResTools.writeDownsampledBlock(creator, s, s-1, gridBlock );

							System.out.println( "s" + s + ": " + progress.incrementAndGet() + "/" + grid.size() + " done." );
						} ) ).get();
			}
			catch (InterruptedException | ExecutionException e)
			{
				System.out.println( "Failed to write downsample step s" + s +" for " + creator + "'. Error: " + e );
				e.printStackTrace();
				return false;
			}

			System.out.println( "Resaved  s" + s + " level, took: " + (System.currentTimeMillis() - time ) + " ms." );
		}

		myPool.shutdown();

		// finalize something if necessary
		creator.finish();

		return true;
	}

	private final static <T extends NativeType<T>> void saveS0Block(
			final long[][] gridBlock,
			final Supplier<ComputeS0Block> s0compute,
			final SchemeCreator creator )
	{
		final RandomAccessibleInterval<T> img = s0compute.get().compute( gridBlock, creator );

		final String dataset = creator.dataset( gridBlock, 0 );
		final N5Writer n5 = creator.getN5WriterSupplier().get();

		N5Utils.saveNonEmptyBlock( img, n5, dataset, gridBlock[ 2 ], Views.iterable( img ).firstElement().createVariable() );
	}
}

