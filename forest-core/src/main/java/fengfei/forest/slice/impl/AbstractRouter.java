package fengfei.forest.slice.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import fengfei.forest.slice.Equalizer;
import fengfei.forest.slice.OverflowType;
import fengfei.forest.slice.SliceResource;
import fengfei.forest.slice.SliceResource.Function;
import fengfei.forest.slice.Router;
import fengfei.forest.slice.SelectType;
import fengfei.forest.slice.Slice;
import fengfei.forest.slice.exception.NonExistedSliceException;
import fengfei.forest.slice.exception.UnSupportedException;

public abstract class AbstractRouter<Key> implements Router<Key> {

	protected Equalizer<Key> equalizer;
	protected OverflowType overflowType = OverflowType.Last;
	protected SelectType selectType;
	protected Map<String, String> defaultExtraInfo = new HashMap<>();

	@SuppressWarnings("unchecked")
	public AbstractRouter() {
		equalizer = (Equalizer<Key>) new LongEqualizer();
	}

	public AbstractRouter(Equalizer<Key> equalizer) {
		this.equalizer = equalizer;
	}

	public AbstractRouter(Equalizer<Key> equalizer, SelectType selectType) {
		super();
		this.equalizer = equalizer;
		this.selectType = selectType;
	}

	protected SliceResource dealOverflow(Key key, Function function, long id,
			boolean isDealOver) {
		if (!isSupported(overflowType)) {
			throw new UnSupportedException("unSupported overflowType :"
					+ overflowType);
		}
		if (!isDealOver) {
			throw new NonExistedSliceException("id=" + id
					+ " non-existed slice.");
		}
		switch (overflowType) {
		case First:
			return function == null ? first() : first(function);
		case Last:
			return function == null ? last() : last(function);
		case New:
			return null;
		case Exception:
			throw new NonExistedSliceException("id=" + id
					+ " non-existed slice.");
		default:
			throw new NonExistedSliceException("id=" + id
					+ " non-existed slice.");
		}
	}

	/**
	 * get a special function slice of logicslice
	 * 
	 * @param logicSlice
	 * @param key
	 * @param function
	 * @param id
	 * @param isDealOver
	 * @return
	 */
	protected SliceResource getResource(Slice<Key> slice, Key key,
			Function function, long id, boolean isDealOver) {
		if (slice == null) {
			return dealOverflow(key, function, id, isDealOver);
		}
		SliceResource resource = slice.get(id, function);
		if (resource == null) {
			Router<Key> router = slice.getChildRouter();
			return router.locate(key);
		}
		return resource;
	}

	protected SliceResource getResource(Slice<Key> slice, Key key, long id,
			boolean isDealOver) {
		if (slice == null || slice instanceof NullSlice) {
			return dealOverflow(key, null, id, isDealOver);
		}
		SliceResource resource = slice.getAny(id);
		if (resource == null) {
			Router<Key> router = slice.getChildRouter();
			return router.locate(key);
		}
		return resource;
	}

	public void addResourceSet(Long sliceId, Slice<Key> slice) {
		getSlices().put(sliceId, slice);
	}

	protected AtomicLong ids = new AtomicLong();

	public void setEqualizer(Equalizer<Key> equalizer) {
		this.equalizer = equalizer;
	}

	public void setOverflowType(OverflowType overflowType) {
		if (!isSupported(overflowType)) {
			throw new UnSupportedException("unSupported overflowType :"
					+ overflowType);
		}
		this.overflowType = overflowType;
	}

	public OverflowType getOverflowType() {
		return overflowType;
	}

	public void setSelectType(SelectType selectType) {
		this.selectType = selectType;
	}

	public Map<String, String> getDefaultExtraInfo() {
		return defaultExtraInfo;
	}

	public void setDefaultExtraInfo(Map<String, String> defaultExtraInfo) {
		this.defaultExtraInfo = defaultExtraInfo;
	}

	@Override
	public void register(Slice<Key> slice) {
		addslice(slice);
	}

	protected Slice<Key> updateNullSlice(Slice<Key> slice) {
		if (slice != null && slice instanceof NullSlice) {
			Long sliceId = slice.getSliceId();
			getSlices().remove(sliceId);
			slice = null;
			slice = createSlice(sliceId);
		}
		return slice;
	}

	@Override
	public void register(Long sliceId, SliceResource resource) {
		Slice<Key> slice = getSlices().get(sliceId);
		if (slice == null) {
			slice = createSlice(sliceId);
		}
		update(slice, resource);
	}

	protected Slice<Key> createSlice(Long sliceId) {
		Slice<Key> slice = new ReadWriteSlice<>();
		slice.setSliceId(sliceId);
		return slice;
	}

	private void update(Slice<Key> slice, SliceResource resource) {
		slice = updateNullSlice(slice);
		Map<String, String> extraInfo = new HashMap<>(getDefaultExtraInfo());
		extraInfo.putAll(slice.getParams());
		extraInfo.putAll(resource.getExtraInfo());
		resource.addParams(extraInfo);
		slice.add(resource);
		// getSlices().put(slice.getSliceId(), slice);
		addslice(slice);
	}

	public abstract void addslice(Slice<Key> slice);

	@Override
	public void update(Long sliceId, SliceResource resource) {
		Slice<Key> slice = getSlices().get(sliceId);
		if (slice == null) {
			throw new NonExistedSliceException(
					"Non Existed slice for slice id:" + sliceId);
		}
		update(slice, resource);
	}

	@Override
	public void remove(SliceResource resource) {
		if (resource == null || resource.getSliceId() == null) {
			throw new IllegalArgumentException("arg resource is imperfect.");
		} else if (getSlices().containsKey(resource.getSliceId())) {
			Slice<Key> slice = getSlices().get(resource.getSliceId());
			slice.remove(resource);
		} else {
			throw new NonExistedSliceException(
					"Can't remove,non existed slice for slice id:"
							+ resource.getSliceId());
		}
	}

	@Override
	public void remove(Long sliceId) {
		if (sliceId == null) {
			throw new IllegalArgumentException("arg resource is imperfect.");
		} else if (getSlices().containsKey(sliceId)) {
			getSlices().remove(sliceId);
		} else {
			throw new NonExistedSliceException(
					"Can't remove, non existed slice for slice id:" + sliceId);
		}
	}

	public abstract boolean isSupported(OverflowType overflowType);
}