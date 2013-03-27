package fengfei.forest.slice.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fengfei.forest.slice.Plotter;
import fengfei.forest.slice.SliceResource;

public class LoopPlotter implements Plotter {

	final AtomicInteger count = new AtomicInteger();
	private int currentIndex;

	@Override
	public int to(long seed, List<SliceResource> availableResources, List<SliceResource> failResources) {
		currentIndex = count.getAndIncrement();
		int index = Math.abs(currentIndex % availableResources.size());
		return index;
	}

	public int getCurrentIndex() {
		return currentIndex;
	}
}