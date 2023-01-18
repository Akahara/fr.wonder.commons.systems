package fr.wonder.commons.systems.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.wonder.commons.loggers.Logger;
import fr.wonder.commons.loggers.SimpleLogger;

public class ResourceTracker {
	
	private final Set<AliveResource> aliveResources = new HashSet<>();
	private final Map<Class<?>, Integer> allocationCounts = new HashMap<>();
	
	private final Logger logger = new SimpleLogger("resources");
	
	public void track(Object resource) {
		AliveResource res = new AliveResource();
		res.resource = resource;
		res.allocationTrace = Thread.currentThread().getStackTrace();
		res.allocationTrace = Arrays.copyOfRange(res.allocationTrace, 2, res.allocationTrace.length); // remove this function call trace
		if(!aliveResources.add(res)) {
			logger.merr("Resource was added twice! " + resource);
			return;
		}
		allocationCounts.compute(resource.getClass(), (c,a) -> a==null ? 1 : a+1);
	}
	
	public void untrack(Object resource) {
		AliveResource r = new AliveResource();
		r.resource = resource;
		if(!aliveResources.remove(r))
			logger.merr("Unknown resource was removed! " + resource);
	}
	
	public void printAlive(boolean verbose) {
		Map<Class<?>, List<AliveResource>> sorted = new HashMap<>();
		for(AliveResource res : aliveResources)
			sorted.computeIfAbsent(res.resource.getClass(), c->new ArrayList<>()).add(res);
		for(Entry<Class<?>, List<AliveResource>> entry : sorted.entrySet()) {
			Class<?> key = entry.getKey();
			List<AliveResource> stillAlive = entry.getValue();
			logger.info(String.format("Out of %d instances of %s, %d are still alive", allocationCounts.get(key), key.getSimpleName(), stillAlive.size()));
			if(verbose) {
				for(AliveResource alive : stillAlive) {
					logger.info(String.format("  %s allocated at", alive.resource));
					for(int i = 0; i < alive.allocationTrace.length && i < 3; i++)
						logger.info("    " + alive.allocationTrace[i]);
				}
			} else {
				for(AliveResource alive : stillAlive) {
					logger.info(String.format("  %s allocated at %s", alive.resource, alive.allocationTrace[0]));
				}
			}
		}
		for(Class<?> allocatedOnce : allocationCounts.keySet()) {
			if(!sorted.containsKey(allocatedOnce))
				logger.info(String.format("All %d instances of %s were freed", allocationCounts.get(allocatedOnce), allocatedOnce.getSimpleName()));
		}
	}

	public void printAlive() {
		printAlive(false);
	}

}

class AliveResource {
	
	Object resource;
	StackTraceElement[] allocationTrace;
	
	@Override
	public int hashCode() {
		return resource.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof AliveResource ?
				resource.equals(((AliveResource)obj).resource) :
				resource.equals(obj);
	}
	
}
